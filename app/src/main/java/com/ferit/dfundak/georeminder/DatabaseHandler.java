package com.ferit.dfundak.georeminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Dora on 17/06/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {

    private static DatabaseHandler mDatabaseHandler = null;


    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "geoReminder";
    private static final String TABLE_REMINDERS = "reminders";

    // Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";
    private static final String KEY_LAT = "latitude";
    private static final String KEY_LONG = "longitude";
    private static final String KEY_RADIUS = "radius";
    private static final String KEY_IMAGE_NAME = "imageName";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_AUDIO_NAME = "audioName";

    private DatabaseHandler(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHandler getInstance(Context context){
        if(mDatabaseHandler == null){
            mDatabaseHandler = new DatabaseHandler(context);
        }
        return mDatabaseHandler;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE);
        this.onCreate(db);
    }

    //SQL statements
    static final String CREATE_TABLE = "CREATE TABLE " + TABLE_REMINDERS + " (" +
            KEY_ID + " INTEGER PRIMARY KEY NOT NULL," +
            KEY_TITLE + " TEXT," +
            KEY_DESCRIPTION + " TEXT,"+
            KEY_DATE + " TEXT," +
            KEY_TIME + " TEXT," +
            KEY_LAT + " REAL," +
            KEY_LONG + " REAL,"+
            KEY_RADIUS + " REAL," +
            KEY_IMAGE_NAME + " TEXT, " +
            KEY_ADDRESS + " TEXT," +
            KEY_AUDIO_NAME + " TEXT);";

    static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_REMINDERS;

    static final String SELECT_ALL = "SELECT " + KEY_ID + "," + KEY_TITLE + "," + KEY_DESCRIPTION +
            ","+ KEY_DATE + "," + KEY_TIME + "," + KEY_LAT + "," + KEY_LONG + ","+ KEY_RADIUS + ","
            + KEY_IMAGE_NAME + ","+ KEY_ADDRESS + "," + KEY_AUDIO_NAME+ " FROM " + TABLE_REMINDERS;

    public void insertReminder(reminderItem reminder){
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ID, reminder.getID());
        contentValues.put(KEY_TITLE, reminder.getTitle());
        contentValues.put(KEY_DESCRIPTION, reminder.getDescription());
        contentValues.put(KEY_DATE, reminder.getDate());
        contentValues.put(KEY_TIME, reminder.getTime());
        contentValues.put(KEY_LAT, reminder.getLat());
        contentValues.put(KEY_LONG, reminder.getLong());
        contentValues.put(KEY_RADIUS, reminder.getRadius());
        contentValues.put(KEY_IMAGE_NAME, reminder.getImageName());
        contentValues.put(KEY_ADDRESS, reminder.getAddress());
        contentValues.put(KEY_AUDIO_NAME, reminder.getAudioName());

        SQLiteDatabase writableDatabase = this.getWritableDatabase();
        long rowInserted = writableDatabase.insert(TABLE_REMINDERS, KEY_ID, contentValues);
        if(rowInserted != -1)
            Log.i("dora", "New row added, row id: " + rowInserted);
        else
            Log.i("dora", "SMTH WENT WRONG" + rowInserted);
        writableDatabase.close();
    }

    public void updateReminder(reminderItem reminder, int idSelected){
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_ID, idSelected);
        contentValues.put(KEY_TITLE, reminder.getTitle());
        contentValues.put(KEY_DESCRIPTION, reminder.getDescription());
        contentValues.put(KEY_DATE, reminder.getDate());
        contentValues.put(KEY_TIME, reminder.getTime());
        contentValues.put(KEY_LAT, reminder.getLat());
        contentValues.put(KEY_LONG, reminder.getLong());
        contentValues.put(KEY_RADIUS, reminder.getRadius());
        contentValues.put(KEY_IMAGE_NAME, reminder.getImageName());
        contentValues.put(KEY_ADDRESS, reminder.getAddress());
        contentValues.put(KEY_AUDIO_NAME, reminder.getAudioName());

        SQLiteDatabase writableDatabase = this.getWritableDatabase();
        long rowUpdated = writableDatabase.update(TABLE_REMINDERS, contentValues, KEY_ID + "="+ idSelected, null);

        if(rowUpdated != -1)
            Log.i("dora", "row updated" + rowUpdated);
        else
            Log.i("dora", "SMTH WENT WRONG" + rowUpdated);
        writableDatabase.close();
    }

    public void deleteFromTable (int id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_REMINDERS + " WHERE " + KEY_ID + "='"+ id +"'");
        db.close();
    }

    public reminderItem selectFromTable (int idSelected){
        SQLiteDatabase db = this.getWritableDatabase();
        String SELECT_ID = ("SELECT * FROM " + TABLE_REMINDERS + " WHERE " + KEY_ID + "='"+ idSelected +"'");

        Cursor reminderCursor = db.rawQuery(SELECT_ID, null);
        reminderItem reminder = new reminderItem();
        if(reminderCursor.moveToFirst()){
            do{
                int id = reminderCursor.getInt(0);
                String title = reminderCursor.getString(1);
                String description = reminderCursor.getString(2);
                String date = reminderCursor.getString(3);
                String time = reminderCursor.getString(4);
                double lat = reminderCursor.getDouble(5);
                double lon = reminderCursor.getDouble(6);
                double radius = reminderCursor.getDouble(7);
                String imageName = reminderCursor.getString(8);
                LatLng pinnedLocation = new LatLng(lat, lon);
                String address = reminderCursor.getString(9);
                String audioName = reminderCursor.getString(10);
                if(lat == 0.0 && lon == 0.0){
                    pinnedLocation = null;
                }
                reminder = new reminderItem(id, pinnedLocation, radius, title, description, date, time, imageName, address, audioName);

            }while(reminderCursor.moveToNext());
        }
        reminderCursor.close();
        db.close();
        return reminder;
    }

    public ArrayList<reminderItem> getAllReminders(){
        SQLiteDatabase writableDatabase = this.getWritableDatabase();
        Cursor reminderCursor = writableDatabase.rawQuery(SELECT_ALL, null);
        ArrayList<reminderItem> reminders = new ArrayList<>();
        if(reminderCursor.moveToFirst()){
            do{
                int id = reminderCursor.getInt(0);
                String title = reminderCursor.getString(1);
                String description = reminderCursor.getString(2);
                String date = reminderCursor.getString(3);
                String time = reminderCursor.getString(4);
                double lat = reminderCursor.getDouble(5);
                double lon = reminderCursor.getDouble(6);
                double radius = reminderCursor.getDouble(7);
                String imageName = reminderCursor.getString(8);
                LatLng pinnedLocation = new LatLng(lat, lon);
                String address = reminderCursor.getString(9);
                String audioName = reminderCursor.getString(10);
                if(lat == 0.0 && lon == 0.0){
                    pinnedLocation = null;
                }
                reminders.add(new reminderItem(id, pinnedLocation, radius, title, description, date, time, imageName, address, audioName));

            }while(reminderCursor.moveToNext());
        }
        reminderCursor.close();
        writableDatabase.close();
        return reminders;
    }

}
