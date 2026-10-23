package com.superdevlabs.petfood.portioncalc

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.superdevlabs.petfood.portioncalc.ads.AdsManager
import com.superdevlabs.petfood.portioncalc.data.AppDatabase
import com.superdevlabs.petfood.portioncalc.data.PetRepository
import com.superdevlabs.petfood.portioncalc.data.SettingsRepository
import com.superdevlabs.petfood.portioncalc.data.WikiRepository
import com.superdevlabs.petfood.portioncalc.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Simple manual dependency container shared by the whole app. */
class AppContainer(app: Application) {
    /** Wikimedia asks API clients to identify themselves with a descriptive User-Agent. */
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "PetFoodCalculator/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})")
                    .build(),
            )
        }
        .build()

    val database = AppDatabase.create(app)
    val pets = PetRepository(database)
    val wiki = WikiRepository(httpClient, database.wikiCacheDao())
    val settings = SettingsRepository(app)
    val ads = AdsManager(app)
    val reminders = ReminderScheduler(app)
}

class PetFoodApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.reminders.ensureChannel()
        appScope.launch {
            container.pets.seedPresetsIfNeeded()
            container.pets.pruneFeedingLog()
        }
    }

    /** Coil handles every network photo (download, downsampling, memory + disk cache). */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.httpClient })) }
            .crossfade(true)
            .build()
}
