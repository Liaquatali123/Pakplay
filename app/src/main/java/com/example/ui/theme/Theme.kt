package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = BackgroundDark,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = CyanGlow,
    secondary = PurpleElectric,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SurfaceContainerDark,
    onSecondaryContainer = PurpleDim,
    tertiary = AmberWarm,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceContainerDark,
    outline = CardBorderDark,
    error = CoralRed,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = CyanDark,
    onPrimary = SurfaceLight,
    primaryContainer = SurfaceVariantLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = PurpleDim,
    onSecondary = SurfaceLight,
    secondaryContainer = SurfaceContainerLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = OrangeFlame,
    onTertiary = SurfaceLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = SurfaceContainerLight,
    outline = CardBorderLight,
    error = CoralRed,
    onError = SurfaceLight
)

@Composable
fun PakPlayTheme(
    darkTheme: Boolean = true, // Dark theme default for video player
    dynamicColor: Boolean = false, // Keep signature styling
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = PakPlayTheme(darkTheme, dynamicColor, content)

