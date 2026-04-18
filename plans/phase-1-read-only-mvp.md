# Phase 1 — Read-Only MVP

This document is the detailed execution plan for **Phase 1** of
MyFAQ.app. It builds on the Phase 0 foundations (KMP scaffold,
generated client, encrypted DB, secure storage, CI) and delivers
the first usable app: a read-only phpMyFAQ browser with
multi-instance support, category navigation, FAQ detail, and
server-backed search.

Phase 1 ends when the dogfood build is distributed to the phpMyFAQ
team via TestFlight and Play internal testing.

## Goals

- Replace the Phase 0 MockEngine with real HTTP engines (OkHttp on
  Android, Darwin on iOS) so the app talks to live phpMyFAQ 4.2+
  instances.
- Implement the Workspaces screen and the add-instance flow using
  `GET /api/v3.2/meta` (with legacy fallback).
- Build the core read screens: Home (sticky/popular/latest/news
  tabs), Categories (tree browser), FAQ list, FAQ detail, Search,
  and News.
- Add basic in-memory + SQLite TTL caching so repeated navigation
  doesn't re-fetch. No FTS, no background sync — those are Phase 2.
- Wire up the Paywall shell screen with hard-coded copy and
  non-functional purchase buttons so the layout can be validated
  before real IAP code lands in Phase 3.
- Ship dogfood builds to the phpMyFAQ team for early feedback.

## Non-goals

- No offline mode. Content is fetched live; cached data is a
  performance optimization, not an offline guarantee.
- No FTS5 local search. Search goes to the server endpoint only.
- No background sync, no attachment cache, no "download all for
  offline". Those are Phase 2.
- No real IAP. The Paywall screen is visual only.
- No write operations (ask, comment, rate, login). Phase 3.
- No push notifications. Phase 5.

## API endpoints exercised

Phase 1 adds these endpoints to the `MyFaqApi` interface (Phase 0
only had `/meta`):

| Endpoint | Used by |
|----------|---------|
| `GET /api/v3.2/meta` | Add-instance, sync bootstrap |
| `GET /api/v3.2/categories` | Categories screen |
| `GET /api/v3.2/faqs/{categoryId}` | FAQ list per category |
| `GET /api/v3.2/faq/{categoryId}/{faqId}` | FAQ detail |
| `GET /api/v3.2/faqs/popular` | Home tab |
| `GET /api/v3.2/faqs/latest` | Home tab |
| `GET /api/v3.2/faqs/trending` | Home tab |
| `GET /api/v3.2/faqs/sticky` | Home tab |
| `GET /api/v3.2/search?q=…` | Search screen |
| `GET /api/v3.2/searches/popular` | Search suggestions |
| `GET /api/v3.2/tags` | Tag chips on FAQ detail |
| `GET /api/v3.2/news` | News tab / timeline |
| `GET /api/v3.2/comments/{recordId}` | FAQ detail comments section |
| `GET /api/v3.2/open-questions` | Home or dedicated list |

## New DTOs

```kotlin
// All in app.myfaq.shared.api.dto, @Serializable,
// ignoreUnknownKeys = true

data class Category(val id: Int, val name: String, val description: String, val parentId: Int?)
data class FaqSummary(val id: Int, val categoryId: Int, val question: String, val updated: String?)
data class FaqDetail(
    val id: Int, val categoryId: Int, val question: String,
    val answer: String, val keywords: String?, val author: String?,
    val created: String?, val updated: String?,
    val isSticky: Boolean, val isActive: Boolean,
    val tags: List<String>, val attachments: List<Attachment>,
)
data class Attachment(val id: Int, val filename: String, val size: Long?, val mime: String?)
data class SearchResult(val id: Int, val categoryId: Int, val question: String, val answer: String?)
data class NewsItem(val id: Int, val title: String, val body: String, val author: String?, val created: String?)
data class Comment(val id: Int, val faqId: Int, val author: String, val body: String, val created: String?)
data class Tag(val id: Int, val name: String)
data class OpenQuestion(val id: Int, val question: String, val author: String?, val created: String?)
data class PopularSearch(val searchTerm: String, val count: Int)
```

DTOs are intentionally loose (`String?` dates, nullable fields) to
tolerate server variations. Strict domain models are mapped in the
repository layer.

## Architecture additions

### Repository pattern

Phase 1 introduces a `Repository` layer between the API and UI:

