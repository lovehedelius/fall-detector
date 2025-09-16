package org.feup.fallguys.fallarm;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import Bio.Library.namespace.BioLib;

public class AccelerometerListenerService extends Service {
    private FallAlertHelper fallAlert;
    private BioLib lib = null;
    private int battery;
    private double conversionFactor = 4.0 / 128 * 9.8;

    @Override
    public void onCreate() {
        super.onCreate();
        fallAlert = FallAlertHelper.getInstance(getApplicationContext()); // A helper class containing all the code related to sending an alert
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String macAddress = intent.getStringExtra("mac_address");

        // Define the handler to handle the relevant data sent from the accelerometer
        Handler handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case BioLib.STATE_CONNECTED:
                        broadcastConnectionStatus(true);
                        askForSensitivity();
                        break;
                    case BioLib.MESSAGE_DISCONNECT_TO_DEVICE:
                        broadcastConnectionStatus(false);
                        break;
                    case BioLib.MESSAGE_ACC_SENSIBILITY:
                        double sensitivity = (msg.arg1 == 1) ? 4 : 2;
                        conversionFactor = sensitivity / 128 * 9.8;
                        break;
                    case BioLib.MESSAGE_ACC_UPDATED:
                        BioLib.DataACC accelerometerData = (BioLib.DataACC) msg.obj;
                        detectFall(accelerometerData.X, accelerometerData.Y, accelerometerData.Z);
                        broadcastAccelerometerUpdate();
                        break;
                    case BioLib.MESSAGE_DATA_UPDATED:
                        BioLib.Output output = (BioLib.Output) msg.obj;
                        battery = output.battery;
                        break;
                }
            }
        };

        // Use the BioLib library to connect to the accelerometer
        try {
            lib = new BioLib(this, handler);
        } catch (Exception e) {
            Log.e("Listener", "Could not create BioLib");
        }

        try {
            lib.Connect(macAddress, 0);
        } catch (Exception e) {
            Log.e("Listener", "Could not connect");
        }

        return START_REDELIVER_INTENT;
    }

    private void askForSensitivity() {
        // Ask the accelerometer to send a message telling which sensitivity it is set to
        try {
            lib.GetAccSensibility();
        } catch (Exception e) {
            Log.e("Listener", "Failed when accessing sensitivity");
        }
    }

    private void detectFall(byte xByte, byte yByte, byte zByte) {
        // If the magnitude is closer to 0 the movement is more similar to free-fall
        double x = xByte * conversionFactor;
        double y = yByte * conversionFactor;
        double z = zByte * conversionFactor;
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (magnitude < 5) {
            fallAlert.startAlertProcess();
        }
    }

    private void broadcastConnectionStatus(boolean isConnected) {
        // To enable other activities to show messages about connection
        Intent intent = new Intent("DEVICE_CONNECTION_STATUS");
        intent.putExtra("isConnected", isConnected);
        intent.putExtra("battery", battery);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void broadcastAccelerometerUpdate() {
        // A broadcast which happens continuously to allow the settings to keep track of connection and battery
        Intent intent = new Intent("ACCELEROMETER_UPDATE");
        intent.putExtra("battery", battery);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}