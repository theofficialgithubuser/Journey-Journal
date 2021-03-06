package com.example.journeyjournal;

import androidx.annotation.Nullable;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewJournalActivity extends BaseActivity {

    ImageView image;
    TextInputLayout title, description, location;
    Button select_location;
    String lat, lon, place_name;
    Uri uri;
    Boolean imageSelected = false;
    private static final int REQUEST_CODE_MAP = 101;

    private static final int ERROR_DIALOG_REQUEST = 9001;

    SharedPreferences sharedPreferences;

    final String journal_url = "http://journeyjournal.pythonanywhere.com/journal/add";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_journal);

        image = findViewById(R.id.image);
        title = findViewById(R.id.title);
        description = findViewById(R.id.description);
        location = findViewById(R.id.location);

        location.setEnabled(false);

        if (isServiceOk()) {
            init();
        }
    }

    public void selectImageClick(View view) {
        ImagePicker.with(NewJournalActivity.this)
                .crop()
                .compress(1024)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MAP) {
            if (resultCode == Activity.RESULT_OK) {
                lat = (String) data.getStringExtra("lat");
                lon = (String) data.getStringExtra("lon");
                place_name = data.getStringExtra("place_name");
                location.getEditText().setText(place_name);
            }
        } else if (resultCode == Activity.RESULT_OK) {
            uri = data.getData();
            imageSelected = true;
            image.setImageURI(uri);

        }
    }

    public void JournalSubmitClick(View view) {
        String title_ = title.getEditText().getText().toString();
        String description_ = description.getEditText().getText().toString();
        String place_name_ = location.getEditText().getText().toString();
        String photo_ = image.toString();

        if (!title_.isEmpty()) {
            title.setError(null);
            title.setErrorEnabled(false);
            if (!description_.isEmpty()) {
                description.setError(null);
                description.setErrorEnabled(false);
                if (!place_name_.isEmpty()) {
                    location.setError(null);
                    location.setErrorEnabled(false);
                    if (image.getDrawable() != null && imageSelected) {
                        // Add new journal logic here
                        new AddJournal().execute(title_, description_, photo_, lat, lon, place_name_);
                    } else {
                        Toast.makeText(this, "Please select image", Toast.LENGTH_LONG).show();
                    }
                } else {
                    location.setError("Please select location");
                }
            } else {
                description.setError("Please enter description");
            }
        } else {
            title.setError("Please enter title");
        }
    }

    public class AddJournal extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String title = strings[0];
            String description = strings[1];
            String photo = strings[2];
            String lat = strings[3];
            String lon = strings[4];
            String name_place = strings[5];
            sharedPreferences = getSharedPreferences("JourneyJournal", Context.MODE_PRIVATE);
            String access_token = sharedPreferences.getString("access_token", "");

            File file = new File(uri.getPath());
            final MediaType MEDIA_TYPE = MediaType.parse("image/*");


            OkHttpClient okHttpClient = new OkHttpClient();
            MultipartBody formBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", title)
                    .addFormDataPart("description", description)
                    .addFormDataPart("photo", file.getName(), RequestBody.create(MEDIA_TYPE, file))
                    .addFormDataPart("lat", lat)
                    .addFormDataPart("long", lon)
                    .addFormDataPart("place_name", name_place)
                    .build();

            Request request = new Request.Builder()
                    .url(journal_url)
                    .addHeader("Authorization", "Bearer " + access_token)
                    .post(formBody)
                    .build();
            Response response = null;

            try {
                response = okHttpClient.newCall(request).execute();
                Log.i("ERRORRRR", String.valueOf(response));
                if (response.code() == 201) {
                    // parse response to json
                    String result = response.body().string();
                    JSONObject resultJson = new JSONObject(result);
                    NewJournalActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NewJournalActivity.this, "Journal saved", Toast.LENGTH_LONG).show();
                        }
                    });
                    Intent intent = new Intent(NewJournalActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    NewJournalActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NewJournalActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                // Display error message maybe Internal server error or Something went wrong
                NewJournalActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(NewJournalActivity.this, "Oops! Something went wrong", Toast.LENGTH_LONG).show();
                    }
                });
                e.printStackTrace();
            }
            return null;
        }
    }

    private void init() {
        select_location = findViewById(R.id.select_location);

        select_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NewJournalActivity.this, MapActivity.class);
                startActivityForResult(intent, REQUEST_CODE_MAP);
            }
        });
    }

    public boolean isServiceOk() {

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(NewJournalActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            // everything is fine and user can make map requests
            return true;

        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // an error occurred but we can resolve it
            Log.d("isServiceOk", "");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(NewJournalActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();

        } else {
            Toast.makeText(this, "You cannot make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}