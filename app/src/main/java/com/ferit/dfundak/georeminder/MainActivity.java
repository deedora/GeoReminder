package com.ferit.dfundak.georeminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements OnCompleteListener<Void> {

    //UI
    private FloatingActionButton addButton;
    private ListView reminderList;

    //gepfence
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;
    LocationListener mLocationListener;
    LocationManager mLocationManager;

    //permissions
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    //variables
    ArrayList<reminderItem> reminders;
    public static final String MY_PREFS_NAME = "MyPrefsFile";
    private boolean fromApp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addButton = (FloatingActionButton) findViewById(R.id.add_button);
        reminderList = (ListView) this.findViewById(R.id.remindersLV);

        refreshDataset();

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddNewItem.class);
                intent.putExtra("ID", 0);
                startActivity(intent);
            }
        });

        reminderList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int i, long l) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setMessage("Would you like to remove selected reminder?")
                        .setCancelable(false)
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog, int id){

                                        reminderItem reminder = (reminderItem) adapterView.getItemAtPosition(i);
                                        deleteFromTable(reminder.getID());
                                        removeAlarm(reminder.getID());
                                        refreshDataset();
                                    }
                                });
                alertDialogBuilder.setNegativeButton("No",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = alertDialogBuilder.create();
                alert.show();

                return true;
            }
        });

        reminderList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                reminderItem reminder = (reminderItem) adapterView.getItemAtPosition(i);
                Intent intent = new Intent(MainActivity.this, AddNewItem.class);
                intent.putExtra("ID", reminder.getID());
                fromApp = false;
                removeAlarm(reminder.getID());
                startActivity(intent);
            }
        });

        this.mLocationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        this.mLocationListener = new SimpleLocationListener();

        mGeofencePendingIntent = null;
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofenceList = new ArrayList<>();
    }

    private void checkGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            GPSDisabledAlert();
        }
    }

    private void GPSDisabledAlert(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled on your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Go to Settings",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void removeAlarm(int id) {
        Log.i("dora", "id removed" + id);
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), id, intent, 0);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        removeGeofences();
    }

    //populate listView
    private void refreshDataset() {
        reminders = this.loadReminders();
        ReminderAdapter reminderAdapter = new ReminderAdapter(reminders);
        this.reminderList.setAdapter(reminderAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        Boolean fromNotification = prefs.getBoolean("fromNotification", false);
        populateGeofenceList();

        if (fromNotification == false) {
            if (!checkPermissions()) {
                requestPermissions();
            } else if (!mGeofenceList.isEmpty()) {
                checkGPS();
                startGeoFences();
                startTracking();
            } else {
                stopTracking();
            }
            refreshDataset();
        }
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("fromNotification", false);
        editor.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("fromNotification", false);
        editor.commit();
        stopTracking();
    }

    //database actions
    private ArrayList<reminderItem> loadReminders() {
        ArrayList<reminderItem> items = DatabaseHandler.getInstance(this).getAllReminders();
        return items;
    }

    private void deleteFromTable(int id) {
        DatabaseHandler.getInstance(this).deleteFromTable(id);
    }


    private GeofencingRequest getGeofencingRequest() {
        if (!mGeofenceList.isEmpty()) {
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
            builder.addGeofences(mGeofenceList);
            return builder.build();
        }
        return null;
    }

    public void startGeoFences() {
        if (!checkPermissions()) {
            mPendingGeofenceTask = PendingGeofenceTask.ADD;
            requestPermissions();
            return;
        }
        addGeofences();
    }

    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        if (getGeofencingRequest() != null) {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnCompleteListener(this);
        }
    }

    //start tracking location
    private void startTracking() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String locationProvider = this.mLocationManager.getBestProvider(criteria, true);
        long minTime = 1000;
        float minDistance = 10;
        this.mLocationManager.requestLocationUpdates(locationProvider, minTime, minDistance,
                this.mLocationListener);
    }

    private void stopTracking() {
        if (this.mLocationManager != null) {
            this.mLocationManager.removeUpdates(this.mLocationListener);
        }
    }

    private class SimpleLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }

    //remove geofence
    public void removeGeofences() {
        mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mGeofenceList.clear();
                        Log.i("dora", "REMOVED");

                        if (fromApp) {
                            populateGeofenceList();
                            startGeoFences();
                        } else {
                            fromApp = true;
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("dora", "FAILED TO REMOVE");
                    }
                });
    }

    //on add/remove show toast
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        mPendingGeofenceTask = PendingGeofenceTask.NONE;
        if (task.isSuccessful()) {
            updateGeofencesAdded(!getGeofencesAdded());

            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(Constants.GEOFENCES_ADDED_KEY, !getGeofencesAdded())
                    .apply();
        } else {
            Log.w("dora", "add/remove geofence error");
        }
    }

    private void updateGeofencesAdded(boolean added) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.GEOFENCES_ADDED_KEY, added)
                .apply();
    }

    private boolean getGeofencesAdded() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeoFenceTransitionsIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //populate list
    private void populateGeofenceList() {
        reminders = this.loadReminders();

        for (int i = 0; i < reminders.size(); i++) {
            int mId = reminders.get(i).getID();
            LatLng mPinnedLocation = reminders.get(i).getPinnedLocation();
            double mRadius = reminders.get(i).getRadius();
            String mDate = reminders.get(i).getDate();
            String mTime = reminders.get(i).getTime();

            if (mRadius == 0) {
                mRadius = 50;
            }

            long expirationInMilliseconds;
            long GEOFENCE_EXPIRATION_IN_HOURS = 336;

            expirationInMilliseconds = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;


            if (mPinnedLocation == null) {
                continue;
            } else {
                Log.i("dora", "add geofence " + mId + " " + mRadius + " ||" + mPinnedLocation.latitude + " " + mPinnedLocation.longitude + "|| " + mDate + " " + mTime);
                mGeofenceList.add(new Geofence.Builder()
                        .setRequestId(Integer.toString(mId))
                        .setCircularRegion(
                                mPinnedLocation.latitude,
                                mPinnedLocation.longitude,
                                (float) mRadius
                        )
                        .setExpirationDuration(expirationInMilliseconds)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build());
            }
        }

        if (mGeofenceList.isEmpty()) {
            stopTracking();
        }
    }

    private long getTimeInMs(String date, String time) {

        String[] partsDate = date.split("\\.");
        String[] partsTime = time.split(":");

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, Integer.parseInt(partsDate[2]));
        cal.set(Calendar.MONTH, Integer.parseInt(partsDate[1]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(partsDate[0]));
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(partsTime[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(partsTime[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void performPendingGeofenceTask() {
        if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences();
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            removeGeofences();
        }
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else return false;
    }

    private void requestPermissions() {
        String[] permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(this, permission, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("dora", "Location permission granted");
                        performPendingGeofenceTask();
                    } else {
                        Log.d("dora", "Location permission denyed");
                        askForPermission();
                    }
                    break;
                }
            }
        }
    }

    private void askForPermission() {
        boolean explain = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (explain) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Permission")
                    .setMessage("App needs your permission to give you full experience.")
                    .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions();
                            dialog.cancel();
                        }
                    })
                    .show();
        } else {
            Log.i("dora", "No LOCATION permission");
        }

    }
}
