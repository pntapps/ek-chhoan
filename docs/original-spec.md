# Build Prompt: "Ek Chhoan" (এক ছোঁয়া) — Elder-Friendly Android Launcher (Kotlin + Jetpack Compose)

> Copy everything below this line into your AI coding assistant (Android Studio Gemini, Claude, Cursor, etc.).
> Build in the milestone order at the end. Do not skip the compliance rules.

---

## 1. Role and goal

You are a senior Android engineer. Build a complete, production-quality Android app in **Kotlin** using **Jetpack Compose** in **Android Studio**.

The app is a **home-screen launcher for elderly people in Bangladesh/West Bengal**, many of whom cannot read well or type. It replaces the normal Android home screen with one simple screen containing:

1. A greeting and the date (Bengali)
2. Up to 6 **family cards** (photo + name). Tapping a card calls that person through the phone's default dialer
3. Four big buttons: **YouTube, Camera, Gallery, Torch**
4. One big **internet status bar** that shows if internet is working (Wi-Fi / mobile data / Wi-Fi without internet / offline)
5. A small locked **settings** button that only family members can open

Nothing else. No emergency feature, no contacts app, no SMS, no location, no tracking, no server, no ads, no analytics.

The elder must be able to use everything **without reading or typing**.

---

## 2. Tech stack and project setup

- Language: Kotlin (latest stable)
- UI: Jetpack Compose with Material 3, single `MainActivity`
- Build: Gradle Kotlin DSL (`build.gradle.kts`) with a version catalog (`libs.versions.toml`)
- `minSdk = 26`
- `compileSdk` and `targetSdk` = the latest API level Google Play currently requires for new apps (check Play Console; use 35 or higher)
- Architecture: MVVM — `ViewModel` + `StateFlow`, unidirectional data flow
- Storage: **Jetpack DataStore** (Preferences) + `kotlinx.serialization` for the family list. No Room needed.
- Images: **Coil** (`coil-compose`) for family photos
- Navigation: `navigation-compose` (Home, Unlock, Settings, Edit Family Member, Onboarding)
- No dependency-injection framework; use a simple manual `AppContainer` in the `Application` class
- No `INTERNET` permission. The app never talks to a network.
- Package name (applicationId and namespace): `com.pntapps.ekchhoan` — final, do not change

### Folder structure

```
app/src/main/java/com/pntapps/ekchhoan/
  EkChhoanApp.kt                 // Application, holds AppContainer
  MainActivity.kt             // single activity, launcher entry
  data/
    FamilyMember.kt           // @Serializable data class
    SettingsRepository.kt     // DataStore read/write
    PhotoStore.kt             // copy picked photo into filesDir
  system/
    InternetMonitor.kt        // connectivity -> StateFlow<InternetState>
    TorchController.kt        // CameraManager torch -> StateFlow<Boolean>
    AppLauncher.kt            // dial, YouTube, camera, gallery, internet panel
    DefaultLauncherHelper.kt  // RoleManager / home settings
    Speaker.kt                // optional TextToSpeech readout
  ui/
    theme/ (Color.kt, Type.kt, Theme.kt, Dimens.kt)
    home/ (HomeScreen.kt, HomeViewModel.kt, FamilyCard.kt, AppTile.kt, TorchTile.kt, InternetBar.kt)
    lock/ (PictureLockScreen.kt)
    settings/ (SettingsScreen.kt, EditMemberScreen.kt, SettingsViewModel.kt)
    onboarding/ (OnboardingScreen.kt)
app/src/main/res/
  values/strings.xml          // English
  values-bn/strings.xml       // Bengali (default UI language)
  font/                       // Atkinson Hyperlegible + Noto Sans Bengali (bundled .ttf)
```

---

## 3. Design system (match exactly)

### Colors
| Token | Hex | Use |
|---|---|---|
| Background | `#F2F4F7` | Screen background |
| Surface | `#FFFFFF` | Cards and tiles |
| Border | `#E2E6EC` | Card borders, neutral pills |
| Ink | `#111827` | Main text |
| InkSoft | `#374151` | Date, secondary text |
| CallGreen | `#1B7F3B` | Call badge on family cards |
| YouTubeRed | `#C62828` | YouTube icon chip |
| CameraPurple | `#5B3CC4` | Camera icon chip |
| GalleryOrange | `#B4530A` | Gallery icon chip |
| TorchOnBg / TorchOnBorder | `#FFE08A` / `#E0A800` | Torch tile when on |

