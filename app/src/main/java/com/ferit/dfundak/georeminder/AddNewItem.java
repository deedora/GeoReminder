package com.ferit.dfundak.georeminder;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class AddNewItem extends AppCompatActivity {

    //permissions
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_STORAGE_WRITING = 3;

    //UI
    private LinearLayout addTime;
    private LinearLayout addLocation;
    private LinearLayout addImage;
    private TextView locationAddress;
    private ImageView imageView;

    //Intent
    public static final int KEY_REQUEST_LOCATION = 10;
    public static final double KEY_LAT = 0;
    public static final double KEY_LNG = 0;
    public static final float KEY_RADIUS = 1;

    //
    private String mCurrentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_item);

        addImage = (LinearLayout) findViewById(R.id.add_image);
        addLocation = (LinearLayout) findViewById(R.id.add_location);
        addTime = (LinearLayout) findViewById(R.id.add_time);
        locationAddress = (TextView) findViewById(R.id.location_textView);
        imageView = (ImageView) findViewById(R.id.image_view);


        addLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkPermission("LOCATION")) {
                    requestPermission("LOCATION");
                }else{
                    startLocationActivity();
                }
            }
        });

        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkPermission("CAMERA") || !checkPermission("STORAGE")) {
                    if(!checkPermission("CAMERA")){
                        requestPermission("CAMERA");
                    }
                    if(!checkPermission("STORAGE")){
                        requestPermission("STORAGE");
                    }
                }else{
                    try {
                        takePicture();
                    } catch (IOException e) {
                        Log.e("dora", "Can't take picture");
                    }
                }
            }
        });

        addTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTime();
            }
        });

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void setTime() {
        
    }

    private void startLocationActivity() {
        Intent intent = new Intent(AddNewItem.this, LocationActivity.class);
        this.startActivityForResult(intent, KEY_REQUEST_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case KEY_REQUEST_LOCATION:
                if(resultCode == RESULT_OK) {
                    if( data.getExtras() != null) {
                        double lat = data.getExtras().getDouble("KEY_LAT");
                        double lng = data.getExtras().getDouble("KEY_LNG");
                        float radius = data.getExtras().getFloat("KEY_RADIUS");
                        Log.i("dora ", lat +" "+ lng + "  " + radius);
                        LatLng location = new LatLng(lat, lng);
                        setLocationAddress(location);
                    }
                    else Log.i("dora", "fail");
                    break;
                }
            case REQUEST_IMAGE_CAPTURE:
                if(resultCode == RESULT_OK) {
                    Uri imageUri = Uri.parse(mCurrentPhotoPath);
                    File file = new File(imageUri.getPath());
                    try {
                        //show image in ImageView
                        InputStream ims = new FileInputStream(file);
                        imageView.setImageBitmap(BitmapFactory.decodeStream(ims));
                    } catch (FileNotFoundException e) {
                        return;
                    }
                    MediaScannerConnection.scanFile(AddNewItem.this,
                            new String[]{imageUri.getPath()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                }
                            });
                    break;
                }
        }
    }

    private void requestPermission(String key){
        switch (key) {
            case "LOCATION": {
                Log.i("dora", "request location permission");
                String[] permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(AddNewItem.this, permission, REQUEST_LOCATION_PERMISSION);
                break;
            }
            case "CAMERA": {
                Log.i("dora", "request camera permission");
                String[] permission = new String[]{Manifest.permission.CAMERA};
                ActivityCompat.requestPermissions(AddNewItem.this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
                break;
            }
            case "STORAGE": {
                Log.i("dora", "request storage permission");
                String[] permission = new String[]{Manifest.permission.CAMERA};
                ActivityCompat.requestPermissions(AddNewItem.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_WRITING);
                break;
            }
        }
    }

    private boolean checkPermission(String key){
        switch (key) {
            case "LOCATION": {
                if(ActivityCompat.checkSelfPermission(AddNewItem.this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                    return true;
                }
                else return false;
            }
            case "CAMERA": {
                if(ActivityCompat.checkSelfPermission(AddNewItem.this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED){
                    return true;
                }
                else return false;
            }
            case "STORAGE": {
                if(ActivityCompat.checkSelfPermission(AddNewItem.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                    return true;
                }
                else return false;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("dora", "Location permission granted");
                        startLocationActivity();
                    } else {
                        Log.d("dora", "Location permission denyed");
                        askForPermission("LOCATION");
                    }
                    break;
                }
            }
            case REQUEST_IMAGE_CAPTURE: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("dora", "Camera permission granted");
                        if(checkPermission("STORAGE")) {
                            try {
                                takePicture();
                            } catch (IOException e) {
                                Log.e("dora", "Can't take picture");
                            }
                        }
                        else {
                            requestPermission("STORAGE");
                        }
                    } else {
                        Log.d("dora", "Camera permission denyed");
                        askForPermission("CAMERA");
                    }
                    break;
                }
            }
            case REQUEST_STORAGE_WRITING: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("dora", "Storage permission granted");
                        if(checkPermission("CAMERA")){
                            try {
                                takePicture();
                            } catch (IOException e) {
                                Log.e("dora", "Can't take picture");
                            }
                        }
                        else{
                            requestPermission("CAMERA");
                        }
                    } else {
                        Log.d("dora", "Storage permission denyed");
                        askForPermission("STORAGE");
                    }
                    break;
                }
            }
        }
    }

    private void askForPermission(String key){
        switch (key) {
            case "LOCATION": {
                boolean explain = ActivityCompat.shouldShowRequestPermissionRationale(AddNewItem.this, android.Manifest.permission.ACCESS_FINE_LOCATION);
                if (explain) {
                    this.displayDialog("LOCATION");
                } else {
                    Log.i("dora", "No permission");
                }
                break;
            }
            case "CAMERA": {
                boolean explain = ActivityCompat.shouldShowRequestPermissionRationale(AddNewItem.this, android.Manifest.permission.CAMERA);
                if (explain) {
                    this.displayDialog("CAMERA");
                } else {
                    Log.i("AddNewItem", "No permission");
                }
                break;
            }
            case "STORAGE": {
                boolean explain = ActivityCompat.shouldShowRequestPermissionRationale(AddNewItem.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (explain) {
                    this.displayDialog("STORAGE");
                } else {
                    Log.i("AddNewItem", "No permission");
                }
            }
        }
    }

    private void displayDialog(final String key) {
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
                        switch (key) {
                            case "LOCATION": {
                                requestPermission("LOCATION");
                                dialog.cancel();
                            }
                            case "CAMERA": {
                                requestPermission("CAMERA");
                                dialog.cancel();
                            }
                            case "STORAGE": {
                                requestPermission("STORAGE");
                                dialog.cancel();
                            }
                        }
                    }
                })
                .show();
    }

    void setLocationAddress(LatLng latLng){
        if (Geocoder.isPresent()) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> nearByAddresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (nearByAddresses.size() > 0) {
                    locationAddress.setText("");
                    StringBuilder stringBuilder = new StringBuilder();
                    Address nearestAddress = nearByAddresses.get(0);
                    stringBuilder
                            .append(nearestAddress.getAddressLine(0))
                            .append(", ")
                            .append(nearestAddress.getLocality())
                            .append(", ")
                            .append(nearestAddress.getCountryName());
                    locationAddress.append(stringBuilder.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Uri photoURI;
    private void takePicture() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Check if there is camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File for photo
            File filePhoto = null;
            try {
                filePhoto = createImageFile();
            } catch (IOException e) {
                Log.e("Error.", "" + e);
                return;
            }
            if (filePhoto != null) {
                photoURI = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", createImageFile());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {

        String imageFileName = "GeoReminder_" + System.currentTimeMillis()+".jpg";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

}
