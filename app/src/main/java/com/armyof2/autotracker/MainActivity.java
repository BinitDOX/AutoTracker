package com.armyof2.autotracker;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import static com.armyof2.autotracker.SignInActivity.userUid;

public class MainActivity extends AppCompatActivity {

    public static String TRACK_ID;
    public static final int ERROR_DIALOG_REQUEST = 9001;
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002;
    public static final int PERMISSIONS_REQUEST_ENABLE_GPS = 9003;
    private static final String TAG = "MainActivity";
    private TextView ownID;
    private EditText trackId;
    private EditText phoneNo;
    private Intent intent;
    private Switch tSwitch;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Toast mToast;
    private boolean mLocationPermissionGranted = false;
    private ArrayList<String> TIDs;
    private ProgressDialog progress;
    private FusedLocationProviderClient mFusedLocationClient;
    public static String targetUid;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adjustFontScale(getResources().getConfiguration());
        adjustDisplayScale(getResources().getConfiguration());
        setContentView(R.layout.activity_main);

        trackId = (EditText) findViewById(R.id.et_trackid);
        ownID = (TextView) findViewById(R.id.tv_title2);
        phoneNo = (EditText) findViewById(R.id.et_phoneno);
        TIDs = new ArrayList<>();
        tSwitch = (Switch) findViewById(R.id.switch1);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
        intent = new Intent(this, MapActivity.class);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        progress = new ProgressDialog(this);
        progress.setMessage("Connecting to Server...");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCanceledOnTouchOutside(false);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        myRef.child(userUid);
        ownID.setText("Your Tracking ID:" + userUid);

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String value = dataSnapshot.getKey();
                TIDs.add(value);
                Log.d("TAG", "onChildAdded: " + TIDs);
                if(dataSnapshot.getKey().equals(userUid)) {
                    boolean val = dataSnapshot.child("Track Allow").getValue(Boolean.class);
                    tSwitch.setChecked(val);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getKey().equals(userUid)) {
                    boolean val = dataSnapshot.child("Track Allow").getValue(Boolean.class);
                    tSwitch.setChecked(val);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        tSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                myRef.child(userUid).child("Track Allow").setValue(isChecked);
            }

        });

    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called.");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult()!=null) {
                    Location location = task.getResult();
                    //GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "onComplete: latitude: " + location.getLatitude());
                    Log.d(TAG, "onComplete: longitude: " + location.getLongitude());
                    myRef.child(userUid).child("Track Latitude").setValue(location.getLatitude());
                    myRef.child(userUid).child("Track Longitude").setValue(location.getLongitude());
                }
            }
        });

    }

    public void onTrackButtonClicked(View view) {
        if (!isNetworkAvailable(this)) {
            Toast.makeText(this, "Are you connected to the internet?", Toast.LENGTH_SHORT).show();
            return;
        }
        if (trackId.getText().toString().equals("")) {
            Toast.makeText(this, "Please specify a tracking ID", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phoneNo.getText().toString().equals("")) {
            Toast.makeText(this, "Please specify a phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        progress.setMessage("Tracking the target...");
        progress.show();

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String value = dataSnapshot.getKey();
                if(!value.equals(trackId.getText().toString()))
                    badToast();
                else if(value.equals(trackId.getText().toString())&&Boolean.parseBoolean(dataSnapshot.child("Track Allow").getValue().toString()))
                    sendSMS("TRACK " + userUid);
                else
                    someToast();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void sendSMS(String msg){
        if(phoneNo.getText().toString().length()!=10) {
            Toast.makeText(this, "Invalid phone no.", Toast.LENGTH_SHORT).show();
            progress.hide();
            return;
        }
        try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNo.getText().toString(), null, msg, null, null);
                progress.hide();
                targetUid = trackId.getText().toString();
                goodToast();
        } catch (Exception ex) {
                ex.printStackTrace();
        }
    }

    public void copyToClipboard(View view){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", userUid);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), "Tracking ID Copied",Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    public void goodToast() {
        mToast.setText("Tracking Success!");
        progress.hide();
        mToast.show();
    }

    public void someToast() {
        mToast.setText("Target device's track state is set to false");
        progress.hide();
        mToast.show();
    }

    public void badToast() {
        mToast.setText("Target device could not be found!");
        progress.hide();
        mToast.show();
    }

    public static boolean isNetworkAvailable(Context con) {
        try {
            ConnectivityManager cm = (ConnectivityManager) con
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getLastKnownLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if(mLocationPermissionGranted){
                    getLastKnownLocation();
                }
                else{
                    getLocationPermission();
                }
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(checkMapServices()){
            if(mLocationPermissionGranted){
                getLastKnownLocation();
                Log.d(TAG, "onResume: gotit");
            }
            else{
                getLocationPermission();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent in = new Intent(this, SignInActivity.class);
        startActivity(in);
        finish();
    }

    public void adjustFontScale(Configuration configuration) {
        if (configuration != null && configuration.fontScale != 1.0) {
            configuration.fontScale = (float) 1.0;
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.fontScale * metrics.density;
            this.getResources().updateConfiguration(configuration, metrics);
        }
    }

    public void adjustDisplayScale(Configuration configuration) {
        if (configuration != null) {
            Log.d("TAG", "adjustDisplayScale: " + configuration.densityDpi);
            if (configuration.densityDpi >= 485)
                configuration.densityDpi = 500;
            else if (configuration.densityDpi >= 300)
                configuration.densityDpi = 400;
            else if (configuration.densityDpi >= 100)
                configuration.densityDpi = 200;
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.densityDpi * metrics.density;
            this.getResources().updateConfiguration(configuration, metrics);
        }
    }
}