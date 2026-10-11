# Play Console: Data safety form answers

Fill in **Policy → App content → Data safety** using these answers. They match the app's code
(no accounts, local-only storage, AdMob child-directed, AD_ID permission removed) and Google's
published disclosure for the Mobile Ads SDK. Google updates that page with new SDK versions, so
check it before submitting:
[Google Play data disclosure for the Google Mobile Ads SDK](https://developers.google.com/admob/android/privacy/play-data-disclosure).

## Data collection and security

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** (through the Google Mobile Ads SDK) |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **Yes**: in-app (delete artworks) and by clearing app data / uninstalling. The developer stores nothing. |

## Data types

Declare these as **collected** by the Google Mobile Ads SDK. For each one, set "Is this data processed ephemerally?" to **No**, and "Is collection required?" to **Required** (ads can't be turned off).

| Category → Data type | Purposes |
| --- | --- |
| Location → **Approximate location** (derived from IP address) | Advertising or marketing; Analytics; Fraud prevention, security, and compliance |
| App activity → **App interactions** | Advertising or marketing; Analytics; Fraud prevention, security, and compliance |
| App info and performance → **Crash logs** | Analytics; Fraud prevention, security, and compliance |
| App info and performance → **Diagnostics** | Analytics; Fraud prevention, security, and compliance |
| Device or other IDs → **Device or other IDs** (app set ID) | Advertising or marketing; Analytics; Fraud prevention, security, and compliance |

**Shared:** Google's disclosure page says whether each type counts as "shared" for your SDK version.
Data sent to a service provider that processes it on your behalf usually isn't "sharing" under Play's
definitions. Follow the table on Google's page.

**Not collected by the app:** name, email, phone, contacts, photos/videos (artworks never leave the device),
files, precise location, messages, audio, calendar, health, financial info, web history.

## Families requirements already handled in the code

- Ad requests tagged child-directed and under age of consent, max ad content rating G (`ads/Ads.kt`).
- `com.google.android.gms.permission.AD_ID` removed in `AndroidManifest.xml`.
- AdMob is a Families Self-Certified Ads SDK.
- In your AdMob account, also set the app's child-directed treatment / COPPA setting if AdMob offers it for your account (under the app's settings or Blocking controls).
