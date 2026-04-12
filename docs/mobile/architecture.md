# Mobile Architecture

## Module diagram

```
┌──────────────┐      ┌──────────────┐
│  androidApp  │      │    iosApp     │
│  (Compose)   │      │  (SwiftUI)   │
└──────┬───────┘      └──────┬───────┘
       │                     │
       └────────┬────────────┘
                │
       ┌────────▼────────┐
       │     shared      │
       │ (Kotlin/Multi-  │
       │   platform)     │
       └────────┬────────┘
                │
    ┌───────────┼───────────┐
    │           │           │
┌───▼──┐  ┌────▼───┐  ┌────▼───┐
│ api/ │  │ data/  │  │ plat-  │
│      │  │        │  │ form/  │
└──────┘  └────────┘  └────────┘
```

## Boundaries

- **`shared/commonMain`** — all business logic, DTOs, API client
  wrapper, DAOs, `Entitlements` facade, Koin DI modules. Depends only
  on Kotlin stdlib, Ktor, kotlinx.serialization, SQLDelight, and Koin.

- **`shared/androidMain`** — `actual` implementations for:
  - `SecureStore` (EncryptedSharedPreferences)
  - `DatabaseDriverFactory` (SQLCipher + AndroidSqliteDriver)
  - `EntitlementsPlatform` (stub; Phase 3 wires Play Billing)

- **`shared/iosMain`** — `actual` implementations for:
  - `SecureStore` (Keychain Services)
  - `DatabaseDriverFactory` (NativeSqliteDriver + Data Protection)
  - `EntitlementsPlatform` (stub; Phase 3 wires StoreKit 2)

- **`androidApp`** — thin Jetpack Compose host. Depends on `shared`.
  Contains only UI code and the Android `Application` subclass that
  bootstraps Koin with the Android platform module.

- **`iosApp`** — thin SwiftUI host. Consumes the `Shared.framework`
  XCFramework produced by the KMP build. Contains only SwiftUI views
  and the `@main` App struct that bootstraps Koin.

## `expect` / `actual` surface

```
expect class SecureStore { put, get, remove, clear }
expect class DatabaseDriverFactory { create(passphrase) }
expect object EntitlementsPlatform { isPro() }
```

Nothing else is `expect`ed in Phase 0. Additional platform hooks
(push, billing, biometrics) land in their owning phases.

## Data flow (Phase 0)

```
App launch
  ↓
initKoin (platform module + shared module)
  ↓
MetaLoader.load()
  ↓
MyFaqApiImpl → HttpClient (MockEngine in Phase 0)
  ↓
Meta DTO deserialized via kotlinx.serialization
  ↓
UI renders version string
```

Phase 1 replaces MockEngine with real per-platform HTTP engines
(OkHttp on Android, Darwin on iOS) and adds real network calls.
