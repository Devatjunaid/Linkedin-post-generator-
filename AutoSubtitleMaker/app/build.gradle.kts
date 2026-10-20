import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// AdMob ids. Debug builds always use Google's public test units so you never click your
// own live ads (that gets AdMob accounts suspended). Release builds use the real units.
val admobAppId = "ca-app-pub-1948347222012540~6939661409"
val releaseBannerId = "ca-app-pub-1948347222012540/3171061920"
val releaseInterstitialId = "ca-app-pub-1948347222012540/7430615996"
val releaseNativeId = "ca-app-pub-1948347222012540/6117534323"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testNativeId = "ca-app-pub-3940256099942544/2247696110"

android {
    namespace = "com.superdevlabs.autosubtitles"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.superdevlabs.autosubtitles"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = admobAppId

        // Real phones (arm) + x86_64 emulators. Drops JNA's obsolete mips/armeabi binaries.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BANNER_AD_UNIT", "\"$testBannerId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", "\"$testInterstitialId\"")
            buildConfigField("String", "NATIVE_AD_UNIT", "\"$testNativeId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "BANNER_AD_UNIT", "\"$releaseBannerId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", "\"$releaseInterstitialId\"")
            buildConfigField("String", "NATIVE_AD_UNIT", "\"$releaseNativeId\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Keep .so files uncompressed and page-aligned (Play's 16 KB page size requirement).
            useLegacyPackaging = false
        }
    }

    lint {
        // Media3 Transformer/effect APIs are @UnstableApi; every use site is annotated with
        // @OptIn(UnstableApi::class). Keep this as a warning so a missed annotation never blocks
        // a release build.
        warning += "UnsafeOptInUsageError"
        // JNA (used by the Vosk speech engine) references desktop java.awt classes it never
        // loads on Android.
        disable += "InvalidPackage"
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        optIn.addAll(
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Offline speech recognition (Apache 2.0). No API key, no network calls, no credits.
    implementation(libs.vosk.android)
    implementation("net.java.dev.jna:jna:${libs.versions.jna.get()}@aar")

    testImplementation(libs.junit)
}
