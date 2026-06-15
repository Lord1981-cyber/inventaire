package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = SleekTextDark,
    surface = Color(0xFF201A25),
    onBackground = SleekPurpleBg,
    onSurface = SleekPurpleBg
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPurple,
    onPrimary = Color.White,
    primaryContainer = SleekPurpleLight,
    onPrimaryContainer = SleekPurpleDark,
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = SleekPurpleContainer,
    onSecondaryContainer = SleekTextDark,
    tertiary = Pink40,
    background = SleekPurpleBg,
    onBackground = SleekTextDark,
    surface = Color.White,
    onSurface = SleekTextDark,
    surfaceVariant = SleekPurpleSurface,
    onSurfaceVariant = SleekTextMuted,
    outline = SleekBorder,
    error = SleekErrorText,
    onError = Color.White,
    errorContainer = SleekErrorBg,
    onErrorContainer = SleekErrorText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Force Light Theme for Sleek Interface
  dynamicColor: Boolean = false, // Force consistent branding
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
