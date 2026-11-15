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

fun getDarkColorScheme(accentColor: Color = AccentCyan) = darkColorScheme(
    primary = accentColor,
    onPrimary = Color.Black,
    primaryContainer = Zinc800,
    onPrimaryContainer = Zinc100,
    secondary = accentColor.copy(alpha = 0.8f),
    onSecondary = Color.Black,
    secondaryContainer = Zinc850,
    onSecondaryContainer = Zinc200,
    tertiary = AccentEmerald,
    onTertiary = Color.Black,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc850,
    onSurfaceVariant = Zinc400,
    outline = Zinc800,
    outlineVariant = Zinc700,
    error = AccentRose,
    onError = Color.White
)

fun getLightColorScheme(accentColor: Color = AccentCyan) = lightColorScheme(
    primary = accentColor,
    onPrimary = Color.White,
    primaryContainer = Zinc200,
    onPrimaryContainer = Zinc950,
    secondary = accentColor,
    onSecondary = Color.White,
    secondaryContainer = Zinc100,
    onSecondaryContainer = Zinc900,
    tertiary = AccentEmerald,
    onTertiary = Color.White,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = Color.White,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Zinc600,
    outline = Zinc300,
    outlineVariant = Zinc200,
    error = AccentRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = AccentCyan,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(accentColor)
        else -> getDarkColorScheme(accentColor) // Keep dark minimalist theme default for developer bridge
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
