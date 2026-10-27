/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

/** How strongly the seed color is spread across the UI. SUBTLE is the original look. */
enum class ThemeColorStyle(val paletteStyle: PaletteStyle) {
    SUBTLE(PaletteStyle.TonalSpot),
    VIBRANT(PaletteStyle.Vibrant),
    EXPRESSIVE(PaletteStyle.Expressive),
    RAINBOW(PaletteStyle.Rainbow),
    FRUIT_SALAD(PaletteStyle.FruitSalad),
}

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    colorStyle: ThemeColorStyle = ThemeColorStyle.SUBTLE,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDefaultColor = themeColor == DefaultThemeColor
    // System wallpaper colors are used as-is only for the default color with the subtle style
    val useSystemDynamicColor =
        isDefaultColor && colorStyle == ThemeColorStyle.SUBTLE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // A colorful style on the default color is seeded from the wallpaper's primary color
        val seedColor = if (isDefaultColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context).primary
        } else {
            themeColor
        }
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme,
            // The 2025 spec only defines the tonal styles; the colorful ones use the 2021 spec
            specVersion = if (colorStyle == ThemeColorStyle.SUBTLE) {
                ColorSpec.SpecVersion.SPEC_2025
            } else {
                ColorSpec.SpecVersion.SPEC_2021
            },
            style = colorStyle.paletteStyle,
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
