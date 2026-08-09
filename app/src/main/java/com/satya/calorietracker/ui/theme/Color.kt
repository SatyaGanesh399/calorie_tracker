package com.satya.calorietracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.satya.calorietracker.data.prefs.AccentColor

/**
 * Material 3 colour schemes generated from an accent seed.
 *
 * Rather than hand-writing six pairs of schemes, we derive a tonal ramp from each seed
 * by holding hue and saturation and moving lightness — close enough to the real M3
 * tonal palette for a personal app, and it means adding an accent is one enum entry.
 */

// Fixed semantic colours that shouldn't shift with the accent.
val CalorieOrange = Color(0xFFEF6C3E)
val ProteinRed = Color(0xFFE05260)
val CarbAmber = Color(0xFFE9A23B)
val FatBlue = Color(0xFF4C8DD9)
val FiberGreen = Color(0xFF57A773)
val WaterBlue = Color(0xFF38A3D1)
val WeightViolet = Color(0xFF7C6BD6)

val GoalGood = Color(0xFF3FA46A)
val GoalClose = Color(0xFFE0A020)
val GoalOver = Color(0xFFD9534F)

private fun Color.withTone(tone: Int): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    // Pull saturation down as we approach the extremes so light tones don't look neon.
    val t = tone.coerceIn(0, 100) / 100f
    val saturationScale = when {
        t > 0.9f -> 0.20f
        t > 0.8f -> 0.45f
        t < 0.15f -> 0.85f
        else -> 1f
    }
    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(hsv[0], (hsv[1] * saturationScale).coerceIn(0f, 1f), t)
        )
    )
}

private fun neutral(seed: Color, tone: Int): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(hsv[0], 0.05f, tone.coerceIn(0, 100) / 100f)
        )
    )
}

fun lightSchemeFor(accent: AccentColor): ColorScheme {
    val seed = Color(accent.seed)
    val alt = Color(accent.seed).shiftHue(35f)
    return lightColorScheme(
        primary = seed.withTone(38),
        onPrimary = Color.White,
        primaryContainer = seed.withTone(90),
        onPrimaryContainer = seed.withTone(16),
        secondary = seed.withTone(45).desaturate(0.4f),
        onSecondary = Color.White,
        secondaryContainer = seed.withTone(92).desaturate(0.3f),
        onSecondaryContainer = seed.withTone(18),
        tertiary = alt.withTone(42),
        onTertiary = Color.White,
        tertiaryContainer = alt.withTone(91),
        onTertiaryContainer = alt.withTone(16),
        background = neutral(seed, 99),
        onBackground = neutral(seed, 11),
        surface = neutral(seed, 99),
        onSurface = neutral(seed, 11),
        surfaceVariant = neutral(seed, 92),
        onSurfaceVariant = neutral(seed, 34),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = neutral(seed, 97),
        surfaceContainer = neutral(seed, 95),
        surfaceContainerHigh = neutral(seed, 93),
        surfaceContainerHighest = neutral(seed, 91),
        outline = neutral(seed, 55),
        outlineVariant = neutral(seed, 82),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        inverseSurface = neutral(seed, 20),
        inverseOnSurface = neutral(seed, 95),
        inversePrimary = seed.withTone(80),
        scrim = Color.Black
    )
}

fun darkSchemeFor(accent: AccentColor): ColorScheme {
    val seed = Color(accent.seed)
    val alt = Color(accent.seed).shiftHue(35f)
    return darkColorScheme(
        primary = seed.withTone(78),
        onPrimary = seed.withTone(18),
        primaryContainer = seed.withTone(30),
        onPrimaryContainer = seed.withTone(90),
        secondary = seed.withTone(76).desaturate(0.4f),
        onSecondary = seed.withTone(20),
        secondaryContainer = seed.withTone(32).desaturate(0.3f),
        onSecondaryContainer = seed.withTone(90),
        tertiary = alt.withTone(78),
        onTertiary = alt.withTone(18),
        tertiaryContainer = alt.withTone(30),
        onTertiaryContainer = alt.withTone(90),
        background = neutral(seed, 8),
        onBackground = neutral(seed, 90),
        surface = neutral(seed, 8),
        onSurface = neutral(seed, 90),
        surfaceVariant = neutral(seed, 26),
        onSurfaceVariant = neutral(seed, 78),
        surfaceContainerLowest = neutral(seed, 5),
        surfaceContainerLow = neutral(seed, 11),
        surfaceContainer = neutral(seed, 13),
        surfaceContainerHigh = neutral(seed, 17),
        surfaceContainerHighest = neutral(seed, 22),
        outline = neutral(seed, 58),
        outlineVariant = neutral(seed, 30),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = neutral(seed, 90),
        inverseOnSurface = neutral(seed, 20),
        inversePrimary = seed.withTone(38),
        scrim = Color.Black
    )
}

private fun Color.shiftHue(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return Color(android.graphics.Color.HSVToColor(floatArrayOf((hsv[0] + degrees + 360f) % 360f, hsv[1], hsv[2])))
}

private fun Color.desaturate(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(hsv[0], (hsv[1] * (1f - amount)).coerceIn(0f, 1f), hsv[2])
        )
    )
}

/** Traffic-light colour for a day's calories against target — used by History. */
fun goalStatusColor(consumed: Double, target: Double): Color = when {
    target <= 0.0 -> GoalClose
    consumed == 0.0 -> GoalClose
    consumed in (target * 0.9)..(target * 1.1) -> GoalGood
    consumed in (target * 0.75)..(target * 1.25) -> GoalClose
    else -> GoalOver
}
