package com.skrine525.school10;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.text.format.Time;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShareActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    Button shareButton, dateButton;
    String dateOfWork = null;
    Time dateOfWorkTime = null;
    Spinner subjectSpinner, teacherSpinner;
    TextView status_textView;

    String UserData_Name, UserData_Surname, UserData_Email = null;
    long UserData_Class = 0;

    @Override
    protected void onResume() {
        super.onResume();

        String Name = sharedPreferences.getString("Name", "");
        String Surname = sharedPreferences.getString("Surname", "");
        String Email = sharedPreferences.getString("Surname", "");
        long Class = sharedPreferences.getLong("Class", 0);

        //  Проверка на Email временно удалена
        if(Name.equals("") || Surname.equals("") || /*Email.equals("") || */Class == 0){
            Intent intent = new Intent(ShareActivity.this, RegisterActivity.class);
            startActivity(intent);
        }
        else{
            if(UserData_Name == null)
                UserData_Name = Name;
            if(UserData_Surname == null)
                UserData_Surname = Surname;
            if(UserData_Email == null)
                UserData_Email = Email;
            if(UserData_Class == 0)
                UserData_Class = Class;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);

        Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            final Uri contentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (contentUri != null) {
                setContentView(R.layout.activity_share);

                shareButton = findViewById(R.id.button_Share);
                dateButton = findViewById(R.id.button_Date);
                subjectSpinner = findViewById(R.id.spinner_Subject);
                teacherSpinner = findViewById(R.id.spinner_Teacher);
                status_textView = findViewById(R.id.textView_Status);

                shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(teacherSpinner.getSelectedItemPosition() == 0)
                            Toast.makeText(getApplicationContext(), "Выберите учителя!", Toast.LENGTH_SHORT).show();
                        else if(subjectSpinner.getSelectedItemPosition() == 0)
                            Toast.makeText(getApplicationContext(), "Выберите предмет!", Toast.LENGTH_SHORT).show();
                        else if(dateOfWork == null)
                            Toast.makeText(getApplicationContext(), "Выберите дату работы!", Toast.LENGTH_SHORT).show();
                        else{
                            SendContentToServer sender = new SendContentToServer();
                            sender.execute(contentUri, new String[] {UserData_Surname, UserData_Name}, UserData_Class, teacherSpinner.getSelectedItem(), subjectSpinner.getSelectedItem(), dateOfWork);
                        }
                    }
                });

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
                Toast.makeText(this, "Контент не выбран!", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }
    }

    private class SendContentToServer extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            teacherSpinner.setEnabled(false);
            subjectSpinner.setEnabled(false);
            dateButton.setEnabled(false);
            shareButton.setEnabled(false);
            Toast.makeText(getApplicationContext(), "Началась загрузка файла на сервер.", Toast.LENGTH_SHORT).show();
            status_textView.setText("");
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Uri contentUri = (Uri) objects[0];
            String[] userName = (String[]) objects[1];
            long classId = (long) objects[2];
            String selectedTeacher = (String) objects[3];
            String selectedSubject = (String) objects[4];
            String dateOfWork = (String) objects[5];

            String contentFileExtension = MimeTypeMap.getFileExtensionFromUrl(contentUri.toString());
            File file = downloadContentToCache(contentUri, "content." + contentFileExtension);

            SimpleYandexDisk client = new SimpleYandexDisk(getResources().getString(R.string.yandex_api_token));

            try{
                publishProgress("Получение текущей даты с сервера...");  // Оповещение через Status TextView
                String currentDate = getCurrentDate();

                Resources resources = getResources();

                String[] pathArray = {
                        selectedTeacher,
                        selectedSubject,
                        resources.getStringArray(R.array.classes)[(int) classId],
                        userName[0] + "_" + userName[1] + "_"+dateOfWork+"_"+currentDate+"."+contentFileExtension};

                String href = null;
                for(;;){
                    String path = "Школа 10";
                    for(int i = 0; i < pathArray.length; i++){
                        path += "/" + pathArray[i];
                    }

                    publishProgress("Запрос на загрузку файла...");  // Оповещение через Status TextView
                    Response response = client.getUploadServerURL(path);
                    String stringResponse = response.body().string();

                    JSONObject jsonObject = new JSONObject(stringResponse);
                    if(jsonObject.has("error")){
                        String error = jsonObject.getString("error");
                        if(error.equals("DiskPathDoesntExistsError")) {
                            String new_path = "Школа 10";
                            for(int i = 0; i < pathArray.length - 1; i++){
                                new_path += "/" + pathArray[i];
                                publishProgress("Создание каталога "+(i+1)+"...");  // Оповещение через Status TextView
                                client.makeDirectory(new_path);
                            }
                            continue;
                        } else if (error.equals("DiskResourceAlreadyExistsError")){
                            return "Нельзя отправлять файлы так часто. Повторите попытку через 1 минуту.";
                        } else {
                            return "Yandex.Disk error: " + error;
                        }
                    }

                    jsonObject = new JSONObject(stringResponse);
                    href = jsonObject.getString("href");
                    break;
                }
                if(href != null){
                    publishProgress("Загрузка файла на сервер..."); // Оповещение через Status TextView
                    Response response = client.uploadFile(href, file);

                    switch (response.code()){
                        case 201:
                        case 202:
                            return null;

                        case 507:
                            return "Ошибка: В хранилище недостаточно места.";

                        default:
                            return "HTTP Response Code: " + response.code();
                    }
                }
                else{
                    return "Неудалость загрузить файл.";
                }
            }
            catch (IOException | JSONException e) {
                return e;
            }
        }

        @Override
        protected  void onProgressUpdate(Object... objects){
            String text = (String) objects[0];
            status_textView.setText(text);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            if(o != null) {
                teacherSpinner.setEnabled(true);
                subjectSpinner.setEnabled(true);
                dateButton.setEnabled(true);
                shareButton.setEnabled(true);
                Toast.makeText(getApplicationContext(), "Во время загрузки произошла ошибка.\nПовторите попытку.", Toast.LENGTH_LONG).show();
                status_textView.setText((String) o);
            }
            else{
                Toast.makeText(getApplicationContext(), "Файл успешно загружен на сервер.", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }

        private String getCurrentDate() throws IOException, JSONException {
            OkHttpClient client = new OkHttpClient();
            Request request  = new Request.Builder().url("http://radabot.ddns.net/school10/time.php").build();
            Response response = client.newCall(request).execute();
            JSONObject object = new JSONObject(response.body().string());
            return object.getString("date");
        }

        private File downloadContentToCache(Uri uri, String filename) {
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
