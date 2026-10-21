# Permissions and sensitive behaviour: justification text

## Do we need a Play Console Permissions Declaration Form?

**No.** As of 21 October 2026, Google Play requires a declaration form only for:
- SMS and Call Log permissions (READ_CALL_LOG, WRITE_CALL_LOG, PROCESS_OUTGOING_CALLS, SMS group)
- Background location
- MANAGE_EXTERNAL_STORAGE
- QUERY_ALL_PACKAGES
- READ_MEDIA_IMAGES and READ_MEDIA_VIDEO
- REQUEST_INSTALL_PACKAGES
- USE_EXACT_ALARM
- USE_FULL_SCREEN_INTENT
- AccessibilityService (non-tool)
- Health Connect
- Foreground-service types

**CALL_PHONE is none of these.** It is a normal "dangerous" runtime permission in the Phone group. It needs no form, but its use must be clear to users and match the store listing and privacy policy.
Sources: https://support.google.com/googleplay/android-developer/answer/9888170 and https://support.google.com/googleplay/android-developer/answer/16558241

The `checkForbiddenPermissions<Variant>` Gradle task guarantees that the merged manifest contains only these permissions:
- ACCESS_NETWORK_STATE
- ACCESS_WIFI_STATE
- CALL_PHONE
- VIBRATE
- androidx.core's own signature-level `com.pntapps.ekchhoan.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

None of these is a restricted permission.

Use the texts below wherever a reviewer asks: the "App access" note, an appeal, a policy email, or a future form.

---

## CALL_PHONE

**Short (≤ 500 chars, for any Console text box)**

> Ek Chhoan is a simple home screen for elders who cannot read or type well. Family members appear as large photo cards. If the family turns on "One-tap call" in the locked settings, tapping a card calls that person directly (ACTION_CALL), because the extra dialer step confuses many elders. The permission is requested only when that switch is turned on; if denied or off, the app uses ACTION_DIAL. It never reads call logs or phone state and never calls without a tap.

**Long version**

- **Core feature:** The app's main job is to let an elderly person call a family member with one touch on a large photo.
- **When it is requested:** Never at install or first launch. A family member opens the locked settings and turns on **One-tap call**. Only then does the app show the system CALL_PHONE prompt. If the prompt is denied, the switch stays off.
- **What it does:** Each tap on a family card starts `Intent.ACTION_CALL` with that card's `tel:` number, which is saved on the device. Calls happen only after an explicit tap. There are no automatic, scheduled or background calls, and the app never calls numbers it was not given.
- **Fallback:** With the switch off, or the permission denied, a tap starts `Intent.ACTION_DIAL`, which needs no permission. The dialer opens with the number pre-filled.
- **What it does not do:** It does not request READ_CALL_LOG, READ_PHONE_STATE, PROCESS_OUTGOING_CALLS, ANSWER_PHONE_CALLS or any SMS permission. It does not ask to become the default dialer, so ROLE_DIALER is not used.
- **Disclosure:** The setting's caption explains it in Bengali and English. The privacy policy has a CALL_PHONE section.

---

## Default home screen (launcher) behaviour

**Text for reviewers / App access notes**

> Ek Chhoan is a home screen (launcher) app. MainActivity declares the standard MAIN + HOME + DEFAULT intent filter. During first-run onboarding the user is asked to make it the default home app: on Android 10+ through the system RoleManager ROLE_HOME dialog, and on Android 8–9 through the system "Home app" settings screen. The user can refuse. The app never changes the default silently and does not use Device Admin, Device Owner, lock task/kiosk mode, AccessibilityService, overlays (SYSTEM_ALERT_WINDOW) or QUERY_ALL_PACKAGES. The Back button is ignored only on the app's own home page, which is normal for launchers. Home, Recents and the notification shade keep working. Settings has "Switch back to normal home screen", which opens the system home-app chooser so the user can leave at any time. The app is also listed in the app drawer (MAIN + LAUNCHER) for setup.

Notes:
- Launchers have no extra Play declaration. They must follow Device and Network Abuse rules: no interfering with other apps or system functions, and the user must stay able to switch away. They must also follow the Deceptive Behavior and Impersonation rules.
- Package visibility uses specific `<queries>` entries (YouTube, Google Photos, camera, dialer, gallery, image viewer, https, TTS service), not QUERY_ALL_PACKAGES.
- The YouTube tile uses the icon loaded at runtime from the installed YouTube app. The YouTube logo is not bundled, and the listing states there is no affiliation. This keeps clear of Impersonation and intellectual property issues.

---

## ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE (normal permissions, no prompt)

> Used only to show the internet status bar on the home screen: Wi-Fi, Wi-Fi without internet, mobile data or offline. They come from ConnectivityManager and NetworkCapabilities. The app has no INTERNET permission, does not scan for Wi-Fi networks, does not read SSIDs or BSSIDs, and uses no location.

## VIBRATE (normal, only if `Vibrator` is used directly)

> Short haptic click on each tap, for elders who need tactile confirmation.
