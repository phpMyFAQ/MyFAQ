# MyFAQ.app

Native iOS + Android client for phpMyFAQ. Planning stage — no code yet, only `plans/mobile-app-plan.md`.

## Status

- Repo: `phpMyFAQ/MyFAQ` (this checkout). Current contents: `plans/`, `LICENSE`. Source tree lands in Phase 0.
- App versions track phpMyFAQ minor versions (app `1.0.0` → phpMyFAQ `4.2.x`).

## Locked decisions (do not re-open without deliberate revisit)

- **Stack**: Kotlin Multiplatform shared module; SwiftUI (iOS); Jetpack Compose (Android).
- **Brand**: MyFAQ.app, domain `https://myfaq.app`.
- **Model**: freemium. Read + offline free forever. Writes (login, ask, comment, rate, register) behind Pro unlock in Phase 3.
- **Minimum phpMyFAQ version**: `4.2.0`. No back-compat to `v3.1`. OpenAPI client generated from 4.2.x spec only.
- **iOS bundle ID**: `app.myfaq.ios`. Irreversible in store.
- **Marketing site**: `https://myfaq.app` is a single landing page with download badges at launch. No mirrored docs.

## Scope guardrails

- **v1 is read-only.** No writes, no auth prompts in free tier.
- **No `admin/api/*`.** Session + CSRF not a fit for native client. App is not a second admin panel.
- **No push in v1.** Server contract missing.
- **Excluded entirely**: `faq/create|update`, `category` POST, `backup/{type}`.
- API surface pinned to phpMyFAQ public `v4.0` (`mobile/spec/openapi/v.4.0.yaml`). Most list endpoints return paginated `{ success, data, meta }` wrappers.

## Shared libs (locked)

Ktor · kotlinx.serialization · SQLDelight (FTS5) · Koin · openapi-generator · WorkManager/BGTaskScheduler · Coil/SDWebImageSwiftUI · StoreKit 2 / Play Billing v7+.

Secure storage: iOS Keychain + Android `androidx.security.crypto`. DB encrypted: SQLCipher (Android), Data Protection Class B (iOS). Credentials NEVER in SQLite.

## Phases

- **0** — foundations: CI, KMP scaffold, generated client, `Entitlements` stub returns `false`. Detailed plan: `plans/phase-0-foundations.md`.
- **1** — read-only MVP online, server search, paywall shell non-functional.
- **2** — offline: SQLite + FTS5 + background sync + attachments. Public v1.0.0, free tier only.
- **3** — Pro: StoreKit 2 + Play Billing, login/OAuth2, ask/comment/rate, `pending_writes` queue. v2.0.0.
- **4** — polish: a11y, l10n, telemetry scrub.
- **5** — stretch: widgets, watch, iPad split-view, push (pending server).

## Pro SKUs (to finalize before Phase 3)

`pro_lifetime` (one-time) + `pro_annual` (~1/3 lifetime price). Tech plan supports either alone or both.

## Open questions (see plan §"Open questions")

1. Pro pricing model — before Phase 3.
2. Push architecture — before Phase 5.

## Server-side asks (nice-to-have, non-blocking)

Filed as phpMyFAQ issues: tombstones (`faqs/deleted?since=`), ETag on list endpoints, OAuth discovery, push registration contract.

**Already shipped upstream**: `GET /api/v4.0/meta` (single bootstrap call — version, title, language, available languages, features, logo URL, OAuth discovery). App uses it for instance selector + sync bootstrap.

## Working on this repo

- Primary doc: `plans/mobile-app-plan.md`. Treat as source of truth for architecture questions.
- Phase 0 plan: `plans/phase-0-foundations.md`.
- When editing the plan: preserve "Decided" / "Open questions" structure. Do not move locked items back to open without explicit user request.
- Mobile app source tree will live in this same repo (`phpMyFAQ/MyFAQ`), not in a separate `myfaq-app` repo.
