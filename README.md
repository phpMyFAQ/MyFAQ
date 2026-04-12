# MyFAQ.app

Native iOS and Android client for [phpMyFAQ](https://www.phpmyfaq.de),
built with Kotlin Multiplatform, SwiftUI, and Jetpack Compose.

**Status: Phase 0 (foundations)**. The apps display a placeholder
screen that proves the shared module, API client, encrypted database,
and secure storage are wired end-to-end. No user-visible features yet
-- those land in Phase 1.

- Website: [myfaq.app](https://myfaq.app)
- phpMyFAQ minimum version: **4.2.0**
- Business model: freemium (read + offline free forever; writes behind
  Pro unlock in Phase 3)

---

## Prerequisites

| Tool              | Minimum   | Install                                      |
|-------------------|-----------|----------------------------------------------|
| JDK               | 17+       | `brew install openjdk` or [Temurin](https://adoptium.net) |
| Android SDK       | API 35    | [Android Studio](https://developer.android.com/studio) or `sdkmanager` |
| Xcode             | 15.0      | Mac App Store (macOS only)                   |
| XcodeGen          | latest    | `brew install xcodegen`                      |
| Git               | 2.x       | pre-installed on macOS                       |

Run the bootstrap checker:

```bash
mobile/scripts/bootstrap.sh
```

### Android SDK quick setup (without Android Studio)

```bash
brew install --cask android-commandlinetools
sdkmanager "platforms;android-35" "build-tools;35.0.0"
echo "sdk.dir=$HOME/Library/Android/sdk" > mobile/local.properties
```

### Xcode first-launch setup

If you see `xcodebuild` errors about `IDESimulatorFoundation`:

```bash
sudo xcodebuild -runFirstLaunch
sudo xcodebuild -license accept
```

---

## Repository layout

```
phpMyFAQ/MyFAQ
├── plans/                          Planning docs (source of truth)
│   ├── mobile-app-plan.md          Architecture & feature plan
│   └── phase-0-foundations.md      Phase 0 detailed execution plan
├── docs/mobile/                    Technical docs
│   ├── architecture.md             Module diagram & boundaries
│   ├── build.md                    Build reference
│   └── phase-0-handoff.md          Pinned versions & decisions
├── .github/workflows/              CI/CD
│   ├── mobile-ci.yml               Lint + test + debug builds
│   ├── mobile-openapi-sync.yml     API client regeneration
│   └── mobile-release.yml          Signed release (Phase 1)
└── mobile/                         Source tree (Gradle root)
    ├── shared/                     Kotlin Multiplatform module
    │   ├── src/commonMain/         Business logic, API, DB, DI
    │   ├── src/androidMain/        Android platform actuals
    │   ├── src/iosMain/            iOS platform actuals
    │   └── src/commonTest/         Shared tests
    ├── androidApp/                 Jetpack Compose host
    ├── iosApp/                     SwiftUI host (XcodeGen)
    ├── spec/openapi/               Pinned phpMyFAQ API spec
    └── scripts/                    Dev tooling
```

---

## Building & running

### Android

```bash
cd mobile
./gradlew :androidApp:assembleDebug
```

The debug APK lands at
`mobile/androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

Install on a connected device or emulator:

```bash
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Or open `mobile/` as a project in Android Studio and run from the IDE.

### iOS

```bash
# 1. Build the shared KMP framework
cd mobile
./gradlew :shared:assembleSharedDebugXCFramework

# 2. Generate the Xcode project (one-time, re-run after project.yml changes)
cd iosApp
xcodegen generate

# 3a. Open in Xcode and run (Cmd+R)
open iosApp.xcodeproj

# 3b. Or build from CLI
xcodebuild \
  -project iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

To run on a simulator from CLI after building:

```bash
xcrun simctl boot "iPhone 16" 2>/dev/null
xcrun simctl install booted \
  build/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch booted app.myfaq.ios
```

> **Note:** After changing shared Kotlin code, rebuild the
> XCFramework (step 1) before rebuilding in Xcode. The pre-build
> script in `project.yml` does this automatically but adds build
> time. For faster iteration, disable it in Xcode's Build Phases
> and run the Gradle task manually.

---

## Running tests

```bash
cd mobile

# Shared module tests (JVM — fast, no device/simulator needed)
./gradlew :shared:testDebugUnitTest

# Shared module iOS tests (requires macOS + Xcode)
./gradlew :shared:iosSimulatorArm64Test

# All shared tests
./gradlew :shared:allTests
```

### What the tests cover (Phase 0)

| Test file | What it validates |
|-----------|-------------------|
| `MyFaqApiTest` | `/meta` deserialization, unknown-key tolerance, MetaLoader rendering |
| `EntitlementsTest` | `Entitlements.isPro()` always returns `false` |
| `SecureStoreContract` | Round-trip put/get/remove/clear (abstract; run by platform tests) |

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Shared logic | Kotlin Multiplatform |
| Android UI | Jetpack Compose, Material 3 |
| iOS UI | SwiftUI |
| HTTP | Ktor (MockEngine in Phase 0; OkHttp/Darwin in Phase 1) |
| Serialization | kotlinx.serialization |
| Database | SQLDelight + SQLCipher (Android) / Data Protection (iOS) |
| Secure storage | EncryptedSharedPreferences (Android) / Keychain (iOS) |
| DI | Koin |
| API codegen | openapi-generator (Kotlin multiplatform target) |

### Pinned versions

See [`mobile/gradle/libs.versions.toml`](mobile/gradle/libs.versions.toml)
for the single source of truth. Key versions:

- Kotlin **2.1.20**, Gradle **9.4.1**, AGP **8.10.0**
- Ktor **3.0.3**, SQLDelight **2.0.2**, Koin **4.0.0**
- Android min SDK **26**, iOS **16.0**

---

## Regenerating the API client

The Kotlin API client is generated from the pinned phpMyFAQ OpenAPI
spec at `mobile/spec/openapi/v3.2.yaml`. To update:

```bash
# Download the spec from a phpMyFAQ release
curl -fsSL \
  https://raw.githubusercontent.com/thorsten/phpMyFAQ/4.2.0/docs/openapi.yaml \
  -o mobile/spec/openapi/v3.2.yaml

# Regenerate (requires openapi-generator-cli or Docker)
mobile/scripts/generate-api-client.sh
```

Or trigger the `mobile-openapi-sync` GitHub Actions workflow.

---

## Project structure (shared module)

```
shared/src/commonMain/kotlin/app/myfaq/shared/
├── api/
│   ├── MyFaqApi.kt              Interface + impl (Phase 0: /meta only)
│   ├── MetaLoader.kt            Callback facade for platform UI
│   ├── HttpClientFactory.kt     MockEngine wiring (Phase 0)
│   └── dto/Meta.kt              /meta response DTO
├── data/
│   └── DatabaseFactory.kt       Encrypted DB creation + passphrase mgmt
├── di/
│   └── SharedModule.kt          Koin modules + initKoin()
├── domain/
│   └── Instance.kt              Instance + AuthMode domain models
├── entitlements/
│   └── Entitlements.kt          Pro gate facade + expect object
└── platform/
    ├── SecureStore.kt            expect class (put/get/remove/clear)
    └── DatabaseDriverFactory.kt  expect class (create with passphrase)
```

---

## Phase roadmap

| Phase | What ships | Status |
|-------|-----------|--------|
| **0** | KMP scaffold, CI, encrypted DB, secure storage, Entitlements stub | **current** |
| **1** | Workspaces, categories, FAQ list/detail, server search, paywall shell | planned |
| **2** | Offline: SQLite + FTS5, background sync, attachments (public v1.0.0) | planned |
| **3** | Pro: StoreKit 2 + Play Billing, login/OAuth2, ask/comment/rate (v2.0.0) | planned |
| **4** | Accessibility, localization, telemetry scrub | planned |
| **5** | Widgets, watch app, iPad split-view, push notifications | planned |

See [`plans/mobile-app-plan.md`](plans/mobile-app-plan.md) for the
full architecture plan and
[`plans/phase-0-foundations.md`](plans/phase-0-foundations.md) for
Phase 0 details.

---

## Contributing

1. Check out the repo and run `mobile/scripts/bootstrap.sh`
2. Make changes under `mobile/`
3. Run `cd mobile && ./gradlew :shared:allTests` before pushing
4. CI runs on every push to `main` and on PRs (path-filtered to
   `mobile/**`)

### Code style

- Kotlin: enforced by ktlint + detekt (run via `./gradlew ktlintCheck detekt`)
- Swift: swiftlint (configured in CI)
- Commit messages: [Conventional Commits](https://www.conventionalcommits.org/)

---

## License

[Mozilla Public License 2.0](LICENSE)

## Copyright

© 2026 Thorsten Rinne. All rights reserved.
