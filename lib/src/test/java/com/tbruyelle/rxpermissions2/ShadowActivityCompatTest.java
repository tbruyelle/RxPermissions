package com.tbruyelle.rxpermissions2;

import android.app.Activity;
import android.content.pm.PackageManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE)
public final class ShadowActivityCompatTest {

    @Test
    public final void processPendingPermissionsRequest_shouldClearPermissionInstance() {
        final Activity activity = mock(Activity.class);
        ShadowActivityCompat.setCurrentPermissionRequest(new ShadowActivityCompat.PermissionRequest(new String[0], 0));

        ShadowActivityCompat.processPendingPermissionsRequest(activity, true);

        Assert.assertNull(ShadowActivityCompat.getCurrentPermissionRequest());
    }

    @Test
    public final void processPendingPermissionsRequest_shouldCallOnRequestPermissionsResultWithSuccessfulValues() {
        final Activity activity = mock(Activity.class);
        ShadowActivityCompat.setCurrentPermissionRequest(new ShadowActivityCompat.PermissionRequest(new String[]{"xxx"}, 0));

        ShadowActivityCompat.processPendingPermissionsRequest(activity, true);

        verify(activity).onRequestPermissionsResult(0, new String[]{"xxx"}, new int[]{PackageManager.PERMISSION_GRANTED});
    }

    @Test
    public final void processPendingPermissionsRequest_shouldCallOnRequestPermissionsResultWithFailedValues() {
        final Activity activity = mock(Activity.class);
        ShadowActivityCompat.setCurrentPermissionRequest(new ShadowActivityCompat.PermissionRequest(new String[]{"xxx"}, 0));

        ShadowActivityCompat.processPendingPermissionsRequest(activity, false);

        verify(activity).onRequestPermissionsResult(0, new String[]{"xxx"}, new int[]{PackageManager.PERMISSION_DENIED});
    }
}
