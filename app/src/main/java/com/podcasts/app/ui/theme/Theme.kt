package com.podcasts.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.podcasts.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Palette.Blue600,
    onPrimary = Palette.White,
    primaryContainer = Palette.Blue50,
    onPrimaryContainer = Palette.Blue700,
    secondary = Palette.Grey600,
    onSecondary = Palette.White,
    secondaryContainer = Palette.Grey100,
    onSecondaryContainer = Palette.Grey900,
    background = Palette.White,
    onBackground = Palette.Grey900,
    surface = Palette.White,
    onSurface = Palette.Grey900,
    surfaceVariant = Palette.Grey50,
    onSurfaceVariant = Palette.Grey600,
    outline = Palette.Grey300,
    outlineVariant = Palette.Grey200,
    error = Palette.Red600,
    onError = Palette.White,
)

private val DarkColors = darkColorScheme(
    primary = Palette.Blue200,
    onPrimary = Palette.Grey900,
    primaryContainer = Palette.Blue700,
    onPrimaryContainer = Palette.Blue50,
    secondary = Palette.Grey300,
    onSecondary = Palette.Grey900,
    secondaryContainer = Palette.Grey700,
    onSecondaryContainer = Palette.Grey100,
    background = Palette.DarkBackground,
    onBackground = Palette.Grey100,
    surface = Palette.DarkSurface,
    onSurface = Palette.Grey100,
    surfaceVariant = Palette.DarkSurfaceVariant,
    onSurfaceVariant = Palette.Grey300,
    outline = Palette.Grey700,
    outlineVariant = Palette.Grey800,
    error = Palette.Red300,
    onError = Palette.Grey900,
)

@Composable
fun PodcastsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = PodcastsTypography,
        shapes = PodcastsShapes,
        content = content,
    )
}
