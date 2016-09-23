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

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxPermissionsSample";

    private Camera camera;
    private SurfaceView surfaceView;
    private RxPermissions rxPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rxPermissions = RxPermissions.getInstance(this);
        rxPermissions.setLogging(true);

        setContentView(R.layout.act_main);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        findViewById(R.id.enableCamera).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                rxPermissions.request(Manifest.permission.CAMERA)
                    .subscribe(new Consumer<Boolean>() {
                                   @Override
                                   public void accept(Boolean granted) throws Exception {
                                       Log.i(TAG, "Permission result " + granted);
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
                                   }
                               },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable t) throws Exception {
                                Log.e(TAG, "onError", t);
                            }
                        },
                        new Action() {
                            @Override
                            public void run() {
                                Log.i(TAG, "OnComplete");
                            }
                        });
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}
