package com.tbruyelle.rxpermissions.sample;

import android.Manifest;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.RxView;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxPermissionsSample";

    private Camera mCamera;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxPermissions.getInstance(this).setLogging(true);

        setContentView(R.layout.act_main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        RxView.clicks(findViewById(R.id.enableCamera))
                .compose(RxPermissions.ensure(this, Manifest.permission.CAMERA))
                .subscribe(granted -> {
                            Log.i(TAG, " TRIGGER Received result " + granted);
                            if (granted) {
                                releaseCamera();
                                mCamera = Camera.open(0);
                                try {
                                    mCamera.setPreviewDisplay(mSurfaceView.getHolder());
                                    mCamera.startPreview();
                                } catch (IOException e) {
                                    Log.e(TAG, "Error while trying to display the camera preview", e);
                                }
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Permission denied, can't enable the camera",
                                        Toast.LENGTH_SHORT).show();
                            }
                        },
                        t -> Log.e(TAG, "onError", t),
                        () -> Log.i(TAG, "OnComplete")
                );
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}
