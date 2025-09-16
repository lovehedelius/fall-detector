package org.feup.fallguys.fallarm;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        // Set the action bar
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.arrow_icon);
            actionBar.setTitle("Fall Log");
        }

        // Set up the list of falls
        ListView fallList = findViewById(R.id.fall_list);
        ProfileDataHelper helper = new ProfileDataHelper(this);
        Cursor cursor = helper.getFallHistoryCursor();
        Map<String, List<Fall>> fallsByMonth = parseFallsByMonth(cursor);
        FallHistoryAdapter adapter = new FallHistoryAdapter(this, fallsByMonth);
        fallList.setAdapter(adapter);

        // When a fall is clicked the information about it is packed in an intent and the details page is opened
        fallList.setOnItemClickListener((parent, view, position, id) -> {
            Object clickedItem = adapter.getItem(position);
            if (clickedItem instanceof Fall) {
                Fall fall = (Fall) clickedItem;
                Bundle bundle = new Bundle();
                bundle.putString("date", fall.getDate());
                bundle.putString("time", fall.getTime());
                bundle.putDouble("latitude", fall.getLatitude());
                bundle.putDouble("longitude", fall.getLongitude());
                bundle.putBoolean("disabled", fall.getDisabled());

                Intent intent = new Intent(HistoryActivity.this, FallDetailsActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    public Map<String, List<Fall>> parseFallsByMonth(Cursor cursor) {
        Map<String, List<Fall>> fallsByMonth = new LinkedHashMap<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        // Get the logged falls from the database with the most recent first
        if (cursor.moveToLast()) {
            do {
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                boolean disabled = cursor.getInt(cursor.getColumnIndexOrThrow("disabled")) == 1;
                String month = date.substring(0, 7);

                // Convert the date and time to the right format and add the fall to the map as a Fall object
                try {
                    Date parsedDate = inputFormat.parse(month);
                    String formattedMonth = outputFormat.format(parsedDate);
                    String formattedTime = time.substring(0, 5);
                    Fall fall = new Fall(date, formattedTime, latitude, longitude, disabled);

                    if (!fallsByMonth.containsKey(formattedMonth)) {
                        fallsByMonth.put(formattedMonth, new ArrayList<>());
                    }
                    fallsByMonth.get(formattedMonth).add(fall);
                } catch (Exception e) {
                    Log.e("History", "Error when adding month with falls to map");
                }
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        return fallsByMonth;
    }

    // A class to store all the data for a fall
    public class Fall {
        private final String date;
        private final String time;
        private final double latitude;
        private final double longitude;
        private boolean disabled;

        public Fall(String date, String time, double latitude, double longitude, boolean disabled) {
            this.date = date;
            this.time = time;
            this.latitude = latitude;
            this.longitude = longitude;
            this.disabled = disabled;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public boolean getDisabled() {
            return disabled;
        }
    }

    public class FallHistoryAdapter extends BaseAdapter {
        private final Context context;
        private final List<Object> items;

        public FallHistoryAdapter(Context context, Map<String, List<Fall>> fallsByMonth) {
            this.context = context;
            this.items = new ArrayList<>();
            for (Map.Entry<String, List<Fall>> entry : fallsByMonth.entrySet()) {
                items.add(entry.getKey().substring(0, entry.getKey().length() - 5)); // Add a month header to the list
                items.addAll(entry.getValue()); // Add falls to the list
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return (items.get(position) instanceof String) ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2; // Header and item types
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            if (viewType == 0) { // Header
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.month_header, parent, false);
                }
                TextView monthTextView = convertView.findViewById(R.id.month_header);
                monthTextView.setText((String) items.get(position));
            } else { // Fall item
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TextView text1 = convertView.findViewById(android.R.id.text1);
                TextView text2 = convertView.findViewById(android.R.id.text2);

                Fall fall = (Fall) items.get(position);
                text1.setText(fall.date);
                text2.setText(fall.time);
            }
            return convertView;
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