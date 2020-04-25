package com.skrine525.school10;

import android.content.Intent;
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
    EditText userNameEditText, userSurnameEditText, userEmailEditText;
    Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userClassSpinner = findViewById(R.id.spinner_Class);
        userNameEditText = findViewById(R.id.editText_Name);
        userSurnameEditText = findViewById(R.id.editText_Surname);
        userEmailEditText = findViewById(R.id.editText_Email);
        saveButton = findViewById(R.id.button_Save);

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String User_Name = sharedPreferences.getString("Name", "");
        String User_Surname = sharedPreferences.getString("Surname", "");
        if(!User_Name.equals("") || !User_Surname.equals("")){
            userNameEditText.setText(User_Name);
            userSurnameEditText.setText(User_Surname);
            userEmailEditText.setText(sharedPreferences.getString("Email", ""));
            userClassSpinner.setSelection((int) sharedPreferences.getLong("Class", 0));
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userNameEditText.getText().toString().equals(""))
                    Toast.makeText(getApplicationContext(), "Укажите имя!", Toast.LENGTH_SHORT).show();
                else if (userSurnameEditText.getText().toString().equals(""))
                    Toast.makeText(getApplicationContext(), "Укажите фамилию!", Toast.LENGTH_SHORT).show();
                else if (userEmailEditText.getText().toString().equals(""))
                    Toast.makeText(getApplicationContext(), "Укажите Email!", Toast.LENGTH_SHORT).show();
                else if (userClassSpinner.getSelectedItemId() == 0)
                    Toast.makeText(getApplicationContext(), "Выберите класс!", Toast.LENGTH_SHORT).show();
                else{
                    Editor editor = sharedPreferences.edit();
                    String formatedName = userNameEditText.getText().toString().replaceAll(" ", "");
                    formatedName = formatedName.substring(0, 1).toUpperCase()+formatedName.substring(1).toLowerCase();
                    String formatedSurname = userSurnameEditText.getText().toString().replaceAll(" ", "");
                    formatedSurname = formatedSurname.substring(0, 1).toUpperCase()+formatedSurname.substring(1).toLowerCase();

                    editor.putString("Name", formatedName);
                    editor.putString("Surname", formatedSurname);
                    editor.putString("Email", userEmailEditText.getText().toString());
                    editor.putLong("Class", userClassSpinner.getSelectedItemId());
                    if(editor.commit()){
                        Toast.makeText(getApplicationContext(), "Данные успешно сохранены! ", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else
                        Toast.makeText(getApplicationContext(), "Не удалось сохранить данные!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
