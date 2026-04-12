# MyFAQ.app — Native Mobile App Plan

This document describes the planned architecture for **MyFAQ.app**, a pair of
native iOS and Android apps that provide a first-class phpMyFAQ experience
with multi-instance support and full offline browsing.

- **Product name**: MyFAQ.app
- **Domain**: <https://myfaq.app> (owned by the phpMyFAQ project)
- **Business model**: freemium. All read and offline features are free
  forever. Write features (ask a question, post a comment, rate, register)
  are gated behind a paid Pro unlock introduced in a later release (see
  "Monetization and freemium gating" below).

This is a planning document, not a commitment. Phases, milestones, and tech
choices should be revisited before any implementation work begins.

## Goals and non-goals

### Goals

- Native iOS and Android apps, branded MyFAQ.app, that read any phpMyFAQ
  4.2+ instance via its public `v3.2` REST API, and (in a later paid
  release) write to it.
- Multi-instance support: a single install of the app can register and
  switch between several phpMyFAQ installations, similar to how a mail
  app manages multiple accounts.
- Full offline browsing of cached categories, FAQs, tags, news, glossary,
  and attachments. Offline browsing is part of the free tier.
- Local full-text search over the cached content, with a fall-through to
  the server's search endpoint when the device is online.
- Respect the three authentication modes phpMyFAQ exposes: anonymous,
  `x-pmf-token`, and user session (password login or OAuth2).

### Non-goals (v1)

- Replacing the admin panel. The `admin/api/*` surface uses session
  cookies plus CSRF tokens and is not designed for a native client.
  Admin features stay on the web UI.
