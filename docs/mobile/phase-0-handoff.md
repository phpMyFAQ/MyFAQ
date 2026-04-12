# Phase 0 Handoff

Phase 0 scaffolds the mobile source tree so later phases start from
a known-good, tested baseline. This document records what was pinned,
what was built, and what Phase 1 can assume.

## Pinned tool versions

| Tool                     | Version       | Notes                              |
|--------------------------|---------------|-------------------------------------|
| Kotlin                   | 2.1.0         | KMP + Compose compiler              |
| Gradle                   | (wrapper)     | Bundled with repo, matches Kotlin    |
| Android Gradle Plugin    | 8.7.3         | Compatible with Kotlin 2.1           |
| JDK                      | Temurin 17    | `JAVA_HOME`, CI, and Gradle JVM      |
| Android compile SDK      | 35            |                                      |
| Android min SDK          | 26            | Per plan                             |
| iOS deployment target    | 16.0          | Per plan                             |
| Ktor                     | 3.0.3         |                                      |
| kotlinx.serialization    | 1.7.3         |                                      |
| kotlinx.coroutines       | 1.9.0         |                                      |
| kotlinx.datetime         | 0.6.1         |                                      |
| SQLDelight               | 2.0.2         |                                      |
| Koin                     | 4.0.0         |                                      |
| SQLCipher (Android)      | 4.6.1         |                                      |
| Compose BOM              | 2024.12.01    |                                      |
| openapi-generator        | 7.10.0        | Pinned in CI + generate script       |

**Important**: verify these against the latest stable releases before
the first real build. They were selected as the best-available
stable set at the time Phase 0 was written and may need a bump.

## What was built

- `mobile/` source tree with `shared`, `androidApp`, `iosApp`
  modules following the canonical KMP layout.
- `shared/commonMain`: API wrapper (`MyFaqApi`), `/meta` DTO,
  `MetaLoader`, `HttpClientFactory` (Phase 0 MockEngine),
  `Entitlements` facade stub, `SecureStore` + `DatabaseDriverFactory`
  expect declarations, Koin DI wiring, SQLDelight schema v1
  (instances table only), `DatabaseFactory` with passphrase
  management.
- `shared/androidMain`: `SecureStore` (EncryptedSharedPreferences),
  `DatabaseDriverFactory` (SQLCipher), `EntitlementsPlatform` stub.
- `shared/iosMain`: `SecureStore` (Keychain), `DatabaseDriverFactory`
  (NativeSqliteDriver + Data Protection), `EntitlementsPlatform` stub.
- `shared/commonTest`: `MyFaqApiTest` (full fixture, unknown-keys
  tolerance, MetaLoader rendering), `EntitlementsTest` (always
  false), `SecureStoreContract` (abstract round-trip + clear
  assertions).
- `androidApp`: Compose Material 3 host with `PhaseZeroScreen` that
  loads `/meta` from MockEngine and renders the version string.
  ProGuard rules for release builds.
- `iosApp`: SwiftUI host with `ContentView` that loads `/meta` and
  renders the version string. XcodeGen `project.yml` for reproducible
  `.xcodeproj` generation.
- `.github/workflows/mobile-ci.yml`: lint, shared-unit, android-build,
  ios-build, shared-ios-test.
- `.github/workflows/mobile-openapi-sync.yml`: spec download +
  client regeneration + auto-PR.
- `.github/workflows/mobile-release.yml`: placeholder for Phase 1.
- `mobile/scripts/bootstrap.sh` + `generate-api-client.sh`.
- `docs/mobile/architecture.md`, `build.md`, this handoff doc.

## What is NOT done (Phase 1 picks these up)

- **No real HTTP engine wired in production**. Phase 0 ships
  `ktor-client-mock` in `commonMain`; Phase 1 replaces this with
  OkHttp/Darwin and moves mock to `commonTest` only.
- **No OpenAPI spec committed yet**. `mobile/spec/openapi/v3.2.yaml`
  must be downloaded from phpMyFAQ 4.2.0 before the generator script
  can run. CI workflow handles this automatically.
- **No Gradle wrapper committed**. Run `gradle wrapper` inside
  `mobile/` on first checkout to bootstrap.
- **No real Xcode project committed**. Run `xcodegen generate` inside
  `mobile/iosApp/` to produce `iosApp.xcodeproj`.
- **No signing**. CI builds are unsigned debug artifacts.
- **No store listings, screenshots, or privacy disclosures**.
- **Entitlements always returns `false`**. Real billing lands in
  Phase 3.

## Decisions recorded

- Repo: `phpMyFAQ/MyFAQ`, mobile tree under `mobile/`.
- Minimum phpMyFAQ: 4.2.0, no v3.1 back-compat.
- iOS bundle ID: `app.myfaq.ios`.
- Android application ID: `app.myfaq.android`.
- SQLDelight schema version starts at 1; migrations append-only.
- DB passphrase generated lazily, stored in SecureStore, never leaves
  the device.
- iOS does NOT use SQLCipher — Data Protection Class B suffices.
- `kotlinx.serialization` configured with `ignoreUnknownKeys = true`
  and `explicitNulls = false` for forward compat.
- CI workflows prefixed `mobile-` and path-filtered to `mobile/**`.
- Release tags prefixed `mobile-v` (e.g. `mobile-v0.0.0-foundations`).

## Phase 1 can assume

- A working generated client wrapper for phpMyFAQ v3.2 (just add real
  engine + real spec).
- A working encrypted database with schema migration framework.
- A working secure-storage abstraction on both platforms.
- A working Koin graph shared by both hosts.
- A working CI pipeline for lint, unit tests, and debug builds.
- A single `Entitlements.isPro()` gate ready for Phase 3.
