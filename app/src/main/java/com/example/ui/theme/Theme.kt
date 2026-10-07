package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpaceDarkColorScheme = darkColorScheme(
    primary = SupernovaPurple,
    onPrimary = Color.White,
    primaryContainer = SupernovaPurpleContainer,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = CosmicCyan,
    onSecondary = SpaceVoid,
    secondaryContainer = CosmicCyanContainer,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = DeepNebulaMagenta,
    onTertiary = Color.White,
    tertiaryContainer = DeepNebulaMagentaContainer,
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = SpaceBackground,
    onBackground = StarlightPrimary,
    surface = SpaceSurface,
    onSurface = StarlightPrimary,
    surfaceVariant = SpaceCard,
    onSurfaceVariant = StarlightSecondary,
    outline = SpaceCardBorder,
    outlineVariant = Color(0xFF1E2D4E),
    error = MarsCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = SpaceDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SpaceSurface.toArgb()
                window.navigationBarColor = SpaceSurface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
