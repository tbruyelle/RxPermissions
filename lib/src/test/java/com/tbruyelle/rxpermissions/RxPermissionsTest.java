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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.junit.*;
import org.mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class RxPermissionsTest {

    @Mock
    Context mCtx;

    RxPermissions mRxPermissions;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mCtx.getApplicationContext()).thenReturn(mCtx);
        mRxPermissions = spy(new RxPermissions(mCtx));
        RxPermissions.sSingleton = mRxPermissions;
        doNothing().when(mRxPermissions).startShadowActivity(any(String[].class));
        // Default deny all permissions
        doReturn(false).when(mRxPermissions).isGranted(anyString());
        // Default no revoked permissions
        doReturn(false).when(mRxPermissions).isRevoked(anyString());
    }

    private Observable<Object> trigger() {
        return Observable.just(null);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_preM() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_granted() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_granted() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(new Permission(permission, true)));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_preM() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(new Permission(permission, true)));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_alreadyGranted() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_denied() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_denied() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(new Permission(permission, false)));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_revoked() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isRevoked(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_revoked() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isRevoked(permission)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(new Permission(permission, false)));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_severalPermissions_granted() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_granted() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        List<Permission> ps = new ArrayList<>();
        ps.add(new Permission(permissions[0], true));
        ps.add(new Permission(permissions[1], true));
        sub.assertReceivedOnNext(ps);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_severalPermissions_oneDenied() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_severalPermissions_oneRevoked() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isRevoked(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0,
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_oneAlreadyGranted() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isGranted(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0,
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        List<Permission> ps = new ArrayList<>();
        ps.add(new Permission(permissions[0], true));
        ps.add(new Permission(permissions[1], true));
        sub.assertReceivedOnNext(ps);
        ArgumentCaptor<String[]> requestedPermissions = ArgumentCaptor.forClass(String[].class);
        verify(mRxPermissions).startShadowActivity(requestedPermissions.capture());
        assertEquals(1, requestedPermissions.getValue().length);
        assertEquals(Manifest.permission.READ_PHONE_STATE, requestedPermissions.getValue()[0]);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_oneDenied() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        List<Permission> ps = new ArrayList<>();
        ps.add(new Permission(permissions[0], true));
        ps.add(new Permission(permissions[1], false));
        sub.assertReceivedOnNext(ps);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_severalPermissions_oneRevoked() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        when(mRxPermissions.isRevoked(Manifest.permission.CAMERA)).thenReturn(true);

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(0,
                new String[]{Manifest.permission.READ_PHONE_STATE},
                new int[]{PackageManager.PERMISSION_GRANTED});

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        List<Permission> ps = new ArrayList<>();
        ps.add(new Permission(permissions[0], true));
        ps.add(new Permission(permissions[1], false));
        sub.assertReceivedOnNext(ps);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void subscription_trigger_granted() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};
        PublishSubject<Object> trigger = PublishSubject.create();

        trigger.compose(mRxPermissions.ensure(permission)).subscribe(sub);
        trigger.onNext(null);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertNoTerminalEvent();
        sub.assertReceivedOnNext(singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void eachSubscription_trigger_granted() {
        TestSubscriber<Permission> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};
        PublishSubject<Object> trigger = PublishSubject.create();

        trigger.compose(mRxPermissions.ensureEach(permission)).subscribe(sub);
        trigger.onNext(null);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertNoTerminalEvent();
        sub.assertReceivedOnNext(singletonList(new Permission(permission, true)));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_allDenied_allRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(activity.shouldShowRequestPermissionRationale(anyString())).thenReturn(true);

        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, new String[]{"p1", "p2"})
                .subscribe(sub);

        sub.assertCompleted();
        sub.assertNoErrors();
        sub.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_allDenied_oneRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(activity.shouldShowRequestPermissionRationale("p1")).thenReturn(true);

        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, new String[]{"p1", "p2"})
                .subscribe(sub);

        sub.assertCompleted();
        sub.assertNoErrors();
        sub.assertReceivedOnNext(Collections.singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_allDenied_noRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);

        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, new String[]{"p1", "p2"})
                .subscribe(sub);

        sub.assertCompleted();
        sub.assertNoErrors();
        sub.assertReceivedOnNext(Collections.singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_oneDeniedRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(mRxPermissions.isGranted("p1")).thenReturn(true);
        when(activity.shouldShowRequestPermissionRationale("p2")).thenReturn(true);

        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, new String[]{"p1", "p2"})
                .subscribe(sub);

        sub.assertCompleted();
        sub.assertNoErrors();
        sub.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void shouldShowRequestPermissionRationale_oneDeniedNotRationale() {
        when(mRxPermissions.isMarshmallow()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(mRxPermissions.isGranted("p2")).thenReturn(true);

        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        mRxPermissions.shouldShowRequestPermissionRationale(activity, new String[]{"p1", "p2"})
                .subscribe(sub);

        sub.assertCompleted();
        sub.assertNoErrors();
        sub.assertReceivedOnNext(Collections.singletonList(false));
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
        when(mCtx.checkSelfPermission("p")).thenReturn(PackageManager.PERMISSION_GRANTED);

        boolean granted = mRxPermissions.isGranted("p");

        assertTrue(granted);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void isGranted_denied() {
        // unmock isGranted
        doCallRealMethod().when(mRxPermissions).isGranted(anyString());
        doReturn(true).when(mRxPermissions).isMarshmallow();
        when(mCtx.checkSelfPermission("p")).thenReturn(PackageManager.PERMISSION_DENIED);

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
        when(mCtx.getPackageManager()).thenReturn(pm);
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
        when(mCtx.getPackageManager()).thenReturn(pm);
        when(pm.isPermissionRevokedByPolicy(eq("p"), anyString())).thenReturn(false);

        boolean revoked = mRxPermissions.isRevoked("p");

        assertFalse(revoked);
    }
}
