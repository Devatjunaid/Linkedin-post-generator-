# Gigometer — what to declare in Google Play Console

**Short answer: yes, the app collects data — but only through Google AdMob.**
Gigometer itself has no server and no account. Trips, GPS routes, addresses and
settings are stored only on the phone and are never uploaded by the app. The
**Google Mobile Ads SDK** (AdMob) *does* send data to Google, so Play counts
that as data your app collects and shares.

---

## 1. Data safety form

### Overview questions

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (AdMob uses HTTPS) |
| Do you provide a way for users to request that their data is deleted? | **No** — the app keeps no server-side data. (Users delete on-device data by deleting trips or uninstalling; ad data is tied to the resettable Android advertising ID.) |

### Data types to tick

All of the rows below come from the AdMob SDK, following Google's own AdMob
data-disclosure guidance. For each: **Collected = Yes, Shared = Yes,
Processed ephemerally = No, Required (users can't turn it off)**.

| Category → Data type | Purposes to tick |
|---|---|
| **Location → Approximate location** (from IP address) | Advertising or marketing · Analytics · Fraud prevention, security, and compliance |
| **App activity → App interactions** (ad views/taps, app launches) | Advertising or marketing · Analytics · Fraud prevention, security, and compliance |
| **App info and performance → Crash logs** | Analytics · Fraud prevention, security, and compliance |
| **App info and performance → Diagnostics** | Analytics · Fraud prevention, security, and compliance |
| **App info and performance → Other app performance data** | Analytics · Advertising or marketing |
| **Device or other IDs → Device or other IDs** (advertising ID, app set ID) | Advertising or marketing · Analytics · Fraud prevention, security, and compliance |

### Precise location: the one judgement call

GPS location is used **on the phone** to measure miles. The app never uploads
it, so on its own it is *not* "collected" in Play's sense.

The exception is **street addresses**. To show "123 Main St → Oak Ave", the app
calls Android's built-in `Geocoder`, which sends the trip's start and end
coordinates to Google's geocoding service and gets back an address. The
conservative, safest choice is to declare that:

| Category → Data type | Collected | Shared | Ephemeral | Required? | Purpose |
|---|---|---|---|---|---|
| **Location → Precise location** | Yes | No | **Yes** | **Optional** (Settings › Street addresses can be turned off) | App functionality |

If you'd rather not declare precise location at all, turn **Street addresses**
off by default (`lookupAddresses = false` in `AppSettings`) or remove the
feature; then leave Precise location unticked.

### Do NOT tick

Personal info (name, email, phone…), Financial info, Health and fitness,
Messages, Photos/videos, Audio, Files and docs, Calendar, Contacts, Web
browsing, Installed apps. The optional "Name on reports" setting is stored only
on the phone and printed into the PDF the user exports themselves. It is never
sent anywhere by the app.

---

## 2. Other Play Console declarations

| Section | What to answer |
|---|---|
| **Ads** | **Yes, my app contains ads** |
| **Advertising ID** | **Yes** — used for *Advertising or marketing* and *Analytics* (play-services-ads adds the `AD_ID` permission automatically) |
| **Location permissions** (background location) | Required, because the app requests `ACCESS_BACKGROUND_LOCATION`. Feature to describe: *"Automatically records business driving trips for IRS mileage deduction logs while the user drives, even when the app is closed."* Mileage tracking is an accepted use case. You must upload a **short video (YouTube link)** showing (1) the in-app disclosure dialog ("Location use") that appears before the system prompt, and (2) a trip being recorded automatically with the notification visible. |
| **Foreground service** (Android 14+) | Declare type **Location**. Use case: *"Records the GPS route and distance of a driving trip the user started (or that was auto-detected) while showing an ongoing notification with a Stop button."* Upload a video of a trip recording with the notification. |
| **Target audience and content** | **18 and over** (gig drivers). Not designed for children. |
| **Content rating** | Fill in the questionnaire. No violence, gambling or user-generated content → typically rated *Everyone*. |
| **Financial features** | *"My app doesn't provide any financial features."* It's a mileage log, not a bank, loan or tax-filing service. |
| **Health apps** | Not a health app. |
| **App access** | *All functionality is available without special access* (no login). |
| **News / Government / COVID** | No. |
| **Privacy policy URL** | Required. Host `docs/privacy-policy.html` (see below) and paste its URL. Also put that URL in `app/src/main/res/values/strings.xml` → `privacy_policy_url` so the in-app link works. |

---

## 3. AdMob account steps (before release)

1. Create the app in AdMob (Android, "not yet published" is fine) and create two
   ad units: **Banner** and **Native advanced**.
2. Put the IDs in `gigometer/gradle.properties`:
   ```
   ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
   ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
   ADMOB_NATIVE_ID=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
   ```
   Debug builds always use Google's test ads, so you can't accidentally click
   your own live ads while testing.
3. In AdMob › **Privacy & messaging**, create a **GDPR** message and a
   **US state regulations** message. The app already shows them through
   Google's User Messaging Platform when they're required, and adds
   "Ad privacy choices" to Settings when needed.
4. After publishing, add the Play Store link to the app in AdMob and set up
   **app-ads.txt** on your developer website.
