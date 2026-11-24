# Signing Setup for NT-Manager

## Files Changed

1. **app/build.gradle.kts** - Updated signing configurations:
   - Added explicit `debug` signing config (Android SDK default)
   - Kept existing `release` config that reads from `keystore.properties`
   - Both debug and release build types now properly reference signing configs

2. **keystore.properties** - Created with release keystore path and credentials:
   ```
   storeFile=keystore/release.jks
   storePassword=change-me
   keyAlias=nt-manager
   keyPassword=change-me
   ```

3. **keystore/release.jks** - Release keystore file (from uploaded zip)
   - Base64 encoded and ready for GitHub repo secrets

4. **keystore/debug.keystore** - Placeholder for Android debug keystore

5. **.gitignore** - Updated to exclude:
   - `keystore/` directory
   - `keystore.properties`
   - `*.jks`
   - `*.keystore`
   - `*.jks.b64`

## GitHub Repository Secrets

Add the following secret to your GitHub repository (`prexhgaming/NT-Manager`):

| Secret Name | Value |
|-------------|-------|
| `RELEASE_KEYSTORE_BASE64` | `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' |

## CI Integration

The CI workflow (`.github/workflows/build-apk.yml`) already references these secrets. To use the custom keystore in CI:

1. Add `RELEASE_KEYSTORE_BASE64` to GitHub Secrets
2. The workflow will automatically decode and use it to sign release builds

## Local Build Verification

To build locally with the signing configuration:

```bash
./gradlew assembleDebug  # Debug APK (signed with Android debug key)
./gradlew assembleRelease  # Release APK (signed with NT-Manager release key)
```

## Notes

- The `keystore.properties` file contains placeholder passwords (`change-me`). Update them with your actual keystore passwords.
- The debug keystore is the standard Android SDK debug keystore (not stored in this repo).
- The release keystore is stored encrypted in the repository secrets to avoid committing credentials.
