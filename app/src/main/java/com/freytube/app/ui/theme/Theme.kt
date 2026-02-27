package com.freytube.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextWhite,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextWhite,
    secondary = Secondary,
    onSecondary = TextWhite,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = TextWhite,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextWhiteSecondary,
    error = Error,
    onError = TextWhite,
    outline = Color(0xFF444444)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,
    onPrimary = TextWhite,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = TextWhite,
    secondary = SecondaryDark,
    onSecondary = TextWhite,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = TextDark,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextDarkSecondary,
    error = Error,
    onError = TextWhite,
    outline = Color(0xFFDDDDDD)
)

@Composable
fun FreyTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FreyTubeTypography,
        content = content
    )
}
