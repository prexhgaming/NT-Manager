# NT Manager

Android application skeleton built with the Gradle Kotlin DSL and a version catalog.

## Toolchain

| Component | Version |
| --- | --- |
| Gradle | 9.9.2 (wrapper) |
| Android Gradle Plugin | 9.5.2 (stable) |
| JDK | 17 |
| compileSdk / targetSdk | 37 |
| minSdk | 28 (Android 9) |

SDK levels and dependency versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) - change them there, not in `app/build.gradle.kts`.

## Layout

```
app/
  build.gradle.kts              module config (namespace bin.nt.plus, signing, build types)
  proguard-rules.pro            R8 rules
  src/main/AndroidManifest.xml
  src/main/java/bin/nt/plus/    MainActivity
  src/main/res/                 layout, colors (light + night), Material 3 theme, adaptive icon
  src/test/java/                JVM unit tests
gradle/libs.versions.toml       version catalog
.github/workflows/build-apk.yml CI: builds the release APK and pushes it to Telegram
.github/scripts/telegram.py     Telegram notifier used by the workflow
```

## Build

Requires JDK 17 and an Android SDK with platform 37 installed (or let AGP download it).

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew test                 # JVM unit tests
./gradlew lint                 # Android lint
```

## Signing

Release builds are signed with `keystore.properties` when it exists next to `settings.gradle.kts`:

```properties
storeFile=keystore/release.jks
storePassword=******
keyAlias=nt-manager
keyPassword=******
```

Without that file the release variant falls back to the debug key so CI still produces an installable APK. Keystores and `keystore.properties` are git-ignored - keep them in CI secrets.

## CI

`.github/workflows/build-apk.yml` runs on every push/PR to `main`: JDK 17, `./gradlew assembleRelease`, then posts the APK (and the log on failure) to Telegram. It needs the repository secrets `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID`.
