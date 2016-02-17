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
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class RxPermissions {

    public static final String TAG = "RxPermissions";
    private static RxPermissions sSingleton;

    public static RxPermissions getInstance(Context ctx) {
        if (sSingleton == null) {
            sSingleton = new RxPermissions(ctx.getApplicationContext());
        }
        return sSingleton;
    }

    private Context mCtx;

    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    private Map<String, PublishSubject<Permission>> mSubjects = new HashMap<>();
    private boolean mLogging;

    RxPermissions(Context ctx) {
        mCtx = ctx;
    }

    public void setLogging(boolean logging) {
        mLogging = logging;
    }

    private void log(String message) {
        if (mLogging) {
            Log.d(TAG, message);
        }
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
        return requestEach(null, permissions);
    }

    /**
     * Register one or several permission requests and returns an observable that
     * emits a {@link Permission} for each requested permission.
     * <p>
     * The request is only executed when the `trigger` observable emits something.
     * <p>
     * For SDK &lt; 23, the observable will immediately emit true, otherwise
     * the user response to that request.
     * <p>
     * It handles multiple requests to the same permission, in that case the
     * same observable will be returned.
     */
    public Observable<Permission> requestEach(final Observable<?> trigger,
                                              final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermissions.request/requestEach requires at least one input permission");
        }
        return oneOf(trigger, pending(permissions))
                .flatMap(new Func1<Object, Observable<Permission>>() {
                    @Override
                    public Observable<Permission> call(Object o) {
                        return request_(permissions);
                    }
                });
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
        return request(null, permissions);
    }

    /**
     * Register one or several permission requests and returns an observable that
     * emits an aggregation of the answers. If all  requested permissions were
     * granted, it emits true, else false.
     * <p>
     * The request is only executed when the `trigger` observable emits something.
     * <p>
     * For SDK &lt; 23, the observable will immediately emit true, otherwise
     * the user response to that request.
     * <p>
     * It handles multiple requests to the same permission, in that case the
     * same observable will be returned.
     */
    public Observable<Boolean> request(final Observable<?> trigger, final String... permissions) {
        return requestEach(trigger, permissions)
                // Transform Observable<Permission> to Observable<Boolean>
                .buffer(permissions.length)
                .flatMap(new Func1<List<Permission>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(List<Permission> permissions) {
                        if (permissions.isEmpty()) {
                            // Occurs during orientation change, when the subject receives onComplete.
                            // In that case we don't want to propagate that empty list to the
                            // subscriber, only the onComplete.
                            return Observable.empty();
                        }
                        // Return true if all permissions are granted.
                        for (Permission p : permissions) {
                            if (!p.granted) {
                                return Observable.just(false);
                            }
                        }
                        return Observable.just(true);
                    }
                });
    }

    /**
     * Returns an empty observable is there is no pending permission
     * request.
     * Else returns a one-item observable.
     */
    private Observable<?> pending(final String... permissions) {
        for (String p : permissions) {
            PublishSubject s = mSubjects.get(p);
            if (s == null || !s.hasCompleted()) {
                return Observable.empty();
            }
        }
        return Observable.just(null);
    }

    private Observable<Object> oneOf(Observable<?> trigger, Observable<?> pending) {
        if (trigger == null) {
            return Observable.just(null);
        }
        return Observable.merge(trigger, pending);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Permission> request_(final String... permissions) {
        if (mLogging) {
            log("Requesting permissions " + TextUtils.join(", ", permissions));
        }

        List<Observable<Permission>> list = new ArrayList<>(permissions.length);
        List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create a observable for each of them.
        // This helps to handle concurrent requests, for instance when there is one
        // request for CAMERA and STORAGE, and another request for CAMERA only, only
        // one observable will be create for the CAMERA.
        // At the end, the observables are combined to have a unique response.
        for (String permission : permissions) {
            if (isGranted(permission)) {
                // Already granted, or not Android M
                // Return a granted Permission object.
                list.add(Observable.just(new Permission(permission, true)));
                continue;
            }

            if (isRevoked(permission)) {
                // Revoked by a policy, return a denied Permission object.
                list.add(Observable.just(new Permission(permission, false)));
                continue;
            }

            PublishSubject<Permission> subject = mSubjects.get(permission);
            // Create a new subject if not exists OR if completed.
            // This last case occurs on configuration change, and in that case
            // we need to recreate a new subject, but without request the permission
            // again.
            if (subject == null || subject.hasCompleted()) {
                if (subject == null) {
                    unrequestedPermissions.add(permission);
                }
                subject = PublishSubject.create();
                mSubjects.put(permission, subject);
            }
            list.add(subject);
        }

        if (!unrequestedPermissions.isEmpty()) {
            startShadowActivity(unrequestedPermissions
                    .toArray(new String[unrequestedPermissions.size()]));
        }
        return Observable.concat(Observable.from(list));
    }

    /**
     * Invokes Activity.shouldShowRequestPermissionRationale and wraps
     * the returned value in an observable.
     * <p>
     * In case of multiple permissions, only emits true if
     * Activity.shouldShowRequestPermissionRationale returned true for
     * all revoked permissions.
     * <p>
     * You shouldn't call this method is all permissions haven been granted.
     * <p>
     * For SDK &lt; 23, the observable will always emit false.
     */
    public Observable<Boolean> shouldShowRequestPermissionRationale(final Activity activity,
                                                                    final String... permissions) {
        if (!isMarshmallow()) {
            return Observable.just(false);
        }
        return Observable.just(shouldShowRequestPermissionRationale_(activity, permissions));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean shouldShowRequestPermissionRationale_(final Activity activity,
                                                          final String... permissions) {
        for (String p : permissions) {
            if (!isGranted(p) && !activity.shouldShowRequestPermissionRationale(p)) {
                return false;
            }
        }
        return true;
    }

    void startShadowActivity(String[] permissions) {
        log("startShadowActivity " + TextUtils.join(", ", permissions));
        Intent intent = new Intent(mCtx, ShadowActivity.class);
        intent.putExtra("permissions", permissions);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mCtx.startActivity(intent);
    }

    /**
     * Returns true if the permission is already granted.
     * <p>
     * Always true if SDK &lt; 23.
     */
    public boolean isGranted(String permission) {
        return !isMarshmallow() || isGranted_(permission);
    }

    /**
     * Returns true if the permission has been revoked by a policy.
     * <p>
     * Always false if SDK &lt; 23.
     */
    public boolean  isRevoked(String permission) {
        return isMarshmallow() && isRevoked_(permission);
    }

    boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isGranted_(String permission) {
        return mCtx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isRevoked_(String permission) {
        return mCtx.getPackageManager().isPermissionRevokedByPolicy(permission, mCtx.getPackageName());
    }

    /**
     * Invokes onCompleted on all registered subjects.
     * <p>
     * This should un-subscribe the observers.
     */
    public void onDestroy() {
        log("onDestroy");
        for (Subject subject : mSubjects.values()) {
            subject.onCompleted();
        }
    }

    void onRequestPermissionsResult(int requestCode,
                                    String permissions[], int[] grantResults) {
        for (int i = 0, size = permissions.length; i < size; i++) {
            log("onRequestPermissionsResult  " + permissions[i]);
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
