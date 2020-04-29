package com.skrine525.school10;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

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

        // Отрисовываем Layout
        setContentView(R.layout.activity_main);

        // Получаем объекты элементов интерфейса
        sendButton = findViewById(R.id.button_Send);
        optionsButton = findViewById(R.id.button_Options);
        exitButton = findViewById(R.id.button_Exit);
        updateButton = findViewById(R.id.button_Update);

        // Получаем SharedPreferences хранилище
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);

        if(sharedPreferences.getString("Name", "").equals("")){
            // Если в SharedPreferences нет параметра Name, то запускаем Activity регистрации
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getContentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                getContentIntent.setType("*/*");
                startActivityForResult(getContentIntent, 0);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            switch (requestCode){
                case 0:
                    Uri contentUri = data.getData();
                    String contentType = getContentResolver().getType(contentUri);

                    Intent shareIntent = new Intent(this, ShareActivity.class);
                    shareIntent.setAction(Intent.ACTION_VIEW);
                    shareIntent.setDataAndType(contentUri, contentType);
                    startActivity(shareIntent);
                    break;
            }
        }
        else if(resultCode == RESULT_CANCELED)
            if(Build.VERSION.SDK_INT >= 25)
                Snackbar.make(sendButton, "Вы не выбрали файл", Snackbar.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), "Вы не выбрали файл", Toast.LENGTH_SHORT).show();
    }

    // Класс для скачивания обновления с сервера
    private class ApplicationUpdater extends AsyncTask{
        ProgressDialog progressDialog;
        String href = "";

        ApplicationUpdater(String href){
            this.href = href;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Настраиваем ProgressDialog
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Загрузка...");
            progressDialog.setCancelable(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            // Скачиваем APK-пакет
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
                    publishProgress(downloadedSize, totalSize);         // Выводим процесс загрузки в ProgressDialog
                }

                fileOutputStream.close();
                inputStream.close();

                apkFile = file;
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Возвращаем File объект скачанного APK-пакета
            return apkFile;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);

            // Выводим процесс загрузки в ProgressDialog
            int progress = (int) (((Number) values[0]).floatValue() / ((Number) values[1]).floatValue() * 100);         // Расчитываем прогресс в процентах
            progressDialog.setProgress(progress);                                                                       // Обновляем ProgressDialog
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            progressDialog.cancel();                                                                    // Закрываем ProgressDialog

            if(o != null){
                // Если o не пуст, то устанавливаем пакет
                File apkFile = (File) o;                                                                // Преобразуем объект в File
                Intent installIntent = new Intent(Intent.ACTION_VIEW);                                  // Создаем новый Intent
                Uri apkURI = FileProvider.getUriForFile(MainActivity.this,
                        getApplicationContext().getPackageName() + ".provider", apkFile);      // Получаем URI APK-пакета
                Log.d("Provider URI", apkURI.toString());
                installIntent.setData(apkURI);                                                          // Устанавливаем URI в качестве данных Intent
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);                          // Устанавливаем флаг на разрешение чтения URI

                startActivity(installIntent);                                                           // Запускаем Activity установки
            }
            else
                // Иначе выводим сообщение об ошибке
                Toast.makeText(getApplicationContext(), "Не удалось скачать обновление!", Toast.LENGTH_SHORT).show();
        }
    }

    private class CheckForUpdatesHandler extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            // Получаем входные данные из объектов
            int currentVersionCode = (int) objects[0];          // Текущая версия пакета

            // Делаем запрос на сервер для поиска новой версии
            OkHttpClient client = new OkHttpClient();
            try {
                Request request  = new Request.Builder().url("http://radabot.ddns.net/school10/app/meta.json").build();
                Response response = client.newCall(request).execute();
                JSONObject object = new JSONObject(response.body().string());
                int versionCode = object.getInt("versionCode");
                if(versionCode > currentVersionCode)
                    // Если нашли версию, то возвращаем JSONObject ответа
                    return object;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            // Возвращаем null, чтобы ничего не делать
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if(o != null){
                // Если o не пусто, то обрабатываем обновление приложения
                JSONObject jsonObject = (JSONObject) o; // Преобразуем объект в JSONObject

                // Переменные, хранящие информацию об обновлении
                String versionName = "";
                String description = "";
                String href = "";

                // Пытаемся получить информацию об обновлении из JSONObject
                try {
                    versionName = jsonObject.getString("versionName");
                    description = jsonObject.getString("description");
                    href = jsonObject.getString("href");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;                                                     // Если ловим исключение, то просто выходим из метода
                }
                final String finalHref = href;                                  // Временная final переменная href
                
                updateButton.setVisibility(View.VISIBLE);                                                                                // Делаем кнопку Обновить видимой

                // Создаем AlertDialog обновления
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Обновление " + versionName);
                builder.setMessage(description);
                builder.setCancelable(false);

                // Устанавливаем кнопку Загрузить для AlertDialog
                builder.setPositiveButton("Загрузить", new AlertDialog.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Загружаем обновление
                        ApplicationUpdater applicationUpdater = new ApplicationUpdater(finalHref);
                        applicationUpdater.execute();
                    }
                });

                // Устанавливаем кнопку Отмена для AlertDialog
                builder.setNegativeButton("Отмена", new AlertDialog.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Закрываем AlertDialog
                        dialog.cancel();
                    }
                });

                // Устанавливаем обработчик нажатий для кнопки Обновить
                updateButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        builder.create().show();
                    }
                });

                // Показываем AlertDialog обновления
                builder.create().show();
            }
        }
    }
}
