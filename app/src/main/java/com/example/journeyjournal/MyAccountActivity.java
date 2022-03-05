package com.example.journeyjournal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyAccountActivity extends BaseActivity {
    TextView fullname;
    TextView join_date;
    TextView email;
    SharedPreferences sharedPreferences;

    final String current_user_url = "http://journeyjournal.pythonanywhere.com/user/current-user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        fullname = findViewById(R.id.fullname);
        join_date = findViewById(R.id.join_date);
        email = findViewById(R.id.email);

        sharedPreferences = getSharedPreferences("JourneyJournal", Context.MODE_PRIVATE);

        new GetCurrentUser().execute();
    }


    public class GetCurrentUser extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String access_token = sharedPreferences.getString("access_token", "");
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(current_user_url)
                    .addHeader("Authorization", "Bearer " + access_token)
                    .build();
            Response response = null;

            try {
                response = okHttpClient.newCall(request).execute();
                if (response.code() == 200) {
                    String result = response.body().string();
                    JSONObject resultJson = new JSONObject(result);
                    String fullname_ = resultJson.getString("fullname");
                    String join_date_ = resultJson.getString("date_joined");
                    String email_ = resultJson.getString("email");
                    MyAccountActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("CURRENT_USER", fullname_);
                            fullname.setText(fullname_);
                            join_date.setText(join_date_);
                            email.setText(email_);
                        }
                    });
                } else if (response.code() == 403) {
                    // token invalid/expired or malformed token
                    startActivity(new Intent(MyAccountActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Log.i("ERROR", "ERROR");
                }
            } catch (Exception e) {
                Log.i("EXCEPTION", "EXCEPTION");
                e.printStackTrace();
            }
            return null;
        }
    }
}