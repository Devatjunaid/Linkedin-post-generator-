plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Google's official AdMob test IDs are used for debug builds so you never click your own live ads
// (that is invalid traffic and can get the AdMob account suspended). Release builds use your IDs.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testNativeId = "ca-app-pub-3940256099942544/2247696110"

val liveAdmobAppId = "ca-app-pub-1948347222012540~4764943685"
val liveBannerId = "ca-app-pub-1948347222012540/3080986060"
val liveInterstitialId = "ca-app-pub-1948347222012540/4231336075"
val liveNativeId = "ca-app-pub-1948347222012540/9825698675"

android {
    namespace = "com.inkformlabs.pdfsigner"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.inkformlabs.pdfsigner"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "AD_UNIT_BANNER", "\"$testBannerId\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$testInterstitialId\"")
            buildConfigField("String", "AD_UNIT_NATIVE", "\"$testNativeId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["admobAppId"] = liveAdmobAppId
            buildConfigField("String", "AD_UNIT_BANNER", "\"$liveBannerId\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$liveInterstitialId\"")
            buildConfigField("String", "AD_UNIT_NATIVE", "\"$liveNativeId\"")
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
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.exifinterface)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.pdfbox.android)
}
