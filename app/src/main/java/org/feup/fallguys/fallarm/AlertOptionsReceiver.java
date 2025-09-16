package org.feup.fallguys.fallarm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AlertOptionsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ProfileDataHelper helper = new ProfileDataHelper(context);
        FallAlertHelper fallAlert = FallAlertHelper.getInstance(context);
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("id", -1);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        // Listen for the broadcasts sent by the action buttons in the notification
        if ("ACTION_DISABLE_ALERT".equals(action)) {
            helper.setLastAlertDisabled(true);
            notificationManager.cancel(notificationId);
        } else if ("ACTION_SEND_NOW".equals(action)) {
            fallAlert.sendSmsAlert();
            notificationManager.cancel(notificationId);

            // Pass on the information to MainActivity that the alert is already sent
            Intent newIntent = new Intent("CANCEL_TIMER");
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(newIntent);
        }
    }
}
