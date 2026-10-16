plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Google's official AdMob test IDs. Never click real ads during development.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testNativeId = "ca-app-pub-3940256099942544/2247696110"

fun prop(name: String, fallback: String): String =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "com.glowdash.warningidentifier"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.glowdash.warningidentifier"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"

        manifestPlaceholders["admobAppId"] = testAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"$testNativeId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID", testAppId)
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${prop("ADMOB_BANNER_ID", testBannerId)}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${prop("ADMOB_INTERSTITIAL_ID", testInterstitialId)}\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"${prop("ADMOB_NATIVE_ID", testNativeId)}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            // Keep native libraries uncompressed so they are 16 KB-aligned in the APK/AAB.
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // CameraX returns ListenableFuture; this puts the class on the compile classpath.
    implementation(libs.guava)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    // AdMob pulls in androidx.fragment 1.1.0 via Play services; Play Console flags it as outdated.
    implementation(libs.androidx.fragment)

}
