// Top-level build file.
// AGP 9 ships built-in Kotlin, so org.jetbrains.kotlin.android is intentionally NOT applied.
// AGP's built-in Kotlin uses the Kotlin Gradle plugin found on the build classpath when it is
// newer than its own runtime dependency, so we pin KGP here to keep the Compose compiler
// plugin (which must match KGP exactly) in sync.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Keep in sync with `kotlin` in gradle/libs.versions.toml.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
