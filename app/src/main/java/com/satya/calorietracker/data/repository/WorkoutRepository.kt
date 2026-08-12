package com.satya.calorietracker.data.repository

import com.satya.calorietracker.data.db.ExerciseDao
import com.satya.calorietracker.data.db.ExerciseEntity
import com.satya.calorietracker.data.db.ExerciseProgressRow
import com.satya.calorietracker.data.db.PersonalRecordRow
import com.satya.calorietracker.data.db.SessionWithSets
import com.satya.calorietracker.data.db.WorkoutDao
import com.satya.calorietracker.data.db.WorkoutSessionEntity
import com.satya.calorietracker.data.db.WorkoutSetEntity
import com.satya.calorietracker.data.db.WorkoutVolumeRow
import com.satya.calorietracker.domain.model.Equipment
import com.satya.calorietracker.domain.model.ExerciseCategory
import com.satya.calorietracker.domain.model.SetInputMode
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Which numbers the set editor should ask for. */
val ExerciseEntity.inputMode: SetInputMode
    get() = when {
        tracksDistance -> SetInputMode.DURATION_DISTANCE
        tracksDuration -> SetInputMode.DURATION
        !tracksWeight -> SetInputMode.REPS_ONLY
        else -> SetInputMode.WEIGHT_REPS
    }

// Deliberately not named `category` / `equipment`: members always win over extensions
// in Kotlin, so same-named extensions here would silently never be called.
val ExerciseEntity.categoryType: ExerciseCategory get() = ExerciseCategory.fromId(category)
val ExerciseEntity.equipmentType: Equipment get() = Equipment.fromId(equipment)

