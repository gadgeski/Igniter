package com.gadgeski.igniter.ui.theme

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
    primary = IgniterPrimary,
    onPrimary = IgniterOnPrimary,

    secondary = IgniterSecondary,
    onSecondary = IgniterOnSecondary,

    tertiary = IgniterTertiary,
    onTertiary = IgniterOnTertiary,

    background = IgniterBackgroundDark,
    onBackground = IgniterOnBackgroundDark,

    surface = IgniterSurfaceDark,
    onSurface = IgniterOnSurfaceDark,

    surfaceVariant = IgniterSurfaceVariantDark,
    onSurfaceVariant = IgniterOnSurfaceVariantDark,

    outline = IgniterOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = IgniterPrimary,
    onPrimary = IgniterOnPrimary,

    secondary = IgniterSecondary,
    onSecondary = IgniterOnSecondary,

    tertiary = IgniterTertiary,
    onTertiary = IgniterOnTertiary,

    background = IgniterBackgroundLight,
    onBackground = IgniterOnBackgroundLight,

    surface = IgniterSurfaceLight,
    onSurface = IgniterOnSurfaceLight,

    surfaceVariant = IgniterSurfaceVariantLight,
    onSurfaceVariant = IgniterOnSurfaceVariantLight,

    outline = IgniterOutlineLight
)

@Composable
fun IgniterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
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