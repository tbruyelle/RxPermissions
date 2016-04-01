# RxPermissions

[![Build Status](https://api.travis-ci.org/tbruyelle/RxPermissions.svg?branch=v0.7.0)](https://travis-ci.org/tbruyelle/RxPermissions)

This library allows the usage of RxJava with the new Android M permission model.

Example (with Retrolambda for brevity, but not required):

```java
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
```

If you need to trigger the permission request from a specific event, you need to setup your event
as an observable inside an initialization phase.

You can use [JakeWharton/RxBinding](https://github.com/JakeWharton/RxBinding) to turn your view to
an observable (not included in the library).

Example :

```java
// Must be done during an initialization phase like onCreate
RxView.clicks(findViewById(R.id.enableCamera))
    .compose(RxPermissions.getInstance(this).ensure(Manifest.permission.CAMERA))
    .subscribe(granted -> {
        // R.id.enableCamera has been clicked
    });
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

You can also observe a detailed result with `requestEach` or `ensureEach` :

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

**As mentioned above, because your app may be restarted during the permission request, the request
must be done during an initialization phase**. This may be `Activity.onCreate/onResume`, or
`View.onFinishInflate` or others.

If not, and if your app is restarted during the permission request (because of a configuration
change for instance), the user's answer will never be emitted to the subscriber.

## Migration to 0.6.x

Version 0.6.0 replaced the methods `request(trigger, permission...)` and `requestEach(trigger, permission...)`
by *composables* methods `ensure(permission...)` and `ensureEach(permission...)`. Read the second
example to see how to use them.

## Status

This library is still beta, so contributions are welcome. 
I'm currently using it in production since months without issue.

## Benefits

- Avoid worrying about the framework version. If the sdk is pre-M, the observer will automatically
receive a granted result.

- Prevents you to split your code between the permission request and the result handling.
Currently without this library you have to request the permission in one place and handle the result
in `Activity.onRequestPermissionsResult()`.

- All what RX provides about transformation, filter, chaining...

## Setup

To use this library your `minSdkVersion` must be >= 9.

In your build.gradle :

```gradle
repositories {
    jcenter() // If not already there
}

dependencies {
    compile 'com.tbruyelle.rxpermissions:rxpermissions:0.7.0@aar'
}
```
