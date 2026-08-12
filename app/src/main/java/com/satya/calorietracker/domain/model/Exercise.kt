package com.satya.calorietracker.domain.model

/** Broad grouping, used for the filter chips in the exercise picker. */
enum class ExerciseCategory(val id: String, val label: String, val emoji: String) {
    CHEST("CHEST", "Chest", "🫁"),
    BACK("BACK", "Back", "🔙"),
    LEGS("LEGS", "Legs", "🦵"),
    SHOULDERS("SHOULDERS", "Shoulders", "🎽"),
    ARMS("ARMS", "Arms", "💪"),
    CORE("CORE", "Core", "🧱"),
    FULL_BODY("FULL_BODY", "Full body", "🏋️"),
    CARDIO("CARDIO", "Cardio", "🏃");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: FULL_BODY
    }
}

enum class Equipment(val id: String, val label: String) {
    BARBELL("BARBELL", "Barbell"),
    DUMBBELL("DUMBBELL", "Dumbbell"),
    MACHINE("MACHINE", "Machine"),
    CABLE("CABLE", "Cable"),
    BODYWEIGHT("BODYWEIGHT", "Bodyweight"),
    KETTLEBELL("KETTLEBELL", "Kettlebell"),
    BAND("BAND", "Band"),
    CARDIO_MACHINE("CARDIO_MACHINE", "Cardio"),
    OTHER("OTHER", "Other");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

/**
 * How a set of this exercise is measured. Derived from the entity's tracks* flags so
 * the set editor can ask for the right two or three numbers and nothing else.
 */
enum class SetInputMode {
    /** kg × reps — the overwhelming majority. */
    WEIGHT_REPS,

    /** Reps only, e.g. push-ups, or weighted via an optional load. */
    REPS_ONLY,

    /** A hold: seconds, optionally loaded. Planks, dead hangs, wall sits. */
    DURATION,

    /** Time and distance. Runs, rides, swims, rows. */
    DURATION_DISTANCE
}
