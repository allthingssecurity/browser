package com.example.northstarquest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val darkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentTeal,
    background = DarkNavy,
    surface = Slate
)

private val lightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentTeal
)

@Composable
fun NorthStarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkColorScheme else lightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

