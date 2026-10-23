package com.example.smartbillscanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = AccentChampagne,
    tertiary = AccentChampagne,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceHigh,
    outline = SurfaceBorder,
    error = DangerRed,
    onPrimary = OnGold,
    onSecondary = BgDark,
    onTertiary = BgDark,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted
)

// System bars are handled by enableEdgeToEdge() in MainActivity (transparent bars, light icons).
@Composable
fun SmartBillScannerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
