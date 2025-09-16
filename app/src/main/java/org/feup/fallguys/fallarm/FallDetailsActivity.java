package org.feup.fallguys.fallarm;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;


public class FallDetailsActivity extends AppCompatActivity {
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fall_details);

        // Set the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_icon);
            actionBar.setTitle("Fall Details");
        }

        // Display the information about the clicked fall
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String date = bundle.getString("date");
            String time = bundle.getString("time");
            latitude = bundle.getDouble("latitude");
            longitude = bundle.getDouble("longitude");
            boolean disabled = bundle.getBoolean("disabled");

            TextView dateView = findViewById(R.id.date);
            TextView timeView = findViewById(R.id.time);
            TextView alertInfoView = findViewById(R.id.alert_information);

            String dateInfo = "Date: " + date;
            String timeInfo = "Time: " + time;
            dateView.setText(dateInfo);
            timeView.setText(timeInfo);
            if (disabled) {
                alertInfoView.setText(R.string.alert_was_disabled);
            }
        }

        // Replace text with map if location is available. It is assumed that a fall at the exact coordinates 0, 0 will not happen.
        if (latitude != 0.0 && longitude != 0.0) {
            TextView gpsDisabledView = findViewById(R.id.gps_disabled_text);
            gpsDisabledView.setVisibility(View.GONE);

            // Set up the map
            MapView mapView = findViewById(R.id.map);
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue("org.feup.fallguys.fallarm/1.0");
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            GeoPoint fallLocation = new GeoPoint(latitude, longitude);
            mapView.getController().setCenter(fallLocation);

            // Add a marker
            Marker marker = new Marker(mapView);
            marker.setPosition(fallLocation);
            mapView.getController().setZoom(15.00);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        } else {
            org.osmdroid.views.MapView mapView = findViewById(R.id.map);
            mapView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


