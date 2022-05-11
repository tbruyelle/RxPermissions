package com.tbruyelle.rxpermissions3;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * @author stqb
 * @description: Android_VideoTools
 * @date :2022/5/11 上午10:45
 */
public class RxPermissionViewModel extends ViewModel {
    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    final Map<String, PublishSubject<Permission>> mSubjects = new HashMap<>();
    boolean mLogging;
    ActivityResultLauncher<String[]> permissionRequest = null;
}
