# NT-Manager — project notes

## Stack
- Native Android, **plain Java** (no Kotlin sources), AGP 9.5.2, minSdk 28 / target+compileSdk 37.
- Views + XML layouts, Material 3 (`Theme.Material3.DayNight.NoActionBar`), no Compose.

## Build / verify
- The PolyDev sandbox has **no JDK and no Android SDK**, so `./gradlew` cannot run here.
  Verify changes by inspection; real builds happen in GitHub Actions.
- CI: `.github/workflows/build-apk.yml` (builds debug + release, uploads APKs, Telegram notify).
- `.base/` is a pristine template copy — do not edit it.

## Signing
- `keystore.properties` + `keystore/release.jks` are git-ignored; CI recreates both from
  repo secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`,
  `SIGNING_STORE_PASSWORD` before Gradle runs.
- `app/build.gradle.kts` signs debug **and** release with that key when `keystore.properties`
  exists, and falls back to unsigned when it does not.

## UI structure
- `activity_main.xml` is a ConstraintLayout: AppBarLayout → FragmentContainerView → BottomNavigationView.
- `FileBrowserFragment` creates two independent `FilePanelView` instances (left/right) sharing one API.
- Bottom bar menu items are **id-only** (no `android:title`) and `labelVisibilityMode="unlabeled"`.

## Git
- Default branch is `main`.
