# Play Console: Data safety answers for Ek Chhoan

Where: Play Console > Ek Chhoan > Policy and programs > App content > Data safety.
Applies to versionCode 1 (1.0.0) and later, as long as the dependency set stays the same (no network, analytics, crash or ads SDK).

## Why the answer is "no data collected"

- Google defines **collection** as sending data off the user's device. Data that is only accessed and processed on the device does not need to be disclosed.
- The app has no `INTERNET` permission, so it cannot send anything off the device. The `checkForbiddenPermissions<Variant>` Gradle task fails the build if any library adds `INTERNET`.
- Family names, phone numbers, photos and settings stay in the app's private storage (DataStore plus `files/photos/`).
- Android Auto Backup and device transfer are run by the OS into the **user's own** Google account. The developer cannot read that data, so it is not developer collection.
- Opening the dialer, YouTube, Camera or Gallery happens only when the user taps a button for that action. Google exempts this kind of user-initiated, expected on-device transfer from "sharing". The intents also carry no personal data, except the phone number the user chose to call.
- Source: https://support.google.com/googleplay/android-developer/answer/10787469

## Questionnaire

### Step 1: Overview
Read the overview, then click Next.

### Step 2: Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |

Choosing **No** skips the encryption-in-transit and data-deletion questions. If the Console still shows them:

| Question | Answer |
|---|---|
| Is all of the user data collected by your app encrypted in transit? | Not applicable (no data collected). Choose **Yes** only if the Console forces an answer. |
| Do you provide a way for users to request that their data is deleted? | Optional. If asked: users delete everything on the device by removing family members, clearing app storage or uninstalling. There is no server-side data. |

### Step 3: Data types
Skipped because the answer above is "No". If it is shown, leave **every** category unchecked: Location, Personal info, Financial info, Health and fitness, Messages, Photos and videos, Audio files, Files and docs, Calendar, Contacts, App activity, Web browsing, App info and performance, Device or other IDs.

> Contacts, Photos and **App info (installed apps)** are not ticked even though the favourite-apps picker reads the launcher app list and the user can pick one contact's number and photos. All of these are processed **only on the device** and never leave it, so they are not "collected" (Google's on-device processing rule).

### Step 4: Preview
The store listing should show:
- **No data shared with third parties**
- **No data collected**

Click **Submit**.

## Related App content declarations

| Section | Answer |
|---|---|
| Privacy policy | https://www.pntapps.com/privacy (also linked inside the app: Settings > About > Privacy policy). |
| Ads | **No, my app does not contain ads** |
| Advertising ID | **No**. The app does not use the advertising ID. The manifest strips `com.google.android.gms.permission.AD_ID` with `tools:node="remove"`. |
| App access | **All functionality is available without any access restrictions.** There is no login. Settings are behind a 3-picture lock that the user creates during first-run onboarding. Optional note for reviewers: "On first run, choose any 3 pictures as the lock. Long-press the lock icon on Home for 5 s, or tap it, to open the picture lock." |
| Content rating | See release-checklist.md |
| Target audience | 18 and over only (see release-checklist.md) |
| News app | No |
| Health apps | No health features |
| Financial features | None |
| Government app | No |
| Data safety | As above |

## When this form must change
Update Data safety **before** releasing a build that does any of these:
- Adds any SDK that sends data (crash reporting, analytics, Firebase, ads, remote config).
- Adds the `INTERNET` permission. The Gradle guard would also need an intentional allow-list change.
- Uploads photos or contacts anywhere, or adds an account or sync feature.
