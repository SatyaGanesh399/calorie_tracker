package com.satya.calorietracker.data.prefs

import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.ThemeMode
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.domain.model.UserProfile
import kotlinx.serialization.Serializable

/** Accent choices offered in Settings > Appearance and in widget configuration. */
enum class AccentColor(val id: String, val label: String, val seed: Long) {
    GREEN("GREEN", "Fresh green", 0xFF2E7D5B),
    BLUE("BLUE", "Deep blue", 0xFF2C5FA8),
    VIOLET("VIOLET", "Violet", 0xFF6C4CC4),
    ORANGE("ORANGE", "Sunset", 0xFFD2691E),
    ROSE("ROSE", "Rose", 0xFFB3446C),
    TEAL("TEAL", "Teal", 0xFF00696E);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: GREEN
    }
}

/** Everything the user can configure, in one immutable snapshot. */
@Serializable
data class UserPreferences(
    val goals: NutritionGoals = NutritionGoals.DEFAULT,
    val profile: UserProfile = UserProfile(),
    val unitSystemId: String = UnitSystem.METRIC.id,
    val themeModeId: String = ThemeMode.SYSTEM.id,
    val dynamicColor: Boolean = true,
    val accentId: String = AccentColor.GREEN.id,
    /** Extra meal slots the user invented, e.g. "Pre-workout". */
    val customMeals: List<String> = emptyList(),
    val enabledProviderIds: Set<String> = setOf("open_food_facts"),
    val reminders: List<Reminder> = Reminder.defaults(),
    val quickWaterAmountsMl: List<Int> = listOf(250, 500, 750, 1000),
    val onboardingComplete: Boolean = false,
    /** When true, changing the profile re-derives the calorie and macro targets. */
    val autoGoalsFromProfile: Boolean = true,
    val goalWeightKg: Double = 72.0,
    val startWeightKg: Double? = null
) {
    val unitSystem: UnitSystem get() = UnitSystem.fromId(unitSystemId)
    val themeMode: ThemeMode get() = ThemeMode.fromId(themeModeId)
    val accent: AccentColor get() = AccentColor.fromId(accentId)

    companion object {
        val DEFAULT = UserPreferences()
    }
}
