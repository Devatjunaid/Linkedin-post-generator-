import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 compiles Kotlin itself (built-in Kotlin), so org.jetbrains.kotlin.android is not applied.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Real AdMob IDs are only used in release builds. Debug builds use Google's public
// test units so that testing on your own device never produces invalid traffic.
val admobAppId = "ca-app-pub-1948347222012540~8531199818"
val releaseBannerId = "ca-app-pub-1948347222012540/7131233092"
val releaseInterstitialId = "ca-app-pub-1948347222012540/4505069751"
val releaseNativeId = "ca-app-pub-1948347222012540/6842538616"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testNativeId = "ca-app-pub-3940256099942544/2247696110"

fun quoted(value: String) = "\"$value\""

android {
    namespace = "com.sunarc.inverterlogger"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sunarc.inverterlogger"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = admobAppId
    }

    buildTypes {
        debug {
            buildConfigField("String", "BANNER_AD_UNIT", quoted(testBannerId))
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", quoted(testInterstitialId))
            buildConfigField("String", "NATIVE_AD_UNIT", quoted(testNativeId))
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BANNER_AD_UNIT", quoted(releaseBannerId))
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", quoted(releaseInterstitialId))
            buildConfigField("String", "NATIVE_AD_UNIT", quoted(releaseNativeId))
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
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

ksp {
    arg("room.generateKotlin", "true")
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
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.play.services.ads)
    implementation(libs.google.ump)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
}