```
UI (Compose / SwiftUI)
  ↓ observes
ViewModel / ObservableObject
  ↓ calls
Repository (interface)
  ↓ delegates to
ApiRepository (online-first, TTL cache fallback)
  ↓ uses
MyFaqApi + InstancesDao + CacheStore
```

Each repository method:
1. Checks the in-memory cache (keyed by instance + endpoint + params).
2. If fresh (within TTL), returns cached data.
3. Otherwise hits the network, stores the result, returns it.
4. On network failure, returns stale cache if available, else throws.

### CacheStore

A simple key-value store backed by SQLDelight:

```sql
CREATE TABLE cache_entries (
    instance_id TEXT NOT NULL,
    cache_key   TEXT NOT NULL,
    json_body   TEXT NOT NULL,
    fetched_at  INTEGER NOT NULL,
    ttl_seconds INTEGER NOT NULL,
    PRIMARY KEY (instance_id, cache_key)
);
```

TTLs from `mobile-app-plan.md`: categories 24h, FAQs 6h, news 1h,
search 10min.

### Navigation

- **Android**: Jetpack Navigation Compose with a bottom nav bar
  (Home, Categories, Search, Settings) and stack navigation within
  each tab.
- **iOS**: SwiftUI `TabView` with `NavigationStack` per tab.

### ViewModel / shared presenters

Shared `ViewModel`-like classes in `commonMain` using
`kotlinx.coroutines.flow.StateFlow`. Android consumes them directly
via `collectAsState()`. iOS wraps them in `@ObservableObject`
adapters.

## Screen inventory (Phase 1)

### 1. Workspaces

- List of registered instances (title, version, last ping, status dot).
- "Add instance" button → Add Instance sheet.
- Tap instance → set as active, navigate to Home.
- Long-press → rename, clear cache, delete.
- Empty state with illustration and "Add your first instance" CTA.

### 2. Add Instance

- URL text field with `https://` prefix enforced.
- On submit: call `GET /api/v3.2/meta`, show confirmation card
  (title, version, language).
- On error: specific diagnostic (DNS, TLS, HTTP code, JSON parse).
- Optional: API token field (stored in SecureStore, not SQLite).
- Save → insert `instances` row, navigate to Home.

### 3. Home

- Tabs/chips: Sticky, Popular, Latest, News.
- Each tab loads its endpoint, renders a card list.
- Pull-to-refresh on each tab.
- Tap FAQ card → FAQ detail. Tap news card → News detail.

### 4. Categories

- Tree list with parent/child hierarchy (indent or expand/collapse).
- Tap category → FAQ list for that category.
- Show FAQ count per category if the API provides it.

### 5. FAQ List

- Title bar: category name.
- List of FAQ summaries (question + last updated).
- Tap → FAQ detail.

### 6. FAQ Detail

- Question as heading.
- Answer HTML rendered in a platform WebView (WKWebView on iOS,
  AndroidWebView on Android) with a minimal injected stylesheet
  that respects dark mode.
- Tags shown as chips below the answer.
- Comments section (collapsed by default, tap to expand).
- "Rate this FAQ" row — visible but tapping shows Pro upsell.
- Share button (system share sheet with FAQ URL).
- Attachments list if present (tap opens in system viewer).

### 7. Search

- Single text field, debounced 300ms.
- Results from `GET /search?q=…`.
- Popular searches shown when field is empty (from
  `GET /searches/popular`).
- Tap result → FAQ detail.

### 8. Paywall Shell

- Reached from any Pro-gated action (rate, ask, comment).
- Header: "Unlock MyFAQ Pro".
- Bullet list of Pro features (hard-coded copy).
- Two placeholder buttons: "Annual" and "Lifetime" — both show a
  toast "Coming soon" in Phase 1.
- "Restore purchases" link (also non-functional in Phase 1).

### 9. Settings (minimal)

- Active instance info (title, version, URL).
- "Switch instance" → Workspaces.
- "Clear cache" for active instance.
- App version.
- "About" → open-source licenses.

## SQLDelight schema additions

Phase 1 adds `cache_entries` to schema version 2. Migration:

```sql
-- migrations/1.sqm
CREATE TABLE IF NOT EXISTS cache_entries (
    instance_id TEXT NOT NULL,
    cache_key   TEXT NOT NULL,
    json_body   TEXT NOT NULL,
    fetched_at  INTEGER NOT NULL,
    ttl_seconds INTEGER NOT NULL,
    PRIMARY KEY (instance_id, cache_key)
);
```

