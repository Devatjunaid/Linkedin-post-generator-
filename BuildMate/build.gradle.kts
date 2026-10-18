// Top-level build file.
// AGP 9 ships built-in Kotlin, so org.jetbrains.kotlin.android is intentionally NOT applied anywhere.
// The classpath entry below pins the Kotlin Gradle plugin that built-in Kotlin uses, so it matches
// the Compose compiler plugin version (both come from libs.versions.toml -> kotlin).
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
