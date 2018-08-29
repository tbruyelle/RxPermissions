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

package com.tbruyelle.rxpermissions2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.M)
public class RxPermissionsTest {

    private FragmentActivity mActivity;

    private RxPermissions mRxPermissions;

    @Before
    public void setup() {
        ActivityController<FragmentActivity> activityController = Robolectric.buildActivity(FragmentActivity.class);
        mActivity = spy(activityController.setup().get());
        mRxPermissions = spy(new RxPermissions(mActivity));
        mRxPermissions.mRxPermissionsFragment = spy(mRxPermissions.mRxPermissionsFragment);
        final RxPermissionsFragment rxPermissionsFragment = spy(mRxPermissions.mRxPermissionsFragment.get());
        when(rxPermissionsFragment.getActivity()).thenReturn(mActivity);
        when(mRxPermissions.mRxPermissionsFragment.get()).thenReturn(rxPermissionsFragment);
        // Default deny all permissions
        doReturn(false).when(mRxPermissions).isGranted(anyString());
        // Default no revoked permissions
        doReturn(false).when(mRxPermissions).isRevoked(anyString());
    }

    private Observable<Object> trigger() {
        return Observable.just(RxPermissions.TRIGGER);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_preM() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_granted() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_granted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_granted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEachCombined(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_preM() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_preM() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEachCombined(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_alreadyGranted() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_denied() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(false);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_denied() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_denied() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensureEachCombined(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_revoked() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isRevoked(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(false);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_revoked() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isRevoked(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_revoked() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isRevoked(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEachCombined(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(new Permission(permission, false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_severalPermissions_granted() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(permissions, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_granted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(permissions, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0], true), new Permission(permissions[1], true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_severalPermissions_granted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEachCombined(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(permissions, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0] + ", " + permissions[1], true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_severalPermissions_oneDenied() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(permissions, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(false);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_severalPermissions_oneRevoked() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isRevoked(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValue(false);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_oneAlreadyGranted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isGranted(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0], true), new Permission(permissions[1], true));
        ArgumentCaptor<String[]> requestedPermissions = ArgumentCaptor.forClass(String[].class);
        verify(mRxPermissions).requestPermissionsFromFragment(requestedPermissions.capture());
        assertEquals(1, requestedPermissions.getValue().length);
        assertEquals(Manifest.permission.READ_PHONE_STATE, requestedPermissions.getValue()[0]);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_severalPermissions_oneAlreadyGranted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isGranted(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEachCombined(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0] + ", " + permissions[1], true));
        ArgumentCaptor<String[]> requestedPermissions = ArgumentCaptor.forClass(String[].class);
        verify(mRxPermissions).requestPermissionsFromFragment(requestedPermissions.capture());
        assertEquals(1, requestedPermissions.getValue().length);
        assertEquals(Manifest.permission.READ_PHONE_STATE, requestedPermissions.getValue()[0]);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_oneDenied() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(permissions, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0], true), new Permission(permissions[1], false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_severalPermissions_oneDenied() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensureEachCombined(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(permissions, result);

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0] + ", " + permissions[1], false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_oneRevoked() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isRevoked(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0], true), new Permission(permissions[1], false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_severalPermissions_oneRevoked() {
        TestObserver<Permission> sub = new TestObserver<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isRevoked(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEachCombined(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminated();
        sub.assertValues(new Permission(permissions[0] + ", " + permissions[1], false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_trigger_granted() {
        TestObserver<Boolean> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};
        PublishSubject<Object> trigger = PublishSubject.create();

        trigger.compose(mRxPermissions.ensure(permission)).subscribe(sub);
        trigger.onNext(1);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertNotTerminated();
        sub.assertValue(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_trigger_granted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};
        PublishSubject<Object> trigger = PublishSubject.create();

        trigger.compose(mRxPermissions.ensureEach(permission)).subscribe(sub);
        trigger.onNext(1);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertNotTerminated();
        sub.assertValue(new Permission(permission, true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscriptionCombined_trigger_granted() {
        TestObserver<Permission> sub = new TestObserver<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};
        PublishSubject<Object> trigger = PublishSubject.create();

        trigger.compose(mRxPermissions.ensureEachCombined(permission)).subscribe(sub);
        trigger.onNext(1);
        mRxPermissions.onRequestPermissionsResult(new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertNotTerminated();
        sub.assertValue(new Permission(permission, true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_allDenied_allRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(activity.shouldShowRequestPermissionRationale(anyString())).thenReturn(true);

        TestObserver<Boolean> sub = new TestObserver<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, "p1", "p2")
                .subscribe(sub);

        sub.assertComplete();
        sub.assertNoErrors();
        sub.assertValue(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_allDenied_oneRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(activity.shouldShowRequestPermissionRationale("p1")).thenReturn(true);

        TestObserver<Boolean> sub = new TestObserver<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, "p1", "p2")
                .subscribe(sub);

        sub.assertComplete();
        sub.assertNoErrors();
        sub.assertValue(false);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_allDenied_noRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);

        TestObserver<Boolean> sub = new TestObserver<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, "p1", "p2")
                .subscribe(sub);

        sub.assertComplete();
        sub.assertNoErrors();
        sub.assertValue(false);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_oneDeniedRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(mRxPermissions.isGranted("p1")).thenReturn(true);
        when(activity.shouldShowRequestPermissionRationale("p2")).thenReturn(true);

        TestObserver<Boolean> sub = new TestObserver<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, "p1", "p2")
                .subscribe(sub);

        sub.assertComplete();
        sub.assertNoErrors();
        sub.assertValue(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_oneDeniedNotRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(mRxPermissions.isGranted("p2")).thenReturn(true);

        TestObserver<Boolean> sub = new TestObserver<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, "p1", "p2")
                .subscribe(sub);

        sub.assertComplete();
        sub.assertNoErrors();
        sub.assertValue(false);
    }

    @Test
    public void isGranted_preMarshmallow() {
        // unmock isGranted
        doCallRealMethod().when(mRxPermissions).isGranted(anyString());
        doReturn(false).when(mRxPermissions).isMarshmallow();

        boolean granted = mRxPermissions.isGranted("p");

        assertTrue(granted);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void isGranted_granted() {
        // unmock isGranted
        doCallRealMethod().when(mRxPermissions).isGranted(anyString());
        doReturn(true).when(mRxPermissions).isMarshmallow();
        when(mActivity.checkSelfPermission("p")).thenReturn(PackageManager.PERMISSION_GRANTED);

        boolean granted = mRxPermissions.isGranted("p");

        assertTrue(granted);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void isGranted_denied() {
        // unmock isGranted
        doCallRealMethod().when(mRxPermissions).isGranted(anyString());
        doReturn(true).when(mRxPermissions).isMarshmallow();
        when(mActivity.checkSelfPermission("p")).thenReturn(PackageManager.PERMISSION_DENIED);

        boolean granted = mRxPermissions.isGranted("p");

        assertFalse(granted);
    }

    @Test
    public void isRevoked_preMarshmallow() {
        // unmock isRevoked
        doCallRealMethod().when(mRxPermissions).isRevoked(anyString());
        doReturn(false).when(mRxPermissions).isMarshmallow();

        boolean revoked = mRxPermissions.isRevoked("p");

        assertFalse(revoked);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void isRevoked_true() {
        // unmock isRevoked
        doCallRealMethod().when(mRxPermissions).isRevoked(anyString());
        doReturn(true).when(mRxPermissions).isMarshmallow();
        PackageManager pm = mock(PackageManager.class);
        when(mActivity.getPackageManager()).thenReturn(pm);
        when(pm.isPermissionRevokedByPolicy(eq("p"), anyString())).thenReturn(true);

        boolean revoked = mRxPermissions.isRevoked("p");

        assertTrue(revoked);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void isGranted_false() {
        // unmock isRevoked
        doCallRealMethod().when(mRxPermissions).isRevoked(anyString());
        doReturn(true).when(mRxPermissions).isMarshmallow();
        PackageManager pm = mock(PackageManager.class);
        when(mActivity.getPackageManager()).thenReturn(pm);
        when(pm.isPermissionRevokedByPolicy(eq("p"), anyString())).thenReturn(false);

        boolean revoked = mRxPermissions.isRevoked("p");

        assertFalse(revoked);
    }
}
