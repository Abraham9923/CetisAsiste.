package com.example.cetisasiste.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// El diseño en Figma solo define modo claro, por eso la app usa siempre este esquema.
private val LightColorScheme = lightColorScheme(
    primary = Brand800,
    onPrimary = TextOnBrand,
    primaryContainer = SurfacePill,
    onPrimaryContainer = Brand900,
    secondary = BrandAccent,
    onSecondary = TextOnBrand,
    background = SurfaceBackground,
    onBackground = Brand900,
    surface = SurfaceBackground,
    onSurface = Brand900,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary
)

@Composable
fun CetisAsisteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
