package com.devatjunaid.renewalert.domain

import com.devatjunaid.renewalert.data.CycleUnit

/** A quick-add starting point. Prices vary by country and plan, so the user types the price. */
data class Template(val name: String, val category: String, val cycle: CycleUnit = CycleUnit.MONTH)

object Catalog {
    val categories = listOf(
        "Video", "Music", "Productivity", "Cloud", "Education", "Health",
        "News", "Gaming", "Shopping", "Finance", "Utilities", "Other",
    )

    val currencies = listOf(
        "USD", "EUR", "GBP", "INR", "PKR", "AED", "SAR", "CAD", "AUD", "NZD", "JPY", "CNY",
        "CHF", "SEK", "NOK", "DKK", "PLN", "TRY", "BRL", "MXN", "ZAR", "NGN", "EGP", "BDT",
        "IDR", "MYR", "PHP", "SGD", "THB", "VND", "KRW", "HKD", "QAR", "KWD",
    )

    val templates: List<Template> = listOf(
        Template("Netflix", "Video"), Template("YouTube Premium", "Video"), Template("Disney+", "Video"),
        Template("Prime Video", "Video"), Template("Max", "Video"), Template("Hulu", "Video"),
        Template("Apple TV+", "Video"), Template("Paramount+", "Video"), Template("Peacock", "Video"),
        Template("Crunchyroll", "Video"), Template("Shahid", "Video"), Template("JioHotstar", "Video"),
        Template("Spotify", "Music"), Template("Apple Music", "Music"), Template("YouTube Music", "Music"),
        Template("Amazon Music", "Music"), Template("Deezer", "Music"), Template("Tidal", "Music"),
        Template("Audible", "Music"), Template("SoundCloud Go", "Music"),
        Template("Microsoft 365", "Productivity", CycleUnit.YEAR), Template("Google One", "Cloud"),
        Template("iCloud+", "Cloud"), Template("Dropbox", "Cloud"), Template("OneDrive", "Cloud"),
        Template("Notion", "Productivity"), Template("Adobe Creative Cloud", "Productivity"),
        Template("Canva Pro", "Productivity"), Template("Grammarly", "Productivity"),
        Template("ChatGPT Plus", "Productivity"), Template("Claude Pro", "Productivity"),
        Template("GitHub Copilot", "Productivity"), Template("1Password", "Productivity", CycleUnit.YEAR),
        Template("NordVPN", "Utilities", CycleUnit.YEAR), Template("ExpressVPN", "Utilities"),
        Template("Proton", "Utilities"), Template("LinkedIn Premium", "Productivity"),
        Template("Duolingo Super", "Education", CycleUnit.YEAR), Template("Coursera Plus", "Education"),
        Template("Skillshare", "Education", CycleUnit.YEAR), Template("Masterclass", "Education", CycleUnit.YEAR),
        Template("Headspace", "Health"), Template("Calm", "Health", CycleUnit.YEAR), Template("Strava", "Health"),
        Template("Gym membership", "Health"), Template("Peloton", "Health"), Template("Fitbit Premium", "Health"),
        Template("The New York Times", "News"), Template("The Economist", "News"), Template("Medium", "News"),
        Template("Substack", "News"), Template("Washington Post", "News"),
        Template("Xbox Game Pass", "Gaming"), Template("PlayStation Plus", "Gaming"),
        Template("Nintendo Switch Online", "Gaming", CycleUnit.YEAR), Template("Apple Arcade", "Gaming"),
        Template("Amazon Prime", "Shopping", CycleUnit.YEAR), Template("Walmart+", "Shopping"),
        Template("Costco", "Shopping", CycleUnit.YEAR), Template("Uber One", "Shopping"),
        Template("DoorDash DashPass", "Shopping"), Template("Mobile plan", "Utilities"),
        Template("Internet", "Utilities"), Template("Electricity", "Utilities"),
        Template("Car insurance", "Finance"), Template("Rent", "Finance"), Template("Domain name", "Utilities", CycleUnit.YEAR),
    )

    fun search(query: String): List<Template> {
        val q = query.trim()
        if (q.isEmpty()) return templates
        return templates.filter { it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true) }
    }
}
