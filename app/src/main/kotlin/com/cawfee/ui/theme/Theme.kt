package com.cawfee.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Espresso-toned brand palette used when dynamic color is unavailable.
private val Espresso = Color(0xFF6F4E37)
private val Crema = Color(0xFFC8A27C)
private val Caramel = Color(0xFFB07D45)

private val LightColors = lightColorScheme(
    primary = Espresso,
    secondary = Caramel,
    tertiary = Crema,
)

private val DarkColors = darkColorScheme(
    primary = Crema,
    secondary = Caramel,
    tertiary = Espresso,
)

/**
 * Material 3 theme. Uses dynamic color on Android 12+ (Material You) and falls back to
 * the brand palette below. Dark mode follows the system, overridable by the caller.
 */
@Composable
fun CawfeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
