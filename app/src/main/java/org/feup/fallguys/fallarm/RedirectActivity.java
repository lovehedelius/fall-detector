package org.feup.fallguys.fallarm;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class RedirectActivity extends AppCompatActivity {
    ProfileDataHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new ProfileDataHelper(this);

        // Take the user to the setup if it is the first time using the app, otherwise the home page
        if (helper.isSetUp()) {
            Intent intent = new Intent(RedirectActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(RedirectActivity.this, NameActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.close();
    }
}