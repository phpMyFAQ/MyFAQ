# Phase 0 — Foundations

This document is the detailed execution plan for **Phase 0** of
MyFAQ.app. It expands the single-bullet summary in
`plans/mobile-app-plan.md` into the concrete tasks, tooling, and exit
criteria needed to stand up the project before any user-visible
feature work begins.

Phase 0 ends when a developer can check out the repository, run a
single command per platform, and get a signed debug build of both
apps installed on a simulator / emulator — with the shared module,
generated API client, encrypted database, secure storage, and
`Entitlements` facade stub all wired up end-to-end but not yet
exercised by any real screen.

## Goals

- Stand up the mobile source tree inside the existing
  `phpMyFAQ/MyFAQ` repository (<https://github.com/phpMyFAQ/MyFAQ>)
  with a reproducible, hermetic build on macOS (iOS) and Linux
  (Android).
- Lock the Kotlin Multiplatform module layout and the two native host
  projects (SwiftUI + Jetpack Compose) so later phases only add
  features, never re-shape the build.
- Commit the first generated Kotlin API client from the pinned phpMyFAQ
  `v3.2` OpenAPI specification and prove it compiles into both targets.
- Implement the data-at-rest and credentials-at-rest primitives every
  later phase depends on: encrypted SQLDelight database, Keychain /
  Keystore wrappers, and an `Entitlements` facade stub that always
  returns `false`.
- Have CI green on every push, with lint, unit tests, and
  signed-artifact debug builds for both platforms.

## Non-goals

- No UI beyond a placeholder screen on each platform that demonstrates
  the shared module is wired in (e.g. a label displaying the pinned
  phpMyFAQ version the generated client was built against).
- No networking against a real phpMyFAQ instance. All Phase 0 tests run
  against Ktor `MockEngine` fixtures.
- No Pro / billing integration. `Entitlements` is a hard-coded stub.
- No store listings, screenshots, or privacy disclosures. Those land in
  Phase 4.

## Deliverables

1. The skeleton below checked into `phpMyFAQ/MyFAQ` on `main`
   alongside the existing `plans/` and `LICENSE`.
2. A green GitHub Actions run covering lint, unit tests, and debug
   builds for both platforms.
3. A tagged `mobile-v0.0.0-foundations` pre-release on GitHub so
   future phases have a stable baseline to diff against. The
   `mobile-` tag prefix keeps the mobile release train independent of
   any future non-mobile tags in the same repository.
4. `docs/mobile/phase-0-handoff.md` listing the decisions made, the
   tool versions pinned, and any carry-overs into Phase 1.

## Repository layout

The mobile source tree is nested under a top-level `mobile/`
directory so it coexists cleanly with the existing `plans/` and
`LICENSE`, and so any future non-mobile content in the same
repository stays out of the Gradle / Xcode working set.

```text
phpMyFAQ/MyFAQ  (repository root — this checkout)
├── LICENSE
├── CLAUDE.md
├── plans/                             # existing planning docs
│   ├── mobile-app-plan.md
│   └── phase-0-foundations.md
├── docs/
│   └── mobile/
│       ├── architecture.md            # module diagram + boundaries
│       ├── build.md                   # how to build locally
│       └── phase-0-handoff.md
├── .github/
│   └── workflows/
│       ├── mobile-ci.yml              # lint + unit + debug builds
│       ├── mobile-openapi-sync.yml    # regenerates the API client on
│       │                              # phpMyFAQ release tags
│       └── mobile-release.yml         # tag-driven signed artifact build
└── mobile/
    ├── settings.gradle.kts            # Gradle root for the mobile build
    ├── build.gradle.kts
    ├── gradle.properties
    ├── gradlew / gradlew.bat
    ├── buildSrc/                      # shared Gradle conventions in Kotlin
    ├── gradle/
    │   └── libs.versions.toml         # single source of truth for deps
    ├── shared/                        # Kotlin Multiplatform module
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── commonMain/kotlin/
    │       │   └── app/myfaq/shared/
    │       │       ├── api/           # generated client lives here
    │       │       ├── data/          # SQLDelight models, DAOs
    │       │       ├── domain/        # pure models, no framework deps
    │       │       ├── entitlements/  # Entitlements facade
    │       │       └── platform/      # expect declarations
    │       ├── commonTest/
    │       ├── androidMain/kotlin/    # actual impls for Android
    │       └── iosMain/kotlin/        # actual impls for iOS
    ├── androidApp/                    # Jetpack Compose host
    │   ├── build.gradle.kts
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       └── kotlin/app/myfaq/android/
    ├── iosApp/                        # SwiftUI host
    │   ├── iosApp.xcodeproj
    │   └── iosApp/
    │       ├── iosAppApp.swift
    │       └── ContentView.swift
    ├── spec/
    │   └── openapi/
    │       ├── v3.2.yaml              # pinned spec copy
    │       └── VERSION                # phpMyFAQ tag it was captured at
    ├── scripts/
    │   ├── generate-api-client.sh     # wraps openapi-generator
    │   └── bootstrap.sh               # one-shot dev env check
    ├── .editorconfig
    ├── .gitignore                     # Gradle / Xcode / DerivedData
    └── README.md                      # mobile-specific quickstart
```

The repository root `LICENSE` is reused — the mobile tree does not
ship its own. GitHub Actions workflow files must live at
`.github/workflows/` (not inside `mobile/`), so mobile workflows are
prefixed `mobile-*.yml` to keep them visually grouped and out of the
way of any non-mobile workflows added later.

## Pinned tool versions

Recorded in `mobile/gradle/libs.versions.toml` and in
`docs/mobile/phase-0-handoff.md`. Bump deliberately; never
auto-upgrade.

- Kotlin: latest stable at Phase 0 kickoff (target `2.x`).
- Gradle: matching Kotlin's recommended minimum.
- Android Gradle Plugin: matching Kotlin + AGP compatibility matrix.
- JDK: Temurin 17 for both local builds and CI.
- Android compile SDK: current stable (expected API 35 at kickoff).
- Android min SDK: 26, per `mobile-app-plan.md`.
- Xcode: latest stable supported by the KMP Kotlin version.
- iOS deployment target: iOS 16, per `mobile-app-plan.md`.
- Ktor, kotlinx.serialization, SQLDelight, Koin, openapi-generator:
  latest stable at kickoff, pinned verbatim.

Exact numeric versions are filled in at the moment the repository is
created and captured in the handoff doc so future maintainers can
reproduce the kickoff state.

## Shared module scaffolding

The `shared` module exposes a minimal surface to native code and hides
every framework dependency inside `commonMain`.

### Source sets

- `commonMain` — all business logic, DTOs, API client, DAOs,
  `Entitlements` facade, Koin modules.
- `commonTest` — JVM-backed tests with a Ktor `MockEngine` and an
  in-memory SQLDelight driver.
- `androidMain` — `actual` implementations for secure storage, the
  SQLCipher driver, and the Android Keystore entitlement cache.
- `iosMain` — `actual` implementations for Keychain wrappers and the
  Core Data-protected SQLDelight driver.

### `expect` / `actual` surface introduced in Phase 0

```kotlin
// commonMain
expect class SecureStore {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
}

expect fun createDatabaseDriver(
    name: String,
    passphrase: ByteArray,
): SqlDriver

expect object EntitlementsPlatform {
    fun isPro(): Boolean   // Phase 0: always false
}
```

Nothing else is `expect`ed in Phase 0 — additional platform hooks
(push, billing, biometrics) land in their owning phases.

### Koin modules

- `apiModule` — Ktor `HttpClient`, `Json`, and the generated API
  client.
- `dataModule` — SQLDelight database, DAOs.
- `platformModule` — `SecureStore` and `EntitlementsPlatform`
  bindings.
- `appModule` — aggregates the above; consumed by both hosts via a
  `startKoin` call in a shared `initKoin()` helper.

## Generated API client

### Source of truth

The pinned copy at `spec/openapi/v3.2.yaml` is captured from the
phpMyFAQ tag recorded in `spec/openapi/VERSION`. Phase 0 captures this
once, from the phpMyFAQ `4.2.0` tag, in keeping with the locked
minimum-version decision.

### Generation

- Generator: `openapi-generator-cli` with the `kotlin` generator,
  `library=multiplatform`, `serializationLibrary=kotlinx_serialization`.
- Invocation: `mobile/scripts/generate-api-client.sh`, callable
  locally and from CI. The script writes output to
  `mobile/shared/src/commonMain/kotlin/app/myfaq/shared/api/generated/`.
- Commit policy: the generated sources are committed. The build does
  not regenerate on each run — reproducibility beats freshness. The
  dedicated `mobile-openapi-sync.yml` workflow opens a PR whenever
  `mobile/spec/openapi/v3.2.yaml` changes.

### Hand-written wrapper

A thin hand-written layer under `api/` wraps the generated client so
calling code never imports generated types directly. This lets later
phases replace the generator without rippling into DAOs or UI.

```kotlin
interface MyFaqApi {
    suspend fun meta(): Meta
    // Phase 1 adds categories, faqs, search…
}
```

Phase 0 implements exactly one endpoint through this wrapper — `/meta`
— because it is the single call the placeholder screen exercises and
it validates the full generated-client pipeline.

### Forward compatibility

Configure `kotlinx.serialization` with
`ignoreUnknownKeys = true` and `explicitNulls = false`. This is a
Phase 0 decision that applies to every later DTO.

## Database layer

### SQLDelight schema (Phase 0 subset)

Only the tables required for the instance selector and for proving
encryption works are created in Phase 0. The full schema from
`mobile-app-plan.md` lands in Phase 2.

```sql
-- shared/src/commonMain/sqldelight/app/myfaq/shared/data/Instances.sq
CREATE TABLE instances (
    id                   TEXT PRIMARY KEY NOT NULL,
    display_name         TEXT NOT NULL,
    base_url             TEXT NOT NULL,
    api_version          TEXT NOT NULL,
    auth_mode            TEXT NOT NULL,
    last_successful_ping INTEGER,
    created_at           INTEGER NOT NULL,
    updated_at           INTEGER NOT NULL
);
```

Secrets (`api_client_token`, OAuth client ID / secret, session cookie)
are referenced by stable keys that resolve through `SecureStore`.
Nothing sensitive touches SQLite.

### Encryption

- **Android**: SQLCipher via the `sqldelight-android-driver` with a
  passphrase derived from a hardware-backed key in the Android
  Keystore. The key is generated lazily on first launch.
- **iOS**: `NativeSqliteDriver` with `onConfiguration` enabling
  `NSFileProtectionCompleteUntilFirstUserAuthentication`. iOS does not
  use SQLCipher in v1 — Data Protection Class B meets the
  threat model in `mobile-app-plan.md`.

### Migration policy

Phase 0 ships schema version `1`. Every later phase adds a numbered
migration file under
`shared/src/commonMain/sqldelight/migrations/`. Never edit existing
migration files; always append.

## Secure storage wrappers

- **Android**: `androidx.security.crypto.EncryptedSharedPreferences`
  for the `SecureStore`, backed by a master key in the Keystore. Each
  instance UUID namespaces its entries.
- **iOS**: Keychain Services via a small Objective-C bridging helper
  exposed through `iosMain`. Service = `app.myfaq.ios`, access group
  reserved for a future App Group / share extension.

Both implementations are unit-tested with platform-specific tests in
`androidUnitTest` and `iosTest` source sets, plus an abstract contract
test shared through `commonTest` so both adapters pass the same
behavioural suite.

## `Entitlements` facade stub

```kotlin
// commonMain
object Entitlements {
    fun isPro(): Boolean = EntitlementsPlatform.isPro()
}

// androidMain + iosMain (Phase 0)
actual object EntitlementsPlatform {
    actual fun isPro(): Boolean = false
}
```

UI code in Phase 1 already guards Pro entry points with
`Entitlements.isPro()`; Phase 3 replaces the `actual` bodies with real
StoreKit 2 and Play Billing implementations without touching any
calling site.

## Host apps

### Android (`androidApp`)

- Jetpack Compose, Material 3.
- Single activity, single screen (`MainActivity` + `AppRoot.kt`).
- The screen calls `MyFaqApi.meta()` against a Ktor `MockEngine`
  seeded with a fixture and renders the returned version string. This
  proves the shared module, Koin graph, generated client, and
  serialization are all wired correctly.
- `R8` / minification is enabled for the `release` build even though
  no release artifact is published in Phase 0; catching keep-rule
  issues now is cheaper than catching them in Phase 2.

### iOS (`iosApp`)

- SwiftUI `App` entry point, one `ContentView` backed by a shared
  observable that calls into the shared framework.
- The shared framework is produced by the KMP `iosSimulatorArm64`,
  `iosArm64`, and `iosX64` targets via a `XCFramework` assembled at
  build time. `CocoaPods` is **not** used; the Xcode project references
  the framework via a `File > Add Packages…`-equivalent local path to
  keep builds hermetic.
- The app is unsigned in Phase 0 except for the Apple-provided
  development profile used by CI simulators.

## CI/CD (Phase 0 scope)

Two GitHub Actions workflows land in Phase 0. `mobile-release.yml`
is committed but unused until Phase 1 tags `mobile-v0.1.0`.

All workflows live at `.github/workflows/mobile-*.yml` (workflow
files cannot live under `mobile/`) and scope their `paths:` filters
to `mobile/**`, `.github/workflows/mobile-*.yml`, and
`plans/phase-0-foundations.md` so they skip unrelated changes
elsewhere in the repository. Each job sets
`defaults.run.working-directory: mobile` so `./gradlew` invocations
resolve against the nested Gradle root.

### `mobile-ci.yml`

Runs on every push and pull request that touches `mobile/**`.

Jobs:

1. **`lint`** — `./gradlew ktlintCheck detekt`, plus `swiftlint` for
   the iOS host.
2. **`shared-unit`** — `./gradlew :shared:allTests` on Ubuntu. Runs
   `commonTest` against the JVM target and `androidUnitTest`.
3. **`android-build`** — `./gradlew :androidApp:assembleDebug` on
   Ubuntu. Uploads the APK as a workflow artifact for smoke testing.
4. **`ios-build`** — `xcodebuild -scheme iosApp -destination
   'generic/platform=iOS Simulator'` on `macos-latest`, run from
   `mobile/iosApp/`. Depends on a cached `XCFramework` produced by
   `./gradlew :shared:assembleXCFramework`.
5. **`shared-ios-test`** — `./gradlew :shared:iosSimulatorArm64Test`
   on `macos-latest`. Runs the `iosTest` source set (secure store
   contract tests, serialization round-trips).

Caches:

- Gradle and Kotlin/Native caches keyed on `libs.versions.toml`.
- Xcode DerivedData keyed on the KMP version and the iOS deployment
  target.

Secrets: none in Phase 0. Signing, notarization, and Play Publisher
credentials are added in Phase 1 alongside the first real release.

### `mobile-openapi-sync.yml`

Triggered `workflow_dispatch` plus a `repository_dispatch` from the
main `phpMyFAQ/phpMyFAQ` repo on tagged releases. Steps:

1. Download the tagged `docs/openapi.yaml` from the main repo.
2. Copy it to `mobile/spec/openapi/v3.2.yaml` and update
   `mobile/spec/openapi/VERSION`.
3. Run `mobile/scripts/generate-api-client.sh`.
4. Open a pull request against `main` with the diff. A human reviews
   before merging.

## Testing strategy

Phase 0 establishes the patterns; Phase 1 fills them out.

- **Fixture directory**: `shared/src/commonTest/resources/fixtures/`
  holds JSON captures from a real phpMyFAQ `4.2.0` dev install. At
  minimum, a `/meta` response lands here in Phase 0.
- **Ktor `MockEngine`**: all Phase 0 tests exercise the generated
  client against `MockEngine` responses constructed from the fixture
  directory.
- **Secure storage contract test**: an abstract test in `commonTest`
  is implemented by `androidUnitTest` and `iosTest`, both running the
  same assertions against their native backend.
- **Serialization round-trip test**: every DTO generated so far is
  round-tripped through `Json` to catch forward-compat regressions.

## Documentation

Four docs land with Phase 0:

- `mobile/README.md` — mobile-tree summary, link to `myfaq.app`,
  one-line build instructions per platform. The repository-root
  `README.md` (if introduced) only points at this file.
- `docs/mobile/architecture.md` — module diagram, data flow, and the
  `expect` / `actual` contract surface.
- `docs/mobile/build.md` — prerequisites (JDK, Android SDK, Xcode),
  `mobile/scripts/bootstrap.sh` usage, how to run tests, how to
  regenerate the API client.
- `docs/mobile/phase-0-handoff.md` — exact tool versions pinned,
  decisions recorded, and a checklist of everything Phase 1 can
  assume is in place.

## Security posture at end of Phase 0

- HTTPS-only HTTP client (`http://` is rejected outside a dev flavor).
- No credentials stored anywhere yet, but the wrappers that will hold
  them are tested and working.
- The encrypted database is created on first launch with a passphrase
  that never leaves `SecureStore`.
- No analytics SDKs. No crash reporter. No telemetry. The first bytes
  leaving the device are the `MockEngine`-mocked calls in the
  placeholder screen.
- `Entitlements.isPro()` is `false`. No billing code paths exist.

## Risks

- **KMP tooling churn**: Kotlin and KMP release cadence is aggressive.
  Mitigation: pin every version in `libs.versions.toml`, upgrade on an
  explicit ticket, never on a whim.
- **Generated client drift**: the Kotlin generator's output can change
  subtly between generator versions. Mitigation: pin the generator
  version, commit the generated sources, and review the PR produced by
  `mobile-openapi-sync.yml`.
- **iOS Keychain edge cases** (first unlock, device restore): the
  contract test covers the happy path only; Phase 1 adds Keychain
  access-group and first-unlock cases when real credentials land.
- **SQLCipher + SQLDelight** integration sometimes lags the latest
  SQLDelight release. Mitigation: pin the SQLDelight version used in
  Phase 0 to a combination that has a known-good SQLCipher driver.

## Exit criteria

Phase 0 is done when **all** of the following are true:

1. The layout above is present on `main` of `phpMyFAQ/MyFAQ` under
   the `mobile/` directory, without disturbing the existing `plans/`
   or `LICENSE`.
2. A freshly cloned checkout builds on macOS and Linux with no manual
   fix-ups, driven only by `mobile/scripts/bootstrap.sh` and
   `./gradlew` (run from `mobile/`).
3. Both host apps launch, pull `/meta` from `MockEngine`, and render
   the version string.
4. The SQLDelight database is created encrypted on first launch, and
   a round-trip `INSERT` / `SELECT` against `instances` succeeds in a
   test.
5. The `SecureStore` contract tests pass on both platforms.
6. `Entitlements.isPro()` returns `false` on both platforms and is
   covered by a test.
7. CI is green on `main` for three consecutive runs.
8. `docs/mobile/phase-0-handoff.md` is merged and lists the pinned
   tool versions.
9. The repository is tagged `mobile-v0.0.0-foundations` as the Phase
   0 baseline.

Anything not on this list is out of scope for Phase 0 and belongs to
Phase 1 or later. Resist the urge to add "just one more" screen, DAO,
or endpoint — the point of Phase 0 is that every later phase starts
from an identical, boring baseline.

## Handoff to Phase 1

Phase 1 can assume:

- A working generated client for phpMyFAQ `v3.2`, with a hand-written
  wrapper interface.
- A working encrypted database with a schema migration framework.
- A working secure-storage abstraction on both platforms.
- A working Koin graph shared by both hosts.
- A working CI pipeline that will run lint, unit tests, and debug
  builds for any new code added on top.
- A single `Entitlements.isPro()` gate that can be flipped by the UI
  and will automatically start returning real values in Phase 3.

Phase 1 adds: the Workspaces screen, the real add-instance flow
(backed by `/meta` against a live instance), the Categories and FAQ
list screens, and the server-backed search path.
