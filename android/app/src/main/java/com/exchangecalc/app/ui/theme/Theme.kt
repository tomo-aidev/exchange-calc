package com.exchangecalc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.exchangecalc.app.util.AppSettings

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    surface = Color.White,
    background = PrimaryLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    surface = Color(0xFF1C1C1E),
    background = PrimaryDark,
)

@Composable
fun ExchangeCalcTheme(
    darkTheme: Boolean = AppSettings.getInstance().darkModeEnabled || isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
