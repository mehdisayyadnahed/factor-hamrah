package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = BrandBlueContainer,
    secondary = BrandPurpleContainer,
    onSecondary = BrandOnPurpleContainer,
    secondaryContainer = Color(0xFF4F378B),
    onSecondaryContainer = BrandPurpleContainer,
    tertiary = AmberOrange,
    background = AppBgDark,
    onBackground = TextPrimaryLight,
    surface = AppSurfaceDark,
    onSurface = TextPrimaryLight,
    surfaceVariant = AppSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryLight,
    outline = AppBorderDark,
    outlineVariant = Color(0xFF23272E)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = BrandOnBlueContainer,
    secondary = BrandPurple,
    onSecondary = Color.White,
    secondaryContainer = BrandPurpleContainer,
    onSecondaryContainer = BrandOnPurpleContainer,
    tertiary = AmberOrange,
    background = AppBgLight,
    onBackground = TextPrimaryDark,
    surface = AppSurfaceLight,
    onSurface = TextPrimaryDark,
    surfaceVariant = AppSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryDark,
    outline = AppBorderLight,
    outlineVariant = AppBorderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Force RTL layout direction for Persian UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

