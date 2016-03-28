package com.tbruyelle.rxpermissions.sample;

import android.Manifest;
import android.content.Context;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.RxView;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.IOException;

import rx.Observable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxPermissionsSample";

    private Camera camera;
    private SurfaceView surfaceView;
    private TextView locTextView;
    private RxPermissions rxPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rxPermissions = RxPermissions.getInstance(this);
        rxPermissions.setLogging(true);

        setContentView(R.layout.act_main);
        locTextView = (TextView) findViewById(R.id.locTextView);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        RxView.clicks(findViewById(R.id.enableCamera))
                // Ask for permissions when button is clicked
                .compose(rxPermissions.ensure(Manifest.permission.CAMERA))
                .subscribe(granted -> {
                            Log.i(TAG, " TRIGGER Received result " + granted);
                            if (granted) {
                                releaseCamera();
                                camera = Camera.open(0);
                                try {
                                    camera.setPreviewDisplay(surfaceView.getHolder());
                                    camera.startPreview();
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

        // For location Permission.
        RxView.clicks(findViewById(R.id.enableLocation))
                .compose(rxPermissions.ensure(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION))
                .subscribe(granted -> {
                            if (granted) {
                                locTextView.setText("Location granted!");
                                LocationManager lm = (LocationManager) MainActivity.this
                                        .getSystemService(Context.LOCATION_SERVICE);
                                Location gpsLoc = null;
                                Location netLoc = null;
                                Location bstLoc = null;
                                final boolean gpsEnable = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                                final boolean networkEnable = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                                if (gpsEnable) {
                                    gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                }
                                if (networkEnable) {
                                    netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                }
                                if (gpsLoc != null && netLoc != null) {
                                    bstLoc = gpsLoc.getAccuracy() >= netLoc.getAccuracy() ? gpsLoc : netLoc;
                                } else {
                                    bstLoc = gpsLoc != null ? gpsLoc : netLoc;
                                }
                                if (bstLoc != null) {
                                    final double lat = bstLoc.getLatitude();
                                    final double lng = bstLoc.getLongitude();
                                    String locString = String.format("Location granted!: lat %f, lng %f", lat, lng);
                                    locTextView.setText(locString);
                                }
                            } else {
                                locTextView.setText("Location Permission denied.");
                            }
                        },
                        t -> Log.e(TAG, "onError", t),
                        () -> Log.i(TAG, "onComplete")
                );

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}
