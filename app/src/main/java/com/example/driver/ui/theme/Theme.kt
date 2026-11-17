package com.example.driver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ModernDarkColorScheme = darkColorScheme(
    primary = TaxiAmberPrimary,
    onPrimary = ObsidianDark,
    secondary = EmeraldSuccess,
    background = ObsidianDark,
    surface = ObsidianSurface,
    onBackground = SurfaceWhite,
    onSurface = SurfaceWhite
)

private val ModernLightColorScheme = lightColorScheme(
    primary = TaxiAmberPrimary,
    onPrimary = ObsidianDark,
    secondary = EmeraldSuccess,
    background = PearlBackground,
    surface = SurfaceWhite,
    onBackground = ObsidianDark,
    onSurface = ObsidianDark
)

@Composable
fun DriverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ModernDarkColorScheme else ModernLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