- Write operations of any kind. Ask-a-question, comments, ratings, and
  registration are all deferred to the paid release (see "Monetization
  and freemium gating" below). v1 is read-only.
- Push notifications. phpMyFAQ only exposes `push/generate-vapid-keys`
  on the admin API today, and a dedicated server contract for device-
  token registration is needed before the app can support push.

## Platforms and tech stack

MyFAQ.app uses **Kotlin Multiplatform (KMP) with native UI on each
platform**. This decision is final for v1.

- **Shared module (Kotlin Multiplatform)**: networking, models, database,
  sync, search, authentication, entitlement checks, and the instance
  selector state.
- **iOS UI**: SwiftUI, consuming the shared module via KMP's generated
  Objective-C framework.
- **Android UI**: Jetpack Compose.
- **Rationale**: one copy of the API client, cache layer, and sync logic;
  native look and feel on both platforms; no JavaScript runtime on
  device; straightforward path to watchOS and Wear OS later if desired.

### Shared libraries

- **HTTP**: Ktor client
- **Serialization**: kotlinx.serialization
- **Database**: SQLDelight (typed SQL, with FTS5 support on both
  platforms)
- **Secure storage**: `androidx.security.crypto` on Android, iOS Keychain
  via an expect/actual wrapper
- **Dependency injection**: Koin
- **OpenAPI codegen**: `openapi-generator`, run from the committed
  `https://github.com/thorsten/phpMyFAQ/blob/main/docs/openapi.yaml`
- **Background sync**: WorkManager on Android, BGTaskScheduler on iOS
- **Image loading**: Coil on Android, SDWebImageSwiftUI on iOS
- **In-app purchase / entitlements**: StoreKit 2 on iOS, Google Play
  Billing Library v7+ on Android, with a small `expect/actual`
  entitlement facade in the shared module so feature gates can be
  written once

## API surface used by the app

The plan is grounded in the current phpMyFAQ route inventory (public API
`v3.2`, specification at `https://github.com/thorsten/phpMyFAQ/blob/main/docs/openapi.yaml`).

### Read path (anonymous, cached) — free tier

- `GET /api/v3.2/meta` — single bootstrap call returning version,
  title, language, available languages, enabled features, logo URL,
  and OAuth discovery metadata. Used by the instance selector and the
  sync pipeline bootstrap step.
- `GET /api/v3.2/version`, `/title`, `/language` — legacy fallback for
  instances that predate `/meta`.
- `GET /api/v3.2/categories`
- `GET /api/v3.2/faqs`, `/faqs/{categoryId}`, `/faqs/tags/{tagId}`
- `GET /api/v3.2/faqs/popular`, `/latest`, `/trending`, `/sticky`
- `GET /api/v3.2/faq/{categoryId}/{faqId}`
- `GET /api/v3.2/search?q=…`, `/searches/popular`
- `GET /api/v3.2/tags`, `/glossary`, `/news`
- `GET /api/v3.2/comments/{recordId}`
- `GET /api/v3.2/attachments/{faqId}` plus direct file download
- `GET /api/v3.2/pdf/{categoryId}/{faqId}` — returns a URL that the app
  opens in the system viewer.
- `GET /api/v3.2/open-questions`

### Write path (token or session required) — Pro tier

None of these are available in the free tier. They ship behind the Pro
entitlement check:

- `POST /api/v3.2/login` — session cookie flow
- `POST /oauth/token`, `GET /oauth/authorize` — OAuth2 flow
- `POST /api/v3.2/register` (`x-pmf-token`)
- `POST /api/v3.2/question` (`x-pmf-token`) — a user submits a new
  question

### Explicitly excluded from any tier

- All `admin/api/*` routes. Session plus CSRF makes them a poor fit, and
  the app should not become a second admin panel.
- `POST /api/v3.2/faq/create|update`, `POST /api/v3.2/category` — editing
  FAQs from a phone is a niche workflow; defer.
- `GET /api/v3.2/backup/{type}` — admin-grade, no reason to expose.

### Code generation strategy

Pin the OpenAPI specification, regenerate a Kotlin client in CI on each
phpMyFAQ release tag, and commit the generated client as a versioned
artifact inside the mobile repository. The generated client is not
regenerated at build time, so mobile releases remain reproducible
independently of phpMyFAQ upstream. Unknown or missing fields must not
crash the client — models should ignore unknown keys to stay
forward-compatible with future 3.x minor versions.

## Multi-instance FAQ selector

### Data model (`Instance` row)

```text
id                   uuid
display_name         string
base_url             https only, trailing-slash normalized
api_version          string, e.g. "v3.2"
favicon_blob         bytes
language_override    nullable, else Accept-Language
api_client_token     keychain reference, nullable
auth_mode            NONE | TOKEN | USER_SESSION | OAUTH2
oauth_client_id      keychain reference, nullable
oauth_client_secret  keychain reference, nullable
user_credentials_ref keychain reference, nullable
last_successful_ping timestamp
created_at           timestamp
updated_at           timestamp
```

### Add-instance flow

1. The user enters a base URL. The app normalizes it to
   `https://host/api/v3.2` and rejects `http://` outside of a dev build.
2. The app calls `GET /api/v3.2/meta` for the bootstrap payload
   (version, title, language, available languages, enabled features,
   logo URL, OAuth discovery metadata). On success it shows a
   confirmation sheet with the detected title and version. On failure
   it shows a precise diagnostic (DNS, TLS, HTTP status, JSON shape).
   The legacy `GET /version` + `/title` + `/language` fan-out is kept
   only as a fallback for instances that do not yet expose `/meta`.
3. Optional step: the user enters an `x-pmf-token` or signs in. Credentials
   go straight into the Keychain / Keystore, never into the SQLite
   database.
4. On save, the app schedules an initial sync for that instance.

### Selector UI

- A "Workspaces" screen lists every registered instance with title,
  version, last sync timestamp, and an online status dot.
- Tapping an instance sets it as the active context; all data-bound
  screens scope their queries by `instance_id`.
- Long-press opens rename, re-authenticate, clear cache, and delete.
- QR-code add: scan a QR that encodes `{base_url, token?}` — useful for
  enterprise rollouts.
- Deep-link handler for the custom scheme `myfaq://add?url=...&token=...`
  and universal links under `https://myfaq.app/add?url=...`, so any
  phpMyFAQ web install can offer an "Open in MyFAQ.app" button on the
  admin landing page.

## Authentication

Authentication is only exercised by Pro-tier write features. In the free
tier the app only talks to the anonymous endpoints and never prompts for
credentials, so users can browse a phpMyFAQ install without signing in.

When a user unlocks Pro, three modes map directly onto phpMyFAQ's
existing surface:

1. **Anonymous** — the default. Read-only endpoints only.
2. **API client token** — the user pastes the site's `api.apiClientToken`
   value. The app sends it as the `x-pmf-token` header on every request.
   This enables `POST /question`, `POST /register`, and similar. Stored
   in the Keychain / Keystore.
3. **User session** — `POST /api/v3.2/login` returns a session cookie.
   The app stores the cookie in a per-instance `CookieJar` backed by
   encrypted storage and refreshes it when the server returns 401. This
   supports personalized content and rate-limit exemptions.
4. **OAuth2** — the existing `/oauth/authorize` plus `/oauth/token`
   endpoints with PKCE. This is the preferred mode for multi-user
   corporate installs.

A biometric gate (Face ID or fingerprint) is required to unlock any
instance whose auth mode is not `NONE`. The gate unlocks the Keychain or
Keystore entry, not the app itself.

## Offline storage

### Schema (SQLDelight, per instance)

```text
instances       (see the "Multi-instance FAQ selector" section)
categories      (id, parent_id, name, description, lang, sort,
                 etag, fetched_at)
faqs            (id, category_id, lang, question, answer_html, keywords,
                 author, created, updated, sticky, active, rating_avg,
                 rating_count, etag, fetched_at)
faq_tags        (faq_id, tag_id)
tags            (id, name)
attachments     (id, faq_id, filename, size, mime, local_path, etag)
news            (id, title, body_html, author, created, etag)
glossary        (id, term, definition, lang)
comments        (id, faq_id, author, body, created)
search_index    FTS5 virtual table over
                (faq_id, question, answer_plain, keywords)
sync_state      (resource, last_synced_at, cursor)
pending_writes  (id, instance_id, kind, payload_json, created,
                 attempts, last_error)
```

### Cache policy

- Default TTL per resource: categories 24 hours, FAQs 6 hours, news 1
  hour, search results 10 minutes, attachments until cache eviction.
- Honor HTTP `ETag` / `If-None-Match` when the server sends them; fall
  back to `Last-Modified`; fall back to a client-side `fetched_at + ttl`
  check if neither is present.
- Attachments are stored on disk in an app-private directory, hashed by
  URL; the database row holds the local path. The total attachment cache
  is capped (default 500 MB, user-configurable).
- Eviction: LRU on the attachment blob directory, TTL on text rows, and
  an explicit "clear cache" action per instance.

### Local search

- Online, first try: `GET /api/v3.2/search?q=…`.
- Offline or when the server fails: query the local SQLite FTS5 index.
- Merge-on-tie: prefer the server result set but fall back instantly so
  search always feels responsive.
- Query-intent chips ("Popular", "Latest", "Trending", "Sticky") map
  directly onto the existing endpoints.

### Forward and backward compatibility

The sync layer must tolerate fields it does not understand. Deleted-on-
server detection requires a tombstone list — phpMyFAQ does not expose
one today, so v1 uses a full re-list diff per sync window (cheap for
typical FAQ sizes). This is tracked as a server-side feature request
(see "Server-side prerequisites" below).

## Sync strategy

### Sync triggers

- App launch, if the last sync is older than 15 minutes.
- Pull-to-refresh on any list screen.
- Instance switch.
- Background: every 6 hours on both platforms via WorkManager /
  BGTaskScheduler, best-effort and subject to OS throttling.
- Foreground network recovery, via the reachability callback after the
  device goes back online.

### Sync pipeline per instance

1. Bootstrap: `GET /meta` (falling back to `version` + `title` +
   `language` for pre-`/meta` installs). Detect a breaking version
   change and force a schema re-sync if the major version changes.
2. Structural: `categories`, `tags`, `glossary`.
3. Content: `faqs` paginated per category, plus `faqs/sticky`,
   `faqs/popular`, and `faqs/latest` for the home widgets.
4. Ancillary: `news`, `open-questions`.
5. Index rebuild: upsert modified rows into the FTS5 virtual table.
6. Attachments sync: lazy — the app only fetches an attachment when an
   FAQ is first viewed, unless the user has opted in to "download all
   for offline".

Per-instance sync runs on a single coroutine dispatcher so a slow
instance never blocks others.

### Conflict and failure handling

Only relevant once the Pro write release ships:

- All writes go through `pending_writes` first and are replayed once the
  instance is reachable.
- A failed write keeps its original payload, increments `attempts`, and
  shows a persistent banner in the affected screen until resolved or
  dismissed.

## Screen inventory

The mobile app is a mobile-first adaptation of the existing web UI:

1. **Workspaces** — instance list, add, switch (see "Multi-instance FAQ
   selector" above).
2. **Home** — per-active-instance dashboard showing sticky, popular,
   latest, and news. Users swipe between tabs.
3. **Categories** — tree browser. Tap to drill into a category FAQ list.
4. **FAQ detail** — question, rendered answer HTML in a native WebView
   component with CSS injected from the app theme, rating (Pro), tags,
   attachments, and comments. The rating control is visible but
   displays a Pro upsell when tapped in the free tier.
5. **Search** — single input, live results as the user types, local and
   remote results merged.
6. **Glossary** — alphabetized list with search within.
7. **Ask a question** (Pro) — form gated by `x-pmf-token` or user
   session. Offline writes are enqueued to `pending_writes`. In the
   free tier this entry point is visible but leads to the Pro upsell.
8. **News** — timeline.
9. **Settings** — cache size, background sync toggle, language override,
   theme (system, light, dark), per-instance data controls, and the
   Pro subscription / restore purchases controls.
10. **Paywall** — a single dedicated upsell screen used by every Pro
    feature gate. Lists what Pro unlocks, shows price, and runs the
    native purchase sheet.
11. **About** — phpMyFAQ version per instance, app version, open-source
    licenses.

### Accessibility

- Respect OS font scaling — never fix text sizes in the answer WebView.
- Every tappable element has a content description or accessibility
  label.
- Answer HTML rendering is verified with VoiceOver and TalkBack.

### Theming

- Match phpMyFAQ's Bootstrap look lightly, but defer to platform norms:
  iOS uses SF Symbols for navigation icons and Android uses Material 3.
- Do not ship the phpMyFAQ CSS bundle inside the WebView. Use a tuned
  minimal stylesheet that respects system dark mode.

## Security and privacy

- HTTPS only. Certificate pinning is optional and per-instance; users on
  enterprise PKI can enable pinning by pasting a leaf or intermediate
  SHA-256 fingerprint.
- All credentials live in the Keychain or EncryptedSharedPreferences,
  never in SQLite.
- The database is encrypted at rest on Android using SQLCipher via the
  SQLDelight driver. On iOS the app relies on Data Protection Class B
  (`NSFileProtectionCompleteUntilFirstUserAuthentication`).
- The app never logs request bodies or tokens. The network debug logger
  is compiled out of release builds.
- No third-party analytics SDKs in v1. If crash reporting is needed, use
  self-hosted Sentry or the Apple and Google native crash reporters only,
  and scrub URLs to drop query strings that could contain tokens.
- Clearing app data wipes instances, cache, and Keychain entries
  atomically (iOS enumerates by service tag; Android uses
  `clearApplicationUserData`).

## Monetization and freemium gating

MyFAQ.app is freemium. The entire free tier is permanent and never
time-limited; Pro is an upsell for users who actively participate in a
phpMyFAQ community rather than only read it.

### What is free, forever

- Multi-instance Workspaces with unlimited instances
- All anonymous read endpoints
- Full offline cache, background sync, local FTS5 search
- Attachment downloads, PDF viewing, glossary, news
- Theming, language override, biometric protection of per-instance
  secrets

### What requires a Pro unlock

- User session login and OAuth2 sign-in
- `POST /api/v3.2/question` — ask a new question
- `POST /api/v3.2/register` — create a user on a phpMyFAQ install
- FAQ rating submission
- Comment posting
- The offline write queue (`pending_writes`) and the retry banner
- Any future write endpoint added to the public API

### Entitlement storage and enforcement

- Purchases go through **StoreKit 2** on iOS and **Google Play Billing
  Library v7+** on Android. No custom receipt server in v1 — use the
  store-native receipt validation paths.
- The shared module exposes an `Entitlements` facade with one
  `expect/actual` implementation per platform. UI code calls
  `Entitlements.isPro()` before revealing any gated action.
- Entitlement state is cached in the encrypted database with a short
  TTL, then re-verified on app launch and whenever a purchase event
  fires.
- "Restore purchases" is available from Settings and from the Paywall
  screen, and is mandatory for App Store review.

### Pricing and SKUs (to finalize before Phase 3)

Two SKUs in both stores:

- `pro_lifetime` — one-time purchase, unlocks Pro forever on the user's
  store account.
- `pro_annual` — auto-renewing annual subscription at roughly one third
  the lifetime price.

Price points and the exact split are marketing decisions; the technical
plan supports either SKU existing in isolation or both together.

### Store policy notes

- Apple requires that any external purchase path (e.g., buying Pro on
  myfaq.app with a credit card) either is not mentioned at all in the
  iOS build, or goes through StoreKit External Purchase Link Entitlement
  for eligible regions. Simplest path for v1: in-app purchase only.
- Google Play permits alternative billing in some regions, but the v1
  build uses Google Play Billing only.
- Both stores require that the free tier is genuinely useful without
  ever purchasing, which is the case here — read and offline work
  fully.

## CI/CD and distribution

- **Repository**: `phpMyFAQ/MyFAQ` on GitHub
  (<https://github.com/phpMyFAQ/MyFAQ>). The mobile app source tree
  lives in this repo alongside the plans; it is not a separate
  monorepo. Tagged releases track phpMyFAQ minor versions — a `1.0.0`
  app targets phpMyFAQ `4.2.x`.
- **App identifiers**:
  - iOS bundle ID: `app.myfaq.ios`
  - Android application ID: `app.myfaq.android`
- **Store listing names**: "MyFAQ.app" (display) and "MyFAQ.app —
  phpMyFAQ for iOS / Android" (subtitle). The domain `myfaq.app` is
  the primary marketing landing page.
- **CI**: GitHub Actions with a platform matrix (iOS on a macOS runner,
  Android on an Ubuntu runner). Jobs: lint, unit tests, UI tests, and
  signed-artifact builds.
- **OpenAPI job**: on a phpMyFAQ tag, regenerate the Kotlin client and
  open a pull request against the mobile repository. Humans review the
  diff before merging.
- **Signing**: iOS via an App Store Connect API key stored in GitHub
  encrypted secrets. Android via the Play Publisher API, with the
  upload key rotated through Play App Signing.
- **Distribution**: Apple App Store, Google Play, plus a sideload APK
  published on GitHub Releases and signed with the project release key
  (see `release.md` section 13). The sideload APK must include the
  billing library stub but hide Pro upsells — sideload users never see
  a broken purchase button, they see a "managed by Play" notice.
- **Crash-free budget**: block a release if crash-free sessions drop
  below 99.5% during staged rollout.

## Testing strategy

- **Unit tests** (shared module): HTTP mapping, cache invalidation, sync
  state machine, and search ranking. Run against a recorded set of JSON
  fixtures captured from a real phpMyFAQ dev install.
- **Contract tests**: replay the committed OpenAPI specification against
  the client with schemathesis-style fuzzing to catch drift.
- **End-to-end**: a dockerized phpMyFAQ running in CI (the existing
  `docker-compose.yml` in the main repo is the starting point), seeded
  with a known dataset. Android instrumented tests via Espresso, iOS via
  XCUITest.
- **Device matrix**: the oldest supported iOS minus two, so iOS 16 during
  2026, and Android API 26 and above. A Pixel plus a low-end device in
  CI.
- **Network chaos**: tests run with a Ktor `MockEngine` that injects
  timeouts, 5xx responses, malformed JSON, and partial payloads.

## Server-side prerequisites

The plan is viable today, but a few small server-side additions make it
materially better. File these as phpMyFAQ issues:

1. **Tombstones** — `GET /api/v3.2/faqs/deleted?since=…` returning FAQ
   IDs deleted since a cursor. Without it, the app must diff full lists
   per sync window.
2. **ETag and If-None-Match** on list endpoints — the app can already
   use `fetched_at + ttl`, but HTTP-native caching is cheaper and
   correct.
3. **OAuth discovery** at `/.well-known/oauth-authorization-server` —
   removes the need for users to enter `client_id` and `client_secret`
   manually.
4. **Push registration endpoint** — for future push notifications of new
   FAQs or answered questions. Out of v1 scope but worth sketching the
   contract now.

### Already shipped upstream

- **`GET /api/v3.2/meta`** — single bootstrap call (version, title,
  language, available languages, enabled features, logo URL, OAuth
  discovery metadata). Now implemented in phpMyFAQ; the app uses it
  for the instance selector and the sync bootstrap step, and only
  falls back to the legacy three-call fan-out for older installs.

These are nice-to-have and not blockers. File them separately so the
mobile work can proceed against today's API.

## Phased milestones

### Phase 0 — foundations

Detailed plan: [`phase-0-foundations.md`](phase-0-foundations.md).

- Repository and CI skeleton.
- KMP module scaffolding, SwiftUI and Compose shells.
- Generated API client from the committed OpenAPI spec.
- Instance model, Keychain / Keystore wrappers, encrypted database.
- `Entitlements` facade stub (always returns `false` — real
  implementation lands in Phase 3).

### Phase 1 — read-only MVP (free tier, first public release)

- Workspaces and instance-add flow.
- Categories, FAQ list, FAQ detail (online-only).
- Basic caching with TTL (no FTS yet).
- Search via the server endpoint.
- Paywall screen shell with hard-coded copy, wired to non-functional
  upsell entry points so the layout is validated before IAP exists.
- Dogfood build to the phpMyFAQ team.

### Phase 2 — offline (still free tier)

- SQLite schema, sync pipeline, background sync.
- FTS5 local search with merge-with-server behavior.
- Attachment cache and per-instance cache controls.
- Settings screen, language override, theming.
- Public v1.0.0 on App Store and Play, free tier only. No Pro features
  active yet.

### Phase 3 — Pro release (paid write path)

- StoreKit 2 + Play Billing integration, real `Entitlements` facade.
- `pro_lifetime` and `pro_annual` SKUs live in both stores.
- Login and OAuth2 flows.
- Ask-a-question form with `pending_writes` queue.
- Comment posting gated by auth and by `Entitlements.isPro()`.
- Rating submission.
- Restore-purchases flow in Settings and on the Paywall screen.
- v2.0.0 release. Existing users on v1 see the upsell for the first
  time.

### Phase 4 — polish

- Accessibility pass (VoiceOver and TalkBack).
- Localization — reuse the existing phpMyFAQ translation files where
  possible.
- Telemetry scrubber verified.
- Public TestFlight and Play internal testing lanes kept active.
- Store listings, screenshots, privacy disclosures, App Privacy
  nutrition labels (no tracking, no third-party SDKs).

### Phase 5 (post-launch) — stretch

- Offline write queue battle-tested across flaky networks.
- Push notifications, pending the server endpoint under "Server-side
  prerequisites".
- Widget for iOS Home Screen and Android home widget showing latest or
  popular for the active instance.
- Watch app for saved FAQs.
- iPad-optimized split-view layout.

## Decided

These items are locked for v1 and should not be re-opened without a
deliberate revisit:

- **Tech stack**: Kotlin Multiplatform shared module, SwiftUI on iOS,
  Jetpack Compose on Android.
- **Brand**: MyFAQ.app, on the domain <https://myfaq.app>.
- **Commercial model**: freemium. Reads and offline are free forever.
  Writes (login, ask, comment, rate, register) ship behind a Pro
  unlock in Phase 3.
- **Minimum phpMyFAQ version**: `4.2.0`. No back-compat to `v3.1`. The
  generated OpenAPI client targets the 4.2.x specification only, and
  the sync bootstrap assumes the 4.2 route inventory.
- **iOS bundle ID**: `app.myfaq.ios`. Irreversible in the App Store
  once submitted.
- **Marketing site**: `https://myfaq.app` ships at launch as a single
  landing page with App Store and Play download badges. Docs are not
  mirrored from this repository.

## Open questions

Decide each of these before the relevant phase starts:

1. **Pro pricing** — lifetime only, subscription only, or both? Local
   price points per territory? Decide before Phase 3.
2. **Push roadmap** — do we want a self-hosted push relay under the
   phpMyFAQ project, or lean on APNs / FCM directly with server
   endpoints that store device tokens per user? Decide before Phase 5.
