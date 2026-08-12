package com.satya.calorietracker.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * The exercise library: bundled exercises plus anything you create yourself.
 *
 * The `tracks*` flags decide which fields the set editor shows, so a barbell squat
 * asks for kg and reps while a run asks for time and distance, without needing
 * separate tables or a type hierarchy.
 */
@Entity(
    tableName = "exercises",
    indices = [Index("name"), Index("category"), Index("lastUsedAt"), Index("isFavorite")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** ExerciseCategory id: CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, CARDIO, FULL_BODY */
    val category: String,
    /** Equipment id: BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BAND, CARDIO, OTHER */
    val equipment: String,
    val primaryMuscle: String,

    val tracksWeight: Boolean = true,
    val tracksReps: Boolean = true,
    val tracksDuration: Boolean = false,
    val tracksDistance: Boolean = false,

    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0L
)

/** One trip to the gym. */
@Entity(tableName = "workout_sessions", indices = [Index("date"), Index("startedAt")])
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val startedAt: Long,
    /** Null while the session is still in progress. */
    val endedAt: Long? = null,
    val name: String,
    val notes: String? = null
)

/**
 * One set. [exerciseName] is denormalised on purpose — same reasoning as diary rows:
 * renaming or deleting an exercise must never rewrite what you actually lifted.
 *
 * No foreign key to the session: cascade is handled in the repository instead, which
 * keeps the v1 → v2 migration SQL as simple as possible.
 */
@Entity(
    tableName = "workout_sets",
    indices = [Index("sessionId"), Index("exerciseId"), Index("timestamp")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setNumber: Int,

    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,

    /** Rate of perceived exertion, 1-10. Optional. */
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val notes: String? = null,
    val timestamp: Long = 0L
) {
    /** Tonnage for this set. Warm-ups are excluded from training volume by convention. */
    val volumeKg: Double get() = if (isWarmup) 0.0 else weightKg * reps

    /**
     * Estimated one-rep max, Epley formula. Meaningless above ~12 reps, so we cap it
     * rather than reporting a confident-looking number that isn't.
     */
    val estimatedOneRepMax: Double
        get() = when {
            reps <= 0 || weightKg <= 0.0 -> 0.0
            reps == 1 -> weightKg
            reps > 12 -> 0.0
            else -> weightKg * (1 + reps / 30.0)
        }
}

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val sets: List<WorkoutSetEntity>
) {
    val workingSets: List<WorkoutSetEntity> get() = sets.filterNot { it.isWarmup }
    val totalVolumeKg: Double get() = sets.sumOf { it.volumeKg }
    val totalReps: Int get() = workingSets.sumOf { it.reps }
    val exerciseNames: List<String> get() = sets.map { it.exerciseName }.distinct()
    val durationMinutes: Long?
        get() = session.endedAt?.let { (it - session.startedAt) / 60_000 }
}

// ---------------------------------------------------------------- projections

/** One point on a per-exercise progression chart. */
data class ExerciseProgressRow(
    val date: String,
    val bestWeightKg: Double,
    val bestEstimatedOneRepMax: Double,
    val totalVolumeKg: Double,
    val totalReps: Int
)

/** Training volume per day, for the weekly bar chart. */
data class WorkoutVolumeRow(
    val date: String,
    val volumeKg: Double,
    val setCount: Int
)

/** A personal record for one exercise. */
data class PersonalRecordRow(
    val exerciseId: Long,
    val exerciseName: String,
    val bestWeightKg: Double,
    val repsAtBest: Int,
    val bestEstimatedOneRepMax: Double,
    val achievedAt: Long
)
