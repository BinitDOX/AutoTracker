package com.armyof2.autotracker;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import static com.armyof2.autotracker.SignInActivity.userUid;

public class MainActivity extends AppCompatActivity {

    public static String TRACK_ID;
    private TextView ownID;
    private EditText trackId;
    private EditText phoneNo;
    private Intent intent;
    private Switch tSwitch;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Toast mToast;
    private ArrayList<String> TIDs;
    private ProgressDialog progress;


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
        //intent = new Intent(this, TrackActivity.class);

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
        progress.setMessage("Connecting to Target...");
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
        } catch (Exception ex) {
                ex.printStackTrace();
        }
    }

    public void copyToClipboard(View view){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", userUid);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), "Tracking ID Copied",Toast.LENGTH_SHORT).show();
    }

    public void goodToast() {
        mToast.setText("Server joined!");
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