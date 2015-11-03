# RxPermissions

[![Build Status](https://api.travis-ci.org/tbruyelle/RxPermissions.svg?branch=master)](https://travis-ci.org/tbruyelle/RxPermissions)

This library allows the usage of RxJava with the new Android M permission model.

Example (With Retrolambda for brevity, but not required):

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Must be done during an initialization phase like onCreate
    RxPermissions.getInstance(this)
        .request(Manifest.permission.CAMERA)
        .subscribe(granted -> {
            if (granted) { // Always true pre-M
               // I can control the camera now
            } else {
               // Oups permission denied
            }
        });
}
```

If multiple permissions at the same time, the result is combined :

```java
RxPermissions.getInstance(this)
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
RxPermissions.getInstance(this)
.requestEach(Manifest.permission.CAMERA,
         Manifest.permission.READ_PHONE_STATE)
    .subscribe(permission -> { // will emit 2 Permission objects
        if (permission.granted) {
           // `permission.name` is granted !
        }
    });
```

Look at the `sample` app for more.

## Important read

**Because your app may be restarted during the permission request, the request must be done 
during an initialization phase**. This may be `Activity.onCreate/onResume`, or `View.onFinishInflate` or others.

If not, and if your app is restarted during the permission request (because of a configuration change for instance),
the user's answer will never be emitted to the subscriber.

If you need to trigger the permission request from a specific event and not during initialization phase, you have
to pass an extra parameter to the library methods, the trigger.
The trigger must be an `Observable`, you can use  [JakeWharton/RxBinding](https://github.com/JakeWharton/RxBinding)
to turn your view to an observable (not included in the library).

Example :

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Observable<Void> trigger = RxView.clicks(findViewById(R.id.enableCamera));

    RxPermissions.getInstance(this)
        // The trigger is passed as first arg
        .request(trigger, Manifest.permission.CAMERA)
        .subscribe(granted -> {
            // R.id.enableCamera has been clicked
            // or the app has been restarted during the permission request.
        });
}
```


## Status

**This library is at a very early stage of development, so contributions are welcome.
It should not be used in production, or at your own risk.**

## Benefits

- Avoid worrying about the framework version. If the sdk is pre-M, the observer will automatically receive a granted result.

- Prevents you to split your code between the permission request and the result handling.
Currently without this library you have to request the permission in one place and handle the result in `Activity.onRequestPermissionsResult()`.

- Handles multiple permission requests out of the box.
For instance if during the initialization of your app you request the same permission in 2 different places, only one request will
be made to the framework. As a result, only one popup will appear to the user, but his response will be dispatched to all requesters.

- All what RX provides about transformation, filter, chaining...

## Setup

To use this library your ` minSdkVersion` must be >= 9.

In your build.gradle :

```gradle
repositories {
    maven { url "http://dl.bintray.com/tbruyelle/tbruyelle" }
}

dependencies {
    compile 'com.tbruyelle.rxpermissions:rxpermissions:0.4.2@aar'
}
```
