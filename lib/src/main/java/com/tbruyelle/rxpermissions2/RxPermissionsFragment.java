package com.tbruyelle.rxpermissions2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.subjects.PublishSubject;

public class RxPermissionsFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST_CODE = 42;
    private static final int PERMISSION_REQUEST_INSTALL_PACKAGES_REQUEST_CODE = 9527;

    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    private Map<String, PublishSubject<Permission>> mSubjects = new HashMap<>();
    private boolean mLogging;

    public RxPermissionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @TargetApi(Build.VERSION_CODES.M)
    void requestPermissions(@NonNull String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permissions[i])) {
                    List<String> list = new ArrayList<>(Arrays.asList(permissions));
                    list.remove(i);
                    permissions = list.toArray(new String[0]);

                    try {
                        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri);
                        startActivityForResult(intent, PERMISSION_REQUEST_INSTALL_PACKAGES_REQUEST_CODE);
                    } catch (ActivityNotFoundException exc) {
                        Log.e(RxPermissions.TAG, "Settings activity not found for action: " + Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    }
                    break;
                }
            }
            if (permissions.length != 0) {
                requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSIONS_REQUEST_CODE) return;

        boolean[] shouldShowRequestPermissionRationale = new boolean[permissions.length];

        for (int i = 0; i < permissions.length; i++) {
            shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
        }

        onRequestPermissionsResult(permissions, grantResults, shouldShowRequestPermissionRationale);
    }

    void onRequestPermissionsResult(String permissions[], int[] grantResults, boolean[] shouldShowRequestPermissionRationale) {
        for (int i = 0, size = permissions.length; i < size; i++) {
            log("onRequestPermissionsResult  " + permissions[i]);
            // Find the corresponding subject
            PublishSubject<Permission> subject = mSubjects.get(permissions[i]);
            if (subject == null) {
                // No subject found
                Log.e(RxPermissions.TAG, "RxPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request.");
                return;
            }
            mSubjects.remove(permissions[i]);
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            subject.onNext(new Permission(permissions[i], granted, shouldShowRequestPermissionRationale[i]));
            subject.onComplete();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PERMISSION_REQUEST_INSTALL_PACKAGES_REQUEST_CODE) return;
        log("onActivityResult  " + Manifest.permission.REQUEST_INSTALL_PACKAGES);
        PublishSubject<Permission> subject = mSubjects.get(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        if (subject == null) {
            // No subject found
            Log.e(RxPermissions.TAG, "RxPermissions.onActivityResult invoked but didn't find android.permission.REQUEST_INSTALL_PACKAGES request.");
            return;
        }
        mSubjects.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        boolean granted = getContext().getPackageManager().canRequestPackageInstalls();
        subject.onNext(new Permission(Manifest.permission.REQUEST_INSTALL_PACKAGES, granted, false));
        subject.onComplete();
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean isGranted(String permission) {
        final FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity == null) {
            throw new IllegalStateException("This fragment must be attached to an activity.");
        }
        return fragmentActivity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean isRevoked(String permission) {
        final FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity == null) {
            throw new IllegalStateException("This fragment must be attached to an activity.");
        }
        return fragmentActivity.getPackageManager().isPermissionRevokedByPolicy(permission, getActivity().getPackageName());
    }

    public void setLogging(boolean logging) {
        mLogging = logging;
    }

    public PublishSubject<Permission> getSubjectByPermission(@NonNull String permission) {
        return mSubjects.get(permission);
    }

    public boolean containsByPermission(@NonNull String permission) {
        return mSubjects.containsKey(permission);
    }

    public void setSubjectForPermission(@NonNull String permission, @NonNull PublishSubject<Permission> subject) {
        mSubjects.put(permission, subject);
    }

    void log(String message) {
        if (mLogging) {
            Log.d(RxPermissions.TAG, message);
        }
    }

}