The full content tables (categories, faqs, tags, etc.) stay out of
SQLite until Phase 2 — Phase 1 caches raw JSON blobs only.

## HTTP engine swap

Phase 0 shipped `ktor-client-mock` in `commonMain`. Phase 1:

- Move `ktor-client-mock` to `commonTest` only.
- `androidMain` uses `ktor-client-okhttp`.
- `iosMain` uses `ktor-client-darwin`.
- `HttpClientFactory` becomes platform-aware via `expect`/`actual`:

```kotlin
// commonMain
expect fun createPlatformHttpClient(): HttpClient

// androidMain
actual fun createPlatformHttpClient() = HttpClient(OkHttp) { ... }

// iosMain
actual fun createPlatformHttpClient() = HttpClient(Darwin) { ... }
```

The `Json` configuration (`ignoreUnknownKeys`, `explicitNulls`)
stays in `commonMain`.

## Instance-scoped API client

Each instance gets its own `MyFaqApi` instance bound to its
`base_url`. The DI module provides a factory:

```kotlin
factory { (baseUrl: String) -> MyFaqApiImpl(createPlatformHttpClient(), baseUrl) }
```

The active instance's API client is held in an
`ActiveInstanceManager` singleton that UI code observes.

## Testing strategy

- **API tests**: each new endpoint gets a `MockEngine` test with a
  captured fixture under `commonTest/resources/fixtures/`.
- **Repository tests**: verify cache-hit/miss behavior with a
  fake `CacheStore`.
- **ViewModel tests**: verify state transitions (loading → loaded →
  error) with fake repositories.
- **UI snapshot tests**: deferred to Phase 4. Phase 1 relies on
  manual QA during dogfooding.

## Distribution

- **Android**: Play internal testing track. APK also on GitHub
  Releases as `mobile-v0.1.0`.
- **iOS**: TestFlight internal group (phpMyFAQ team only).
- Both require signing secrets in GitHub Actions (added in Phase 1).

### Release runbook (`mobile-vX.Y.Z` tag)

`.github/workflows/mobile-release.yml` runs on tag push and produces
signed Android artifacts plus a TestFlight upload. Before the first
real release, populate these GitHub secrets:

**Android**
- `ANDROID_KEYSTORE_BASE64` — `base64 -i upload.jks`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

**iOS / TestFlight**
- `IOS_DIST_CERT_P12_BASE64` + `IOS_DIST_CERT_PASSWORD`
- `IOS_PROVISIONING_PROFILE_BASE64`
- `APP_STORE_CONNECT_API_KEY_ID` + `APP_STORE_CONNECT_API_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64` (the `.p8` file, base64-encoded)

If any secret group is missing, the corresponding job skips signing
and produces an unsigned artifact instead of failing — useful for
exercising the workflow before the team has access to the keys.

Cut a release with:
```
git tag -a mobile-v0.1.0 -m "Phase 1 read-only MVP"
git push origin mobile-v0.1.0
```

## Exit criteria

Phase 1 is done when **all** of the following are true:

1. The app connects to a real phpMyFAQ 4.2+ instance via
   `GET /api/v3.2/meta` and shows the instance in Workspaces.
2. Categories, FAQ list, FAQ detail, Home tabs, Search, and News
   screens all render live data from the instance.
3. FAQ detail renders HTML answers correctly in both light and dark
   mode on both platforms.
4. The Paywall screen is reachable from every Pro-gated entry point
   and displays the correct copy.
5. TTL caching prevents redundant network calls on back-navigation.
6. The app handles network errors gracefully (error states, retry
   buttons, stale cache fallback).
7. Dogfood builds are distributed to the phpMyFAQ team via
   TestFlight and Play internal testing.
8. CI is green with the new endpoints and screens.
9. The repository is tagged `mobile-v0.1.0`.

## Handoff to Phase 2

Phase 2 can assume:

- All read API endpoints are wired and tested.
- The repository pattern is established with TTL caching.
- Navigation structure is final (bottom tabs + stack).
- FAQ detail WebView rendering is working and theme-aware.
- The Paywall screen exists and is reachable.
- Dogfood feedback has been incorporated.

Phase 2 adds: full SQLite schema for all content types, FTS5 local
search, background sync via WorkManager/BGTaskScheduler, attachment
cache, and the Settings screen with cache controls. That is the
public v1.0.0.
