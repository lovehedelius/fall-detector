package org.feup.fallguys.fallarm;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class NameActivity extends AppCompatActivity {
    private ProfileDataHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_name);
        Objects.requireNonNull(getSupportActionBar()).hide();

        helper = new ProfileDataHelper(this);
        EditText nameField = findViewById(R.id.name_field);
        Button continueButton = findViewById(R.id.continue_button);

        // Enable the continue button when the user has entered a name
        nameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                continueButton.setEnabled(!s.toString().isBlank());
            }
        });

        // The button stores the entered name in the database and takes the user to the next step of the setup
        continueButton.setOnClickListener(v -> {
            String profileName = nameField.getText().toString();
            helper.setUpProfile(profileName);
            Intent intent = new Intent(NameActivity.this, ContactActivity.class);
            startActivity(intent);
        });
    }
}