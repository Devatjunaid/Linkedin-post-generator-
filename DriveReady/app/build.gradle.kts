plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// AdMob identifiers. Release builds use the production units; debug builds use
// Google's public test units so you never click your own live ads while developing.
val admobAppId = "ca-app-pub-1948347222012540~1473202990"
val prodBannerId = "ca-app-pub-1948347222012540/5525670638"
val prodInterstitialId = "ca-app-pub-1948347222012540/1586425623"
val prodNativeId = "ca-app-pub-1948347222012540/7867306670"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testNativeId = "ca-app-pub-3940256099942544/2247696110"

// >>> PUT YOUR PRIVACY POLICY URL HERE (same URL you enter in Play Console → App content → Privacy policy).
val privacyPolicyUrl = "https://YOUR-DOMAIN.com/driveready/privacy-policy"

android {
    namespace = "com.driveready.testprep"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.driveready.testprep"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$privacyPolicyUrl\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$testBannerId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$testInterstitialId\"")
            buildConfigField("String", "NATIVE_AD_UNIT_ID", "\"$testNativeId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$prodBannerId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$prodInterstitialId\"")
            buildConfigField("String", "NATIVE_AD_UNIT_ID", "\"$prodNativeId\"")
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
            "androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.junit)
}
