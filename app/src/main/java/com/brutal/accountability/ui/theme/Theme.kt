package com.brutal.accountability.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BrutalDarkColorScheme = darkColorScheme(
    primary = BrutalRed,
    onPrimary = TextOnRed,
    primaryContainer = BrutalRedDark,
    onPrimaryContainer = TextPrimary,
    secondary = EmberOrange,
    onSecondary = DeepBlack,
    secondaryContainer = EmberOrangeLight,
    onSecondaryContainer = DeepBlack,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = DividerDark,
    outlineVariant = DividerDark,
    error = BrutalRed,
    onError = TextOnRed
)

@Composable
fun BrutalTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepBlack.toArgb()
            window.navigationBarColor = DeepBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = BrutalDarkColorScheme,
        typography = BrutalTypography,
        content = content
    )
}
