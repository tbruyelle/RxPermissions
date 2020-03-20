package com.tbruyelle.rxpermissions2;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseArray;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class RxPermissionsFragment extends Fragment {

    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    private SparseArray<Map<String, PublishSubject<Permission>>> mSubjects = new SparseArray<>();
    private boolean mLogging;

    private int mRequestCode = -1;
    private State mRequestState = State.READY;

    public RxPermissionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    final void prepareRequest() {
        if (mRequestState != State.READY) {
            throw new IllegalStateException("Another request is already being prepared or waiting to be submitted.");
        }
        mRequestCode++;
        mRequestState = State.PREPARING;
    }

    final void finishRequest() {
        if (mRequestState != State.PREPARING) {
            throw new IllegalStateException("Request is already sealed or submitted.");
        }
        mRequestState = State.SEALED;
    }

    final void cancelRequest() {
        if (mRequestState == State.READY) {
            throw new IllegalStateException("There is no pending request.");
        }
        mRequestState = State.READY;
    }

    @TargetApi(Build.VERSION_CODES.M)
    void requestPermissions(@NonNull String[] permissions) {
        if (mRequestState != State.SEALED) {
            throw new IllegalStateException("Call prepareRequest() and finishRequest() before submitting a request.");
        }
        mRequestState = State.READY;
        requestPermissions(permissions, mRequestCode);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions.length > 0) {
            boolean[] shouldShowRequestPermissionRationale = new boolean[permissions.length];

            for (int i = 0; i < permissions.length; i++) {
                shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
            }

            onRequestPermissionsResult(requestCode, permissions, grantResults, shouldShowRequestPermissionRationale);
        } else {
            onRequestPermissionsCanceled(requestCode);
        }
    }

    @VisibleForTesting
    void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults, boolean[] shouldShowRequestPermissionRationale) {
        final Map<String, PublishSubject<Permission>> subjects = getSubjects(requestCode);
        // Android will throw in requestPermissions when empty array is supplied.
        assert subjects != null;

        for (int i = 0, size = permissions.length; i < size; i++) {
            log("onRequestPermissionsResult  " + permissions[i]);
            // Find the corresponding subject
            Subject<Permission> subject = subjects.get(permissions[i]);
            if (subject == null) {
                // No subject found
                Log.e(RxPermissions.TAG, "RxPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request.");
                return;
            }
            subjects.remove(permissions[i]);
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            subject.onNext(new Permission(permissions[i], granted, shouldShowRequestPermissionRationale[i]));
            subject.onComplete();
        }

        mSubjects.remove(requestCode);
    }

    @VisibleForTesting
    void onRequestPermissionsCanceled(int requestCode) {
        final Map<String, PublishSubject<Permission>> subjects = getSubjectsCopy(requestCode);
        if (!subjects.isEmpty()) {
            mSubjects.get(requestCode).clear();
            for (Map.Entry<String, PublishSubject<Permission>> entry : subjects.entrySet()) {
                final String permission = entry.getKey();
                final Subject<Permission> subject = entry.getValue();
                subject.onNext(new Permission(permission, false, false));
                subject.onComplete();
            }
        }
        mSubjects.remove(requestCode);
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

    @Nullable
    public PublishSubject<Permission> getSubjectByPermission(@NonNull String permission) {
        for (int i = 0, size = mSubjects.size(); i < size; i++) {
            final PublishSubject<Permission> subject = mSubjects.get(mSubjects.keyAt(i)).get(permission);
            if (subject != null) return subject;
        }
        return null;
    }

    public boolean containsByPermission(@NonNull String permission) {
        for (int i = 0, size = mSubjects.size(); i < size; i++) {
            if (mSubjects.get(mSubjects.keyAt(i)).containsKey(permission)) {
                return true;
            }
        }
        return false;
    }

    public void setSubjectForPermission(@NonNull String permission, @NonNull PublishSubject<Permission> subject) {
        if (mRequestState != State.PREPARING) {
            throw new IllegalStateException("Call prepareRequest() before making a new request.");
        }
        getOrCreateSubjects(mRequestCode).put(permission, subject);
    }

    @Nullable
    private Map<String, PublishSubject<Permission>> getSubjects(int requestCode) {
        return mSubjects.get(requestCode);
    }

    @NonNull
    private Map<String, PublishSubject<Permission>> getOrCreateSubjects(int requestCode) {
        if (requestCode > mRequestCode) {
            throw new IllegalArgumentException("Request code is too big. Current max value is " + requestCode + ".");
        }
        Map<String, PublishSubject<Permission>> realSubjects = mSubjects.get(requestCode);
        if (realSubjects == null) {
            realSubjects = new HashMap<>();
            mSubjects.put(requestCode, realSubjects);
        }
        return realSubjects;
    }

    @VisibleForTesting
    @NonNull
    final Map<String, PublishSubject<Permission>> getSubjectsCopy(int requestCode) {
        Map<String, PublishSubject<Permission>> realSubjects = mSubjects.get(requestCode);
        if (realSubjects == null) realSubjects = Collections.emptyMap();
        return new LinkedHashMap<>(realSubjects);
    }

    @VisibleForTesting
    final int getPermissionRequestCode() {
        return mRequestCode;
    }

    void log(String message) {
        if (mLogging) {
            Log.d(RxPermissions.TAG, message);
        }
    }

    enum State {
        READY, PREPARING, SEALED
    }
}