/** "Barbell · Chest" subtitle for the picker. */
val ExerciseEntity.subtitle: String
    get() = "${Equipment.fromId(equipment).label} · $primaryMuscle"

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val notifier: DataChangeNotifier = DataChangeNotifier.NONE
) {

    // ------------------------------------------------------------ sessions

    fun observeActiveSession(): Flow<SessionWithSets?> = workoutDao.observeActiveSession()

    fun observeSessionsForDate(date: LocalDate): Flow<List<SessionWithSets>> =
        workoutDao.observeSessionsForDate(DateUtils.iso(date))

    fun observeRecentSessions(limit: Int = 30): Flow<List<SessionWithSets>> =
        workoutDao.observeRecentSessions(limit)

    fun observeSession(id: Long): Flow<SessionWithSets?> = workoutDao.observeSession(id)

    fun observeWorkoutDates(start: LocalDate, end: LocalDate): Flow<List<String>> =
        workoutDao.observeWorkoutDates(DateUtils.iso(start), DateUtils.iso(end))

    fun observeSessionCount(start: LocalDate, end: LocalDate): Flow<Int> =
        workoutDao.observeSessionCount(DateUtils.iso(start), DateUtils.iso(end))

    suspend fun getSession(id: Long): SessionWithSets? = workoutDao.getSession(id)

    /**
     * Starts a workout. Only one can be open at a time — if a previous session was left
     * running (app killed, phone died), it's closed out first rather than leaving two
     * "active" workouts that would both claim new sets.
     */
    suspend fun startSession(
        name: String = defaultSessionName(),
        date: LocalDate = DateUtils.today()
    ): Long {
        closeAbandonedSessions()
        val id = workoutDao.insertSession(
            WorkoutSessionEntity(
                date = DateUtils.iso(date),
                startedAt = DateUtils.nowMillis(),
                name = name.ifBlank { defaultSessionName() }
            )
        )
        notifier.onDataChanged()
        return id
    }

    suspend fun finishSession(sessionId: Long) {
        val existing = workoutDao.getSession(sessionId)?.session ?: return
        workoutDao.updateSession(existing.copy(endedAt = DateUtils.nowMillis()))
        notifier.onDataChanged()
    }

    suspend fun renameSession(sessionId: Long, name: String, notes: String?) {
        val existing = workoutDao.getSession(sessionId)?.session ?: return
        workoutDao.updateSession(
            existing.copy(name = name.ifBlank { existing.name }, notes = notes?.takeIf { it.isNotBlank() })
        )
    }

    /** No FK cascade on workout_sets, so the sets are removed explicitly here. */
    suspend fun deleteSession(sessionId: Long) {
        workoutDao.deleteSetsForSession(sessionId)
        workoutDao.deleteSessionById(sessionId)
        notifier.onDataChanged()
    }

    private suspend fun closeAbandonedSessions() {
        val stale = workoutDao.allSessions().filter { it.session.endedAt == null }
        stale.forEach { s ->
            // Close it at the timestamp of its last set, or its start if it was empty.
            val lastActivity = s.sets.maxOfOrNull { it.timestamp } ?: s.session.startedAt
            workoutDao.updateSession(s.session.copy(endedAt = lastActivity))
        }
    }

    // ---------------------------------------------------------------- sets

    suspend fun addSet(
        sessionId: Long,
        exercise: ExerciseEntity,
        weightKg: Double,
        reps: Int,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
        rpe: Double? = null,
        isWarmup: Boolean = false,
        notes: String? = null
    ): Long {
        val existing = workoutDao.setsForSession(sessionId)
        val setNumber = existing.count { it.exerciseId == exercise.id } + 1

        val id = workoutDao.insertSet(
            WorkoutSetEntity(
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setNumber = setNumber,
                weightKg = weightKg,
                reps = reps,
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
                rpe = rpe,
                isWarmup = isWarmup,
                notes = notes,
                timestamp = DateUtils.nowMillis()
            )
        )
        exerciseDao.markUsed(exercise.id, DateUtils.nowMillis())
        notifier.onDataChanged()
        return id
    }

    suspend fun updateSet(set: WorkoutSetEntity) {
        workoutDao.updateSet(set)
        notifier.onDataChanged()
    }

    suspend fun deleteSet(setId: Long) {
        workoutDao.deleteSetById(setId)
        notifier.onDataChanged()
    }

    /**
     * The set to pre-fill when you add this exercise again. Repeating last session's
     * numbers is the common case, so the editor opens with them already typed in.
     */
    suspend fun lastPerformance(exerciseId: Long): WorkoutSetEntity? =
        workoutDao.lastSetsFor(exerciseId, 1).firstOrNull()

    suspend fun recentSetsFor(exerciseId: Long, limit: Int = 5): List<WorkoutSetEntity> =
        workoutDao.lastSetsFor(exerciseId, limit)

    // ----------------------------------------------------------- exercises

    suspend fun searchExercises(query: String, limit: Int = 50): List<ExerciseEntity> =
        if (query.isBlank()) exerciseDao.byCategory("ALL", limit)
        else exerciseDao.search(query.trim(), limit)

    suspend fun exercisesByCategory(category: String, limit: Int = 200): List<ExerciseEntity> =
        exerciseDao.byCategory(category, limit)

    suspend fun recentExercises(limit: Int = 12): List<ExerciseEntity> = exerciseDao.recent(limit)

    suspend fun getExercise(id: Long): ExerciseEntity? = exerciseDao.getById(id)

    fun observeAllExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    fun observeFavoriteExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeFavorites()

    suspend fun setExerciseFavorite(id: Long, favorite: Boolean) =
        exerciseDao.setFavorite(id, favorite)

    suspend fun createCustomExercise(
        name: String,
        category: ExerciseCategory,
        equipment: Equipment,
        primaryMuscle: String,
        tracksWeight: Boolean = true,
        tracksReps: Boolean = true,
        tracksDuration: Boolean = false,
        tracksDistance: Boolean = false
    ): Long = exerciseDao.insert(
        ExerciseEntity(
            name = name.trim(),
            category = category.id,
            equipment = equipment.id,
            primaryMuscle = primaryMuscle.trim().ifBlank { category.label },
            tracksWeight = tracksWeight,
            tracksReps = tracksReps,
            tracksDuration = tracksDuration,
            tracksDistance = tracksDistance,
            isCustom = true,
            createdAt = DateUtils.nowMillis()
        )
    )

    suspend fun deleteCustomExercise(id: Long) = exerciseDao.deleteCustom(id)

    // ----------------------------------------------------------- analytics

    suspend fun progressFor(
        exerciseId: Long,
        start: LocalDate,
        end: LocalDate = DateUtils.today()
    ): List<ExerciseProgressRow> =
        workoutDao.progressFor(exerciseId, DateUtils.iso(start), DateUtils.iso(end))

    suspend fun volumeBetween(start: LocalDate, end: LocalDate = DateUtils.today()): List<WorkoutVolumeRow> =
        workoutDao.volumeBetween(DateUtils.iso(start), DateUtils.iso(end))

    fun observePersonalRecords(): Flow<List<PersonalRecordRow>> = workoutDao.observePersonalRecords()

    suspend fun sessionsBetween(start: LocalDate, end: LocalDate): List<SessionWithSets> =
        workoutDao.sessionsBetween(DateUtils.iso(start), DateUtils.iso(end))

    /** Exercises you've actually trained, for the progression picker. */
    suspend fun trainedExercises(): List<ExerciseEntity> =
        exerciseDao.getAll().filter { it.useCount > 0 }.sortedByDescending { it.useCount }

    // -------------------------------------------------------------- backup

    suspend fun exportSessions(): List<SessionWithSets> = workoutDao.allSessions()

    suspend fun exportCustomExercises(): List<ExerciseEntity> =
        exerciseDao.getAll().filter { it.isCustom }

    suspend fun importSessions(sessions: List<WorkoutSessionEntity>, sets: List<WorkoutSetEntity>) {
        workoutDao.insertSessions(sessions)
        workoutDao.insertSets(sets)
        notifier.onDataChanged()
    }

    suspend fun clearAll() {
        workoutDao.deleteAllSets()
        workoutDao.deleteAllSessions()
        notifier.onDataChanged()
    }

    private fun defaultSessionName(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 4..11 -> "Morning workout"
            in 12..16 -> "Afternoon workout"
            in 17..21 -> "Evening workout"
            else -> "Workout"
        }
    }
}
