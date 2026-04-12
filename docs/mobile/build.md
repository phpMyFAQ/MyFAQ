# Building the Mobile Apps

## Prerequisites

| Tool          | Minimum version | Notes                                |
|---------------|-----------------|--------------------------------------|
| JDK           | 17 (Temurin)    | `JAVA_HOME` must be set              |
| Android SDK   | API 35          | `ANDROID_HOME` must be set           |
| Xcode         | 15.0            | macOS only, for iOS builds           |
| XcodeGen      | latest          | `brew install xcodegen`              |
| Git           | 2.x             |                                      |

Run `mobile/scripts/bootstrap.sh` to verify your environment.

## Quick start

### Android

```bash
cd mobile
./gradlew :androidApp:assembleDebug
```

The APK lands at `androidApp/build/outputs/apk/debug/`.

### iOS

```bash
cd mobile
./gradlew :shared:assembleSharedDebugXCFramework

cd iosApp
xcodegen generate
open iosApp.xcodeproj
```

Build and run from Xcode on a simulator or device.

## Running tests

```bash
cd mobile

# Shared module — JVM + Android unit tests
./gradlew :shared:testDebugUnitTest

# Shared module — iOS simulator tests (macOS only)
./gradlew :shared:iosSimulatorArm64Test

# All shared tests
./gradlew :shared:allTests
```

## Regenerating the API client

Requires `openapi-generator-cli` (npm) or Docker.

```bash
# First, place the spec:
curl -fsSL \
  https://raw.githubusercontent.com/thorsten/phpMyFAQ/4.2.0/docs/openapi.yaml \
  -o mobile/spec/openapi/v3.2.yaml

# Then generate:
mobile/scripts/generate-api-client.sh
```

Review the diff carefully before committing. The generated client is
committed to the repo — builds do not regenerate it.

## Gradle cache

Gradle and Kotlin/Native caches are keyed on the version catalog
(`mobile/gradle/libs.versions.toml`). If you bump dependency
versions, expect a cold build on the next run.

## Troubleshooting

- **`System.loadLibrary("sqlcipher")` fails on emulator**: ensure
  the emulator image matches the ABI of the SQLCipher native lib
  (x86_64 or arm64-v8a).
- **Xcode build fails with missing `Shared` framework**: run
  `./gradlew :shared:assembleSharedDebugXCFramework` first.
- **KMP metadata resolution errors**: run
  `./gradlew --stop && ./gradlew clean` to clear stale daemons.
