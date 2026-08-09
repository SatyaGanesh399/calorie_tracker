package com.satya.calorietracker.data.prefs

import kotlinx.serialization.Serializable

enum class ReminderType(
    val id: String,
    val label: String,
    val title: String,
    val body: String,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    BREAKFAST("BREAKFAST", "Breakfast", "🍳 Breakfast", "Don't forget to log your breakfast.", 9, 0),
    LUNCH("LUNCH", "Lunch", "🥗 Lunch", "Time to log your lunch.", 13, 30),
    DINNER("DINNER", "Dinner", "🍽️ Dinner", "Remember to log your dinner.", 20, 30),
    WATER("WATER", "Water", "💧 Water", "Time for some water.", 15, 0),
    WEIGHT("WEIGHT", "Weigh-in", "⚖️ Weigh-in", "Don't forget to record your weight.", 7, 30);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: WATER
    }
}

/**
 * One scheduled nudge. [daysOfWeek] uses java.time values (Monday = 1 … Sunday = 7),
 * so a weekday-only weigh-in is just {1,2,3,4,5}.
 */
@Serializable
data class Reminder(
    val typeId: String,
    val enabled: Boolean = false,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int> = ALL_DAYS
) {
    val type: ReminderType get() = ReminderType.fromId(typeId)

    val timeLabel: String get() = "%02d:%02d".format(hour, minute)

    val daysLabel: String
        get() = when {
            daysOfWeek.size == 7 -> "Every day"
            daysOfWeek == WEEKDAYS -> "Weekdays"
            daysOfWeek == WEEKENDS -> "Weekends"
            daysOfWeek.isEmpty() -> "Never"
            else -> daysOfWeek.sorted().joinToString(", ") { DAY_NAMES[it - 1] }
        }

    /** A stable, small int for PendingIntent request codes and alarm ids. */
    val alarmId: Int get() = 1000 + ReminderType.entries.indexOfFirst { it.id == typeId }

    companion object {
        val ALL_DAYS = setOf(1, 2, 3, 4, 5, 6, 7)
        val WEEKDAYS = setOf(1, 2, 3, 4, 5)
        val WEEKENDS = setOf(6, 7)
        val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        /** All reminders, off by default. The app never nags until you opt in. */
        fun defaults(): List<Reminder> = ReminderType.entries.map {
            Reminder(typeId = it.id, enabled = false, hour = it.defaultHour, minute = it.defaultMinute)
        }
    }
}
