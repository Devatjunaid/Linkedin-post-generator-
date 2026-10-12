import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sudzy.carwash"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sudzy.carwash"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        // Google's TEST AdMob app id. Replace with your own app id from the AdMob console before release.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // SoundPool and MediaPlayer read WAVs straight from the APK, so keep them uncompressed.
    androidResources {
        noCompress += listOf("wav")
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
