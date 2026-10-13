package com.zawa.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ZawaGreen = Color(0xFF19B67A)
val ZawaGreenDark = Color(0xFF0F8F5E)

private val LightColors = lightColorScheme(
    primary = ZawaGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF5E3),
    onPrimaryContainer = Color(0xFF003C26),
    secondary = Color(0xFF4D6357),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFEFF2F6),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFD7DCE2)
)

private val DarkColors = darkColorScheme(
    primary = ZawaGreen,
    onPrimary = Color(0xFF003C26),
    primaryContainer = ZawaGreenDark,
    onPrimaryContainer = Color(0xFFCFF5E3),
    secondary = Color(0xFFB5CCBD),
    background = Color(0xFF101114),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF181A1E),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF23262D),
    onSurfaceVariant = Color(0xFFB5BAC2),
    outline = Color(0xFF34373E)
)

@Composable
fun ZawaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
