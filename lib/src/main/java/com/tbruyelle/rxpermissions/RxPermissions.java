/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tbruyelle.rxpermissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class RxPermissions {

    private static RxPermissions sSingleton;

    public static RxPermissions getInstance(Context ctx) {
        if (sSingleton == null) {
            sSingleton = new RxPermissions();
            sSingleton.mCtx = ctx.getApplicationContext();
        }
        return sSingleton;
    }

    private Context mCtx;

    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    private Map<String, PublishSubject<Permission>> mSubjects = new HashMap<>();

    private RxPermissions() {

    }

    /**
     * Register one or several permission requests and returns an observable that
     * emits a {@link Permission} for each requested permission.
     * <p>
     * For SDK &lt; 23, the observable will immediately emit true, otherwise
     * the user response to that request.
     * <p>
     * It handles multiple requests to the same permission, in that case the
     * same observable will be returned.
     */
    public Observable<Permission> requestEach(final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermissions.request requires at least one input permission");
        }
        if (isGranted(permissions)) {
            // Already granted, or not Android M
            // Map all requested permissions to granted Permission objects.
            return Observable.from(permissions)
                    .map(new Func1<String, Permission>() {
                        @Override
                        public Permission call(String s) {
                            return new Permission(s, true);
                        }
                    });
        }
        return request_(permissions);
    }

    /**
     * Register one or several permission requests and returns an observable that
     * emits an aggregation of the answers. If all  requested permissions were
     * granted, it emits true, else false.
     * <p>
     * For SDK &lt; 23, the observable will immediately emit true, otherwise
     * the user response to that request.
     * <p>
     * It handles multiple requests to the same permission, in that case the
     * same observable will be returned.
     */
    public Observable<Boolean> request(final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermissions.request requires at least one input permission");
        }
        if (isGranted(permissions)) {
            // Already granted, or not Android M
            return Observable.just(true);
        }
        return request_(permissions)
                .toList()
                .map(new Func1<List<Permission>, Boolean>() {
                    @Override
                    public Boolean call(List<Permission> permissions) {
                        for (Permission p : permissions) {
                            if (!p.granted) {
                                return false;
                            }
                        }
                        return true;
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Permission> request_(final String... permissions) {

        List<Observable<Permission>> list = new ArrayList<>(permissions.length);
        List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create a observable for each of them.
        // This helps to handle concurrent requests, for instance when there is one
        // request for CAMERA and STORAGE, and another request for CAMERA only, only
        // one observable will be create for the CAMERA.
        // At the end, the observables are combined to have a unique response.
        for (String permission : permissions) {
            PublishSubject<Permission> subject = mSubjects.get(permission);
            if (subject == null) {
                subject = PublishSubject.create();
                mSubjects.put(permission, subject);
                unrequestedPermissions.add(permission);
            }
            list.add(subject);
        }
        if (!unrequestedPermissions.isEmpty()) {
            startShadowActivity(permissions);
        }
        return Observable.concat(Observable.from(list));
    }

    /**
     * Invokes Activity.shouldShowRequestPermissionRationale and wraps
     * the returned value in an observable.
     * <p>
     * For SDK &lt; 23, the observable will always emit false.
     */
    public Observable<Boolean> shouldShowRequestPermissionRationale(final Activity activity, final String permission) {
        if (isMarshmallow()) {
            return shouldShowRequestPermissionRationale_(activity, permission);
        }
        return Observable.just(false);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Boolean> shouldShowRequestPermissionRationale_(final Activity activity, final String permission) {
        return Observable.just(activity.shouldShowRequestPermissionRationale(permission));
    }

    void startShadowActivity(String[] permissions) {
        Intent intent = new Intent(mCtx, ShadowActivity.class);
        intent.putExtra("permissions", permissions);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mCtx.startActivity(intent);
    }

    /**
     * Returns true if the permissions is already granted.
     * <p>
     * Always true if SDK &lt; 23.
     */
    public boolean isGranted(String... permissions) {
        return !isMarshmallow() || hasPermission_(permissions);
    }

    private boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasPermission_(String... permissions) {
        for (String permission : permissions) {
            if (mCtx.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    void onRequestPermissionsResult(int requestCode,
                                    String permissions[], int[] grantResults) {
        for (int i = 0, size = permissions.length; i < size; i++) {
            // Find the corresponding subject
            PublishSubject<Permission> subject = mSubjects.get(permissions[i]);
            if (subject == null) {
                // No subject found
                throw new IllegalStateException("RxPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request.");
            }
            mSubjects.remove(permissions[i]);
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            subject.onNext(new Permission(permissions[i], granted));
            subject.onCompleted();
        }
    }
}
