package com.armyof2.autotracker;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.armyof2.autotracker.SignInActivity.userUid;
import static com.armyof2.autotracker.SmsListener.smsHostId;

public class CustomDialogBox extends Dialog implements
        android.view.View.OnClickListener {

    public Activity c;
    public Dialog d;
    public Button yes, no;
    public CheckBox checkBox;
    public FirebaseDatabase database;
    public DatabaseReference myRef;

    public CustomDialogBox(Activity a) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog);
        yes = (Button) findViewById(R.id.btn_yes);
        no = (Button) findViewById(R.id.btn_no);
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);
        checkBox.setOnClickListener(this);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_yes:
                myRef.child(userUid).child("Track User Response").setValue("Allowed");
                dismiss();
                break;
            case R.id.btn_no:
                myRef.child(userUid).child("Track User Response").setValue("Denied");
                dismiss();
                break;
            case R.id.checkBox:
                if(checkBox.isChecked()){
                    myRef.child(userUid).child("Track Always Allow").child(smsHostId[2]).setValue(true);
                }else{
                    myRef.child(userUid).child("Track Always Allow").child(smsHostId[2]).setValue(false);
                }
            default:
                break;
        }
    }
}
