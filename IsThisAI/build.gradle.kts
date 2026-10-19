// AGP 9 compiles Kotlin itself (built-in Kotlin), so org.jetbrains.kotlin.android is not applied.
// The classpath entry below pins the Kotlin compiler that built-in Kotlin uses to the same
// version as the Compose compiler plugin, so the two always match.
buildscript {
    repositories {
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
