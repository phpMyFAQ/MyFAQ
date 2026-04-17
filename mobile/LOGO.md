# Logo / App Icon Drop Instructions

Drop the phpMyFAQ logo PNGs into the locations below. After dropping
the files, rebuild both apps. No code changes required — the asset
catalogs / drawable folders are already wired into the Xcode project,
the Android manifest, and the start screen.

## iOS

`mobile/iosApp/iosApp/Assets.xcassets/`

| File | Size | Purpose |
|------|------|---------|
| `AppIcon.appiconset/icon-1024.png` | 1024×1024 (opaque, no alpha) | App icon — App Store + home screen |
| `Logo.imageset/logo.png`           |  120×120 | Start-screen logo (1×) |
| `Logo.imageset/logo@2x.png`        |  240×240 | Start-screen logo (2×) |
| `Logo.imageset/logo@3x.png`        |  360×360 | Start-screen logo (3×) |

After dropping, in `Info.plist` make sure `CFBundleIcons` references
`AppIcon` (Xcode does this automatically when the asset is present).

## Android

`mobile/androidApp/src/main/res/`

| File | Size | Purpose |
|------|------|---------|
| `mipmap-mdpi/ic_launcher.png`     |  48×48  | App icon |
| `mipmap-hdpi/ic_launcher.png`     |  72×72  | App icon |
| `mipmap-xhdpi/ic_launcher.png`    |  96×96  | App icon |
| `mipmap-xxhdpi/ic_launcher.png`   | 144×144 | App icon |
| `mipmap-xxxhdpi/ic_launcher.png`  | 192×192 | App icon |
| `drawable/logo.png`               | 512×512 (transparent OK) | Start-screen logo |

`AndroidManifest.xml` already references `@mipmap/ic_launcher`. The
start screen looks up `R.drawable.logo` — keep the filename literal
`logo.png`.

## Start screen

The start screen (Workspaces empty state) renders the logo above the
"Add your first instance" CTA. Logo is also reused at the top of the
Add Instance sheet.
