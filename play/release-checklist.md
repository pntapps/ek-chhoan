# Release checklist: Ek Chhoan (com.pntapps.ekchhoan)

## 0. Requirements as of 21 October 2026

- **Target API level:** since 31 Aug 2026, new apps and updates must target **Android 16 (API 36)**. The app has `targetSdk = 36` and `compileSdk = 36`.
  https://developer.android.com/google/play/requirements/target-sdk
- **16 KB page size:** apps targeting API 35+ must support 16 KB pages on 64-bit devices. Play blocks non-compliant updates from **1 Feb 2027**.
  - We ship no native code of our own.
  - Two AndroidX dependencies bundle small `.so` files: `androidx.datastore:datastore-core-android:1.2.1` (`libdatastore_shared_counter.so`) and `androidx.graphics:graphics-path:1.0.1` (`libandroidx.graphics.path.so`, via Compose ui-graphics).
  - Their arm64-v8a and x86_64 LOAD segments are already 16 KB-aligned (p_align = 0x4000, checked 2026-10-21).
  - Verify the final bundle (see step 6).
  https://developer.android.com/guide/practices/page-sizes
- **Privacy policy:** required for every app, including apps that access no personal data. It must be in the Play Console field **and** inside the app (Settings > Privacy policy).
  https://support.google.com/googleplay/android-developer/answer/10144311

## 1. Version

- [ ] Bump `versionCode` (must increase on every upload) and `versionName` in `app/build.gradle.kts`.

## 2. Upload key and Play App Signing

Play App Signing is mandatory for new apps (AAB). Google keeps the **app signing key**. You keep only the **upload key**. If the upload key is lost, it can be reset through Play Console, so the app is not lost with it.

