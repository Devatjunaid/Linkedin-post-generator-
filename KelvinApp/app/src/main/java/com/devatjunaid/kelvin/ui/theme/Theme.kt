package com.devatjunaid.kelvin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = K.Ice,
    onPrimary = K.Ink,
    secondary = K.Ember,
    onSecondary = K.Ink,
    tertiary = K.Gold,
    background = K.Ink,
    onBackground = K.Text,
    surface = K.Surface,
    onSurface = K.Text,
    surfaceVariant = K.Surface2,
    onSurfaceVariant = K.Muted,
    surfaceContainerHigh = K.Surface2,
    surfaceContainer = K.Surface,
    outline = K.Line,
    error = K.EmberDeep,
)

@Composable
fun KelvinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = KelvinTypography, content = content)
}
