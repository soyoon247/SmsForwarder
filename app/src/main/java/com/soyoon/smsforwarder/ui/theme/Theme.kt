package com.soyoon.smsforwarder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = WhiteSecondary,
    tertiary = AccentGreen,
    background = BlackOlive,
    surface = BlackOliveSurface,
    surfaceVariant = BlackOliveLight,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = White,
    onSurface = White,
    onSurfaceVariant = WhiteSecondary,
    error = AccentRed,
    onError = White,
    errorContainer = Color(0xFF4A1515),
    onErrorContainer = Color(0xFFFFB3B3),
    secondaryContainer = BlackOliveLight,
    onSecondaryContainer = WhiteSecondary,
    outline = WhiteTertiary
)

@Composable
fun SmsForwarderTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BlackOlive.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
