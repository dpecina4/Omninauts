package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ElementType

private val CavernDarkColorScheme =
  darkColorScheme(
    primary = BioCyan,
    onPrimary = CavernObsidian,
    primaryContainer = BioCyanDark,
    onPrimaryContainer = BioCyan,
    secondary = VoltaicViolet,
    onSecondary = CavernObsidian,
    secondaryContainer = VoltaicVioletDark,
    onSecondaryContainer = TextPrimary,
    tertiary = PyroAmber,
    onTertiary = CavernObsidian,
    tertiaryContainer = PyroSurface,
    onTertiaryContainer = PyroGlow,
    background = CavernObsidian,
    onBackground = TextPrimary,
    surface = CavernBasalt,
    onSurface = TextPrimary,
    surfaceVariant = CavernAlloy,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = CavernAlloyHigh,
    outline = CavernBorder,
    error = DangerCrimson,
    onError = CavernObsidian,
  )

val CavernShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
  )

fun ElementType.accentColor(): Color =
  when (this) {
    ElementType.PYRO -> PyroAmber
    ElementType.HYDRO -> HydroAqua
    ElementType.VOLT -> VoltGold
  }

fun ElementType.glowColor(): Color =
  when (this) {
    ElementType.PYRO -> PyroGlow
    ElementType.HYDRO -> HydroGlow
    ElementType.VOLT -> VoltNeon
  }

fun ElementType.surfaceTint(): Color =
  when (this) {
    ElementType.PYRO -> PyroSurface
    ElementType.HYDRO -> HydroSurface
    ElementType.VOLT -> VoltSurface
  }

@Composable
fun CavernautsTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = CavernDarkColorScheme,
    typography = CavernTypography,
    shapes = CavernShapes,
    content = content,
  )
}
