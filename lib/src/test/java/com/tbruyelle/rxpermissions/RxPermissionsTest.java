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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import org.junit.*;
import org.mockito.*;

import java.util.Collections;

import rx.observers.TestSubscriber;

import static org.mockito.Mockito.*;

public class RxPermissionsTest {

    @Mock Activity mActivity;

    RxPermissions mRxPermissions;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mRxPermissions = spy(RxPermissions.getInstance(mActivity));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void singleSubscription_granted() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int id = mRxPermissions.permissionID(permission);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        mRxPermissions.request(permission).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(id, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void singleSubscription_alreadyGranted() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(true);

        mRxPermissions.request(permission).subscribe(sub);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void singleSubscription_denied() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int id = mRxPermissions.permissionID(permission);
        int[] result = new int[]{PackageManager.PERMISSION_DENIED};

        mRxPermissions.request(permission).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(id, new String[]{permission}, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(Collections.singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String permission = Manifest.permission.READ_PHONE_STATE;
        when(mRxPermissions.isGranted(permission)).thenReturn(false);
        int id = mRxPermissions.permissionID(permission);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        mRxPermissions.request(permission).subscribe(sub1);
        mRxPermissions.request(permission).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(id, new String[]{permission}, result);

        verify(mActivity).requestPermissions(any(String[].class), anyInt());
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(Collections.singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void singleSubscription_severalPermissions_granted() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int id = mRxPermissions.permissionID(permissions);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        mRxPermissions.request(permissions).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(id, permissions, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void singleSubscription_severalPermissions_oneDenied() {
        TestSubscriber<Boolean> sub = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int id = mRxPermissions.permissionID(permissions);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};

        mRxPermissions.request(permissions).subscribe(sub);
        mRxPermissions.onRequestPermissionsResult(id, permissions, result);

        sub.assertNoErrors();
        sub.assertTerminalEvent();
        sub.assertUnsubscribed();
        sub.assertReceivedOnNext(Collections.singletonList(false));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_severalSamePermissions() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int id = mRxPermissions.permissionID(permissions);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        mRxPermissions.request(permissions).subscribe(sub1);
        mRxPermissions.request(permissions).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(id, permissions, result);

        verify(mActivity).requestPermissions(any(String[].class), anyInt());
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(Collections.singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_severalMixingPermissions_requestSeveralFirst() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int id = mRxPermissions.permissionID(permissions);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};

        mRxPermissions.request(permissions).subscribe(sub1);
        mRxPermissions.request(Manifest.permission.CAMERA).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(id, permissions, result);

        verify(mActivity).requestPermissions(any(String[].class), anyInt());
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(Collections.singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_severalMixingPermissions_requestOnceFirst() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int id = mRxPermissions.permissionID(permissions);
        int[] result = new int[]{PackageManager.PERMISSION_GRANTED};

        mRxPermissions.request(Manifest.permission.CAMERA).subscribe(sub1);
        mRxPermissions.request(permissions).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(id, new String[]{Manifest.permission.READ_PHONE_STATE}, result);
        mRxPermissions.onRequestPermissionsResult(id, new String[]{Manifest.permission.CAMERA}, result);

        verify(mActivity, times(2)).requestPermissions(any(String[].class), anyInt());
        for (TestSubscriber sub : new TestSubscriber[]{sub1, sub2}) {
            sub.assertNoErrors();
            sub.assertTerminalEvent();
            sub.assertUnsubscribed();
            sub.assertReceivedOnNext(Collections.singletonList(true));
        }
    }

    @Test
    @TargetApi(Build.VERSION_CODES.M)
    public void severalSubscription_severalMixingPermissions_oneDenied() {
        TestSubscriber<Boolean> sub1 = new TestSubscriber<>();
        TestSubscriber<Boolean> sub2 = new TestSubscriber<>();
        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        when(mRxPermissions.isGranted(Matchers.<String>anyVararg())).thenReturn(false);
        int id = mRxPermissions.permissionID(permissions);
        int[] resultGranted = new int[]{PackageManager.PERMISSION_GRANTED};
        int[] resultDenied = new int[]{PackageManager.PERMISSION_DENIED};

        mRxPermissions.request(Manifest.permission.CAMERA).subscribe(sub1);
        mRxPermissions.request(permissions).subscribe(sub2);
        mRxPermissions.onRequestPermissionsResult(id, new String[]{Manifest.permission.READ_PHONE_STATE}, resultDenied);
        mRxPermissions.onRequestPermissionsResult(id, new String[]{Manifest.permission.CAMERA}, resultGranted);

        verify(mActivity, times(2)).requestPermissions(any(String[].class), anyInt());
        sub1.assertNoErrors();
        sub1.assertTerminalEvent();
        sub1.assertUnsubscribed();
        sub1.assertReceivedOnNext(Collections.singletonList(true));
        sub2.assertNoErrors();
        sub2.assertTerminalEvent();
        sub2.assertUnsubscribed();
        sub2.assertReceivedOnNext(Collections.singletonList(false));
    }
}
