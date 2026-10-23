// Top-level build file.
//
// AGP 9 compiles Kotlin itself (built-in Kotlin), so org.jetbrains.kotlin.android is NOT applied
// anywhere. AGP ships with a runtime dependency on an older Kotlin Gradle plugin; putting the
// newer KGP on the build classpath (as documented in the AGP 9.0 release notes) keeps the Kotlin
// compiler and the Compose compiler plugin on the same version.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
