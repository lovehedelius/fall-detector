package org.feup.fallguys.fallarm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private ProfileDataHelper helper;
    private EditText editName;
    private ContactAdapter contactAdapter;
    private boolean accelerometerConnected;
    private int accelerometerBattery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        LocalBroadcastManager.getInstance(this).registerReceiver(accelerometerReceiver, new IntentFilter("DEVICE_CONNECTION_STATUS"));
        LocalBroadcastManager.getInstance(this).registerReceiver(accelerometerReceiver, new IntentFilter("ACCELEROMETER_UPDATE"));

        // Set the action bar
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_icon);
            actionBar.setTitle("Settings");
        }

        // Put the stored name in the name field
        helper = new ProfileDataHelper(this);
        editName = findViewById(R.id.name_field);
        editName.setText(helper.getProfileName());
        editName.clearFocus();

        // Set up ListView and adapter
        ListView listView = findViewById(R.id.contact_list);
        Cursor cursor = helper.getContactsCursor();
        contactAdapter = new ContactAdapter(this, cursor, helper);
        listView.setAdapter(contactAdapter);

        // Set up the "add contact" button to go to NewContactActivity
        Button addContactButton = findViewById(R.id.add_contact_button);
        addContactButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, NewContactActivity.class);
            startActivity(intent);
        });

        // Show the name of the accelerometer stored in the database
        TextView deviceName = findViewById(R.id.connected_device);
        if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                && !helper.getDevice().equals("No device registered")) {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(helper.getDevice());
            deviceName.setText(device.getName());
        }

        // Set up the "connect new device" button to go to SetUpAccelerometerActivity
        Button connectDeviceButton = findViewById(R.id.connect_device_button);
        connectDeviceButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, SetUpAccelerometerActivity.class);
            intent.putExtra("from_activity", "SettingsActivity");
            startActivity(intent);
        });

        // Set up the toggle button for GPS
        SwitchCompat gpsSwitch = findViewById(R.id.gps_switch);
        gpsSwitch.setChecked(helper.isGpsEnabled());
        gpsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> helper.changeGpsEnabled());
    }

    private final BroadcastReceiver accelerometerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Listen to messages from the accelerometer via AccelerometerListenerService to see if it is connected and what the battery level is
            if ("ACCELEROMETER_UPDATE".equals(intent.getAction())) {
                accelerometerConnected = true; // If a message is received the device must be connected
                accelerometerBattery = intent.getIntExtra("battery", -1);
                updateAccelerometerStatus();
            } else if ("DEVICE_CONNECTION_STATUS".equals(intent.getAction())) {
                accelerometerConnected = intent.getBooleanExtra("isConnected", false);
                updateAccelerometerStatus();
            }
        }
    };


    private void updateAccelerometerStatus() {
        // Update the info text about the accelerometer when a new message is received
        TextView connectionStatus = findViewById(R.id.device_status);
        TextView batteryText = findViewById(R.id.battery);
        connectionStatus.setText(accelerometerConnected ? "Connected" : "Not connected");
        batteryText.setText(getString(R.string.battery_text, accelerometerBattery));
        if (!accelerometerConnected) {
            batteryText.setVisibility(View.INVISIBLE);
        } else {
            batteryText.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Update the name in the database when the user goes back to the home page
        if (item.getItemId() == android.R.id.home) {
            helper.changeName(editName.getText().toString());
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onResume() {
        super.onResume();

        // Refresh the cursor and update the data in the ListView when the activity resumes
        Cursor newCursor = helper.getContactsCursor();
        contactAdapter.changeCursor(newCursor);
    }
}
