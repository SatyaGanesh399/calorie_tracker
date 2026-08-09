package com.satya.calorietracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    // ------------------------------------------------------------- reads

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observeById(id: Long): Flow<FoodEntity?>

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE providerId = :providerId AND providerRef = :ref LIMIT 1")
    suspend fun getByProviderRef(providerId: String, ref: String): FoodEntity?

    /**
     * Local search. Ranks exact prefix matches first, then favourites, then most used.
     * `LIKE` with a leading wildcard cannot use the index, but the local catalog is
     * small (seeds + cached items) so this stays instant well past 10k rows.
     */
    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR barcode = :query
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            isPinned DESC,
            isFavorite DESC,
            useCount DESC,
            name ASC
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int): List<FoodEntity>

    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, isFavorite DESC, useCount DESC, name ASC
        LIMIT :limit
        """
    )
    fun observeSearch(query: String, limit: Int): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isFavorite = 1 ORDER BY isPinned DESC, useCount DESC, name ASC")
    fun observeFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isCustom = 1 ORDER BY name ASC")
    fun observeCustom(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods ORDER BY name ASC")
    suspend fun getAll(): List<FoodEntity>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM foods WHERE isCustom = 1")
    fun observeCustomCount(): Flow<Int>

    // ------------------------------------------------------------ writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(foods: List<FoodEntity>): List<Long>

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE foods SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE foods SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE foods SET useCount = useCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markUsed(id: Long, timestamp: Long)

    /** Drops cached API rows that are stale and were never used or favourited. */
    @Query(
        """
        DELETE FROM foods
        WHERE isCustom = 0 AND isFavorite = 0 AND lastUsedAt IS NULL
          AND providerId IS NOT NULL AND cachedAt > 0 AND cachedAt < :before
        """
    )
    suspend fun pruneStaleCache(before: Long): Int

    @Query("DELETE FROM foods")
    suspend fun deleteAll()
}
