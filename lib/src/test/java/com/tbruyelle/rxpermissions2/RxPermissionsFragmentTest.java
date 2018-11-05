package com.tbruyelle.rxpermissions2;

import android.support.v4.app.FragmentActivity;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE)
public class RxPermissionsFragmentTest {

    @Test
    public void getSubjects_shouldReturnNothing_whenThereAreNoSubjects() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        f.prepareRequest();
        final int requestCode = f.getPermissionRequestCode();

        Map<String, ? extends Subject<Permission>> subjects = f.getSubjectsCopy(requestCode);
        Assert.assertEquals(0, subjects.size());
    }

    @Test
    public void getSubjects_shouldReturnSubject_whenSubjectIsRegistered() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        final PublishSubject<Permission> subject = PublishSubject.create();
        f.prepareRequest();
        final int requestCode = f.getPermissionRequestCode();

        f.setSubjectForPermission("xxx", subject);

        final Map<String, ? extends Subject<Permission>> subjects = f.getSubjectsCopy(requestCode);
        Assert.assertEquals(1, subjects.size());
        Assert.assertEquals(subject, subjects.get("xxx"));
    }

    @Test
    public void onRequestPermissionsCanceled_shouldCompleteAndClearSubjects() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        final PublishSubject<Permission> subject = PublishSubject.create();
        f.prepareRequest();
        final int requestCode = f.getPermissionRequestCode();
        f.setSubjectForPermission("xxx", subject);

        f.onRequestPermissionsCanceled(requestCode);

        final Map<String, ? extends Subject<Permission>> subjects = f.getSubjectsCopy(requestCode);
        Assert.assertEquals(0, subjects.size());
        subject.test().assertComplete();
    }

    @Test
    public void onRequestPermissionsResult_shouldCallOnRequestPermissionsCanceled_whenInterrupted() {
        final RxPermissionsFragment f = spy(new RxPermissionsFragment());

        f.onRequestPermissionsResult(42, new String[]{}, new int[]{});

        verify(f).onRequestPermissionsCanceled(42);
    }

    @Test
    public void onRequestPermissionsResult_shouldNotCallOnRequestPermissionsCanceled_whenValid() {
        final RxPermissionsFragment f = spy(new RxPermissionsFragment());
        f.prepareRequest();
        f.setSubjectForPermission("xxx", PublishSubject.<Permission>create());
        f.finishRequest();
        final int requestCode = f.getPermissionRequestCode();

        f.onRequestPermissionsResult(requestCode, new String[]{"xxx"}, new int[]{0});

        verify(f, never()).onRequestPermissionsCanceled(anyInt());
    }

    @Test(expected = AssertionError.class)
    public void onRequestPermissionsResult_shouldThrow_whenThereAreNoSubjectsForRequestCode() {
        final RxPermissionsFragment f = new RxPermissionsFragment();

        // We're expecting AssertionError from assert statement, which is enabled during testing.
        f.onRequestPermissionsResult(42, new String[]{"xxx"}, new int[]{0});
    }

    @Test(expected = IllegalStateException.class)
    public void requestPermissions_shouldThrow_whenRequestIsPreparing() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        f.prepareRequest();

        f.requestPermissions(new String[]{"xxx"});
    }

    @Test(expected = IllegalStateException.class)
    public void requestPermissions_shouldThrow_whenThereIsNoPreparedRequest() {
        final RxPermissionsFragment f = new RxPermissionsFragment();

        f.requestPermissions(new String[]{"xxx"});
    }

    @Test
    public void requestPermissions_shouldSucceed_whenRequestIsSealed() {
        final RxPermissionsFragment f = spy(new RxPermissionsFragment());
        doNothing().when(f).requestPermissions(any(String[].class), anyInt());
        f.prepareRequest();
        f.finishRequest();

        f.requestPermissions(new String[]{"xxx"});

        verify(f).requestPermissions(new String[]{"xxx"});
    }

    @Test(expected = IllegalStateException.class)
    public void prepareRequest_shouldThrow_whenRequestIsSealed() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        f.prepareRequest();
        f.finishRequest();

        f.prepareRequest();
    }

    @Test(expected = IllegalStateException.class)
    public void prepareRequest_shouldThrow_whenRequestIsPreparing() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        f.prepareRequest();

        f.prepareRequest();
    }

    @Test
    public void prepareRequest_shouldSucceed_whenThereIsNoRequest() {
        final RxPermissionsFragment f = new RxPermissionsFragment();

        f.prepareRequest();
    }

    @Test(expected = IllegalStateException.class)
    public void finishRequest_shouldThrow_whenRequestIsSealed() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        f.prepareRequest();
        f.finishRequest();

        f.finishRequest();
    }

    @Test(expected = IllegalStateException.class)
    public void finishRequest_shouldThrow_whenThereIsNoRequest() {
        final RxPermissionsFragment f = new RxPermissionsFragment();

        f.finishRequest();
    }

    @Test
    public void finishRequest_shouldSucceed_whenRequestIsPreparing() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        f.prepareRequest();

        f.finishRequest();
    }

    @Test
    @Config(shadows = {ShadowActivityCompat.class})
    public void requestPermissions_shouldFailSubject_whenThereIsAnotherPendingPermissionRequest() {
        final RxPermissionsFragment f = spy(new RxPermissionsFragment());
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(f, "xxx")
                .commitNow();

        final PublishSubject<Permission> subjectA = PublishSubject.create();
        f.prepareRequest();
        f.setSubjectForPermission("a", subjectA);
        f.finishRequest();
        f.requestPermissions(new String[]{"a"});

        final PublishSubject<Permission> subjectB = PublishSubject.create();
        final TestObserver<Permission> testObserver = subjectB.test();
        f.prepareRequest();
        f.setSubjectForPermission("b", subjectB);
        f.finishRequest();
        f.requestPermissions(new String[]{"b"});

        testObserver
                .assertValue(new Permission("b", false, false))
                .assertComplete();
    }

    @Test
    @Config(shadows = {ShadowActivityCompat.class})
    public void requestPermissions_shouldSucceedSubject_whenThereIsNoOtherPendingPermissionRequest() {
        final RxPermissionsFragment f = spy(new RxPermissionsFragment());
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(f, "xxx")
                .commitNow();

        final PublishSubject<Permission> subjectA = PublishSubject.create();
        f.prepareRequest();
        f.setSubjectForPermission("a", subjectA);
        f.finishRequest();
        f.requestPermissions(new String[]{"a"});

        ShadowActivityCompat.processPendingPermissionsRequest(activity, true);

        final PublishSubject<Permission> subjectB = PublishSubject.create();
        final TestObserver<Permission> testObserver = subjectB.test();
        f.prepareRequest();
        f.setSubjectForPermission("b", subjectB);
        f.finishRequest();
        f.requestPermissions(new String[]{"b"});

        testObserver
                .assertNoValues()
                .assertNotComplete();
    }
}
