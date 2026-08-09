package com.satya.calorietracker.domain.model

/**
 * Meal slots. The four built-ins are fixed; anything else the user invents is stored
 * as [CUSTOM] with a free-text label on the log entry itself.
 */
enum class MealType(val id: String, val displayName: String, val emoji: String, val defaultHour: Int) {
    BREAKFAST("BREAKFAST", "Breakfast", "🍳", 8),
    LUNCH("LUNCH", "Lunch", "🍱", 13),
    DINNER("DINNER", "Dinner", "🍽️", 20),
    SNACK("SNACK", "Snacks", "🍎", 16),
    CUSTOM("CUSTOM", "Custom", "✨", 12);

    companion object {
        val BUILT_INS = listOf(BREAKFAST, LUNCH, DINNER, SNACK)

        fun fromId(id: String?): MealType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SNACK

        /** Best guess of the meal slot for a given hour of day, used to pre-select in the UI. */
        fun suggestedFor(hour: Int): MealType = when (hour) {
            in 4..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..17 -> SNACK
            in 18..22 -> DINNER
            else -> SNACK
        }
    }
}
