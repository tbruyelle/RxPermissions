package com.tbruyelle.rxpermissions2;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;

@Implements(ActivityCompat.class)
public final class ShadowActivityCompat {

    @Nullable
    private static PermissionRequest currentPermissionRequest = null;

    private ShadowActivityCompat() {
    }

    @Implementation(minSdk = 23)
    public static void requestPermissions(@NonNull Activity activity, @NonNull String[] permissions, @IntRange(from = 0) int requestCode) {
        //noinspection ConstantConditions
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("permission cannot be null or empty");
        }
        if (currentPermissionRequest != null) {
            activity.onRequestPermissionsResult(requestCode, new String[0], new int[0]);
        } else {
            currentPermissionRequest = new PermissionRequest(permissions, requestCode);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void processPendingPermissionsRequest(Activity activity, boolean success) {
        if (currentPermissionRequest != null) {
            final int requestCode = currentPermissionRequest.requestCode;
            final String[] permissions = currentPermissionRequest.permissions;
            final int[] grantResults = new int[permissions.length];
            if (success) {
                Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            } else {
                Arrays.fill(grantResults, PackageManager.PERMISSION_DENIED);
            }
            activity.onRequestPermissionsResult(requestCode, permissions, grantResults);
            currentPermissionRequest = null;
        }
    }

    @VisibleForTesting
    static void setCurrentPermissionRequest(@Nullable PermissionRequest request) {
        currentPermissionRequest = request;
    }

    @VisibleForTesting
    @Nullable
    static PermissionRequest getCurrentPermissionRequest() {
        return currentPermissionRequest;
    }

    static class PermissionRequest {
        private final String[] permissions;
        private final int requestCode;

        PermissionRequest(String[] permissions, int requestCode) {
            this.permissions = permissions;
            this.requestCode = requestCode;
        }
    }
}
