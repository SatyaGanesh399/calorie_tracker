package com.satya.calorietracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeBetween(start: String, end: String): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntryEntity?>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): WeightEntryEntity?

    @Query("SELECT * FROM weight_entries ORDER BY date ASC LIMIT 1")
    fun observeFirst(): Flow<WeightEntryEntity?>

    @Query("SELECT * FROM weight_entries WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): WeightEntryEntity?

    /** The most recent entry on or before [date] — used for "weight a week ago". */
    @Query("SELECT * FROM weight_entries WHERE date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun getOnOrBefore(date: String): WeightEntryEntity?

    @Query("SELECT * FROM weight_entries ORDER BY date ASC")
    suspend fun getAll(): List<WeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightEntryEntity>)

    @Update
    suspend fun update(entry: WeightEntryEntity)

    @Delete
    suspend fun delete(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAll()
}

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_entries WHERE date = :date ORDER BY timestamp ASC")
    fun observeForDate(date: String): Flow<List<WaterEntryEntity>>

    @Query("SELECT IFNULL(SUM(amountMl), 0) FROM water_entries WHERE date = :date")
    fun observeTotalForDate(date: String): Flow<Double>

    @Query("SELECT IFNULL(SUM(amountMl), 0) FROM water_entries WHERE date = :date")
    suspend fun getTotalForDate(date: String): Double

    @Query(
        """
        SELECT date AS date, IFNULL(SUM(amountMl), 0) AS amount
        FROM water_entries
        WHERE date BETWEEN :start AND :end
        GROUP BY date ORDER BY date ASC
        """
    )
    fun observeTotalsBetween(start: String, end: String): Flow<List<DateAmountRow>>

    @Query(
        """
        SELECT date AS date, IFNULL(SUM(amountMl), 0) AS amount
        FROM water_entries
        WHERE date BETWEEN :start AND :end
        GROUP BY date ORDER BY date ASC
        """
    )
    suspend fun getTotalsBetween(start: String, end: String): List<DateAmountRow>

    @Query("SELECT * FROM water_entries ORDER BY timestamp ASC")
    suspend fun getAll(): List<WaterEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaterEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WaterEntryEntity>)

    @Delete
    suspend fun delete(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Undo the last glass — the most common correction. */
    @Query("DELETE FROM water_entries WHERE id = (SELECT id FROM water_entries WHERE date = :date ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLastForDate(date: String)

    @Query("DELETE FROM water_entries")
    suspend fun deleteAll()
}
