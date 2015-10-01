/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tbruyelle.rxpermissions;

import static com.tbruyelle.rxpermissions.ProxyRequestPermissionsActivity.ACTION_PERMISSIONS;
import static com.tbruyelle.rxpermissions.ProxyRequestPermissionsActivity.EXTRA_GRANT_RESULTS;
import static com.tbruyelle.rxpermissions.ProxyRequestPermissionsActivity.EXTRA_PERMISSIONS;
import static com.tbruyelle.rxpermissions.ProxyRequestPermissionsActivity.EXTRA_REQ_CODE;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.FuncN;
import rx.subjects.PublishSubject;

public class RxPermissions {

    private Context mContext;

    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    private Map<String, PublishSubject<Boolean>> mSubjects = new HashMap<>();

    // Handles permission request results from the proxy activity.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(this);
            int reqCode = intent.getIntExtra(EXTRA_REQ_CODE, 0);
            String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
            int[] grants = intent.getIntArrayExtra(EXTRA_GRANT_RESULTS);
            onRequestPermissionsResult(reqCode, permissions, grants);
        }
    };

    public RxPermissions(Context context) {
        if (context == null) {
            throw new NullPointerException("context == null");
        }
        mContext = context.getApplicationContext();
    }

    /**
     * Register one or several permission requests and returns an observable.
     * <p/>
     * For SDK &lt; 23, the observable will immediatly emit true, otherwise
     * the user response to that request.
     * <p/>
     * It handles multiple requests to the same permission, in that case the
     * same observable will be returned.
     */
    public Observable<Boolean> request(final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermission.request requires at least one input permission");
        }
        if (isGranted(permissions)) {
            // Already granted, or not Android M
            return Observable.just(true);
        }
        return request_(permissions);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Boolean> request_(final String... permissions) {

        List<Observable<Boolean>> list = new ArrayList<>(permissions.length);
        List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create a observable for each of them.
        // This helps to handle concurrent requests, for instance when there is one
        // request for CAMERA and STORAGE, and another request for CAMERA only, only
        // one observable will be create for the CAMERA.
        // At the end, the observable are combined to have a unique response.
        for (String permission : permissions) {
            PublishSubject<Boolean> subject = mSubjects.get(permission);
            if (subject == null) {
                subject = PublishSubject.create();
                mSubjects.put(permission, subject);
                unrequestedPermissions.add(permission);
            }
            list.add(subject);
        }
        if (!unrequestedPermissions.isEmpty()) {
            final Intent intent = ProxyRequestPermissionsActivity.createIntent(
                    mContext, permissionID(permissions),
                    unrequestedPermissions.toArray(new String[unrequestedPermissions.size()]));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.registerReceiver(mReceiver, new IntentFilter(ACTION_PERMISSIONS));
            mContext.startActivity(intent);
        }

        return Observable.combineLatest(list, combineLatestBools.INSTANCE);
    }

    /**
     * Returns true if the permissions is already granted.
     * <p/>
     * Always true if SDK &lt; 23.
     */
    public boolean isGranted(String... permissions) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasPermission_(permissions);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasPermission_(String... permissions) {
        for (String permission : permissions) {
            if (mContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Must be invoked in {@code Activity.onRequestPermissionsResult}
     * <p/>
     * The method will find the pending requests and emit the response to the
     * matching observables.
     */
    void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            // Find the corresponding subject
            PublishSubject<Boolean> subject = mSubjects.get(permissions[i]);
            if (subject == null) {
                // No subject found
                throw new IllegalStateException("RxPermission.onRequestPermissionsResult invoked but didn't find the corresponding permission request.");
            }
            mSubjects.remove(permissions[i]);
            subject.onNext(grantResults[i] == PackageManager.PERMISSION_GRANTED);
            subject.onCompleted();
        }
    }

    int permissionID(String... permissions) {
        Arrays.sort(permissions);
        String s = "";
        for (String permission : permissions) {
            s += permission;
        }
        return Math.abs(s.hashCode());
    }

    private enum combineLatestBools implements FuncN<Boolean> {
        INSTANCE;

        public Boolean call(Object... args) {
            for (Object arg : args) {
                if (!(Boolean) arg) {
                    return false;
                }
            }
            return true;
        }
    }
}
