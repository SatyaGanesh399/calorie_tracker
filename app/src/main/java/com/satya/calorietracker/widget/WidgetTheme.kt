package com.satya.calorietracker.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.currentState
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.unit.ColorProvider
import com.satya.calorietracker.data.prefs.AccentColor

/**
 * Per-widget options, stored in each widget's own Glance state so two copies of the
 * same widget on the home screen can look different.
 */
object WidgetPrefKeys {
    val THEME = stringPreferencesKey("widget_theme")          // SYSTEM | LIGHT | DARK
    val ACCENT = stringPreferencesKey("widget_accent")        // AccentColor id
    val COMPACT = booleanPreferencesKey("widget_compact")     // fewer lines, bigger numbers
    val TRANSPARENT = booleanPreferencesKey("widget_transparent")
}

enum class WidgetTheme(val id: String, val label: String) {
    SYSTEM("SYSTEM", "Follow system"),
    LIGHT("LIGHT", "Always light"),
    DARK("DARK", "Always dark");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * Glance has two `ColorProvider` factories with the same name in different packages:
 * `androidx.glance.unit.ColorProvider(color)` for a fixed colour, and
 * `androidx.glance.appwidget.unit.ColorProvider(day, night)` for one that follows the
 * system theme. Importing both would be ambiguous, so the day/night one is wrapped here.
 */
private fun dayNight(day: Color, night: Color): ColorProvider =
    androidx.glance.appwidget.unit.ColorProvider(day = day, night = night)

/** Resolved look for one widget instance. */
data class WidgetStyle(
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val accent: AccentColor = AccentColor.GREEN,
    val compact: Boolean = false,
    val transparent: Boolean = false
) {
    private val accentColor: Color get() = Color(accent.seed)

    val background: ColorProvider
        get() = when {
            transparent -> dayNight(Color(0x22000000), Color(0x33000000))
            theme == WidgetTheme.LIGHT -> ColorProvider(Color(0xFFF6FAF6))
            theme == WidgetTheme.DARK -> ColorProvider(Color(0xFF11150F))
            else -> dayNight(Color(0xFFF6FAF6), Color(0xFF11150F))
        }

    val onBackground: ColorProvider
        get() = when {
            transparent -> ColorProvider(Color.White)
            theme == WidgetTheme.LIGHT -> ColorProvider(Color(0xFF191D18))
            theme == WidgetTheme.DARK -> ColorProvider(Color(0xFFE3E7E0))
            else -> dayNight(Color(0xFF191D18), Color(0xFFE3E7E0))
        }

    val muted: ColorProvider
        get() = when {
            transparent -> ColorProvider(Color(0xCCFFFFFF))
            theme == WidgetTheme.LIGHT -> ColorProvider(Color(0xFF5A6157))
            theme == WidgetTheme.DARK -> ColorProvider(Color(0xFF9AA396))
            else -> dayNight(Color(0xFF5A6157), Color(0xFF9AA396))
        }

    val accentProvider: ColorProvider get() = ColorProvider(accentColor)

    val trackProvider: ColorProvider
        get() = when {
            transparent -> ColorProvider(Color(0x55FFFFFF))
            theme == WidgetTheme.LIGHT -> ColorProvider(Color(0xFFDCE5D9))
            theme == WidgetTheme.DARK -> ColorProvider(Color(0xFF2C3329))
            else -> dayNight(Color(0xFFDCE5D9), Color(0xFF2C3329))
        }

    companion object {
        val CALORIE = ColorProvider(Color(0xFFEF6C3E))
        val PROTEIN = ColorProvider(Color(0xFFE05260))
        val CARBS = ColorProvider(Color(0xFFE9A23B))
        val FAT = ColorProvider(Color(0xFF4C8DD9))
        val WATER = ColorProvider(Color(0xFF38A3D1))
        val WEIGHT = ColorProvider(Color(0xFF7C6BD6))

        fun from(prefs: Preferences): WidgetStyle = WidgetStyle(
            theme = WidgetTheme.fromId(prefs[WidgetPrefKeys.THEME]),
            accent = AccentColor.fromId(prefs[WidgetPrefKeys.ACCENT]),
            compact = prefs[WidgetPrefKeys.COMPACT] ?: false,
            transparent = prefs[WidgetPrefKeys.TRANSPARENT] ?: false
        )
    }
}

/**
 * Glance's `currentState()` is nullable (a brand-new widget has no state yet), so every
 * widget goes through this instead of unwrapping it by hand.
 */
@Composable
fun rememberWidgetStyle(): WidgetStyle =
    WidgetStyle.from(currentState<Preferences>() ?: emptyPreferences())
