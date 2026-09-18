package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    secondary = ElectricBlue,
    tertiary = AccentGreen,
    background = DarkNavyBg,
    surface = SurfaceCard,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary
)

private val LightColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = CyanGlow,
    tertiary = AccentGreen,
    background = DarkNavyBg,
    surface = SurfaceCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
