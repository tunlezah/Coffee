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

// ---------------------------------------------------------------------------
// Coffee brand palette. Names follow the drink: espresso for deep browns,
// crema/latte for the light creams, caramel & mocha in between.
// ---------------------------------------------------------------------------
private val Espresso = Color(0xFF6F4E37)      // primary brown
private val EspressoDeep = Color(0xFF3E2B1E)  // darkest roast
private val Crema = Color(0xFFE7C6A0)         // golden crema
private val CremaBright = Color(0xFFF7E3C8)   // lightened crema
private val Caramel = Color(0xFF8C6A4F)       // secondary brown
private val CaramelLight = Color(0xFFF2E1CE)
private val Cinnamon = Color(0xFF9C4F2E)      // tertiary accent
private val CinnamonLight = Color(0xFFFFDBCB)
private val LatteFoam = Color(0xFFFFF8F2)     // light background
private val LatteSurface = Color(0xFFFBEFE4)
private val RoastBean = Color(0xFF211A14)     // dark text / dark background base
private val MochaNight = Color(0xFF1B1410)    // dark background
private val MochaSurfaceHigh = Color(0xFF2A211A)
private val OledBlack = Color(0xFF000000)
private val OledSurfaceHigh = Color(0xFF17110C)

private val LightColors = lightColorScheme(
    primary = Espresso,
    onPrimary = Color.White,
    primaryContainer = CremaBright,
    onPrimaryContainer = EspressoDeep,
    secondary = Caramel,
    onSecondary = Color.White,
    secondaryContainer = CaramelLight,
    onSecondaryContainer = Color(0xFF31200F),
    tertiary = Cinnamon,
    onTertiary = Color.White,
    tertiaryContainer = CinnamonLight,
    onTertiaryContainer = Color(0xFF3A0F00),
    background = LatteFoam,
    onBackground = RoastBean,
    surface = LatteFoam,
    onSurface = RoastBean,
    surfaceVariant = Color(0xFFF0E0D0),
    onSurfaceVariant = Color(0xFF4F4237),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFDF3E9),
    surfaceContainer = LatteSurface,
    surfaceContainerHigh = Color(0xFFF5E9DC),
    surfaceContainerHighest = Color(0xFFEFE2D3),
    outline = Color(0xFF817264),
    outlineVariant = Color(0xFFD3C4B4),
    inverseSurface = Color(0xFF372F28),
    inverseOnSurface = Color(0xFFFDEEDF),
    inversePrimary = Crema,
)

private val DarkColors = darkColorScheme(
    primary = Crema,
    onPrimary = Color(0xFF402C18),
    primaryContainer = Color(0xFF58402B),
    onPrimaryContainer = CremaBright,
    secondary = Color(0xFFD9BFA3),
    onSecondary = Color(0xFF3A2A1A),
    secondaryContainer = Color(0xFF52402E),
    onSecondaryContainer = CaramelLight,
    tertiary = Color(0xFFFFB59A),
    onTertiary = Color(0xFF5A2107),
    tertiaryContainer = Color(0xFF77361B),
    onTertiaryContainer = CinnamonLight,
    background = MochaNight,
    onBackground = Color(0xFFF0DFD1),
    surface = MochaNight,
    onSurface = Color(0xFFF0DFD1),
    surfaceVariant = Color(0xFF4F4237),
    onSurfaceVariant = Color(0xFFD3C4B4),
    surfaceContainerLowest = Color(0xFF120D09),
    surfaceContainerLow = Color(0xFF221A14),
    surfaceContainer = Color(0xFF261D17),
    surfaceContainerHigh = MochaSurfaceHigh,
    surfaceContainerHighest = Color(0xFF352A22),
    outline = Color(0xFF9C8C7B),
    outlineVariant = Color(0xFF4F4237),
    inverseSurface = Color(0xFFF0DFD1),
    inverseOnSurface = Color(0xFF372F28),
    inversePrimary = Espresso,
)

/**
 * Dark palette with true-black window/surface colors so OLED panels can switch
 * those pixels off entirely. Containers keep a whisper of warm brown for depth.
 */
private val OledColors = DarkColors.copy(
    background = OledBlack,
    surface = OledBlack,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = Color(0xFF0D0906),
    surfaceContainer = Color(0xFF120C08),
    surfaceContainerHigh = OledSurfaceHigh,
    surfaceContainerHighest = Color(0xFF1E1610),
)

/**
 * Material 3 theme in the Cawfee coffee palette.
 *
 * The coffee brand colors are the default. Callers may opt into Material You
 * dynamic color on Android 12+ via [dynamicColor]; [oledBlack] switches the dark
 * palette to true-black backgrounds for OLED displays (implies dark).
 */
@Composable
fun CawfeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    oledBlack: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            when {
                oledBlack -> dynamicDarkColorScheme(context).copy(
                    background = OledBlack,
                    surface = OledBlack,
                    surfaceContainerLowest = OledBlack,
                )
                darkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }
        oledBlack -> OledColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
