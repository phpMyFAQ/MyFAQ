# Release signing setup

One-time setup the repo owner has to do before the first `mobile-vX.Y.Z`
tag can produce signed Android and TestFlight builds. After this is
done, every tag push runs `.github/workflows/mobile-release.yml` end to
end with no further manual steps.

All secrets go to **GitHub → Settings → Secrets and variables →
Actions → New repository secret** (or "Environment secret" if you want
a manual approval gate around release).

---

## Android

### 1. Generate the upload keystore (one time, ever)

```bash
keytool -genkey -v \
  -keystore upload.jks \
  -alias myfaq-upload \
  -keyalg RSA -keysize 4096 -validity 36500 \
  -storetype JKS
```

You'll be prompted for:

- **Keystore password** — store it in 1Password / your password manager.
- **Key password** — can be the same as the keystore password.
- Distinguished Name fields — `CN=phpMyFAQ Mobile`, `O=phpMyFAQ`,
  rest as you like. The Play Store doesn't show this.

> **Critical:** back up `upload.jks` somewhere durable
> (1Password attachment, encrypted off-site backup). Losing it means
> you can never publish another update under the same Play listing
> without going through Play's "reset upload key" support flow.

### 2. Encode the keystore for GitHub

```bash
base64 -i upload.jks | pbcopy   # macOS, copies to clipboard
# or:  base64 -i upload.jks > upload.jks.b64
```

### 3. Add the four Android secrets

| Secret name                 | Value |
|-----------------------------|-------|
| `ANDROID_KEYSTORE_BASE64`   | the base64 string from step 2 |
| `ANDROID_KEYSTORE_PASSWORD` | the keystore password from step 1 |
| `ANDROID_KEY_ALIAS`         | `myfaq-upload` |
| `ANDROID_KEY_PASSWORD`      | the key password from step 1 |

That's it for producing a signed AAB + APK. They'll be attached to
the GitHub Release automatically when you push a `mobile-v*` tag.

### 4. (Optional, deferred) Play Console upload

To skip the manual "upload AAB to Play Console" step:

1. In Play Console → Setup → API access, create a service account.
2. Grant it "Release manager" on the app.
3. Download the JSON key.
4. Add as `PLAY_SERVICE_ACCOUNT_JSON` secret (paste the whole JSON).

The current workflow doesn't yet wire this in (Phase 1.1). Until
then, download the `androidApp-release-aab` artifact from the
release run and upload it manually to the internal track.

---

## iOS / TestFlight

You need an Apple Developer account on the phpMyFAQ team and
admin access to App Store Connect.

### 1. Create the app record (one time)

In App Store Connect → My Apps → "+":

- Platform: iOS
- Name: **MyFAQ.app**
- Bundle ID: **app.myfaq.ios** (must match the locked bundle ID;
  create it under Certificates, Identifiers & Profiles → Identifiers
  first if it doesn't exist)
- SKU: anything unique, e.g. `myfaq-ios`

### 2. Distribution certificate

In Xcode → Settings → Accounts → your team → Manage Certificates:

1. "+" → **Apple Distribution**.
2. Right-click the new cert → **Export Certificate…** → save as
   `dist.p12`, set a password (store it in 1Password).
3. Encode and add to GitHub:
   ```bash
   base64 -i dist.p12 | pbcopy
   ```

| Secret name | Value |
|---|---|
| `IOS_DIST_CERT_P12_BASE64` | base64 of `dist.p12` |
| `IOS_DIST_CERT_PASSWORD` | the export password |

### 3. App Store provisioning profile

In the Apple Developer portal → Profiles → "+":

- Type: **App Store** (Distribution)
- App ID: `app.myfaq.ios`
- Certificate: the Apple Distribution cert from step 2
- Name: `MyFAQ App Store`

Download the `.mobileprovision`, then:

```bash
base64 -i MyFAQ_App_Store.mobileprovision | pbcopy
```

| Secret name | Value |
|---|---|
| `IOS_PROVISIONING_PROFILE_BASE64` | base64 of the `.mobileprovision` |

### 4. App Store Connect API key (for the TestFlight upload)

In App Store Connect → Users and Access → Integrations → App Store
Connect API → "+":

- Name: `MyFAQ CI`
- Access: **App Manager** (or Developer if you want the minimum)
- Download the `.p8` immediately — Apple shows it exactly once.

Note the **Key ID** (10-char string) and the **Issuer ID** (UUID at
the top of the page) shown next to the key.

```bash
base64 -i AuthKey_XXXXXXXXXX.p8 | pbcopy
```

| Secret name | Value |
|---|---|
| `APP_STORE_CONNECT_API_KEY_ID` | the 10-char Key ID |
| `APP_STORE_CONNECT_API_ISSUER_ID` | the Issuer UUID |
| `APP_STORE_CONNECT_API_KEY_BASE64` | base64 of the `.p8` file |

### 5. Set the development team in `project.yml`

`mobile/iosApp/project.yml` currently has `DEVELOPMENT_TEAM: ""` —
fill it with your 10-char Apple Team ID (Apple Developer portal →
Membership). Commit that change. Without it, `xcodebuild archive`
can't pick the right cert.

---

## Verifying

Once all the above are in place:

```bash
git tag -a mobile-v0.1.0 -m "Phase 1 read-only MVP"
git push origin mobile-v0.1.0
```

Watch the run at **Actions → mobile-release**. Both jobs should
finish green. After ~15 minutes the build appears under TestFlight →
Internal Testing. The signed AAB + APK appear on the GitHub Release
page automatically.

If a secret is missing the corresponding job logs a `::warning::`
and falls back to producing an unsigned artifact instead of failing
the workflow — useful for sanity-checking the pipeline before the
full secret set is in place.

---

## Rotating / leaked secrets

- **Android upload keystore leaked**: contact Google Play support to
  reset the upload key. Generate a new keystore, update all four
  Android secrets. The app-signing key (held by Google after Play
  App Signing enrollment) is unaffected.
- **iOS distribution cert leaked**: revoke in Apple Developer portal,
  generate a new one, regenerate the provisioning profile, update
  `IOS_DIST_CERT_*` and `IOS_PROVISIONING_PROFILE_BASE64`.
- **App Store Connect API key leaked**: revoke in App Store Connect,
  generate a new key, update the three `APP_STORE_CONNECT_API_*`
  secrets. Key revocation is instant.
