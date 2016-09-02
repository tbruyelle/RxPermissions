package com.tbruyelle.rxpermissions;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShadowActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        List<String> perms = Arrays.asList(intent.getStringArrayExtra("permissions"));
        List<String> yetGrantedPerms = Collections.emptyList();

        // Figure out what permissions are not granted.
        for (String perm : perms) {
            int check = ContextCompat.checkSelfPermission(this,
                                                          perm);

            if (PackageManager.PERMISSION_GRANTED != check) {
                yetGrantedPerms.add(perm);
            }
        }

        // Request permissions if any.
        int permNum = yetGrantedPerms.size();
        if (permNum > 0) {
            ActivityCompat.requestPermissions(this,
                                              yetGrantedPerms.toArray(new String[permNum]),
                                              42);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        RxPermissions.getInstance(this).onRequestPermissionsResult(requestCode, permissions, grantResults);
        finish();
    }
}
