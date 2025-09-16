package org.feup.fallguys.fallarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private ProfileDataHelper helper;
    private boolean smsSentEarly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("FALL_DETECTED"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("CANCEL_TIMER"));

        // Start the service listening to the accelerometer, since we could not get it to keep running when the app is killed
        helper = new ProfileDataHelper(this);
        Intent serviceIntent = new Intent(this, AccelerometerListenerService.class);
        serviceIntent.putExtra("mac_address", helper.getDevice());
        startService(serviceIntent);

        TextView lastFallText = findViewById(R.id.last_fall_text);
        TextView lastFall = findViewById(R.id.last_fall_date);
        Button historyButton = findViewById(R.id.history_button);
        Button closeButton = findViewById(R.id.close_button);

        // Modify the screen based on if a fall has been detected or not
        String date = helper.getDateOfLastFall();
        String time = helper.getTimeOfLastFall();
        if (date == null || time == null) {
            lastFallText.setText(R.string.no_falls_ever);
            historyButton.setVisibility(View.GONE);
        } else {
            LocalDateTime dateTime = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME));
            Duration timeSinceFall = Duration.between(dateTime, LocalDateTime.now());
            if (timeSinceFall.toMinutes() <= 60) { // A fall within the last hour is considered still relevant and the user is informed about it upon opening
                changeScreenToFallDetected(timeSinceFall.getSeconds());
            } else {
                lastFall.setText(date);
            }
        }

        // Set up the history and close buttons

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        closeButton.setOnClickListener(v -> {
            LinearLayout fallRecentlyLayout = findViewById(R.id.fall_recently);
            LinearLayout defaultLayout = findViewById(R.id.default_main);
            fallRecentlyLayout.setVisibility(View.GONE);
            defaultLayout.setVisibility(View.VISIBLE);
            lastFallText.setText(R.string.last_fall_was);
            lastFall.setText(helper.getDateOfLastFall());
            historyButton.setVisibility(View.VISIBLE);
        });
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        // Listens to if the service detects a fall or if the "send now" action button is pressed
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("FALL_DETECTED".equals(intent.getAction())) {
                changeScreenToFallDetected(0);
            } else if ("CANCEL_TIMER".equals(intent.getAction())) {
                smsSentEarly = true;
            }
        }
    };

    private void changeScreenToFallDetected(long secondsSinceFall) {
        LinearLayout defaultLayout = findViewById(R.id.default_main);
        LinearLayout fallRecentlyLayout = findViewById(R.id.fall_recently);
        LinearLayout fallDetectedLayout = findViewById(R.id.fall_detected);
        TextView recentFallText = findViewById(R.id.recent_fall);
        TextView fallTimeView = findViewById(R.id.recent_fall_time);
        TextView contactsAlertedView = findViewById(R.id.contacts_alerted_text);

        // Change the text and the layout of the screen
        recentFallText.setText(R.string.fall_detected);
        defaultLayout.setVisibility(View.GONE);
        fallRecentlyLayout.setVisibility(View.GONE);

        if (secondsSinceFall <= 60 && !helper.getLastAlertDisabled() && !smsSentEarly) { // The fall alert process is ongoing
            TextView timerView = findViewById(R.id.timer);
            Button disableButton = findViewById(R.id.disable_alert_button);

            // Change to the layout containing the timer and define it
            fallDetectedLayout.setVisibility(View.VISIBLE);
            long millisUntilAlert = (60 - secondsSinceFall) * 1000;
            CountDownTimer countDownTimer = new CountDownTimer(millisUntilAlert, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Check every second if the alert was cancelled or already sent
                    if (helper.getLastAlertDisabled() || smsSentEarly) {
                        cancel();
                        fallDetectedLayout.setVisibility(View.GONE);
                        fallRecentlyLayout.setVisibility(View.VISIBLE);
                        fallTimeView.setText(helper.getTimeOfLastFall().substring(0, 5));
                        String alertSentOrDisabled = helper.getLastAlertDisabled() ? "The alert to your contacts was cancelled" : "Your emergency contacts have been alerted";
                        contactsAlertedView.setText(alertSentOrDisabled);
                    }

                    // Format the numbers on the timer
                    int seconds = (int) millisUntilFinished / 1000;
                    int minutes = seconds / 60;
                    seconds = seconds % 60;
                    timerView.setText(String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds));
                }

                @Override
                public void onFinish() {
                    // If the timer runs out without being cancelled the alert was sent
                    fallDetectedLayout.setVisibility(View.GONE);
                    fallRecentlyLayout.setVisibility(View.VISIBLE);
                    fallTimeView.setText(helper.getTimeOfLastFall().substring(0, 5));
                    contactsAlertedView.setText(R.string.contacts_alerted);
                }
            };
            countDownTimer.start();

            // Set up the button to stop the alert process and change the screen
            disableButton.setOnClickListener(v -> {
                countDownTimer.cancel();
                helper.setLastAlertDisabled(true);
                fallDetectedLayout.setVisibility(View.GONE);
                fallRecentlyLayout.setVisibility(View.VISIBLE);
                fallTimeView.setText(helper.getTimeOfLastFall().substring(0, 5));
                contactsAlertedView.setText(R.string.alert_cancelled);
            });
        } else { // The process is not ongoing but the fall happened within the last hour
            fallRecentlyLayout.setVisibility(View.VISIBLE);
            fallTimeView.setText(helper.getTimeOfLastFall().substring(0, 5));
            if (helper.getLastAlertDisabled()) {
                contactsAlertedView.setText(R.string.alert_cancelled);
            } else {
                contactsAlertedView.setText(R.string.contacts_alerted);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}