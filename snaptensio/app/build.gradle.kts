plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Google's official AdMob test IDs. Safe to ship in debug builds only.
private val testAppId = "ca-app-pub-3940256099942544~3347511713"
private val testBanner = "ca-app-pub-3940256099942544/9214589741"
private val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
private val testNative = "ca-app-pub-3940256099942544/2247696110"

private fun prop(name: String, fallback: String): String =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: fallback

android {
    namespace = "com.snaptensio.bplog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.snaptensio.bplog"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "AD_BANNER_ID", "\"$testBanner\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$testInterstitial\"")
            buildConfigField("String", "AD_NATIVE_ID", "\"$testNative\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = prop("SNAPTENSIO_ADMOB_APP_ID", testAppId)
            buildConfigField("String", "AD_BANNER_ID", "\"${prop("SNAPTENSIO_ADMOB_BANNER_ID", testBanner)}\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"${prop("SNAPTENSIO_ADMOB_INTERSTITIAL_ID", testInterstitial)}\"")
            buildConfigField("String", "AD_NATIVE_ID", "\"${prop("SNAPTENSIO_ADMOB_NATIVE_ID", testNative)}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)
    // CameraX returns Guava ListenableFutures from its public API.
    implementation(libs.guava)

    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(libs.junit)
}
