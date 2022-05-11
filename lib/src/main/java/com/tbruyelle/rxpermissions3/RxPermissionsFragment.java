package com.tbruyelle.rxpermissions3;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import java.util.Map;

import io.reactivex.rxjava3.subjects.PublishSubject;


public class RxPermissionsFragment extends Fragment {
    private RxPermissionViewModel viewModel;

    public RxPermissionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = getDefaultViewModelProviderFactory().create(RxPermissionViewModel.class);
        viewModel.permissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result != null) {
                        onRequestPermissionsResult(result);
                    }
                }
        );
    }

    @TargetApi(Build.VERSION_CODES.M)
    void requestPermissions(@NonNull String[] permissions) {
        viewModel.permissionRequest.launch(permissions);
    }

    void onRequestPermissionsResult(@NonNull Map<String, Boolean> result) {
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            log("onRequestPermissionsResult  " + entry.getKey());
            // Find the corresponding subject
            PublishSubject<Permission> subject = viewModel.mSubjects.get(entry.getKey());
            if (subject == null) {
                // No subject found
                Log.e(RxPermissions.TAG, "RxPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request.");
                return;
            }
            viewModel.mSubjects.remove(entry.getKey());
            subject.onNext(new Permission(entry.getKey(),
                    entry.getValue(),
                    shouldShowRequestPermissionRationale(entry.getKey())));
            subject.onComplete();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean isGranted(String permission) {
        final Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("This fragment must be attached to an activity.");
        }
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
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
        viewModel.mLogging = logging;
    }

    public PublishSubject<Permission> getSubjectByPermission(@NonNull String permission) {
        return viewModel.mSubjects.get(permission);
    }

    public void setSubjectForPermission(@NonNull String permission, @NonNull PublishSubject<Permission> subject) {
        viewModel.mSubjects.put(permission, subject);
    }

    void log(String message) {
        if (viewModel.mLogging) {
            Log.d(RxPermissions.TAG, message);
        }
    }

}
