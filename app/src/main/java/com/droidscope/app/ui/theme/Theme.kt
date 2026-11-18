package com.droidscope.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = DroidScopeBlue,
    secondary = DroidScopeAccent,
    background = DroidScopeDark,
    surface = DroidScopeSurface,
)

private val LightColors = lightColorScheme(
    primary = DroidScopeBlue,
    secondary = DroidScopeAccent,
    background = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun DroidScopeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = DroidScopeTypography,
        content = content
    )
}
