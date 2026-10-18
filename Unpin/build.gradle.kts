// Top-level build file. AGP 9 ships built-in Kotlin support, so the
// org.jetbrains.kotlin.android plugin is intentionally NOT applied anywhere.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
