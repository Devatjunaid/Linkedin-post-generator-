package com.superdevlabs.petfood.portioncalc.data

import com.superdevlabs.petfood.portioncalc.domain.FoodType
import com.superdevlabs.petfood.portioncalc.domain.Species
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Real photo + intro text from an English Wikipedia article (images are free-licensed, hosted on Wikimedia Commons). */
data class WikiSummary(val title: String, val imageUrl: String?, val extract: String?, val pageUrl: String?)

/**
 * Looks up Wikipedia's public page-summary API. Only text metadata is fetched here;
 * the photo itself is always loaded by Coil from [WikiSummary.imageUrl].
 */
class WikiRepository(private val client: OkHttpClient, private val cache: WikiCacheDao) {

    suspend fun summary(title: String): WikiSummary? = withContext(Dispatchers.IO) {
        val cached = cache.get(title)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_MS) {
            return@withContext cached.toSummary()
        }
        val fresh = runCatching { fetch(title) }.getOrNull()
        if (fresh != null) {
            cache.upsert(WikiCacheEntity(title, fresh.imageUrl, fresh.extract, fresh.pageUrl, System.currentTimeMillis()))
            fresh
        } else {
            cached?.toSummary()
        }
    }

    private fun fetch(title: String): WikiSummary? {
        val path = URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20")
        val request = Request.Builder()
            .url("https://en.wikipedia.org/api/rest_v1/page/summary/$path")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = JSONObject(response.body.string())
            if (json.optString("type") == "disambiguation") return null
            return WikiSummary(
                title = title,
                imageUrl = pickImage(json),
                extract = json.optString("extract").takeIf { it.isNotBlank() },
                pageUrl = json.optJSONObject("content_urls")?.optJSONObject("mobile")?.optString("page")
                    ?.takeIf { it.isNotBlank() },
            )
        }
    }

    /** A 640 px rendition is sharp on phones without downloading the full original. */
    private fun pickImage(json: JSONObject): String? {
        val original = json.optJSONObject("originalimage")
        val thumb = json.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
        val originalWidth = original?.optInt("width") ?: 0
        if (thumb != null && originalWidth > 640) {
            return thumb.replace(Regex("/\\d+px-"), "/640px-")
        }
        return original?.optString("source")?.takeIf { it.isNotBlank() } ?: thumb
    }

    private fun WikiCacheEntity.toSummary() = WikiSummary(title, imageUrl, extract, pageUrl)

    companion object {
        private val CACHE_MS = TimeUnit.DAYS.toMillis(30)

        /** Article used for the real food photo of each food type. */
        fun foodTitle(type: FoodType, species: Species?): String = when (type) {
            FoodType.DRY -> if (species == Species.CAT) "Cat food" else "Dog food"
            FoodType.WET -> "Pet food"
            FoodType.FRESH -> "Chicken as food"
            FoodType.RAW -> "Raw feeding"
        }
    }
}
