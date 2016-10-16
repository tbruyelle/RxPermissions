package com.tbruyelle.rxpermissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

@TargetApi(Build.VERSION_CODES.M)
public class ShadowActivity extends Activity {
    private static final String KEY_ORIGINAL_PID = "key_original_pid";

    private int mOriginalProcessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            handleIntent(getIntent());

            mOriginalProcessId = Process.myPid();
        } else {
            mOriginalProcessId = savedInstanceState.getInt(KEY_ORIGINAL_PID, mOriginalProcessId);

            boolean restoredInAnotherProcess = mOriginalProcessId != Process.myPid();

            if(restoredInAnotherProcess) {
                finish();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String[] permissions = intent.getStringArrayExtra("permissions");
        requestPermissions(permissions, 42);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        RxPermissions.getInstance(this).onRequestPermissionsResult(requestCode, permissions, grantResults);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_ORIGINAL_PID, mOriginalProcessId);
    }
}
