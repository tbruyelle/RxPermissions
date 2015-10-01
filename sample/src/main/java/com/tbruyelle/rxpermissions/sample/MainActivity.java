package com.tbruyelle.rxpermissions.sample;

import android.Manifest;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.IOException;

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mRxPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void enableCamera(View v) {
        mRxPermissions.request(Manifest.permission.CAMERA)
                .subscribe(granted -> {
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
                });
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
