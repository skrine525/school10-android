package com.skrine525.school10;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    Spinner userClassSpinner;
    EditText userNameEditText, userSurnameEditText;
    Button saveButton;

    boolean isRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // Отрисовываем Layout
        setContentView(R.layout.activity_register);

        // Получаем объекты элементов интерфейса
        userClassSpinner = findViewById(R.id.spinner_Class);
        userNameEditText = findViewById(R.id.editText_Name);
        userSurnameEditText = findViewById(R.id.editText_Surname);
        saveButton = findViewById(R.id.button_Save);

        // Получаем SharedPreferences хранилище
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);

        // Получаем базовые данные из SharedPreferences
        String User_Name = sharedPreferences.getString("Name", "");         // Получаем Имя
        String User_Surname = sharedPreferences.getString("Surname", "");   // Получаем Фамилию

        if(!User_Name.equals("") || !User_Surname.equals("")){
            // Если базовые данные есть в SharedPreferences, то заполняем соответствующие элементы интерфейса
            userNameEditText.setText(User_Name);
            userSurnameEditText.setText(User_Surname);
            userClassSpinner.setSelection((int) sharedPreferences.getLong("Class", 0));
            isRegistered = true;
        }

        // Устанавливаем обработчик нажатий для кнопки Сохранить
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userNameEditText.getText().toString().equals(""))
                    // Если Имя не указано, то выводим сообщение
                    Toast.makeText(getApplicationContext(), "Укажите имя!", Toast.LENGTH_SHORT).show();
                else if (userSurnameEditText.getText().toString().equals(""))
                    // Если Фамилия не указана, то выводим сообщение
                    Toast.makeText(getApplicationContext(), "Укажите фамилию!", Toast.LENGTH_SHORT).show();
                else if (userClassSpinner.getSelectedItemId() == 0)
                    // Если Класс не указан, то выводим сообщение
                    Toast.makeText(getApplicationContext(), "Выберите класс!", Toast.LENGTH_SHORT).show();
                else{
                    // Иначе сохраняем данные в SharedPreferences
                    Editor editor = sharedPreferences.edit();
                    String formattedName = userNameEditText.getText().toString().replaceAll(" ", "");
                    formattedName = formattedName.substring(0, 1).toUpperCase()+formattedName.substring(1).toLowerCase();
                    String formattedSurname = userSurnameEditText.getText().toString().replaceAll(" ", "");
                    formattedSurname = formattedSurname.substring(0, 1).toUpperCase()+formattedSurname.substring(1).toLowerCase();

                    editor.putString("Name", formattedName);
                    editor.putString("Surname", formattedSurname);
                    editor.putLong("Class", userClassSpinner.getSelectedItemId());
                    if(editor.commit()){
                        // Если удалось сохранить данные, то выводим сообщение и завершаем Activity
                        Toast.makeText(getApplicationContext(), "Данные успешно сохранены! ", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else
                        // Иначе выводим сообщение
                        Toast.makeText(getApplicationContext(), "Не удалось сохранить данные!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Обработка нажатия кнопки Назад
        if(isRegistered)
            // Если пользователь зарегистрировна, то выполяем onBackPressed() из super класса
            super.onBackPressed();
        else
            // Если пользователь не зарегистрирован, то выходим из приложения
            finishAffinity();
    }
}
