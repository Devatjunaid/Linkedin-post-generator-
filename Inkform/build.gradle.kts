// Top-level build file.
// AGP 9 ships built-in Kotlin, so the org.jetbrains.kotlin.android plugin is NOT applied anywhere.
// The Kotlin Gradle plugin is pinned here so the Kotlin compiler and the Compose compiler plugin
// always use the same version (see "Upgrade to a higher KGP version" in the AGP 9 release notes).
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Keep in sync with "kotlin" in gradle/libs.versions.toml
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
