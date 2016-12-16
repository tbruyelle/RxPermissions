package com.tbruyelle.rxpermissions2.sample;

import android.Manifest.permission;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;
import com.jakewharton.rxbinding.view.RxView;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import java.io.IOException;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxPermissionsSample";

    private Camera camera;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.setLogging(true);

        setContentView(R.layout.act_main);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        RxJavaInterop.toV2Observable(
              RxView.clicks(findViewById(R.id.enableCamera))
                    .map(new Func1<Void, Object>() {
                        @Override
                        public Object call(final Void aVoid) {
                            return new Object();
                        }
                    })
        )
              // Ask for permissions when button is clicked
              .compose(rxPermissions.ensureEach(permission.CAMERA))
              .subscribe(new Consumer<Permission>() {
                             @Override
                             public void accept(Permission permission) {
                                 Log.i(TAG, "Permission result " + permission);
                                 if (permission.granted) {
                                     releaseCamera();
                                     camera = Camera.open(0);
                                     try {
                                         camera.setPreviewDisplay(surfaceView.getHolder());
                                         camera.startPreview();
                                     } catch (IOException e) {
                                         Log.e(TAG, "Error while trying to display the camera preview", e);
                                     }
                                 } else if (permission.shouldShowRequestPermissionRationale) {
                                     // Denied permission without ask never again
                                     Toast.makeText(MainActivity.this,
                                           "Denied permission without ask never again",
                                           Toast.LENGTH_SHORT).show();
                                 } else {
                                     // Denied permission with ask never again
                                     // Need to go to the settings
                                     Toast.makeText(MainActivity.this,
                                           "Permission denied, can't enable the camera",
                                           Toast.LENGTH_SHORT).show();
                                 }
                             }
                         },
                    new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable t) {
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