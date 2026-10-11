# Play Console answers for Dot Dash!

These match the privacy policy and the app as built: child-directed AdMob, no advertising ID, progress stored only on the device.
They're based on Google's published guidance for the Google Mobile Ads SDK. Re-check Google's current guidance when you fill in the form.

## Data safety

**Does your app collect or share any of the required user data types?** Yes. Only the Google Mobile Ads SDK collects data; the game itself sends nothing.

**Is all of the user data collected by your app encrypted in transit?** Yes.

**Do you provide a way for users to request that their data is deleted?** No. The app keeps no user data on a server. Game progress is stored only on the device.

| Data type | Collected | Shared | Ephemeral? | Required or optional | Purposes |
| --- | --- | --- | --- | --- | --- |
| Location → Approximate location (derived from IP address) | Yes | Yes (Google) | No | Required | Advertising, Fraud prevention |
| App activity → App interactions (ad views and taps) | Yes | Yes (Google) | No | Required | Advertising, Analytics |
| App info and performance → Crash logs, Diagnostics | Yes | Yes (Google) | No | Required | Analytics, Fraud prevention |
| Device or other IDs (app set ID, used by the ads SDK) | Yes | Yes (Google) | No | Required | Advertising, Fraud prevention |

Not collected: name, email, contacts, photos, audio, files, precise location, financial info, health, messages, web history, the advertising ID.

## Other Play Console declarations

- **Target audience:** Ages 5 and under, 6–8.
- **Contains ads:** Yes.
- **Ads SDK:** Google AdMob (self-certified for Families). Child-directed treatment is on in the code (`AdsManager.kt`).
- **Advertising ID:** No. The `AD_ID` permission is removed in `AndroidManifest.xml`.
- **App access:** All functionality is available without special access.
- **Content rating questionnaire:** No violence, no user interaction, no shared location, no purchases.
- **Privacy policy URL:** the public address where you host `privacy-policy/index.html` (see below).

## Hosting the policy for free (GitHub Pages)

1. Put `index.html` in a public GitHub repository, for example `dotdash-privacy`.
2. Go to **Settings › Pages**, choose **Deploy from a branch**, branch `main`, folder `/ (root)`, and save.
3. After a minute the policy is live at `https://<your-username>.github.io/dotdash-privacy/`.
4. Paste that URL into **Play Console › App content › Privacy policy**, and into the store listing.
