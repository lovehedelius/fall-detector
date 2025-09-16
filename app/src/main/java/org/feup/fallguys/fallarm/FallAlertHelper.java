package org.feup.fallguys.fallarm;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.CountDownTimer;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FallAlertHelper {
    private static FallAlertHelper instance;
    private Context context;
    private ProfileDataHelper helper;
    private double[] coordinatesOfLastFall;
    private LocalDateTime timeOfLastAlert;
    private int notificationId;
    private final String CHANNEL_ID = "fall_alert_channel";

    public FallAlertHelper(Context context) {
        helper = new ProfileDataHelper(context);
        this.context = context;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Fall alert notifications", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notifications that a fall alert is about to be sent");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
        timeOfLastAlert = LocalDateTime.MIN;
    }

    public static synchronized FallAlertHelper getInstance(Context context) {
        // Create the helper as a singleton to make sure all classes have access to the same helper object
        if (instance == null) {
            instance = new FallAlertHelper(context);
        }
        return instance;
    }

    public void startAlertProcess() {
        // First try to record the location, proceed when that is done or directly if location is turned off
        coordinatesOfLastFall = new double[3];
        if (helper.isGpsEnabled()) {
            fetchCoordinates(new LocationCallback() {
                @Override
                public void onLocationReceived(double latitude, double longitude) {
                    coordinatesOfLastFall[0] = 1; // 1 in the first position means that the other two values are retrieved coordinates
                    coordinatesOfLastFall[1] = latitude;
                    coordinatesOfLastFall[2] = longitude;
                    proceedWithAlertProcess();
                }

                @Override
                public void onLocationFailed() {
                    coordinatesOfLastFall[0] = 0; // 0 in the first position means that the location was not recorded
                    coordinatesOfLastFall[1] = 0.0;
                    coordinatesOfLastFall[2] = 0.0;
                    proceedWithAlertProcess();
                }
            });
        } else {
            proceedWithAlertProcess();
        }
    }

    private void proceedWithAlertProcess() {
        // Log the fall in the database. Disabled is set to false from the beginning but might be changed later in the process.
        helper.logFall(LocalDate.now(), LocalTime.now(), coordinatesOfLastFall[1], coordinatesOfLastFall[2], false);

        sendNotification();

        // Tell MainActivity that a fall has been detected
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("FALL_DETECTED"));

        // Start a timer to send alert in 1 minute unless cancelled by the user
        CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Check every second if alert has been cancelled
                if (helper.getLastAlertDisabled()) {
                    NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                    notificationManager.cancel(notificationId);
                    cancel();
                }
            }

            @Override
            public void onFinish() {
                if (!helper.getLastAlertDisabled()) {
                    NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                    notificationManager.cancel(notificationId);
                    sendSmsAlert();
                }
            }
        };
        countDownTimer.start();
    }



    private void sendNotification() {
        // Create an intent for the notification to open the app
        notificationId = new Random().nextInt();
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntentOpen = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create an action button which will send a broadcast to cancel the alert
        Intent disableIntent = new Intent(context, AlertOptionsReceiver.class);
        disableIntent.setAction("ACTION_DISABLE_ALERT");
        disableIntent.putExtra("id", notificationId);
        PendingIntent pendingIntentDisable = PendingIntent.getBroadcast(context, 0, disableIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Create an action button which will send a broadcast to send the alert immediately
        Intent sendNowIntent = new Intent(context, AlertOptionsReceiver.class);
        sendNowIntent.setAction("ACTION_SEND_NOW");
        sendNowIntent.putExtra("id", notificationId);
        PendingIntent pendingIntentSendNow = PendingIntent.getBroadcast(context, 0, sendNowIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Build and send the notification

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.app_icon);
        builder.setContentTitle("Fall Detected");
        builder.setContentText("A fall alert will be sent to your contacts in 1 minute");
        builder.setContentIntent(pendingIntentOpen);
        builder.setAutoCancel(true);
        builder.addAction(0,"Cancel Alert", pendingIntentDisable);
        builder.addAction(0, "Send Now", pendingIntentSendNow);
        Notification notification = builder.build();

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(notificationId, notification);
    }

    public void sendSmsAlert() {
        // This prevents multiple alerts from accidentally being sent for the same incident
        if (Duration.between(timeOfLastAlert, LocalDateTime.now()).getSeconds() < 60) {
            return;
        }

        // Get a list of the phone numbers in the database
        Cursor cursor = helper.getContactsCursor();
        List<String> phoneNumbers = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                phoneNumbers.add(cursor.getString(2));
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Generate a message and send to all the contacts
        String alertMessage = generateSms(coordinatesOfLastFall);
        SmsManager smsManager = SmsManager.getDefault();
        for (String number : phoneNumbers) {
            smsManager.sendTextMessage(number, null, alertMessage, null, null);
        }
        timeOfLastAlert = LocalDateTime.now();
    }

    private String generateSms(double[] coordinates) {
        StringBuilder sb = new StringBuilder("FALLARM: ");
        sb.append(helper.getProfileName());
        sb.append(" may have had a fall incident. Contact them and see if they are okay.");

        // Add a Google Maps link if the first value of coordinates indicate that the location was recorded
        if (coordinates[0] == 1) {
            sb.append("\nLocation of fall: https://www.google.com/maps/search/?api=1&query=");
            sb.append(coordinates[1]);
            sb.append(",");
            sb.append(coordinates[2]);
        }
        return sb.toString();
    }


    private void fetchCoordinates(LocationCallback callback) {
        // Record the location and send it back to the calling method, or tell it if something went wrong
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    callback.onLocationReceived(location.getLatitude(), location.getLongitude());
                } else {
                    callback.onLocationFailed();
                }
            }).addOnFailureListener(e -> callback.onLocationFailed());
        } else {
            callback.onLocationFailed();
        }
    }

    // Interface for callback
    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onLocationFailed();
    }
}
