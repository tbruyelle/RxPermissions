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

import rx.Observable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxPermissions";

    private RxPermissions mRxPermissions;
    private Camera mCamera;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRxPermissions = RxPermissions.getInstance(this);

        setContentView(R.layout.act_main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        // If the permission request is triggered by a particular event
        // (a click for instance) we need to pass that information threw
        // an Observable.
        // com.jakewharton.rxbinding.view.RxView does exactly what we need.
        Observable<Object> trigger = RxView.clicks(findViewById(R.id.enableCamera));

        mRxPermissions
                .request(trigger, Manifest.permission.CAMERA)
                .subscribe(granted -> {
                            Log.i(TAG, "Received result " + granted);
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
