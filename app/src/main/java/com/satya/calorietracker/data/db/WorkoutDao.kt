package com.satya.calorietracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT name FROM exercises WHERE isCustom = 0")
    suspend fun seededNames(): List<String>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    /**
     * Type-ahead search. Exercises whose name *starts* with the query rank above ones
     * that merely contain it, so typing "bench" surfaces "Bench press" before
     * "Close-grip bench press"; recently used and favourited exercises float up within
     * each of those groups.
     */
    @Query(
        """
        SELECT * FROM exercises
        WHERE name LIKE '%' || :query || '%'
           OR primaryMuscle LIKE '%' || :query || '%'
           OR equipment LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            isFavorite DESC,
            useCount DESC,
            name ASC
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int): List<ExerciseEntity>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:category = 'ALL' OR category = :category)
        ORDER BY isFavorite DESC, useCount DESC, name ASC
        LIMIT :limit
        """
    )
    suspend fun byCategory(category: String, limit: Int): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: Long)

    @Query("UPDATE exercises SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE exercises SET useCount = useCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markUsed(id: Long, timestamp: Long)

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

@Dao
interface WorkoutDao {

    // ------------------------------------------------------------- sessions

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionWithSets?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionWithSets?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY startedAt DESC")
    fun observeSessionsForDate(date: String): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :start AND :end ORDER BY startedAt DESC")
    suspend fun sessionsBetween(start: String, end: String): List<SessionWithSets>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt ASC")
    suspend fun allSessions(): List<SessionWithSets>

    /** The session still running, if any — there can only be one. */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionWithSets?>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE date BETWEEN :start AND :end")
    fun observeSessionCount(start: String, end: String): Flow<Int>

    @Query("SELECT DISTINCT date FROM workout_sessions WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeWorkoutDates(start: String, end: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSessionEntity>)

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    // ----------------------------------------------------------------- sets

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY timestamp ASC, setNumber ASC")
    suspend fun setsForSession(sessionId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets ORDER BY timestamp ASC")
    suspend fun allSets(): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSetById(id: Long)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Long)

    /** The last time you did this exercise — used to pre-fill the set editor. */
    @Query(
        """
        SELECT * FROM workout_sets
        WHERE exerciseId = :exerciseId AND isWarmup = 0
        ORDER BY timestamp DESC LIMIT :limit
        """
    )
    suspend fun lastSetsFor(exerciseId: Long, limit: Int): List<WorkoutSetEntity>

    // ------------------------------------------------------------ analytics

    @Query(
        """
        SELECT s.date AS date,
               MAX(w.weightKg) AS bestWeightKg,
               MAX(CASE WHEN w.reps BETWEEN 1 AND 12 AND w.weightKg > 0
                        THEN w.weightKg * (1 + w.reps / 30.0) ELSE 0 END) AS bestEstimatedOneRepMax,
               IFNULL(SUM(CASE WHEN w.isWarmup = 0 THEN w.weightKg * w.reps ELSE 0 END), 0) AS totalVolumeKg,
               IFNULL(SUM(CASE WHEN w.isWarmup = 0 THEN w.reps ELSE 0 END), 0) AS totalReps
        FROM workout_sets w
        INNER JOIN workout_sessions s ON s.id = w.sessionId
        WHERE w.exerciseId = :exerciseId AND s.date BETWEEN :start AND :end
        GROUP BY s.date
        ORDER BY s.date ASC
        """
    )
    suspend fun progressFor(exerciseId: Long, start: String, end: String): List<ExerciseProgressRow>

    @Query(
        """
        SELECT s.date AS date,
               IFNULL(SUM(CASE WHEN w.isWarmup = 0 THEN w.weightKg * w.reps ELSE 0 END), 0) AS volumeKg,
               COUNT(w.id) AS setCount
        FROM workout_sets w
        INNER JOIN workout_sessions s ON s.id = w.sessionId
        WHERE s.date BETWEEN :start AND :end
        GROUP BY s.date
        ORDER BY s.date ASC
        """
    )
    suspend fun volumeBetween(start: String, end: String): List<WorkoutVolumeRow>

    /**
     * Heaviest set per exercise, with the reps achieved at that weight.
     * The inner query finds the max weight; the outer one recovers the row it came from.
     */
    @Query(
        """
        SELECT w.exerciseId AS exerciseId,
               w.exerciseName AS exerciseName,
               w.weightKg AS bestWeightKg,
               w.reps AS repsAtBest,
               MAX(CASE WHEN w.reps BETWEEN 1 AND 12 AND w.weightKg > 0
                        THEN w.weightKg * (1 + w.reps / 30.0) ELSE w.weightKg END) AS bestEstimatedOneRepMax,
               w.timestamp AS achievedAt
        FROM workout_sets w
        WHERE w.isWarmup = 0 AND w.weightKg > 0
        GROUP BY w.exerciseId
        ORDER BY w.exerciseName ASC
        """
    )
    fun observePersonalRecords(): Flow<List<PersonalRecordRow>>

    @Query("SELECT COUNT(*) FROM workout_sets WHERE exerciseId = :exerciseId")
    suspend fun setCountFor(exerciseId: Long): Int

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAllSets()
}
