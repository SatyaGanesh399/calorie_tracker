package com.satya.calorietracker.widget

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.LocalContext
import androidx.glance.currentState
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
 * Resolved look for one widget instance.
 *
 * Dark mode is reduced to a plain boolean *before* any colour is picked, rather than
 * leaning on a day/night ColorProvider. That keeps this file to the single
 * ColorProvider factory that lives in glance core, and it means "Always light",
 * "Always dark" and "Follow system" all fall out of one code path.
 *
 * The cost: a widget set to Follow system only picks up a theme change on its next
 * update. Android broadcasts APPWIDGET_UPDATE on configuration change and we also
 * refresh after every data write, so in practice it catches up straight away.
 */
data class WidgetStyle(
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val accent: AccentColor = AccentColor.GREEN,
    val compact: Boolean = false,
    val transparent: Boolean = false,
    /** What the system is currently doing. Only consulted when [theme] is SYSTEM. */
    val systemInDarkMode: Boolean = false
) {
    private val dark: Boolean
        get() = when (theme) {
            WidgetTheme.LIGHT -> false
            WidgetTheme.DARK -> true
            WidgetTheme.SYSTEM -> systemInDarkMode
        }

    val background: ColorProvider
        get() = when {
            // Over an unknown wallpaper a dark scrim is the only thing that reads in both themes.
            transparent -> ColorProvider(Color(0x59000000))
            dark -> ColorProvider(Color(0xFF11150F))
            else -> ColorProvider(Color(0xFFF6FAF6))
        }

    val onBackground: ColorProvider
        get() = when {
            transparent -> ColorProvider(Color.White)
            dark -> ColorProvider(Color(0xFFE3E7E0))
            else -> ColorProvider(Color(0xFF191D18))
        }

    val muted: ColorProvider
        get() = when {
            transparent -> ColorProvider(Color(0xCCFFFFFF))
            dark -> ColorProvider(Color(0xFF9AA396))
            else -> ColorProvider(Color(0xFF5A6157))
        }

    val accentProvider: ColorProvider get() = ColorProvider(Color(accent.seed))

    val trackProvider: ColorProvider
        get() = when {
            transparent -> ColorProvider(Color(0x55FFFFFF))
            dark -> ColorProvider(Color(0xFF2C3329))
            else -> ColorProvider(Color(0xFFDCE5D9))
        }

    companion object {
        val CALORIE = ColorProvider(Color(0xFFEF6C3E))
        val PROTEIN = ColorProvider(Color(0xFFE05260))
        val CARBS = ColorProvider(Color(0xFFE9A23B))
        val FAT = ColorProvider(Color(0xFF4C8DD9))
        val WATER = ColorProvider(Color(0xFF38A3D1))
        val WEIGHT = ColorProvider(Color(0xFF7C6BD6))

        fun from(prefs: Preferences, systemInDarkMode: Boolean): WidgetStyle = WidgetStyle(
            theme = WidgetTheme.fromId(prefs[WidgetPrefKeys.THEME]),
            accent = AccentColor.fromId(prefs[WidgetPrefKeys.ACCENT]),
            compact = prefs[WidgetPrefKeys.COMPACT] ?: false,
            transparent = prefs[WidgetPrefKeys.TRANSPARENT] ?: false,
            systemInDarkMode = systemInDarkMode
        )
    }
}

/**
 * Glance's `currentState()` is nullable (a brand-new widget has no state yet), so every
 * widget goes through this rather than unwrapping it by hand. It also resolves the
 * system night-mode flag that the colour getters need.
 */
@Composable
fun rememberWidgetStyle(): WidgetStyle {
    val context = LocalContext.current
    val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return WidgetStyle.from(
        prefs = currentState<Preferences>() ?: emptyPreferences(),
        systemInDarkMode = nightMode == Configuration.UI_MODE_NIGHT_YES
    )
}
