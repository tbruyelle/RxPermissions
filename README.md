# RxPermissions

[![Build Status](https://api.travis-ci.org/tbruyelle/RxPermissions.svg?branch=master)](https://travis-ci.org/tbruyelle/RxPermissions)

This library allows the usage of RxJava with the new Android M permission model.

```java
 rxPermissions.request(Manifest.permission.CAMERA)
                .subscribe(granted -> { // With retrolambda
                    if (granted) {
                       // I can control the camera now
                    } else {
                       // Oups permission denied
                    }
                });
```

Look at the `sample` app to see more.

## Benefits

- Avoid worrying about the framework version. If the sdk is pre-M, the observer will automatically receive a granted result.

- Prevents to split your code between the permission request and the result handling (usually in `Activity.onRequestPermissionsResult()`).

- Handles multiple permission requests out of the box.
For instance if during the initialization of your app you request the same permission in 2 different places, only one request will
be made to the framework.

- Facilitates testing

- All what RX provides about transformation and filter.

## Setup

In your activity :

```java
 private RxPermissions mRxPermissions;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);

     mRxPermissions = new RxPermissions(this);
 }

 @Override
 public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
     mRxPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
 }

```

