package com.hermes.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF5B8DEF)
private val BlueDark = Color(0xFF2F5FD0)

private val DarkColors = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    secondary = BlueDark,
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF15151C),
    onBackground = Color(0xFFECECF1),
    onSurface = Color(0xFFECECF1),
    error = Color(0xFFEF5350),
)

private val LightColors = lightColorScheme(
    primary = BlueDark,
    onPrimary = Color.White,
    secondary = Blue,
    background = Color(0xFFF7F7FB),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF15151C),
    onSurface = Color(0xFF15151C),
    error = Color(0xFFC62828),
)

@Composable
fun HermesTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
