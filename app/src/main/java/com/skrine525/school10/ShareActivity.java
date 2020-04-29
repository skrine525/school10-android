package com.skrine525.school10;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.text.format.Time;

import com.google.android.material.snackbar.Snackbar;
import com.skrine525.school10.utils.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShareActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    Button shareButton, dateButton;
    Spinner subjectSpinner, teacherSpinner;
    TextView statusTextView, filenameTextView;
    ProgressBar statusProgressBar;
    String dateOfWork = null;
    Time dateOfWorkTime = null;
    Uri contentUri = null;

    String UserData_Name, UserData_Surname = null;
    long UserData_Class = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // Отрисовываем Layout
        setContentView(R.layout.activity_share);

        // Получаем SharedPreferences хранилище
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);

        // Получаем данные с входного Intent
        Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null)
            // Если Activity было запущено из другого приложения, то получаем URI с контентом
            contentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        else if(Intent.ACTION_VIEW.equals(action) && type != null)
            // Если Activity было запущено из этого приложения, то получаем URI с контентом
            contentUri = intent.getData();


        if (contentUri != null) {
            // Если contentUri не пустой, то выполняем настройку Activity

            // DocumentFile объект контента
            DocumentFile documentFile = DocumentFile.fromSingleUri(getApplicationContext(), contentUri);

            // Размер контента
            long contentLength = documentFile.length();

            if(contentLength > 20971520){
                // Если размер контента больше 20 МБ, то выводим AlertDialog с ошибкой
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);
                builder.setTitle("Ошибка");
                builder.setMessage("Максимальный размер содержимого - 20 МБ");
                builder.setCancelable(false);
                builder.setPositiveButton("Закрыть", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Завершаем Activity по нажатию кнопки Закрыть в AlertDialog
                        finish();
                    }
                });
                builder.create().show();
            }
            else if(contentLength == 0){
                // Если размер контента равен 0 Б, то выводим AlertDialog с ошибкой
                AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);
                builder.setTitle("Ошибка");
                builder.setMessage("Содержимое пустое");
                builder.setCancelable(false);
                builder.setPositiveButton("Закрыть", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Завершаем Activity по нажатию кнопки Закрыть в AlertDialog
                        finish();
                    }
                });
                builder.create().show();
            }

            // Получаем объекты элементов интерфейса
            shareButton = findViewById(R.id.button_Share);
            dateButton = findViewById(R.id.button_Date);
            subjectSpinner = findViewById(R.id.spinner_Subject);
            teacherSpinner = findViewById(R.id.spinner_Teacher);
            statusTextView = findViewById(R.id.textView_Status);
            filenameTextView = findViewById(R.id.textView_Filename);
            statusProgressBar = findViewById(R.id.progressBar_Status);

            // Устанавливаем название контента в TextView
            filenameTextView.setText(documentFile.getName());

            // Устанавливаем обработчик нажатий для кнопки Отправить
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(teacherSpinner.getSelectedItemPosition() == 0) {
                        // Если Учитель не выбран, то выводим сообщение
                        if(Build.VERSION.SDK_INT >= 25)
                            Snackbar.make(shareButton, "Выберите учителя!", Snackbar.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getApplicationContext(), "Выберите учителя!", Toast.LENGTH_SHORT).show();
                    }
                    else if(subjectSpinner.getSelectedItemPosition() == 0) {
                        // Если Предмет не выбран, то выводим сообщение
                        if(Build.VERSION.SDK_INT >= 25)
                            Snackbar.make(shareButton, "Выберите предмет!", Snackbar.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getApplicationContext(), "Выберите предмет!", Toast.LENGTH_SHORT).show();
                    }
                    else if(dateOfWork == null) {
                        // Если Дата Работы не выбрана, то выводим сообщение
                        if(Build.VERSION.SDK_INT >= 25)
                            Snackbar.make(shareButton, "Выберите дату работы!", Snackbar.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getApplicationContext(), "Выберите дату работы!", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        // Иначе запускаем AsynkTask загрузки контента на сервер
                        SendContentToServer sender = new SendContentToServer();
                        sender.execute(contentUri, new String[] {UserData_Surname, UserData_Name}, UserData_Class, teacherSpinner.getSelectedItem(), subjectSpinner.getSelectedItem(), dateOfWork);
                    }
                }
            });

            // Устанавливаем обработчик нажатий для кнопки Выбрать дату работы
            dateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            dateOfWorkTime = new Time();
                            dateOfWorkTime.set(dayOfMonth, month, year);
                            String textDate = "";
                            if(dayOfMonth < 10){
                                textDate = "0" + dayOfMonth;
                                dateOfWork = "0" + dayOfMonth;
                            }
                            else {
                                textDate = String.valueOf(dayOfMonth);
                                dateOfWork = String.valueOf(dayOfMonth);
                            }

                            int formatedMonth = month + 1;
                            if(formatedMonth < 10) {
                                textDate += ".0" + formatedMonth;
                                dateOfWork += "-0" + formatedMonth;
                            }
                            else {
                                textDate += "." + formatedMonth;
                                dateOfWork += "-" + formatedMonth;
                            }

                            int shortYear = year % 100;
                            if(shortYear < 10)
                                textDate += ".0"+shortYear;
                            else
                                textDate += "."+shortYear;

                            dateButton.setText(textDate);
                        }
                    };

                    DatePickerDialog datePickerDialog = null;
                    if(dateOfWork == null){
                        Time time = new Time();
                        time.setToNow();
                        datePickerDialog = new DatePickerDialog(ShareActivity.this, onDateSetListener, time.year, time.month, time.monthDay);
                    }
                    else{
                        datePickerDialog = new DatePickerDialog(ShareActivity.this, onDateSetListener, dateOfWorkTime.year, dateOfWorkTime.month, dateOfWorkTime.monthDay);
                    }
                    datePickerDialog.show();
                }
            });
        }
        else {
            // Если contentUri пустой, то выводим сообщение и завершаем Activity
            Toast.makeText(this, "Контент не выбран!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Получаем данные из SharedPreferences
        String Name = sharedPreferences.getString("Name", "");
        String Surname = sharedPreferences.getString("Surname", "");
        long Class = sharedPreferences.getLong("Class", 0);

        if(Name.equals("") || Surname.equals("") || Class == 0){
            // Если одного из данных нет в SharedPreferences, то запсукаем Activity регистрации
            Intent intent = new Intent(ShareActivity.this, RegisterActivity.class);
            startActivity(intent);
        }
        else{
            // Иначе присваиваем значения пустым переменным
            if(UserData_Name == null)
                UserData_Name = Name;
            if(UserData_Surname == null)
                UserData_Surname = Surname;
            if(UserData_Class == 0)
                UserData_Class = Class;
        }
    }

    // Класс для отправки контента на сервер
    private class SendContentToServer extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Подготовка к загрузке файла
            teacherSpinner.setEnabled(false);                                                   // Отключаем выбор Учителя
            subjectSpinner.setEnabled(false);                                                   // Отключаем выбор Предмета
            dateButton.setEnabled(false);                                                       // Отключаем кнопку Выбрать Дату Работы
            shareButton.setEnabled(false);                                                      // Отключаем кнопку Отправить
            // Выводим сообщение
            if(Build.VERSION.SDK_INT >= 25)
                Snackbar.make(shareButton, "Началась загрузка файла на сервер.",
                        Snackbar.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(),
                        "Началась загрузка файла на сервер.", Toast.LENGTH_SHORT).show();
            statusTextView.setText("");                                                         // Очищаем Status TextView
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            // Получаем входные данные из объектов
            Uri contentUri = (Uri) objects[0];
            String[] userName = (String[]) objects[1];
            long classId = (long) objects[2];
            String selectedTeacher = (String) objects[3];
            String selectedSubject = (String) objects[4];
            String dateOfWork = (String) objects[5];

            // Получаем расширение контента
            String contentFileExtension = FilenameUtils.getExtension(DocumentFile.fromSingleUri(getApplicationContext(), contentUri).getName());
            if(!contentFileExtension.equals(""))
                contentFileExtension = "." + contentFileExtension;

            // Получаем сохраненный файл контента
            File file = saveContentToCache(contentUri, "content" + contentFileExtension);

            // Создаем объект SimpleYandexDisk для запросов на Yandex.Disk сервер
            SimpleYandexDisk client = new SimpleYandexDisk(getResources().getString(R.string.yandex_api_token));

            try{
                publishProgress(0, "Получение текущей даты с сервера...");     // Оповещение через Status TextView
                String currentDate = getCurrentDate();                              // Получаем текущую дату с сервера

                Resources resources = getResources();                               // Получаем ресурсы приложения

                // Издаем массив полного пути будущего файла на сервере
                String[] pathArray = {
                        selectedTeacher,
                        selectedSubject,
                        resources.getStringArray(R.array.classes)[(int) classId],
                        userName[0] + "_" + userName[1] + "_"+dateOfWork+"_"+currentDate+contentFileExtension};

                String href = null;                                             // Переменная для хранения URL сервера загрузки
                // Цикл для попыток получить href
                for(;;){
                    String path = "Школа 10";                                   // Переменная полного пути будущего файла на сервере
                    for(int i = 0; i < pathArray.length; i++){
                        // Заполняем переменную path из элементов массива pathArray
                        path += "/" + pathArray[i];
                    }

                    publishProgress(0, "Запрос на загрузку файла...");     // Оповещение через Status TextView

                    // Делаем запрос на загрузку файла на сервер
                    Response response = client.getUploadServerURL(path);
                    String stringResponse = response.body().string();

                    JSONObject jsonObject = new JSONObject(stringResponse);     // JSONObject полученного ответа
                    if(jsonObject.has("error")){
                        // Если JSON ответ имеет поле error, то обрабатываем ошибку
                        String error = jsonObject.getString("error");       // Получаем код ошибки
                        if(error.equals("DiskPathDoesntExistsError")) {
                            // Если пути соответствующего path нет на сервере, то создаем этот путь на сервере
                            String new_path = "Школа 10";
                            for(int i = 0; i < pathArray.length - 1; i++){
                                new_path += "/" + pathArray[i];
                                publishProgress(0, "Создание каталога "+(i+1)+"...");  // Оповещение через Status TextView
                                client.makeDirectory(new_path);
                            }
                            continue;
                        } else if (error.equals("DiskResourceAlreadyExistsError")){
                            // Если такой файл уже существует на сервере, то возвращаем сообщение ошибки
                            return "Нельзя отправлять файлы так часто. Повторите попытку через 1 минуту.";
                        } else {
                            // Если получена неизвестная ошибка, то возвращаем сообщение ошибки
                            return "Yandex.Disk error: " + error;
                        }
                    }

                    // Получаем href
                    jsonObject = new JSONObject(stringResponse);        // JSONObject полученного ответа
                    href = jsonObject.getString("href");         // Получаем href из поля href в JSONObject
                    break;                                              // Выходим из цикла
                }
                if(href != null){
                    // Если href получена, то загружаем файл на сервер
                    publishProgress(0, "Загрузка файла на сервер... ");
                    Response response = client.uploadFile(href, file, new CountingFileRequestBody.ProgressListener() {
                        @Override
                        public void onProgress(long bytesWritten, long contentLength) {
                            // Выводим прогресс загрузки в Status TextView
                            int progress = (int) (((Number) bytesWritten).floatValue() / ((Number) contentLength).floatValue() * 100);          // Расчитываем прогресс в процентах
                            publishProgress(1, progress);                                                                               // Оповещение через Status TextView
                        }
                    });

                    // Обрабатываем код ответа
                    switch (response.code()){
                        case 201:
                            // Если файл принят, то вовращаем null
                        case 202:
                            // Если файл принят, но не обработан, то возвращаем null
                            return null;

                        case 507:
                            // Если нет места в хранилище, то выводим сообщение об ошибке
                            return "Ошибка: В хранилище недостаточно места.";

                        default:
                            // Если сервер вернул неизвестный код, то выводим сообщение об ошибке
                            return "HTTP Response Code: " + response.code();
                    }
                }
                else{
                    // Если href не получена, то возвращаем сообщение об ошибке
                    return "Неудалость загрузить файл.";
                }
            }
            catch (IOException | JSONException e) {
                // Если ловим искючение, то возвращаем объект
                return e;
            }
        }

        @Override
        protected  void onProgressUpdate(Object... objects){
            switch ((int) objects[0]){
                case 0:
                    // Выводим прогресс в Status TextView
                    String text = (String) objects[1];      // Преобразуем первый объект в String
                    statusTextView.setText(text);           // Выводим его в Status TextView
                    break;

                case 1:
                    if(statusProgressBar.getVisibility() == View.INVISIBLE)
                        // Если ProgressBar невидимый, делаем его видимым
                        statusProgressBar.setVisibility(View.VISIBLE);
                    int progress = (int) objects[1];
                    statusProgressBar.setProgress(progress);
                    break;
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if(o != null) {
                // Если o не пуст, то включаем элементы и выводим сообщение
                teacherSpinner.setEnabled(true);                                                   // Включаем выбор Учителя
                subjectSpinner.setEnabled(true);                                                   // Включаем выбор Предмета
                dateButton.setEnabled(true);                                                       // Включаем кнопку Выбрать Дату Работы
                shareButton.setEnabled(true);                                                      // Включаем кнопку Отправить
                // Выводим сообщение
                if(Build.VERSION.SDK_INT >= 25)
                    Snackbar.make(shareButton,
                            "Во время загрузки произошла ошибка. Повторите попытку.",
                            Snackbar.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(),
                            "Во время загрузки произошла ошибка. Повторите попытку.",
                            Toast.LENGTH_LONG).show();
                statusTextView.setText(o.toString());                                                 // Выводим ошибку в Status TextView
                statusProgressBar.setVisibility(View.INVISIBLE);                                      // Скрываем ProgressBar
            }
            else{
                // Иначе выводим сообщение и зыкрываем Activity
                Toast.makeText(getApplicationContext(), "Файл успешно загружен на сервер.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        // Метод получения текущей даты с radabot.ddns.net
        private String getCurrentDate() throws IOException, JSONException {
            OkHttpClient client = new OkHttpClient();
            Request request  = new Request.Builder().url("http://radabot.ddns.net/school10/time.php").build();
            Response response = client.newCall(request).execute();
            JSONObject object = new JSONObject(response.body().string());
            return object.getString("date");
        }

        // Метод сохранения контента в кеш в виде файла
        private File saveContentToCache(Uri uri, String filename) {
            File file = new File(getApplicationContext().getCacheDir(), filename);
            if(file.exists())
                file.delete();

            try{
                InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(file);
                int read;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                return file;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