Family avatar tints (cycle through): `#DCE7FF`/`#1E3A8A`, `#FFE4CC`/`#8A3A00`, `#E6DDFF`/`#4527A0`, `#D4F0E4`/`#0F5132` (background / initial letter color).

Internet bar states:
| State | Background | Border + icon circle | Text |
|---|---|---|---|
| Wi-Fi OK | `#E3F5EC` | `#1B7F3B` | `#0F5132` |
| Mobile data OK | `#E3F5EC` | `#1B7F3B` | `#0F5132` |
| Wi-Fi, no internet | `#FFF0DC` | `#B4530A` | `#7A3300` |
| Offline | `#FDE4E4` | `#C62828` | `#8E1616` |

Light theme only for v1. Ignore system dark mode on the home screen (elders get confused when colors change at night).

### Typography
- Bundle fonts in `res/font`: **Atkinson Hyperlegible** (Latin) and **Noto Sans Bengali** (Bengali). Create a `FontFamily` that includes both so mixed text renders correctly.
- Sizes (in `sp`, but multiply by the user's "text size" setting — see §6):
  - Greeting: 30sp bold
  - Date: 19sp
  - Section title "পরিবার": 21sp bold
  - Family name: 22sp bold
  - Tile label: 20sp bold
  - Internet title: 20sp bold, subtitle 16sp
  - Status pills: 15sp bold
- Never use text smaller than 15sp anywhere on the elder-facing screens.

### Shape and size
- Screen padding 20dp, gaps 12–18dp
- Family card: height 150dp, corner 24dp, 2dp border, avatar circle 72dp, call badge 32dp green circle with phone icon
- App tile: height 84dp, corner 20dp, icon chip 50dp with 15dp corners, white icon
- Internet bar: full width, height 88dp, corner 22dp, 3dp colored border, icon circle 54dp
- **Minimum touch target 72dp** for everything the elder touches
- Settings lock button: 48dp grey circle, top-right

### Layout of Home (top to bottom, no scrolling on a normal 6" phone)
1. Row: greeting + date (left), lock button (right)
2. "পরিবার" title, then 2-column grid of family cards (1–6 members; if more than 4, grid can scroll vertically but keep tiles same size)
3. 2×2 grid: YouTube, Camera, Gallery, Torch
4. Internet status bar (full width)

Icons: use Material Symbols (Rounded) via `material-icons-extended` — phone, smart_display (YouTube placeholder; at runtime prefer the real installed app icon from `PackageManager.getApplicationIcon`), photo_camera, image, flashlight_on, wifi, signal_cellular_alt, warning, wifi_off, lock.

---

## 4. Feature specs

### 4.1 Launcher behaviour
- In `AndroidManifest.xml`, `MainActivity` gets two intent filters:
  - `MAIN` + `LAUNCHER` (so it shows in app drawer for setup)
  - `MAIN` + `HOME` + `DEFAULT` (so it can be the home screen)
- `android:launchMode="singleTask"`, `android:screenOrientation="portrait"`, `android:excludeFromRecents="false"`.
- When the app is the home screen, the Back button on Home does nothing (override with `BackHandler` that consumes the event). Pressing Home always returns to the Home screen and closes any open sub-screen.
- On first launch, show **Onboarding** (§4.8) which asks to set Ek Chhoan as default home:
  - API 29+: `RoleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)`
  - API 26–28: open `Settings.ACTION_HOME_SETTINGS`
- Do NOT use Device Admin, Device Owner, kiosk/lock task, or AccessibilityService.

### 4.2 Greeting and date
- Greeting changes by hour: 5–11 "শুভ সকাল", 12–16 "শুভ দুপুর", 17–19 "শুভ সন্ধ্যা", else "শুভ রাত্রি" (English equivalents in `values/strings.xml`).
- Date: `EEEE, d MMMM` formatted with `Locale.forLanguageTag("bn-BD")` so digits and month names are Bengali (e.g. "মঙ্গলবার, ২১ এপ্রিল").
- Update every minute (a `LaunchedEffect` ticker), and on `ACTION_TIME_CHANGED` / `ACTION_TIMEZONE_CHANGED` / `ACTION_DATE_CHANGED`.

### 4.3 Family cards
Data model:
```kotlin
@Serializable
data class FamilyMember(
    val id: String,          // UUID
    val name: String,        // e.g. "ছেলে" or "রহিম"
    val phone: String,       // e.g. "+8801712345678"
    val photoPath: String?,  // absolute path inside filesDir, or null
    val order: Int
)
```
- Show photo (circle-cropped, Coil) if present, else a tinted circle with the first letter of the name.
- **Tap a card → open default dialer with the number filled in:**
  `Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))`. No permission required. The elder presses the green call button in the dialer.
- **Optional setting "এক চাপে কল" (one-tap call), off by default:** if enabled, use `Intent.ACTION_CALL` and request runtime permission `CALL_PHONE` at the moment the setting is turned on (from Settings, by the family member — never surprise the elder with a permission popup). If the permission is denied or revoked, silently fall back to `ACTION_DIAL`.
- Guard: if no app can handle the intent (tablet without phone), show a large friendly message instead of crashing.
- Long-press on a card does nothing (avoid accidental edits).
- Content description for TalkBack: "Call <name>".

### 4.4 App tiles
- **YouTube:** `packageManager.getLaunchIntentForPackage("com.google.android.youtube")`. If null, open `https://www.youtube.com` with `ACTION_VIEW`. If even that fails, show message "YouTube ইনস্টল নেই".
- **Camera:** `Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)`.
- **Gallery:** `Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)`; fallback `Intent(Intent.ACTION_VIEW).setType("image/*")`; second fallback: launch package `com.google.android.apps.photos`.
- Declare **only** these in `<queries>` (package visibility), nothing broader:
  ```xml
  <queries>
      <package android:name="com.google.android.youtube" />
      <package android:name="com.google.android.apps.photos" />
      <intent><action android:name="android.media.action.STILL_IMAGE_CAMERA" /></intent>
      <intent><action android:name="android.intent.action.DIAL" /><data android:scheme="tel" /></intent>
      <intent><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.APP_GALLERY" /></intent>
      <intent><action android:name="android.intent.action.VIEW" /><data android:mimeType="image/*" /></intent>
      <intent><action android:name="android.intent.action.VIEW" /><data android:scheme="https" /></intent>
  </queries>
  ```
- Wrap every `startActivity` in try/catch for `ActivityNotFoundException` and show the friendly message.
- Debounce taps (ignore second tap within 800 ms) — elders often double-tap.

### 4.5 Torch
- `TorchController` uses `CameraManager`:
  - Find the first camera ID with `FLASH_INFO_AVAILABLE == true`.
  - `setTorchMode(id, enabled)`. **No CAMERA permission is needed for this.**
  - Register `CameraManager.TorchCallback` to keep state in sync (the system quick-settings torch or other apps can change it). Expose `StateFlow<TorchState>` (`On`, `Off`, `Unavailable`).
  - Catch `CameraAccessException` (camera busy) → show "এখন টর্চ চালু করা যাচ্ছে না".
- If the device has no flash (`PackageManager.FEATURE_CAMERA_FLASH` false), hide the tile.
- Tile shows a pill "চালু" (dark pill, yellow tile) or "বন্ধ" (grey pill, white tile).
- Torch tile toggles directly on tap. It must never open another screen.

### 4.6 Internet status bar (read-only + tap to fix)
**Important platform fact:** apps on Google Play cannot turn Wi-Fi or mobile data on/off (since Android 10, `WifiManager.setWifiEnabled` is ignored; mobile data requires system permission). So this bar **shows status only**; it never pretends to be a switch.

`InternetMonitor`:
- Permissions (normal, no popup): `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`.
- Use `ConnectivityManager.registerDefaultNetworkCallback` (and `registerNetworkCallback` with a Wi-Fi `NetworkRequest` to know if Wi-Fi is associated even when it isn't the default network).
- Read `NetworkCapabilities`:
  - `hasTransport(TRANSPORT_WIFI)`, `hasTransport(TRANSPORT_CELLULAR)`
  - `hasCapability(NET_CAPABILITY_VALIDATED)` = internet really works
  - `hasCapability(NET_CAPABILITY_CAPTIVE_PORTAL)` = treat as "Wi-Fi, no internet"
- Also read `WifiManager.isWifiEnabled` (and listen to `WifiManager.WIFI_STATE_CHANGED_ACTION`).
- Derive one sealed state, emitted as `StateFlow<InternetState>` with a 2-second debounce to avoid flicker:

| State | Condition | Icon | Title (bn) | Subtitle (bn) |
|---|---|---|---|---|
| `WifiOk` | default network is Wi-Fi and VALIDATED | wifi | ইন্টারনেট চালু আছে | ওয়াইফাই দিয়ে চলছে |
| `MobileOk` | default network is cellular and VALIDATED | signal bars | ইন্টারনেট চালু আছে | ওয়াইফাই বন্ধ · মোবাইল ডেটা চলছে (or "মোবাইল ডেটা চলছে" if Wi-Fi is on but not connected) |
| `WifiNoInternet` | Wi-Fi connected but not VALIDATED (or captive portal), and no validated cellular | warning | ওয়াইফাই আছে, নেট নেই | রাউটার বন্ধ করে আবার চালু করুন |
| `Offline` | no validated network at all (includes Wi-Fi off + data off, airplane mode) | wifi_off | ইন্টারনেট বন্ধ | ঠিক করতে এখানে চাপুন |

- **Tap behaviour:**
  - In `WifiOk` / `MobileOk`: do nothing except a short confirmation sound/vibration (optional TTS reads the status).
  - In `WifiNoInternet` / `Offline`: open the system Internet panel — API 29+: `Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)`; API 26–28: `Settings.ACTION_WIRELESS_SETTINGS`. This is a small bottom sheet with Wi-Fi and mobile-data switches; it returns to Home when closed.
- Each state must differ by **color + icon + words** (works for colour-blind and non-readers).
- Unregister callbacks in `onCleared()` / when the lifecycle stops; re-check on `ON_RESUME`.

### 4.7 Locked settings (for family only)
- Lock button on Home → **Picture Lock** screen:
  - 3×3 grid of 9 large, distinct pictures (use simple vector icons: cow, mango, house, sun, fish, flower, boat, tree, star).
  - Family sets a sequence of 3 pictures during onboarding. Store only a SHA-256 hash of the sequence (with a random salt) in DataStore.
  - Wrong sequence → shake animation, no message spam. After 5 wrong tries, wait 30 s.
  - Also allow an alternative: **long-press the lock button for 5 seconds** opens the picture lock (so a casual tap does nothing). Make the plain tap show a tiny tooltip "পরিবারের জন্য" and nothing else.
- **Settings screen** (normal-size Material UI is fine here; the family uses it):
  1. **Family members:** list, add, edit, delete, drag to reorder (max 6).
     - Name: text field.
     - Phone: text field **or** "Pick from contacts" button using `ActivityResultContracts.PickContact`-style flow with `Intent.ACTION_PICK` on `ContactsContract.CommonDataKinds.Phone.CONTENT_URI`. This grants temporary access to the one picked contact — **do NOT request `READ_CONTACTS`**.
     - Photo: `ActivityResultContracts.PickVisualMedia(ImageOnly)` (Android Photo Picker, no storage permission). Copy the image into `filesDir/photos/<id>.jpg`, downscale to max 512 px, delete old file on replace/delete.
  2. **One-tap call** switch (see §4.3).
  3. **Text size:** Normal / Large / Extra large (multipliers 1.0 / 1.15 / 1.3). At Extra large, family grid becomes 1 column if needed — no text may be cut off.
  4. **Show/hide tiles:** YouTube, Camera, Gallery, Torch, Internet bar.
  5. **Language:** বাংলা / English using `AppCompatDelegate.setApplicationLocales` (per-app language; add `locales_config.xml`).
  6. **Read buttons aloud** (TTS) on/off — see §4.9.
  7. **Change picture lock.**
  8. **Set as default home / switch back to normal home** (opens home role/settings).
  9. **Open phone settings** (`Settings.ACTION_SETTINGS`) for advanced tasks.
  10. **Privacy policy** link (opens browser) and app version.
- Leaving Settings (back or Home) returns to the locked Home; re-entry requires the lock again after 60 seconds.

### 4.8 Onboarding (first run, done by family member)
Screens, each with one big button:
1. Welcome — what the app does (Bengali + English).
2. Add family members (at least 1, can skip).
3. Set picture lock (choose 3 pictures, confirm once).
4. Make Ek Chhoan the home screen (§4.1).
5. Done → Home.

### 4.9 Optional voice readout
- `Speaker` wraps `TextToSpeech`. Prefer Bengali (`Locale("bn", "BD")` then `bn-IN`); if Bengali voice is missing, fall back to English and show a hint in Settings ("Install Bengali voice in Google Text-to-Speech").
- When enabled, every elder-facing tap speaks the label first ("ছেলেকে কল", "ক্যামেরা", "টর্চ চালু", internet status). Keep the action immediate — speak and act in parallel.

### 4.10 Feedback
- Every tap: short haptic (`HapticFeedbackType.LongPress` or `VibrationEffect.EFFECT_CLICK`) — no `VIBRATE` permission needed for Compose haptics; if using `Vibrator` directly, add `VIBRATE` (normal permission).
- Pressed state: tile scales to 0.97 and darkens slightly.

---

## 5. Google Play compliance rules (must follow)

**Allowed permissions — the manifest must contain ONLY these:**
- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `CALL_PHONE` (only used if the family enables one-tap call; requested at runtime from Settings)
- `VIBRATE` (only if `Vibrator` is used directly)

**Forbidden — do not add, even "for later":**
- `READ_CONTACTS`, `WRITE_CONTACTS`
- `READ_CALL_LOG`, `WRITE_CALL_LOG`, `PROCESS_OUTGOING_CALLS`, `READ_PHONE_STATE`
- `SEND_SMS`, `READ_SMS`, `RECEIVE_SMS`
- Any location permission
- `CAMERA` (not needed for torch)
- `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (Photo Picker needs none)
- `QUERY_ALL_PACKAGES`
- `CHANGE_WIFI_STATE`, `CHANGE_NETWORK_STATE`, `MODIFY_PHONE_STATE`
- `INTERNET`
- `SYSTEM_ALERT_WINDOW`, `BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN`
- `REQUEST_INSTALL_PACKAGES`, `FOREGROUND_SERVICE` (no background work needed)

**Other rules**
- No analytics, crash SDKs that send data, ads SDKs, or Firebase. (If you later add crash reporting, update the Data Safety form.)
- All data stays on the device (DataStore + `filesDir`). Set `android:allowBackup="true"` with `dataExtractionRules` that include DataStore and photos so a phone change keeps the setup.
- Data Safety form: "No data collected, no data shared."
- Provide a privacy policy URL (a simple hosted page stating no data is collected).
- App must not claim to be made by or affiliated with Google/YouTube; use the real YouTube icon only by loading it from the installed app at runtime, not by bundling the logo.
- Store listing: describe it as "simple home screen for elders", not as monitoring or control.

Add a unit test or lint check that fails the build if the merged manifest contains any forbidden permission (parse `build/intermediates/merged_manifests/.../AndroidManifest.xml` in a Gradle task, or use a manifest `tools:node="remove"` for any permission a library tries to add).

---

## 6. Accessibility requirements
- All elder-facing elements ≥ 72dp touch target, contrast ≥ 4.5:1 (the palette above passes).
- Every clickable has `contentDescription` / `semantics { role = Role.Button }`.
- Respect system font scale **and** in-app text size multiplier; test at system font scale 1.3 + Extra large — nothing clipped or overlapping.
- No time-limited interactions on Home, no swipe gestures required, no long-press required for elder actions.
- Don't rely on color alone (internet bar uses icon + text too).

---

## 7. Edge cases to handle
- No family members yet → show one big dashed card "পরিবারের সদস্য যোগ করুন" that opens the picture lock → settings.
- Phone number invalid/empty → card shows a small warning badge; tap opens nothing and speaks "নম্বর নেই".
- YouTube/Gallery/Camera app missing or disabled → friendly message, no crash.
- Device without flash → torch tile hidden.
- Tablet / no telephony (`FEATURE_TELEPHONY` false) → dial still tries `ACTION_DIAL`; show message if no handler.
- Airplane mode → internet bar `Offline`.
- Configuration changes, process death → state restored from DataStore/ViewModel.
- Very small screens (≤ 5") → Home becomes vertically scrollable rather than shrinking text.

---

## 8. Testing checklist
- Unit tests: `InternetState` derivation from fake `NetworkCapabilities` combinations; greeting by hour; picture-lock hashing; family list serialization.
- Compose UI tests: Home renders 1, 4 and 6 members; torch pill text toggles; internet bar shows correct texts for each state (inject fake `InternetMonitor`).
- Manual: test on Android 8, 10, 13, 14+ (emulator + one real low-end phone); with Bengali locale; with TalkBack on; with system font at max.

---

## 9. Build milestones (do them in order, run the app after each)
1. Project setup, theme (colors, fonts, dimens), static Home layout with hard-coded data matching §3.
2. Launcher intent filters, back handling, default-home onboarding step.
3. Family cards with DataStore + ACTION_DIAL.
4. App tiles (YouTube, Camera, Gallery) with fallbacks and `<queries>`.
5. Torch controller + tile.
6. Internet monitor + status bar + tap-to-panel.
7. Picture lock + Settings (family CRUD with Photo Picker and contact picker, text size, show/hide tiles, language, one-tap call).
8. Onboarding flow.
9. TTS readout, haptics, debouncing.
10. Compliance check task, tests, privacy policy text, release build with R8/minify enabled, signed AAB.

After each milestone, list the files changed and anything I need to do manually in Android Studio.
