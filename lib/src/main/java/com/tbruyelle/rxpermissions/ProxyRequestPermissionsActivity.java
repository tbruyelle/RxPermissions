package com.tbruyelle.rxpermissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.M)
public final class ProxyRequestPermissionsActivity extends Activity {

    private static final String INTENT_PREFIX = BuildConfig.APPLICATION_ID + ".";
    private static final String INTENT_EXTRA_PREFIX = INTENT_PREFIX + "extra.";
    private static final String INTENT_BROADCAST_PREFIX = INTENT_PREFIX + ".broadcast";

    public static final String EXTRA_REQ_CODE = INTENT_EXTRA_PREFIX + "REQ_CODE";
    public static final String EXTRA_PERMISSIONS = INTENT_EXTRA_PREFIX + "PERMISSIONS";
    public static final String EXTRA_GRANT_RESULTS = INTENT_EXTRA_PREFIX + "GRANT_RESULTS";
    public static final String ACTION_PERMISSIONS = INTENT_BROADCAST_PREFIX + "PERMISSIONS";

    public static Intent createIntent(Context context, int reqCode, String... permissions) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("At least one input permission is required");
        }
        final Intent intent = new Intent(context, ProxyRequestPermissionsActivity.class);
        intent.putExtra(EXTRA_REQ_CODE, reqCode);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Only request permissions once -
        // permission request result will be delivered to the newly created activity.
        if (savedInstanceState == null) {
            final String[] permissions = getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
            if (permissions != null && permissions.length > 0) {
                requestPermissions(permissions, getIntent().getIntExtra(EXTRA_REQ_CODE, 0));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        final Intent intent = new Intent(ACTION_PERMISSIONS);
        intent.putExtra(EXTRA_REQ_CODE, requestCode);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        intent.putExtra(EXTRA_GRANT_RESULTS, grantResults);
        sendBroadcast(intent);
        finish();
    }
}
