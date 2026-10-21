// Top-level build file. AGP 9 compiles Kotlin itself (built-in Kotlin), so there is no
// org.jetbrains.kotlin.android plugin here. The Compose compiler plugin version sets the
// Kotlin version used for the whole build.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
