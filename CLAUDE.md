# CLAUDE.md — Ek Chhoan

Elder-friendly Android **home-screen launcher** by **PNT Apps** (package `com.pntapps.ekchhoan`). One calm Home screen: contact call cards (Family + custom groups), big app tiles, clock, internet status bar; a family member configures everything in Settings behind a picture lock. Audience: older people in India/Bangladesh who may not read well. Original product spec: `docs/original-spec.md` (the app has since grown beyond it — this file is the current truth).

## Non-negotiables

1. **Google Play compliance / privacy.** The merged manifest may only request `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CALL_PHONE`, `VIBRATE` (+ AndroidX's own `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`). The Gradle task `checkForbiddenPermissions<Variant>` (in `app/build.gradle.kts`) fails `assemble*`/`bundle*`/`install*`/`check` otherwise. **No `INTERNET`**, no analytics/ads/Firebase/crash SDKs, no `QUERY_ALL_PACKAGES` (installed apps are seen through the `MAIN/LAUNCHER` `<queries>` entry), no `READ_CONTACTS` (contact picker grants one row), no storage/media permissions (Photo Picker). All data stays on device (DataStore + `filesDir/photos`). If you add a dependency, check it adds no permission or network code, and update `play/data-safety.md` if anything changes.
2. **Elder UX.** Touch targets ≥ 72dp, elder-facing text ≥ 15sp (`ElderSize`, `elderStyle()`, `FitText`), contrast ≥ 4.5:1 in every theme, never color alone (icon + words), every tap has haptics, Back does nothing on Home, nothing on Home may shift after it appears (Home waits until settings, calling apps and favourites are loaded).
3. **Strings in three languages.** English (`values/`, default), Bengali (`values-bn/`), Hindi (`values-hi/`). Every new user-visible string goes into all three. Files are split: `strings.xml` (app name), `strings_home.xml` (elder screens), `strings_settings.xml` (settings, lock, onboarding).
4. **Licensing/attribution.** GPL-3.0 (`LICENSE`) + section 7 additional terms (`NOTICE`): forks must stay GPL and keep "Developed by PNT Apps" credit in README and in the app UI. Keep Settings → About (developer credit + links in `PntAppsLinks`, `ui/settings/SettingsViewModel.kt`). **The app UI must not mention that it is open source** (owner's choice; the copyright holder isn't bound to show license text).
5. **Git:** the owner decides when to commit/push. Don't commit or push unless explicitly asked.

## Stack & versions

- AGP **9.3.3** with **built-in Kotlin** (no `kotlin-android` plugin; `kotlin-gradle-plugin` 2.4.21 on the buildscript classpath), Gradle 9.5, JDK 17+.
- `compileSdk 37` (Coil 3.6.3 requires it), **`targetSdk 36`** (Play requirement for 2026), `minSdk 26`.
- Jetpack Compose (BOM 2026.09.00) + Material 3, navigation-compose, DataStore Preferences, kotlinx-serialization, Coil 3, AppCompat (per-app language via `AppCompatDelegate.setApplicationLocales`).
- No DI framework: `AppContainer` in `EkChhoanApp.kt`; `Context.container` extension. ViewModels use `viewModelFactory { initializer { … } }`.

## Commands

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug   # the usual check (all must pass)
./gradlew :app:bundleRelease          # R8 release AAB; signing from keystore.properties (git-ignored)
cat app/build/reports/permissions/debug-permissions.txt              # what the permission guard saw
```

Unit tests (JVM, no device) live in `app/src/test/…`: home ordering, theme parsing, family/group serialization, picture-lock hashing, internet-state derivation, tap debouncer, greeting.

## Architecture (app/src/main/java/com/pntapps/ekchhoan/)

```
EkChhoanApp.kt        AppContainer (settings, photos, internet, torch, launcher, defaultLauncher, speaker, lastUnlockAt)
MainActivity.kt       single activity, NavHost routes: home, unlock, settings, edit/{id}?group=, setlock, arrange, onboarding;
                      APP_LANGUAGES / DEFAULT_LANGUAGE ("en"); theme + system-bar colors from the chosen palette
data/
  FamilyMember.kt     FamilyMember(groupId), ContactGroup, AppSettings (everything the family configures), enums
                      TextSize, IconSize, AppTheme (+ parse() migrating old Light/Dark/System), limits:
                      MAX_FAMILY_MEMBERS=6 per group, MAX_GROUPS=5 custom, MAX_FAVORITE_APPS=8, FAMILY_GROUP_ID="family"
  SettingsRepository  DataStore read/write; family JSON, groups JSON, comma-joined favourites / homeOrder
  HomeOrder.kt        HomeItem keys ("contacts","youtube","camera","gallery","torch","internet","app:<pkg>"),
                      AppSettings.homeItems(communicationPackages) = display order, moveHomeItem()
  PhotoStore.kt       picked photo → filesDir/photos/<id>_<ts>.jpg (≤512px); resolve() by file name (survives restore)
  PictureLock.kt      9 pictures, salted SHA-256 of a 3-picture sequence (repeats allowed)
system/
  Contracts.kt        InternetState, InternetMonitor, TorchState, TorchController, LaunchResult, Speaker interfaces
  AppLauncher.kt      every outgoing intent (call/dial, YouTube, camera, gallery, internet panel, URLs, favourite apps),
                      launchableApps(), appInfo(), communicationApps() (dialer/contacts/SMS by role + WhatsApp), TapDebouncer
  InternetMonitor.kt  ConnectivityManager callbacks → debounced StateFlow; pure deriveInternetState() is unit-tested
  TorchController.kt  CameraManager torch (no CAMERA permission)
  DefaultLauncherHelper.kt  RoleManager HOME request / home settings
  Speaker.kt          TextToSpeech in the app's UI language (bn-BD/bn-IN, hi-IN, en)
ui/theme/             Palette per AppTheme (Color.kt), CardStyle (outline vs soft shadow, corner extra, gradient chips),
                      color tokens are @Composable getters reading LocalPalette — use them only in composition
                      (capture into a local val before draw lambdas); LocalTextScale; LocalHomeIcons (icon scale, labels)
ui/common/            elderClickable (haptic + press scale), cardShadow/cardSurface/chipBrush, FitText (shrinks until no
                      word splits, ≥15sp), toAppIconBitmap (adaptive icons zoomed to safe zone), FriendlyMessage
ui/home/              HomeScreen (renders homeItems: full-width runs + 2-column TileGrid), HomeViewModel, cards/tiles
ui/settings/          SettingsScreen (sections), SettingsViewModel (+ EditMemberViewModel, PntAppsLinks),
                      EditMemberScreen (group chooser, contact picker, photo picker), FavoriteApps (searchable AppPickerDialog,
                      AppIcon), ThemePicker (live previews), ArrangeHomeScreen (drag & drop reorder)
ui/lock/              PictureLockScreen / SetLockScreen / SetLockContent
ui/onboarding/        Steps: Welcome(language) → Family → Call (CALL_PHONE) → Apps → Lock → Home(role) → Done
```

Other folders: `play/` (privacy-policy.html, data-safety.md, store-listing.md, permissions-declaration.md, release-checklist.md), `docs/original-spec.md`, `app/src/main/assets/licenses/` (OFL font licenses), `app/src/main/res/font/` (Atkinson Hyperlegible + Noto Sans Bengali; Devanagari uses the system fallback).

## Behaviours worth knowing (bugs already fixed — don't reintroduce)

- **Becoming the default home starts a new activity in the home task.** Onboarding persists its step (`AppSettings.onboardingStep`) and the old app-drawer task finishes itself. `next()` only advances from the step it was created on (double-tap can't skip the lock). Inserting a `Step` shifts persisted indices (harmless).
- **Home must not reflow**: `HomeViewModel` exposes `communicationApps`/`favorites` as null until loaded; HomeScreen draws a blank background until then.
- **Intrinsic measurement**: `GridRows` uses `IntrinsicSize.Min`; never put `BoxWithConstraints`/`SubcomposeLayout` inside tiles or cards (crashed once). `FitText` is built on plain `Text` + `onTextLayout` for that reason.
- **Drag & drop (ArrangeHomeScreen)**: side padding is outer padding (not `contentPadding`) so pointer and item coordinates match; `userScrollEnabled = false` while dragging (the grid would steal the gesture); target = nearest item in the finger's row (full-width cards' centre falls in the column gutter); hysteresis via `lastTarget`; `requestScrollToItem` before moving the first visible item; edge auto-scroll; working copy only saved on Done; TalkBack gets Move up/down custom actions.
- **App icons**: draw installed icons with `toAppIconBitmap()` and a hairline border, same size as the colored chips, or white icons (YouTube) look tiny on white tiles.
- Photos are referenced by file name inside `filesDir/photos` (absolute paths break after restore / on secondary users).
- Defaults: language English; theme Classic; icons with names, Normal size; one-tap call off until CALL_PHONE granted.

## Testing on an emulator (Windows dev machine notes)

- AVD `Medium_Phone` (API 37). A physical phone may also be attached: always target `ANDROID_SERIAL=emulator-5554`.
- In Git Bash prefix adb commands that use device paths with `MSYS_NO_PATHCONV=1` (e.g. `/sdcard/...`), and pass Windows paths (`C:/…`) to `adb install`.
- `local.properties` must escape the SDK path: `sdk.dir=C\:\\Users\\…\\Sdk` (lint errors otherwise). It is git-ignored.
- UI dump: `adb shell uiautomator dump /sdcard/u.xml && adb exec-out cat /sdcard/u.xml`. Long-press-drag: `adb shell input motionevent DOWN x y`, wait ~1.2 s, several `MOVE`, then `UP`.
- Inspect saved settings: `adb exec-out run-as com.pntapps.ekchhoan cat files/datastore/settings.preferences_pb`.
- Settings stay unlocked for 60 s after an unlock (`UNLOCK_GRACE_MS`); force-stop the app to get the lock screen again.

## Code style

Match the surrounding code: concise KDoc on public/important items, comments explain *why*, Compose with `Modifier` as the first optional parameter, `StateFlow` + unidirectional data flow, small composables per file. Keep new settings in `AppSettings` + `SettingsRepository` (with a default that preserves current behaviour), add a unit test for pure logic, and verify on the emulator for UI changes.
