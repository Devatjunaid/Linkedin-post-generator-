# Publishing Studio – Blogger Theme

A one-page website for a Google Play developer / publishing studio, built for **Blogger.com**.
It works on desktop and phones.

## Install on Blogger

1. Open **blogger.com** and pick your blog. Go to **Theme**.
2. Click the **▼ arrow next to "Customize"** and choose **Backup** to save your current theme.
3. Click **▼ → Edit HTML**. Select all the code (Ctrl+A) and delete it.
4. Paste the full contents of `studio-theme.xml` and click **Save**.
5. If Blogger asks to remove old widgets, click **Delete widgets**.

## Personalize it (search the code for `EDIT:`)

| What | Replace |
|---|---|
| Studio name | `Nova Pixel Studio` (header, footer, hero text) and the `N` / `NPS` letters |
| Developer page | `https://play.google.com/store/apps/dev?id=YOUR_DEVELOPER_ID` |
| App links | `com.yourstudio.app1`, `app2`, `app3` → your real package names |
| App names, ratings and descriptions | Inside each `<article class='app-card'>` block |
| App icon | Replace the emoji with `<img src='ICON_URL' alt='App name'/>` |
| Email | `support@yourstudio.com` (use the same email as in Play Console) |
| Location and stats | The contact section and the stats strip |

Copy an `<article class='app-card'>` block to add another app.

## Privacy Policy (needed by Google Play)

In Blogger, go to **Pages → New page**. Give it the title **Privacy Policy** and publish it.
Blogger gives it this address: `https://YOURBLOG.blogspot.com/p/privacy-policy.html`.
The footer already links there. Paste that URL into **Play Console → App content → Privacy policy**.
Do the same for a page titled **Terms and Conditions** if you want one.

The page uses the same styling as the rest of the site.

### Starter privacy policy text (edit before publishing)

> **Privacy Policy**
> Last updated: [DATE]
>
> [STUDIO NAME] ("we", "us") builds the mobile apps listed on this website. This policy explains how we handle information when you use our apps.
>
> **Information we collect.** Our apps do not ask for personal information such as your name or address. Some apps may use third-party services (for example Google Play Services, Google AdMob or Firebase Analytics). These services may collect device identifiers and usage data as described in their own privacy policies.
>
> **How we use information.** We use this information to provide and improve our apps, fix crashes and show relevant ads where applicable.
>
> **Children.** Our apps are not directed at children under 13. We do not knowingly collect their personal information.
>
> **Contact.** If you have questions, email us at [support@yourstudio.com].
