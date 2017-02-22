package com.xdja.zdsb.view;

import com.xdja.zdsb.utils.Zzlog;

import android.content.Intent;
import android.os.Bundle;

public class PassportActivity  extends IDCardRecognizeActivity{

    private static final String TAG = "PassportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Zzlog.out(TAG, "onCreate()");
        Intent intent = getIntent();;
        // 13 Passport
        intent.putExtra("nMainId", 13);
    }

}
