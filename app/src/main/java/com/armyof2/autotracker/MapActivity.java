package com.armyof2.autotracker;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import static com.armyof2.autotracker.MainActivity.targetUid;
import static com.armyof2.autotracker.SignInActivity.userUid;

/**
 * This shows how to create a simple activity with a raw MapView and add a marker to it. This
 * requires forwarding all the important lifecycle methods onto MapView.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mMapView;
    private GoogleMap googleMap;
    private LatLngBounds mapBoundry;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private MarkerOptions marker;
    private Marker cMarker;
    private double lati;
    private double longi;
    private boolean thr = true;
    private boolean x = true;



    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.onCreate(mapViewBundle);
        marker = new MarkerOptions().position(new LatLng(lati, longi)).title("Target");

        mMapView.getMapAsync(this);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getKey().equals(targetUid)) {
                    lati = (double) dataSnapshot.child("Track Latitude").getValue();
                    longi = (double) dataSnapshot.child("Track Longitude").getValue();
                    Log.d("TAG", "onCA: " + lati);
                    Log.d("TAG", "onCA: " + longi);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getKey().equals(targetUid)) {
                    lati = (double) dataSnapshot.child("Track Latitude").getValue();
                    longi = (double) dataSnapshot.child("Track Longitude").getValue();
                    Log.d("TAG", "onCC: " + lati);
                    Log.d("TAG", "onCC: " + longi);
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
    }

    private void setCamera(){
        double bottomBound = lati - .1;
        double leftBound = longi - .1;
        double topBound = lati + .1;
        double rightBound = longi + .1;
        mapBoundry = new LatLngBounds(
                new LatLng(bottomBound, leftBound),
                new LatLng(topBound, rightBound)
        );
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBoundry, 0));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d("TAG", "onMapReady: " + lati);
        Log.d("TAG", "onMapReady: " + longi);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap = map;
        googleMap.setMyLocationEnabled(true);
        cMarker = googleMap.addMarker(marker);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public void update(){
        Log.d("TAG", "update: working!");
        cMarker.setPosition(new LatLng(lati,longi));
        if(x)
            setCamera();
        x = false;
    }

    Runnable runnable = new Runnable() {
        public void run() {
            while (true) {

                try {
                    Thread.sleep(2000);
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            update();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onBackPressed() {
        thr = false;
        finish();
    }
}
