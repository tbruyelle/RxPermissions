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
    public void singleSubscription_preM() {
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
    public void singleSubscription_granted() {
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
    public void singleEachSubscription_granted() {
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
    public void singleEachSubscription_preM() {
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
    public void singleSubscription_alreadyGranted() {
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
    public void singleSubscription_denied() {
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
    public void singleEachSubscription_denied() {
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
    public void singleSubscription_revoked() {
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
    public void singleEachSubscription_revoked() {
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
    public void severalSubscription() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalEachSubscription() {
        TestSubscriber<Permission> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(singletonList(new Permission(permission, true)));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalMixedSubscription() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
        }
        sub1.assertReceivedOnNext(singletonList(true));
        sub2.assertReceivedOnNext(singletonList(new Permission(permission, true)));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void singleSubscription_severalPermissions_granted() {
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
    public void singleEachSubscription_severalPermissions_granted() {
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
    public void singleSubscription_severalPermissions_oneDenied() {
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
    public void singleSubscription_severalPermissions_oneRevoked() {
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
    public void singleEachSubscription_severalPermissions_oneAlreadyGranted() {
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
    public void singleEachSubscription_severalPermissions_oneDenied() {
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
    public void singleEachSubscription_severalPermissions_oneRevoked() {
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
    public void severalSubscription_severalSamePermissions() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalEachSubscription_severalSamePermissions() {
        TestSubscriber<Permission> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            List<Permission> ps = new ArrayList<>();
            ps.add(new Permission(permissions[0], true));
            ps.add(new Permission(permissions[1], true));
            sub.assertReceivedOnNext(ps);
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_severalMixingPermissions_requestSeveralFirst() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensure(Manifest.permission.CAMERA)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalEachSubscription_severalMixingPermissions_requestSeveralFirst() {
        TestSubscriber<Permission> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(Manifest.permission.CAMERA)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, permissions, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
        }
        List<Permission> ps = new ArrayList<>();
        ps.add(new Permission(permissions[0], true));
        ps.add(new Permission(permissions[1], true));
        sub1.assertReceivedOnNext(ps);
        sub2.assertReceivedOnNext(singletonList(new Permission(permissions[1], true)));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_severalMixingPermissions_requestOnceFirst() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(Manifest.permission.CAMERA)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.READ_PHONE_STATE}, result);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.CAMERA}, result);

        verify(mRxPermissions, times(2)).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalEachSubscription_severalMixingPermissions_requestOnceFirst() {
        TestSubscriber<Permission> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(Manifest.permission.CAMERA)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.READ_PHONE_STATE}, result);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.CAMERA}, result);

        verify(mRxPermissions, times(2)).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
        }
        sub1.assertReceivedOnNext(singletonList(new Permission(permissions[1], true)));
        List<Permission> ps = new ArrayList<>();
        ps.add(new Permission(permissions[0], true));
        ps.add(new Permission(permissions[1], true));
        sub2.assertReceivedOnNext(ps);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_severalMixingPermissions_oneDenied() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] resultGranted = new int[]{PackageManager.PERMISSION_GRANTED};
        int[] resultDenied = new int[]{PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensure(Manifest.permission.CAMERA)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensure(permissions)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.READ_PHONE_STATE}, resultDenied);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.CAMERA}, resultGranted);

        verify(mRxPermissions, times(2)).startShadowActivity(any(String[].class));
        sub1.assertNoErrors();
        sub1.assertTerminalEvent();
        sub1.assertUnsubscribed();
        sub1.assertReceivedOnNext(singletonList(true));
        sub2.assertNoErrors();
        sub2.assertTerminalEvent();
        sub2.assertUnsubscribed();
        sub2.assertReceivedOnNext(singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalEachSubscription_severalMixingPermissions_oneDenied() {
        TestSubscriber<Permission> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int[] resultGranted = new int[]{PackageManager.PERMISSION_GRANTED};
        int[] resultDenied = new int[]{PackageManager.PERMISSION_DENIED};

        trigger().compose(mRxPermissions.ensureEach(Manifest.permission.CAMERA)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(permissions)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.READ_PHONE_STATE}, resultDenied);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{Manifest.permission.CAMERA}, resultGranted);

        verify(mRxPermissions, times(2)).startShadowActivity(any(String[].class));
        sub1.assertNoErrors();
        sub1.assertTerminalEvent();
        sub1.assertUnsubscribed();
        sub1.assertReceivedOnNext(singletonList(new Permission(permissions[1], true)));
        sub2.assertNoErrors();
        sub2.assertTerminalEvent();
        sub2.assertUnsubscribed();
        List<Permission> ps = new ArrayList<>();
        ps.add(new Permission(permissions[0], false));
        ps.add(new Permission(permissions[1], true));
        sub2.assertReceivedOnNext(ps);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_afterDestroy() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub2);
        mRxPermissions.onDestroy();
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertNoValues();
        }

        sub1 = new TestSubscriber<>();
        sub2 = new TestSubscriber<>();
        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensure(permission)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalEachSubscription_afterDestroy() {
        TestSubscriber<Permission> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub2);
        mRxPermissions.onDestroy();
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertNoValues();
        }

        sub1 = new TestSubscriber<>();
        sub2 = new TestSubscriber<>();
        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub1);
        trigger().compose(mRxPermissions.ensureEach(permission)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(singletonList(new Permission(permission, true)));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void singleSubscription_trigger_granted() {
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
    public void singleEachSubscription_trigger_granted() {
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
    public void severalSubscription_trigger_afterDestroy() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};
        PublishSubject<Object> trigger = PublishSubject.create();

        trigger.compose(mRxPermissions.ensure(permission)).subscribe(sub1);
        trigger.compose(mRxPermissions.ensure(permission)).subscribe(sub2);
        trigger.onNext(null);
        mRxPermissions.onDestroy();
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertNoValues();
            sub.assertNoTerminalEvent();
        }

        sub1 = new TestSubscriber<>();
        sub2 = new TestSubscriber<>();
        trigger.compose(mRxPermissions.ensure(permission)).subscribe(sub1);
        trigger.compose(mRxPermissions.ensure(permission)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertCompleted();
            sub.assertReceivedOnNext(singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalEachSubscription_trigger_afterDestroy() {
        TestSubscriber<Permission> sub1 = new TestSubscriber<>();
        TestSubscriber<Permission> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};
        PublishSubject<Object> trigger = PublishSubject.create();

        trigger.compose(mRxPermissions.ensureEach(permission)).subscribe(sub1);
        trigger.compose(mRxPermissions.ensureEach(permission)).subscribe(sub2);
        trigger.onNext(null);
        mRxPermissions.onDestroy();
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertNoValues();
            sub.assertNoTerminalEvent();
        }

        sub1 = new TestSubscriber<>();
        sub2 = new TestSubscriber<>();
        trigger.compose(mRxPermissions.ensureEach(permission)).subscribe(sub1);
        trigger.compose(mRxPermissions.ensureEach(permission)).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(0, new String[]{permission}, result);

        verify(mRxPermissions).startShadowActivity(any(String[].class));
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertCompleted();
            sub.assertReceivedOnNext(singletonList(new Permission(permission, true)));
        }
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
