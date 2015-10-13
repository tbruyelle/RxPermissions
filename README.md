# RxPermissions

[![Build Status](https://api.travis-ci.org/tbruyelle/RxPermissions.svg?branch=master)](https://travis-ci.org/tbruyelle/RxPermissions)

This library allows the usage of RxJava with the new Android M permission model.

Example (With Retrolambda for brevity, but not required):

```java
 RxPermissions.getInstance(this) // this = a Context
                .request(Manifest.permission.CAMERA)
                .subscribe(granted -> {
                    if (granted) { // Always true pre-M
                       // I can control the camera now
                    } else {
                       // Oups permission denied
                    }
                });
```

If multiple permissions at the same time, the result is combined :

```java
 RxPermissions.getInstance(this) // this = a Context
                .request(Manifest.permission.CAMERA,
                       Manifest.permission.READ_PHONE_STATE)
                .subscribe(granted -> {
                    if (granted) {
                       // All requested permissions are granted
                    } else {
                       // At least one permission is denied
                    }
                });
```

You can also observe a detailed result with `requestEach` :

```java
 RxPermissions.getInstance(this) // this = a Context
                .requestEach(Manifest.permission.CAMERA,
                       Manifest.permission.READ_PHONE_STATE)
                .subscribe(permission -> { // will emit 2 Permission objects
                    if (permission.granted) {
                       // `permission.name` is granted !
                    }
                });
```

Look at the `sample` app for more.

## Status

**This library is at a very early stage of development, so contributions are welcome.
It should not be used in production, or at your own risk.**

The first improvement needed is lifecycle management (see [#3](https://github.com/tbruyelle/RxPermissions/issues/3)). I need some help to find and design a well shaped solution.

## Benefits

- Avoid worrying about the framework version. If the sdk is pre-M, the observer will automatically receive a granted result.

- Prevents you to split your code between the permission request and the result handling.
Currently without this library you have to request the permission in one place and handle the result in `Activity.onRequestPermissionsResult()`.

- Handles multiple permission requests out of the box.
For instance if during the initialization of your app you request the same permission in 2 different places, only one request will
be made to the framework. As a result, only one popup will appear to the user, but his response will be dispatched to all requesters.

- Easy testing.

- All what RX provides about transformation, filter, chaining...

## Setup

In your build.gradle :

```gradle
repositories {
    maven { url "http://dl.bintray.com/tbruyelle/tbruyelle" }
}

dependencies {
    compile 'com.tbruyelle.rxpermissions:rxpermissions:0.2.0@aar'
}
```
