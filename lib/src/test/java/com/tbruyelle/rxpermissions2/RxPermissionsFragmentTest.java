package com.tbruyelle.rxpermissions2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE)
public class RxPermissionsFragmentTest {

    @Test
    public void getSubjects_shouldReturnNothing_whenThereAreNoSubjects() {
        final RxPermissionsFragment f = new RxPermissionsFragment();

        Map<String, ? extends Subject<Permission>> subjects = f.getSubjects();
        Assert.assertEquals(0, subjects.size());
    }

    @Test
    public void getSubjects_shouldReturnSubject_whenSubjectIsRegistered() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        final PublishSubject<Permission> subject = PublishSubject.create();

        f.setSubjectForPermission("xxx", subject);

        final Map<String, ? extends Subject<Permission>> subjects = f.getSubjects();
        Assert.assertEquals(1, subjects.size());
        Assert.assertEquals(subject, subjects.get("xxx"));
    }

    @Test
    public void onRequestPermissionsCanceled_shouldCompleteAndClearSubjects() {
        final RxPermissionsFragment f = new RxPermissionsFragment();
        final PublishSubject<Permission> subject = PublishSubject.create();
        f.setSubjectForPermission("xxx", subject);

        f.onRequestPermissionsCanceled();

        final Map<String, ? extends Subject<Permission>> subjects = f.getSubjects();
        Assert.assertEquals(0, subjects.size());
        subject.test().assertComplete();
    }

    @Test
    public void onRequestPermissionsResult_shouldCallOnRequestPermissionsCanceled_whenInterrupted() {
        final RxPermissionsFragment f = spy(new RxPermissionsFragment());

        f.onRequestPermissionsResult(42, new String[]{}, new int[]{});

        verify(f).onRequestPermissionsCanceled();
    }

    @Test
    public void onRequestPermissionsResult_shouldNotCallOnRequestPermissionsCanceled_whenValid() {
        final RxPermissionsFragment f = spy(new RxPermissionsFragment());

        f.onRequestPermissionsResult(42, new String[]{"xxx"}, new int[]{0});

        verify(f, never()).onRequestPermissionsCanceled();
    }
}
