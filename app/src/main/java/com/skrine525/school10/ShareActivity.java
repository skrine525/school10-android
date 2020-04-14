package com.skrine525.school10;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShareActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        if(sharedPreferences.getString("Name", "").equals("")){
            Intent intent = new Intent(ShareActivity.this, RegisterActivity.class);
            startActivity(intent);
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                TextView text = findViewById(R.id.testView);
                text.setText(uri.toString());
            }
        }

        Button button = findViewById(R.id.testButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TestHttp http = new TestHttp();
                http.appContext = getApplicationContext();
                http.textView = findViewById(R.id.testView);
                http.execute(getApplicationContext());
            }
        });
    }
}

class TestHttp extends AsyncTask{
    public Context appContext;
    public TextView textView;

    @Override
    protected Object doInBackground(Object[] objects) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://cloud-api.yandex.net/v1/disk/resources?path=HELP_ME")
                //.put() Нужен PUT, а не GET. И я без понятия как он тут работает
                .addHeader("Authorization", "AgAAAAAq6marAADLW70gxx0d50f2nJy6ndBl55A")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try{
            Response response = client.newCall(request).execute();
            //Toast.makeText(appContext, response.body().toString(), Toast.LENGTH_SHORT);
        }
        catch (IOException e) {
            //Toast.makeText(appContext, e.getMessage(), Toast.LENGTH_SHORT);
        }
        return null;
    }
}
