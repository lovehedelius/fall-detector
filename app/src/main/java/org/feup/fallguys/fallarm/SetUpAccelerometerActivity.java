package org.feup.fallguys.fallarm;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SetUpAccelerometerActivity extends AppCompatActivity {
    private final int ENABLE_BT_REQUEST_CODE = 1;
    private final int ALLOW_BT_REQUEST_CODE = 2;
    private String connectedDeviceName;
    private String macAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_up_accelerometer);
        Objects.requireNonNull(getSupportActionBar()).hide();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            String message = "No Bluetooth adapter found";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Check if Bluetooth is on and permissions are granted, and request if not
        if (!btAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, ALLOW_BT_REQUEST_CODE);
                    return;
                }
            }
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, ENABLE_BT_REQUEST_CODE);
        }

        // Set up the ListView and the adapter
        ArrayAdapter<String> devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView deviceList = findViewById(R.id.paired_devices_list);
        deviceList.setAdapter(devicesAdapter);

        // Add the names of all paired devices to the ListView and keep track of their MAC addresses
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        Map<String, String> macAddresses = new HashMap<>();
        if (pairedDevices.isEmpty()) {
            devicesAdapter.add("No devices paired");
        }
        for (BluetoothDevice device : pairedDevices) {
            macAddresses.put(device.getName(), device.getAddress());
            devicesAdapter.add(device.getName());
        }

        // When a device is clicked its address is sent to AccelerometerListenerService which tries to connect with it
        deviceList.setOnItemClickListener((parent, view, position, id) -> {
            connectedDeviceName = ((TextView) view).getText().toString();
            macAddress = macAddresses.get(connectedDeviceName);
            Intent serviceIntent = new Intent(SetUpAccelerometerActivity.this, AccelerometerListenerService.class);
            serviceIntent.putExtra("mac_address", macAddress);
            startService(serviceIntent);
        });

        // Check if the activity was started from the setup or the settings and make the continue button lead back there
        String previousActivity = getIntent().getStringExtra("from_activity");
        Button continueButton = findViewById(R.id.continue_button);
        if ("ContactActivity".equals(previousActivity)) {
            continueButton.setOnClickListener(v -> {
                Intent intent = new Intent(SetUpAccelerometerActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } else {
            TextView infoText = findViewById(R.id.info_text);
            infoText.setVisibility(View.GONE); // If the user got here from the settings they don't need the explanatory text
            continueButton.setOnClickListener(v -> {
                Intent intent = new Intent(SetUpAccelerometerActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Check if Bluetooth permission was granted and ask to turn on Bluetooth if it wws but it is not already turned on
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALLOW_BT_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, ENABLE_BT_REQUEST_CODE);
                } else {
                    String message = "Permission error";
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {
                String message = "Permission to use Bluetooth denied";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle the result of the request to turn on Bluetooth
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_BT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String message = "Bluetooth is on";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to enable Bluetooth adapter", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    private final BroadcastReceiver accelerometerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Listen to the accelerometer message via AccelerometerListenerService to communicate when it connects or disconnects
            if ("DEVICE_CONNECTION_STATUS".equals(intent.getAction())) {
                boolean isConnected = intent.getBooleanExtra("isConnected", false);
                if (isConnected) {
                    ProfileDataHelper helper = new ProfileDataHelper(SetUpAccelerometerActivity.this);
                    helper.setDevice(macAddress); // Store the address of the connected device in the database
                    helper.close();
                }
                String status = isConnected ? "Connected to " : "Disconnected from ";
                String message = status + connectedDeviceName;
                Toast.makeText(SetUpAccelerometerActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("DEVICE_CONNECTION_STATUS");
        LocalBroadcastManager.getInstance(this).registerReceiver(accelerometerReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(accelerometerReceiver);
    }
}