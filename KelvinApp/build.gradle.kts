// Top-level build file. Kotlin is compiled by AGP's built-in Kotlin support,
// so org.jetbrains.kotlin.android is intentionally NOT applied anywhere.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
