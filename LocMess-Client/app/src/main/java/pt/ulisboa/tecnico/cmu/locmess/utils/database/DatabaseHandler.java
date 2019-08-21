package pt.ulisboa.tecnico.cmu.locmess.utils.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;

import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils.toInterestsJson;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils.toInterestsList;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils.toLocationJson;
import static pt.ulisboa.tecnico.cmu.locmess.utils.LocMessJsonUtils.toLocationLocMessObj;

public class DatabaseHandler extends SQLiteOpenHelper {

    public static final String DB_NAME = "locMess.db";
    private static final int DB_VERSION = 5;

    private static final String TABLE_LOCATION = "Location";
    private static final String TABLE_INTEREST = "Interests";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_NAME_INTEREST = "nameInterest";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_RADIUS = "radius";
    private static final String COLUMN_SSID = "ssid";
    private static final String TAG = "LOCMESSDATABASE";
    private static final String COLUMN_ID = "id";
    private final DateTimeFormatter mDateTimeFormatterClient = DateTimeFormat.forPattern("dd/MM/yyy \'at\' HH:mm");

    public DatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        createTableLocation(db);
        createTableInterest(db);


    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INTEREST);

        onCreate(db);
    }


    private void createTableLocation(SQLiteDatabase db) {
        String query2 = "CREATE TABLE " + TABLE_LOCATION + "(" +
                COLUMN_URL + " TEXT , " +
                COLUMN_ID + " INT PRIMARY KEY , " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL," +
                COLUMN_RADIUS + " REAL," +
                COLUMN_SSID + " REAL" +

                ");";
        db.execSQL(query2);
    }


    public ArrayList<LocationLocMess> getAllLocations() {
        ArrayList<LocationLocMess> listLocations = new ArrayList<LocationLocMess>();

        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_LOCATION;
        int counter = 0;

        Cursor c = db.rawQuery(query, null);
        while (c.moveToNext()) {
            LocationLocMess entry = null;
            String column = c.getString(c.getColumnIndex(COLUMN_TYPE));

            if (column.equals(LocationLocMess.LocationType.GPS.getType())) {
                entry = new LocationLocMess(
                        c.getString(c.getColumnIndex(COLUMN_URL)),
                        c.getInt(c.getColumnIndex(COLUMN_ID)),
                        c.getString(c.getColumnIndex(COLUMN_NAME)),
                        c.getFloat(c.getColumnIndex(COLUMN_LATITUDE)),
                        c.getFloat(c.getColumnIndex(COLUMN_LONGITUDE)),
                        c.getFloat(c.getColumnIndex(COLUMN_RADIUS))
                );
            }
            if (column.equals(LocationLocMess.LocationType.WIFI.getType())) {
                entry = new LocationLocMess(
                        c.getString(c.getColumnIndex(COLUMN_URL)),
                        c.getInt(c.getColumnIndex(COLUMN_ID)),
                        LocationLocMess.LocationType.WIFI,
                        c.getString(c.getColumnIndex(COLUMN_NAME)),
                        c.getString(c.getColumnIndex(COLUMN_SSID))
                );
            }
            if (column.equals(LocationLocMess.LocationType.BLE.getType())) {
                entry = new LocationLocMess(
                        c.getString(c.getColumnIndex(COLUMN_URL)),
                        c.getInt(c.getColumnIndex(COLUMN_ID)),
                        LocationLocMess.LocationType.BLE,
                        c.getString(c.getColumnIndex(COLUMN_NAME)),
                        c.getString(c.getColumnIndex(COLUMN_SSID))
                );
            }
            listLocations.add(entry);
            counter++;
        }
        Log.d("DB-GetAllLocations", "count :" + counter);

        db.close();
        c.close();
        return listLocations;
    }

    public void insertLocation(LocationLocMess location) {

        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        values.put(COLUMN_URL, location.getUrl());
        values.put(COLUMN_ID, location.getId());
        values.put(COLUMN_NAME, location.getName());
        String type = location.getType();
        values.put(COLUMN_TYPE, type);


        if (type.equals(LocationLocMess.LocationType.GPS.toString())) {
            values.put(COLUMN_LATITUDE, location.getLatitude());
            values.put(COLUMN_LONGITUDE, location.getLongitude());
            values.put(COLUMN_RADIUS, location.getRadius());
        } else {
            values.put(COLUMN_SSID, location.getSsid());

        }
        try {
            db.insertOrThrow(TABLE_LOCATION, null, values);
            Log.d("DB-insertLocation", "One More Location");


        } catch (android.database.sqlite.SQLiteConstraintException e) {
            //Repeated URL
        }
        db.close();

    }

    public void removeAllLocations() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LOCATION, null, null);
        db.close();
    }

    public void removeLocation(LocationLocMess location) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_LOCATION + " WHERE " + COLUMN_ID + "='" + location.getId() + "'");
        db.close();
    }

    private void createTableInterest(SQLiteDatabase db) {
        String query3 = "CREATE TABLE " + TABLE_INTEREST + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME_INTEREST + " TEXT " +

                ");";
        db.execSQL(query3);
    }

   public void insertInterest(InterestLocMess interest) {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME_INTEREST, interest.getName());

        try {
            db.insertOrThrow(TABLE_INTEREST, null, values);
            Log.d("DB-insertInterest", "One More Interest");

        } catch (android.database.sqlite.SQLiteConstraintException e) {
            //Repeated URL
        }
        db.close();
    }

    public ArrayList<InterestLocMess> getAllInterestLocMess() {
        ArrayList<InterestLocMess> listInterests = new ArrayList<>();

        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_INTEREST;

        Cursor c = db.rawQuery(query, null);
        int counter = 0;
        while (c.moveToNext()) {
            InterestLocMess entry = null;

            entry = new InterestLocMess(
                    c.getString(c.getColumnIndex(COLUMN_NAME_INTEREST))
            );
            listInterests.add(entry);
            counter++;
        }
        Log.d("DB-GetAllInterest", "count :" + counter);


        db.close();
        c.close();
        return listInterests;
    }
}
