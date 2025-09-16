package org.feup.fallguys.fallarm;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContactActivity extends AppCompatActivity {
    private ContactAdapter contactAdapter;
    private ProfileDataHelper helper;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // Set up the ListView
        helper = new ProfileDataHelper(this);
        ListView contactList = findViewById(R.id.contact_list);
        Cursor cursor = helper.getContactsCursor();
        contactAdapter = new ContactAdapter(this, cursor, helper);
        contactList.setAdapter(contactAdapter);

        // Check and ask for required permissions
        List<String> permissionsNeeded = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        if (!permissionsNeeded.isEmpty()) {
            int SMS_AND_GPS_REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), SMS_AND_GPS_REQUEST_CODE);
        }

        // Set up the two buttons

        Button addContactButton = findViewById(R.id.add_contact_button);
        addContactButton.setOnClickListener(v -> {
            Intent intent = new Intent(ContactActivity.this, NewContactActivity.class);
            startActivity(intent);
        });

        continueButton = findViewById(R.id.continue_button);
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(ContactActivity.this, SetUpAccelerometerActivity.class);
            intent.putExtra("from_activity", "ContactActivity");
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the cursor and update the data when the activity resumes
        Cursor newCursor = helper.getContactsCursor();
        contactAdapter.changeCursor(newCursor);

        // Change the button text from skip to continue if at least one contact has been added
        if (helper.hasContact()) {
            continueButton.setText(R.string.continue_button_text);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {

            // Check and handle all answers to permission requests
            for (int i = 0; i < grantResults.length; i++) {
                String message = "";
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    switch (permissions[i]) {
                        case Manifest.permission.SEND_SMS:
                            message = "Fallarm can now send SMS messages";
                            break;
                        case Manifest.permission.ACCESS_FINE_LOCATION:
                            message = "Fallarm can now use your location";

                            // Permission to use location in background must be requested separately
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                int BACKGROUND_GPS_REQUEST_CODE = 2;
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_GPS_REQUEST_CODE);
                            }
                            break;
                        case Manifest.permission.ACCESS_COARSE_LOCATION:
                            message = "Fallarm can now use your location";
                            break;
                        case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                            message = "Fallarm can now use your location in the background";
                            break;
                    }
                } else {
                    switch (permissions[i]) {
                        case Manifest.permission.SEND_SMS:
                            message = "Permission to use SMS denied";
                            break;
                        case Manifest.permission.ACCESS_FINE_LOCATION:
                        case Manifest.permission.ACCESS_COARSE_LOCATION:
                            message = "Permission to use location denied";
                            break;
                        case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                            message = "Permission to use location in background denied";
                            break;
                    }
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }
}