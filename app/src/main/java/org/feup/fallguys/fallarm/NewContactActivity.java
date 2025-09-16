package org.feup.fallguys.fallarm;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class NewContactActivity extends AppCompatActivity {
    private ProfileDataHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_contact);
        Objects.requireNonNull(getSupportActionBar()).hide();

        EditText contactName = findViewById(R.id.contact_name);
        EditText phoneNumber = findViewById(R.id.phone_number);
        Button addButton = findViewById(R.id.add_button);
        helper = new ProfileDataHelper(this);

        // The button stores the new contact in the database and takes the user back to the contacts page
        addButton.setOnClickListener(v -> {
            String name = contactName.getText().toString();
            String number = phoneNumber.getText().toString();
            helper.addContact(name, number);
            finish();
        });
    }
}