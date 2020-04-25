package com.skrine525.school10;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Button sendButton, optionsButton, exitButton, updateButton;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendButton = findViewById(R.id.button_Send);
        optionsButton = findViewById(R.id.button_Options);
        exitButton = findViewById(R.id.button_Exit);
        updateButton = findViewById(R.id.button_Update);

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        if(sharedPreferences.getString("Name", "").equals("")){
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShareActivity.class);
                startActivity(intent);
            }
        });

        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });

        TextView versionTextView = findViewById(R.id.textView_Version);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            versionTextView.setText(packageInfo.versionName);

            CheckForUpdatesHandler checkForUpdatesHandler = new CheckForUpdatesHandler();
            checkForUpdatesHandler.execute(packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class DownloadApplicationUpdate extends AsyncTask{
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setMessage("Загрузка...");
            progressDialog.setCancelable(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            String href = (String) objects[0];

            File apkFile = null;

            try {
                URL url = new URL(href);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.connect();

                File updateDirectory = new File(getApplicationContext().getCacheDir(), "update/");
                if(!updateDirectory.exists())
                    updateDirectory.mkdirs();

                File file = new File(updateDirectory.getAbsolutePath(), "build.apk");
                if(file.exists())
                    file.delete();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                InputStream inputStream = urlConnection.getInputStream();

                int totalSize = urlConnection.getContentLength();
                int downloadedSize = 0;

                byte[] buffer = new byte[1024];
                int bufferLength = 0;

                while ((bufferLength = inputStream.read(buffer)) >0){
                    fileOutputStream.write(buffer, 0, bufferLength);
                    downloadedSize += bufferLength;
                    publishProgress(downloadedSize, totalSize);
                }

                fileOutputStream.close();
                inputStream.close();

                apkFile = file;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return apkFile;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

            int progress = (int) (((Number) values[0]).floatValue() / ((Number) values[1]).floatValue() * 100);
            progressDialog.setProgress(progress);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            progressDialog.cancel();

            if(o != null){
                File apkFile = (File) o;
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                Uri apkURI = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", apkFile);
                Log.d("Provider URI", apkURI.toString());
                installIntent.setData(apkURI);
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(installIntent);
            }
            else
                Toast.makeText(getApplicationContext(), "Не удалось скачать обновление!", Toast.LENGTH_SHORT).show();
        }
    }

    private class CheckForUpdatesHandler extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            int currentVersionCode = (int) objects[0];

            OkHttpClient client = new OkHttpClient();

            try {
                Request request  = new Request.Builder().url("http://radabot.ddns.net/school10/app/meta.json").build();
                Response response = client.newCall(request).execute();
                JSONObject object = new JSONObject(response.body().string());
                int versionCode = object.getInt("versionCode");
                if(versionCode > currentVersionCode)
                    return object;

                return null;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if(o != null){
                JSONObject jsonObject = (JSONObject) o;

                final int versionCode;
                String versionName = "";
                String description = "";
                String href = "";
                try {
                    versionCode = jsonObject.getInt("versionCode");
                    versionName = jsonObject.getString("versionName");
                    description = jsonObject.getString("description");
                    href = jsonObject.getString("href");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                final String finalHref = href;

                Toast.makeText(getApplicationContext(), "Обнаружена новая версия приложения", Toast.LENGTH_SHORT).show();
                updateButton.setVisibility(View.VISIBLE);

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Обновление " + versionName);
                builder.setMessage(description);
                builder.setCancelable(false);
                builder.setPositiveButton("Загрузить", new AlertDialog.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DownloadApplicationUpdate downloadApplicationUpdate = new DownloadApplicationUpdate();
                        downloadApplicationUpdate.execute(finalHref);
                    }
                });

                builder.setNegativeButton("Отмена", new AlertDialog.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                updateButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                            builder.create().show();
                    }
                });
            }
        }
    }
}
