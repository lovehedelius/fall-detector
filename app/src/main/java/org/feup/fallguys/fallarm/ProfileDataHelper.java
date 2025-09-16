package org.feup.fallguys.fallarm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ProfileDataHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "fallarm.db";
    private static final int SCHEMA_VERSION = 1;

    public ProfileDataHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the three tables used to store all the necessary persistent data
        db.execSQL("CREATE TABLE profile (_id INTEGER PRIMARY KEY, name TEXT, accelerometer TEXT, gps INTEGER)");
        db.execSQL("CREATE TABLE contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phoneNumber TEXT)");
        db.execSQL("CREATE TABLE falls (_id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, time TEXT, latitude REAL, longitude REAL, disabled INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Useful for testing but not used in the final app
    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM profile");
        db.execSQL("DELETE FROM contacts");
        db.execSQL("DELETE FROM falls");
        db.close();
    }

    public void setUpProfile(String name) {
        // Adds the users name to the database, the accelerometer is added later
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("accelerometer", "No device registered");
        cv.put("gps", 1); // Location is turned on by default
        getWritableDatabase().insert("profile", "name", cv);
    }

    public boolean isSetUp() {
        // Check if the profile table has a row, meaning the user has at least added a name
        Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM profile", null);
        boolean isSetUp = false;
        if (cursor.moveToFirst()) {
            isSetUp = cursor.getInt(0) > 0;
        }
        cursor.close();
        return isSetUp;
    }

    public String getProfileName() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT name FROM profile LIMIT 1", null);
        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        return name;
    }

    public void changeName(String newName) {
        ContentValues cv = new ContentValues();
        cv.put("name", newName);
        getWritableDatabase().update("profile", cv, null, null);
    }

    public void setDevice(String macAddress) {
        ContentValues cv = new ContentValues();
        cv.put("accelerometer", macAddress);
        getWritableDatabase().update("profile", cv, null, null);
    }

    public String getDevice() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT accelerometer FROM profile LIMIT 1", null);
        String device = null;
        if (cursor.moveToFirst()) {
            device = cursor.getString(0);
        }
        cursor.close();
        return device;
    }

    public void changeGpsEnabled() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT gps FROM profile LIMIT 1", null);
        int gpsBefore = 1;
        if (cursor.moveToFirst()) {
            gpsBefore = cursor.getInt(0);
        }
        cursor.close();
        ContentValues cv = new ContentValues();
        int gpsNew = (gpsBefore + 1) % 2; // Turns 0 to 1 and 1 to 0
        cv.put("gps", gpsNew);
        getWritableDatabase().update("profile", cv, null, null);
    }

    public boolean isGpsEnabled() {
        // If GPS is turned on or off is stored as an integer where 1 represents true and 0 represents false
        Cursor cursor = getReadableDatabase().rawQuery("SELECT gps FROM profile LIMIT 1", null);
        int gps = 1;
        if (cursor.moveToFirst()) {
            gps = cursor.getInt(0);
        }
        cursor.close();
        return gps == 1;
    }

    public void addContact(String name, String phoneNumber){
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phoneNumber", phoneNumber);
        getWritableDatabase().insert("contacts", "name", cv);
    }

    public void deleteContact(int contactId) {
        String[] idAsStringArray = new String[] {String.valueOf(contactId)};
        getWritableDatabase().delete("contacts", "_id = ?", idAsStringArray);
    }

    public boolean hasContact() {
        // Check if the contacts table has at least one row
        Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM contacts", null);
        boolean hasContact = false;
        if (cursor.moveToFirst()) {
            hasContact = cursor.getInt(0) > 0;
        }
        cursor.close();
        return hasContact;
    }

    public Cursor getContactsCursor() {
        return getReadableDatabase().rawQuery("SELECT * FROM contacts", null);
    }

    public void logFall(LocalDate date, LocalTime time, double latitude, double longitude, boolean disabled) {
        // Save all the data of the new fall in the database
        String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String formattedTime = time.format(DateTimeFormatter.ISO_LOCAL_TIME);
        int disabledInt = disabled ? 1 : 0; // If the alert was disabled or not is stored as an integer where 1 represents true and 0 represents false
        ContentValues cv = new ContentValues();
        cv.put("date", formattedDate);
        cv.put("time", formattedTime);
        cv.put("latitude", latitude);
        cv.put("longitude", longitude);
        cv.put("disabled", disabledInt);
        getWritableDatabase().insert("falls", "date", cv);
    }

    public Cursor getFallHistoryCursor() {
        return getReadableDatabase().rawQuery("SELECT * FROM falls", null);
    }

    public String getDateOfLastFall() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT date FROM falls ORDER BY _id DESC LIMIT 1", null); // Finds the last entry in the fall table by sorting on the primary key
        String date = null;
        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        return date;
    }

    public String getTimeOfLastFall() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT time FROM falls ORDER BY _id DESC LIMIT 1", null); // Finds the last entry in the fall table by sorting on the primary key
        String time = null;
        if (cursor.moveToFirst()) {
            time = cursor.getString(0);
        }
        cursor.close();
        return time;
    }

    public void setLastAlertDisabled(boolean disabled) {
        // If the alert was disabled or not is stored as an integer where 1 represents true and 0 represents false
        ContentValues cv = new ContentValues();
        int disabledInt = disabled ? 1 : 0;
        cv.put("disabled", disabledInt);
        getWritableDatabase().update("falls", cv, "_id = (SELECT MAX(_id) FROM falls)", null); // Updates only the last entry by finding the highest primary key
    }

    public boolean getLastAlertDisabled() {
        // If the alert was disabled or not is stored as an integer where 1 represents true and 0 represents false
        Cursor cursor = getReadableDatabase().rawQuery("SELECT disabled FROM falls ORDER BY _id DESC LIMIT 1", null);
        boolean disabled = false;
        if (cursor.moveToFirst()) {
            disabled = cursor.getInt(0) == 1;
        }
        cursor.close();
        return disabled;
    }
}