package com.satya.calorietracker.domain.model

import kotlinx.serialization.Serializable

enum class Gender(val id: String, val label: String) {
    MALE("MALE", "Male"),
    FEMALE("FEMALE", "Female"),
    OTHER("OTHER", "Prefer not to say");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

enum class ActivityLevel(val id: String, val label: String, val description: String, val multiplier: Double) {
    SEDENTARY("SEDENTARY", "Sedentary", "Desk job, little or no exercise", 1.2),
    LIGHT("LIGHT", "Lightly active", "Light exercise 1-3 days a week", 1.375),
    MODERATE("MODERATE", "Moderately active", "Exercise 3-5 days a week", 1.55),
    VERY("VERY", "Very active", "Hard exercise 6-7 days a week", 1.725),
    EXTRA("EXTRA", "Extremely active", "Physical job or twice-daily training", 1.9);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: MODERATE
    }
}

enum class GoalType(val id: String, val label: String, val kcalDelta: Int) {
    LOSE_FAST("LOSE_FAST", "Lose weight (0.75 kg / week)", -750),
    LOSE("LOSE", "Lose weight (0.5 kg / week)", -500),
    LOSE_SLOW("LOSE_SLOW", "Lose weight (0.25 kg / week)", -250),
    MAINTAIN("MAINTAIN", "Maintain weight", 0),
    GAIN_SLOW("GAIN_SLOW", "Gain weight (0.25 kg / week)", 250),
    GAIN("GAIN", "Gain weight (0.5 kg / week)", 500);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: MAINTAIN
    }
}

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("SYSTEM", "System default"),
    LIGHT("LIGHT", "Light"),
    DARK("DARK", "Dark");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/** Everything the calorie calculator needs. All values metric internally. */
@Serializable
data class UserProfile(
    val age: Int = 30,
    val genderId: String = "OTHER",
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val goalWeightKg: Double = 72.0,
    val activityId: String = "MODERATE",
    val goalId: String = "MAINTAIN"
) {
    val gender: Gender get() = Gender.fromId(genderId)
    val activity: ActivityLevel get() = ActivityLevel.fromId(activityId)
    val goal: GoalType get() = GoalType.fromId(goalId)
}