1. Generate the upload keystore once. Keep it **outside** git (`*.jks` and `keystore.properties` are already in `.gitignore`):
   ```
   keytool -genkeypair -v -keystore C:\Users\imran\keys\ekchhoan-upload.jks ^
     -alias upload -keyalg RSA -keysize 4096 -validity 10000 ^
     -dname "CN=PNT Apps, O=PNT Apps, C=BD"
   ```
   (`keytool` is in Android Studio's JBR: `"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"`.)
2. Create `keystore.properties` in the **project root** (`EkChhoan/keystore.properties`, next to `settings.gradle.kts`):
   ```properties
   storeFile=C:/Users/imran/keys/ekchhoan-upload.jks
   storePassword=********
   keyAlias=upload
   keyPassword=********
   ```
   - The keys must be exactly `storeFile`, `storePassword`, `keyAlias` and `keyPassword`, as read by `app/build.gradle.kts`.
   - `storeFile` goes through `file()` in the **app** module, so a relative path is resolved from `app/`. Use an absolute path with forward slashes.
   - If the file is missing, `bundleRelease` produces an **unsigned** bundle that Play will reject.
3. Back up the `.jks` file and the passwords to two separate safe places.
4. On the first upload, in Play Console > Test and release > App integrity > Play App Signing, accept **"Use Google-generated key"** (recommended).

## 3. Build

```
gradlew.bat clean :app:checkForbiddenPermissionsRelease :app:bundleRelease
```
- Output: `app/build/outputs/bundle/release/app-release.aab`.
- `checkForbiddenPermissionsRelease` also runs automatically before `bundleRelease` and `assembleRelease`. Its report is at `app/build/reports/permissions/release-permissions.txt`.
- Check the signature: `keytool -printcert -jarfile app\build\outputs\bundle\release\app-release.aab`. It should show `CN=PNT Apps` (the upload key).
- Optionally test the minified build on a device: `gradlew.bat :app:installRelease` with the upload key, or use `bundletool build-apks --connected-device`.

## 4. Smoke test the release build (R8 full mode)

- [ ] Add, edit, reorder and delete family members. Kill and reopen the app; the list survives (kotlinx.serialization under R8).
- [ ] Photo Picker photo shows on the card. Contact picker fills in the number.
- [ ] One-tap call off: the dialer opens. Turn it on, grant the permission: a direct call starts. Deny it: the app falls back to the dialer.
- [ ] YouTube, Camera, Gallery and Torch work; the internet bar shows each state.
- [ ] Set as default home, then press Home and Back; switch back to the normal home screen.
- [ ] Bengali and English; Extra-large text with system font at maximum; TalkBack reads every tile.
- [ ] Backup: `adb shell bmgr backupnow com.pntapps.ekchhoan`, then uninstall, reinstall and restore. The family list and photos come back.

## 5. Play Console: App content (Policy and programs > App content)

| Item | Answer |
|---|---|
| Privacy policy | https://www.pntapps.com/privacy |
| App access | All functionality available without special access. Add a note about the onboarding picture lock (see data-safety.md). |
| Ads | No ads |
| Content rating | IARC questionnaire below |
| Target audience | **18 and over only**. Answer **No** to "Could your store listing unintentionally appeal to children?". Optionally tick "Restrict minor access". |
| Data safety | See `data-safety.md`. Result: no data collected, no data shared. |
| Advertising ID | No |
| Government / Financial / Health / News | None |

**Target audience rationale:**
- The app is for elders and is set up by adult family members.
- It is not designed for children, so the **Families policy does not apply**, as long as the listing graphics avoid child-appealing elements such as cartoon characters.
- Source: https://support.google.com/googleplay/android-developer/answer/9867159

### Content rating (IARC) questionnaire
- Email: imranansari321@pntapps.com
- Category: **"All Other App Types"**, which covers utility, productivity, communication and launchers. It is not "Game" and not "Social / Communication with user interaction".

| Question | Answer |
|---|---|
| Violence (any kind) | No |
| Fear / horror | No |
| Sexuality / nudity | No |
| Language (profanity, crude humour) | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Gambling (real or simulated) | No |
| Hate speech / discrimination | No |
| Does the app allow users to interact or exchange content with other users? | **No**. Calls go through the phone's dialer, not the app. |
| Does the app share the user's current physical location with other users? | No |
| Does the app allow users to purchase digital goods? | No |
| Does the app contain or provide access to unrestricted internet / web browsing? | **No**. It only launches other installed apps. |
| Is the app primarily a news / educational product? | No |
| User-generated content | No |

Expected rating: **Everyone / PEGI 3 / USK 0 / IARC 3+**.

## 6. Upload

1. Create the app with default language English (US), type **App**, **Free**, and accept the declarations.
2. Fill in the store listing from `store-listing.md`: EN plus the bn-BD translation, icon, feature graphic and screenshots.
3. Start with **Internal testing**:
   - Upload `app-release.aab`.
   - Release notes in EN and BN.
   - Note: new personal developer accounts must run a **closed test with at least 12 testers for 14 days** before production access.
4. App bundle explorer > select the version:
   - Confirm **target SDK 36**.
   - Confirm the permissions list shows only ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CALL_PHONE, (VIBRATE) and the androidx DYNAMIC_RECEIVER permission.
   - Confirm "Memory page size: Supports 16 KB".
5. **Pre-launch report** (Test and release > Testing > Pre-launch report):
   - It runs automatically on closed and open test tracks (and internal, if enabled in settings).
   - Settings: no sign-in needed. Robo test will reach onboarding; optionally upload a Robo script that completes onboarding.
   - Review the **Stability**, **Accessibility** (touch target size, contrast, content labels), **Security**, and **Performance** tabs. Fix any crash, ANR, or missing `contentDescription` before production.
6. Android vitals after release: crash and ANR data are collected by Play itself, not by the app, so they do not affect Data safety.

## 7. Accessibility (spec §6 and Play)

Play has no hard accessibility gate for normal apps. The AccessibilityService policy applies only to apps that declare an AccessibilityService, and this app declares none.

The Pre-launch report's accessibility checks are still the best automated check:
- Touch targets ≥ 48dp (ours are ≥ 72dp)
- Contrast ≥ 4.5:1
- Labels on every clickable

Also do a manual TalkBack pass.
Source: https://support.google.com/googleplay/android-developer/answer/10964491

## 8. Privacy policy and terms on pntapps.com

The app links to these pages from Settings → About (constants in `PntAppsLinks`, `ui/settings/SettingsViewModel.kt`):

| Page | URL |
|---|---|
| Website | https://www.pntapps.com |
| About | https://www.pntapps.com/about |
| Privacy policy | https://www.pntapps.com/privacy |
| Terms of use | https://www.pntapps.com/terms |

1. Make sure https://www.pntapps.com/privacy covers Ek Chhoan. `play/privacy-policy.html` has app-specific text (English + Bengali) to publish there or merge into the existing policy.
2. Open each URL in an incognito window and on a phone. They must be public, not PDFs, not geo-blocked, and need no login.
3. Paste https://www.pntapps.com/privacy in Play Console > App content > Privacy policy.
4. When the policy changes, update its effective date. The URL stays the same.

## 9. After every release

- [ ] Re-check the Data safety answers if any dependency changed. Run `gradlew :app:dependencies` and look for analytics, crash or network SDKs.
- [ ] Re-check `release-permissions.txt`.
- [ ] Each August, check the new target-API deadline and raise `targetSdk`.
