# RxPermissions

[![](https://jitpack.io/v/tbruyelle/RxPermissions.svg)](https://jitpack.io/#tbruyelle/RxPermissions) [![BuildVersion](https://buildstats.info/nuget/RxPermissions)](https://www.nuget.org/packages/RxPermissions/) [![Build Status](https://api.travis-ci.org/tbruyelle/RxPermissions.svg?branch=master)](https://travis-ci.org/tbruyelle/RxPermissions)

This library allows the usage of RxJava with the new Android M permission model.

## Setup

To use this library your `minSdkVersion` must be >= 11.

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.tbruyelle:rxpermissions:0.10.2'
}
```

## Usage

Create a `RxPermissions` instance :

```java
final RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity or Fragment instance
```

**NOTE:** `new RxPermissions(this)` the `this` parameter can be a FragmentActivity or a Fragment. If you are using `RxPermissions` inside of a fragment you should pass the fragment instance(`new RxPermissions(this)`) as constructor parameter rather than `new RxPermissions(fragment.getActivity())` or you could face a `java.lang.IllegalStateException: FragmentManager is already executing transactions`.  

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

You can also get combined detailed result with `requestEachCombined` or `ensureEachCombined` :

```java
rxPermissions
    .requestEachCombined(Manifest.permission.CAMERA,
             Manifest.permission.READ_PHONE_STATE)
    .subscribe(permission -> { // will emit 1 Permission object
        if (permission.granted) {
           // All permissions are granted !
        } else if (permission.shouldShowRequestPermissionRationale)
           // At least one denied permission without ask never again
        } else {
           // At least one denied permission with ask never again
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
