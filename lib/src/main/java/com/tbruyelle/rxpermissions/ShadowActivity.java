package com.tbruyelle.rxpermissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.M)
public class ShadowActivity extends Activity {
    private boolean[] shouldShowRequestPermissionRationale;

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
        String[] permissions = intent.getStringArrayExtra("permissions");
        shouldShowRequestPermissionRationale = new boolean[permissions.length];

        for (int i = 0; i < permissions.length; i++) {
            shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
        }

        requestPermissions(permissions, 42);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        RxPermissions.getInstance(this).onRequestPermissionsResult(requestCode, permissions, grantResults, shouldShowRequestPermissionRationale);
        finish();
    }
}
