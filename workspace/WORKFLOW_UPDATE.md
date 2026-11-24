# Workflow Update - Keystore Signing

## Changes Made

### 1. Updated `.github/workflows/build-apk.yml`
Added a new step that:
- Creates the `keystore/` directory
- Decodes the `SIGNING_KEYSTORE_BASE64` secret from GitHub
- Writes the keystore file to `keystore/release.jks`
- Creates `keystore.properties` with the actual values from secrets:
  - `storePassword` (from `SIGNING_STORE_PASSWORD`)
  - `keyAlias` (from `SIGNING_KEY_ALIAS`)
  - `keyPassword` (from `SIGNING_KEY_PASSWORD`)

This ensures the keystore file and properties are recreated fresh on every build, preventing any cached or invalid signing configurations.

### 2. Existing Files Verified
- **keystore.properties** - Contains the signing configuration (placeholder values, replaced in CI)
- **app/build.gradle.kts** - Already configured to:
  - Read from `keystore.properties` if it exists
  - Use Android debug key as fallback
  - Sign both debug and release variants
- **keystore/release.jks** - The actual release keystore (from uploaded file)
- **keystore/debug.keystore** - Placeholder for debug keystore

## GitHub Secrets Required

Ensure these secrets are set in your repository (`prexhgaming/NT-Manager`):

| Secret Name | Description |
|-------------|-------------|
| `SIGNING_KEYSTORE_BASE64` | Base64-encoded release.jks file |
| `SIGNING_KEY_ALIAS` | The key alias (`nt-manager`) |
| `SIGNING_KEY_PASSWORD` | Key password |
| `SIGNING_STORE_PASSWORD` | Store password |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token (already exists) |
| `TELEGRAM_CHAT_ID` | Telegram chat ID (already exists) |

## How It Works

1. Workflow runs on push to main or manual dispatch
2. New step: **"Decode keystore and create signing properties"**
3. Decodes `SIGNING_KEYSTORE_BASE64` → writes `keystore/release.jks`
4. Creates `keystore.properties` with values from secrets
5. Build step runs `./gradlew assembleRelease`
6. Gradle reads `keystore.properties` and signs the APK with the release key
7. APK is uploaded to Telegram

## Testing

The workflow will now automatically sign release builds with your keystore on every push to main.
