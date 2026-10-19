plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Google's official sample ad units. Debug builds always use these so that testing on your own
// device never produces invalid traffic on your real AdMob account (an AdMob policy requirement).
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testNativeId = "ca-app-pub-3940256099942544/2247696110"

val liveAppId = "ca-app-pub-1948347222012540~4526230990"
val liveBannerId = "ca-app-pub-1948347222012540/3460561336"
val liveInterstitialId = "ca-app-pub-1948347222012540/2147479666"
val liveNativeId = "ca-app-pub-1948347222012540/5567849929"

android {
    namespace = "com.devatjunaid.isthisai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.devatjunaid.isthisai"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "BANNER_AD_UNIT", "\"$testBannerId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", "\"$testInterstitialId\"")
            buildConfigField("String", "NATIVE_AD_UNIT", "\"$testNativeId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = liveAppId
            buildConfigField("String", "BANNER_AD_UNIT", "\"$liveBannerId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", "\"$liveInterstitialId\"")
            buildConfigField("String", "NATIVE_AD_UNIT", "\"$liveNativeId\"")
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
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.animation.ExperimentalAnimationApi",
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(libs.junit)
}
