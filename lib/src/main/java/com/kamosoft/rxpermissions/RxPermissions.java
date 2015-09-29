package com.kamosoft.rxpermissions;

import android.annotation.TargetApi;
import android.app.Activity;
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

    private static RxPermissions sInstance;

    private Activity mActivity;

    private Map<String, PublishSubject<Boolean>> mSubjects = new HashMap<>();


    public RxPermissions(Activity activity) {
        mActivity = activity;
    }

    public Observable<Boolean> request(final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermission.request requires at least on input permission");
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
            mActivity.requestPermissions(unrequestedPermissions.toArray(new String[0]), permissionID(permissions));
        }

        return Observable.combineLatest(list, combineLatestBools.INSTANCE);
    }

    public boolean isGranted(String... permissions) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasPermission_(permissions);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasPermission_(String... permissions) {
        for (String permission : permissions) {
            if (mActivity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            // Find the corresponding subject
            PublishSubject<Boolean> subject = mSubjects.get(permissions[i]);
            if (subject == null) {
                // No subject found
                throw new IllegalStateException("RxPermission.onRequestPermissionsResult invoked but didn't found the corresponding permission request.");
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
