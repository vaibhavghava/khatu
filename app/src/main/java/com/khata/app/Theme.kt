package com.khata.app

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF16A34A)   // RECEIVED transactions
val Red = Color(0xFFDC2626)     // GIVEN transactions
val Blue = Color(0xFF2563EB)    // balance status: receivable
val Amber = Color(0xFFD97706)   // balance status: payable

private val LightC = lightColorScheme(
    primary = Color(0xFF0B6E6E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEEE), onPrimaryContainer = Color(0xFF00201F),
    background = Color(0xFFF4F7F7), onBackground = Color(0xFF101818),
    surface = Color.White, onSurface = Color(0xFF101818),
    surfaceVariant = Color(0xFFE0E8E8), onSurfaceVariant = Color(0xFF465252), outline = Color(0xFF8A9696),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFCFC),
    surfaceContainer = Color(0xFFF0F5F5), surfaceContainerHigh = Color(0xFFEAF0F0), surfaceContainerHighest = Color(0xFFE3EAEA)
)
private val DarkC = darkColorScheme(
    primary = Color(0xFF4FD1D1), onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF00504F), onPrimaryContainer = Color(0xFFCDEEEE),
    background = Color(0xFF0D1313), onBackground = Color(0xFFE2EAEA),
    surface = Color(0xFF151D1D), onSurface = Color(0xFFE2EAEA),
    surfaceVariant = Color(0xFF263232), onSurfaceVariant = Color(0xFFB4C2C2), outline = Color(0xFF7D8B8B),
    surfaceContainerLowest = Color(0xFF0A1010), surfaceContainerLow = Color(0xFF121A1A),
    surfaceContainer = Color(0xFF182121), surfaceContainerHigh = Color(0xFF202A2A), surfaceContainerHighest = Color(0xFF283434)
)

@Composable
fun KhataTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkC else LightC, content = content)
}
