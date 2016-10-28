package com.tbruyelle.rxpermissions;

import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;

/**
 * In case of restore, ensures it's done by the same process, if not, kill self.
 * <p>
 * The goal is to prevent a crash when the activity is restored during a permission request
 * but by another process. In that specific case the library is not able to restore the observable
 * chain. This is a hack to prevent the crash, not a fix.
 * <p>
 * See https://github.com/tbruyelle/RxPermissions/issues/46.
 */
public abstract class EnsureSameProcessActivity extends AppCompatActivity {
    private static final String KEY_ORIGINAL_PID = "key_original_pid";

    private int mOriginalProcessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mOriginalProcessId = Process.myPid();
        } else {
            mOriginalProcessId = savedInstanceState.getInt(KEY_ORIGINAL_PID, mOriginalProcessId);

            boolean restoredInAnotherProcess = mOriginalProcessId != Process.myPid();

            if (restoredInAnotherProcess) {
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_ORIGINAL_PID, mOriginalProcessId);
    }
}
