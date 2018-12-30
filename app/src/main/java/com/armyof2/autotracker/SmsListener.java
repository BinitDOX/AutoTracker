package com.armyof2.autotracker;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.armyof2.autotracker.SignInActivity.userUid;

public class SmsListener extends BroadcastReceiver {

    // Get the object of SmsManager
    private SharedPreferences preferences;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    public static boolean highFlag=false;
    String Channel_ID="Channel";
    public static String smsHostId[];
    NotificationManager notificationManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
            SmsMessage[] msgs = null;
            int Flag=0;
            String comp=" TRACK";
            String msg_from;
            String msgBody=" ";
            if (bundle != null){
                //---retrieve the SMS message received---
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for(int i=0; i<msgs.length; i++)
                    {
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        msgBody += msgs[i].getMessageBody();
                    }
                    Log.d("TAG" ,"onReceive: "+ msgBody);
                    //Toast.makeText(context, "Message received", Toast.LENGTH_SHORT).show();
                    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                    Flag=wifi.getWifiState();



                }catch(Exception e){
//                            Log.d("Exception caught",e.getMessage());
                }
            }

            if(msgBody.contains(comp)) {
                smsHostId = msgBody.split(" ");
                Log.d("TAG", "onReceive: " + smsHostId[2]);

                myRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.getKey().equals(userUid)) {
                            String value = dataSnapshot.child("Track User Response").getValue().toString();
                            Log.d("TAG", "onChildAddedListener: " + value);
                            if (value.equals("Awaiting"))
                                highFlag = true;
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.getKey().equals(userUid)) {
                            String value = dataSnapshot.child("Track User Response").getValue().toString();
                            Log.d("TAG", "onChildAddedListener: " + value);
                            if (value.equals("Awaiting"))
                                highFlag = true;
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

                final WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                wifi.setWifiEnabled(true);
                Toast.makeText(context, "Tracker Enabled", Toast.LENGTH_SHORT).show();
                if (Flag == 0 || Flag == 1) {
                    new CountDownTimer(20000, 1000) {

                        public void onTick(long millisUntilFinished) {
                            wifi.setWifiEnabled(true);
                        }

                        public void onFinish() {
                            wifi.setWifiEnabled(false);
                        }
                    }.start();
                }

            }
        }
    }
}
