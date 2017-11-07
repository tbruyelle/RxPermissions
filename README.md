# RxPermissions

[![Build Status](https://api.travis-ci.org/tbruyelle/RxPermissions.svg?branch=master)](https://travis-ci.org/tbruyelle/RxPermissions)

This library allows the usage of RxJava with the new Android M permission model.

## Setup

To use this library your `minSdkVersion` must be >= 11.

In your build.gradle :

```gradle
repositories {
    jcenter() // If not already there
}

dependencies {
    compile 'com.tbruyelle.rxpermissions:rxpermissions:0.9.4@aar'
}
```

## Setup for RxJava2

Thanks to @vanniktech, RxPermissions now supports RxJava2, just change the package name to `com.tbruyelle.rxpermissions2`.

```gradle
dependencies {
    compile 'com.tbruyelle.rxpermissions2:rxpermissions:0.9.4@aar'
}
```

## Migration to 0.9

Version 0.9 now uses a retained fragment to trigger the permission request from the framework. As a result, the `RxPermissions` class is no more a singleton.
To migrate from 0.8 or earlier, just replace the following :

```java
RxPermissions.getInstance(this) -> new RxPermissions(this) // where this is an Activity instance
```

## Usage

Create a `RxPermissions` instance :

```java
RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity instance
```

Example : request the CAMERA permission (with Retrolambda for brevity, but not required)

```java
// Must be done during an initialization phase like onCreate
rxPermissions
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
    .compose(rxPermissions.ensure(Manifest.permission.CAMERA))
    .subscribe(granted -> {
        // R.id.enableCamera has been clicked
    });
```

If multiple permissions at the same time, the result is combined :

```java
rxPermissions
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
rxPermissions
    .requestEach(Manifest.permission.CAMERA,
             Manifest.permission.READ_PHONE_STATE)
    .subscribe(permission -> { // will emit 2 Permission objects
        if (permission.granted) {
           // `permission.name` is granted !
        } else if (permission.shouldShowRequestPermissionRationale) {
           // Denied permission without ask never again
        } else {
           // Denied permission with ask never again
           // Need to go to the settings
        }
    });
```

Look at the `sample` app for more.

## Important read

**As mentioned above, because your app may be restarted during the permission request, the request
must be done during an initialization phase**. This may be `Activity.onCreate`, or
`View.onFinishInflate`, but not *pausing* methods like `onResume`, because you'll potentially create an infinite request loop, as your requesting activity is paused by the framework during the permission request.

If not, and if your app is restarted during the permission request (because of a configuration
change for instance), the user's answer will never be emitted to the subscriber.

You can find more details about that [here](https://github.com/tbruyelle/RxPermissions/issues/69).

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

# License

```
Copyright (C) 2015 Thomas Bruyelle

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
