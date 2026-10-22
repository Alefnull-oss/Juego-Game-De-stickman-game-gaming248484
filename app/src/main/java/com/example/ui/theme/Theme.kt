package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DismountColorScheme = darkColorScheme(
  primary = DismountWhite,
  onPrimary = DismountBackground,
  primaryContainer = DismountSurfaceElevated,
  onPrimaryContainer = DismountWhite,
  secondary = HazardOrange,
  onSecondary = Color.Black,
  secondaryContainer = DismountSurfaceElevated,
  onSecondaryContainer = HazardOrange,
  tertiary = BoostBlue,
  onTertiary = Color.White,
  background = DismountBackground,
  onBackground = DismountWhite,
  surface = DismountSurface,
  onSurface = DismountWhite,
  surfaceVariant = DismountSurfaceElevated,
  onSurfaceVariant = DismountTextDim,
  error = FractureRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DismountColorScheme,
    typography = Typography,
    content = content
  )
}
