package com.satya.calorietracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    // ------------------------------------------------------------- reads

    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY timestamp ASC")
    fun observeForDate(date: String): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getForDate(date: String): List<LogEntryEntity>

    @Query("SELECT * FROM log_entries WHERE id = :id")
    suspend fun getById(id: Long): LogEntryEntity?

    @Query(
        """
        SELECT :date AS date,
               IFNULL(SUM(calories), 0) AS calories,
               IFNULL(SUM(protein), 0)  AS protein,
               IFNULL(SUM(carbs), 0)    AS carbs,
               IFNULL(SUM(fat), 0)      AS fat,
               IFNULL(SUM(fiber), 0)    AS fiber,
               IFNULL(SUM(sugar), 0)    AS sugar,
               IFNULL(SUM(sodium), 0)   AS sodium,
               COUNT(*)                 AS entryCount
        FROM log_entries WHERE date = :date
        """
    )
    fun observeDailyTotals(date: String): Flow<DailyTotalsRow>

    @Query(
        """
        SELECT :date AS date,
               IFNULL(SUM(calories), 0) AS calories,
               IFNULL(SUM(protein), 0)  AS protein,
               IFNULL(SUM(carbs), 0)    AS carbs,
               IFNULL(SUM(fat), 0)      AS fat,
               IFNULL(SUM(fiber), 0)    AS fiber,
               IFNULL(SUM(sugar), 0)    AS sugar,
               IFNULL(SUM(sodium), 0)   AS sodium,
               COUNT(*)                 AS entryCount
        FROM log_entries WHERE date = :date
        """
    )
    suspend fun getDailyTotals(date: String): DailyTotalsRow

    @Query(
        """
        SELECT date AS date,
               IFNULL(SUM(calories), 0) AS calories,
               IFNULL(SUM(protein), 0)  AS protein,
               IFNULL(SUM(carbs), 0)    AS carbs,
               IFNULL(SUM(fat), 0)      AS fat,
               IFNULL(SUM(fiber), 0)    AS fiber,
               IFNULL(SUM(sugar), 0)    AS sugar,
               IFNULL(SUM(sodium), 0)   AS sodium,
               COUNT(*)                 AS entryCount
        FROM log_entries
        WHERE date BETWEEN :start AND :end
        GROUP BY date
        ORDER BY date ASC
        """
    )
    fun observeTotalsBetween(start: String, end: String): Flow<List<DailyTotalsRow>>

    @Query(
        """
        SELECT date AS date,
               IFNULL(SUM(calories), 0) AS calories,
               IFNULL(SUM(protein), 0)  AS protein,
               IFNULL(SUM(carbs), 0)    AS carbs,
               IFNULL(SUM(fat), 0)      AS fat,
               IFNULL(SUM(fiber), 0)    AS fiber,
               IFNULL(SUM(sugar), 0)    AS sugar,
               IFNULL(SUM(sodium), 0)   AS sodium,
               COUNT(*)                 AS entryCount
        FROM log_entries
        WHERE date BETWEEN :start AND :end
        GROUP BY date
        ORDER BY date ASC
        """
    )
    suspend fun getTotalsBetween(start: String, end: String): List<DailyTotalsRow>

    @Query(
        """
        SELECT mealTypeId AS mealTypeId,
               customMealName AS customMealName,
               IFNULL(SUM(calories), 0) AS calories,
               COUNT(*) AS entryCount
        FROM log_entries
        WHERE date = :date
        GROUP BY mealTypeId, customMealName
        """
    )
    fun observeMealTotals(date: String): Flow<List<MealTotalsRow>>

    @Query("SELECT DISTINCT date FROM log_entries WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeLoggedDates(start: String, end: String): Flow<List<String>>

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    suspend fun getAll(): List<LogEntryEntity>

    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun count(): Int

    @Query("SELECT MIN(date) FROM log_entries")
    suspend fun earliestDate(): String?

    /** Most frequently logged foods, for "Recent" and one-tap repeats. */
    @Query(
        """
        SELECT * FROM log_entries
        WHERE foodId IS NOT NULL
        GROUP BY foodId
        ORDER BY MAX(timestamp) DESC
        LIMIT :limit
        """
    )
    fun observeRecentlyLogged(limit: Int): Flow<List<LogEntryEntity>>

    // ------------------------------------------------------------ writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LogEntryEntity>)

    @Update
    suspend fun update(entry: LogEntryEntity)

    @Delete
    suspend fun delete(entry: LogEntryEntity)

    @Query("DELETE FROM log_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM log_entries WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Query("DELETE FROM log_entries")
    suspend fun deleteAll()
}
