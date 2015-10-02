package com.tbruyelle.rxpermissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.M)
public class ShadowActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            String[] permissions = getIntent().getStringArrayExtra("permissions");
            requestPermissions(permissions, 42);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        RxPermissions.getInstance(this).onRequestPermissionsResult(requestCode, permissions, grantResults);
        finish();
    }
}
