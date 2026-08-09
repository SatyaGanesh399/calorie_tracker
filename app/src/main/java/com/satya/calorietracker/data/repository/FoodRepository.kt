package com.satya.calorietracker.data.repository

import com.satya.calorietracker.data.db.FoodDao
import com.satya.calorietracker.data.db.toDomain
import com.satya.calorietracker.data.db.toEntity
import com.satya.calorietracker.data.remote.AggregatedSearch
import com.satya.calorietracker.data.remote.BarcodeLookup
import com.satya.calorietracker.data.remote.FoodProviderRegistry
import com.satya.calorietracker.data.remote.ProviderStatus
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.FoodSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Owns the food catalog: what's on the phone, what the providers know, and the caching
 * policy that connects the two.
 */
class FoodRepository(
    private val foodDao: FoodDao,
    private val registry: FoodProviderRegistry
) {

    fun observeFavorites(): Flow<List<Food>> =
        foodDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    fun observeRecent(limit: Int = 40): Flow<List<Food>> =
        foodDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    fun observeCustom(): Flow<List<Food>> =
        foodDao.observeCustom().map { list -> list.map { it.toDomain() } }

    fun observeCustomCount(): Flow<Int> = foodDao.observeCustomCount()

    suspend fun getById(id: Long): Food? = foodDao.getById(id)?.toDomain()

    fun providerStatuses(): List<ProviderStatus> = registry.statuses()

    // -------------------------------------------------------------- search

    /** Instant, local-only. Used for the first paint while the network call is in flight. */
    suspend fun searchLocal(query: String): List<Food> = registry.searchLocal(query)

    /**
     * Full search. Remote hits are written into the local catalog straight away, so the
     * second time you look for the same thing it resolves offline and for free.
     */
    suspend fun search(query: String): AggregatedSearch {
        val result = registry.search(query)
        val persisted = result.foods.map { food ->
            if (food.id == 0L && food.providerId != null && food.providerId != "local") {
                cacheRemote(food)
            } else {
                food
            }
        }
        return result.copy(foods = persisted)
    }

    // ------------------------------------------------------------- barcode

    suspend fun lookupBarcode(barcode: String): BarcodeLookup {
        val result = registry.lookupBarcode(barcode)
        return if (result is BarcodeLookup.Found && !result.fromCache) {
            BarcodeLookup.Found(cacheRemote(result.food), fromCache = false)
        } else {
            result
        }
    }

    /** Persist an API result so it survives the next flight-mode moment. */
    private suspend fun cacheRemote(food: Food): Food {
        val ref = food.barcode ?: "${food.providerId}:${food.name.lowercase()}:${food.brand.orEmpty().lowercase()}"
        val existing = food.providerId?.let { foodDao.getByProviderRef(it, ref) }
        if (existing != null) return existing.toDomain()

        val now = System.currentTimeMillis()
        val id = foodDao.insert(food.toEntity(providerRef = ref, cachedAt = now).copy(createdAt = now))
        return food.copy(id = id)
    }

    // -------------------------------------------------------- custom foods

    suspend fun saveCustomFood(food: Food): Long {
        val now = System.currentTimeMillis()
        val entity = food
            .copy(
                sourceId = FoodSource.CUSTOM.id,
                isCustom = true,
                providerId = null,
                createdAt = if (food.createdAt == 0L) now else food.createdAt
            )
            .toEntity(providerRef = null)
            .copy(providerRef = null)
        return if (food.id == 0L) foodDao.insert(entity) else {
            foodDao.update(entity)
            food.id
        }
    }

    suspend fun deleteFood(id: Long) = foodDao.deleteById(id)

    suspend fun setFavorite(id: Long, favorite: Boolean) = foodDao.setFavorite(id, favorite)

    suspend fun setPinned(id: Long, pinned: Boolean) = foodDao.setPinned(id, pinned)

    suspend fun markUsed(id: Long) = foodDao.markUsed(id, System.currentTimeMillis())

    /**
     * Make sure a food exists locally before it is referenced by a diary row, so history
     * never points at something that isn't there.
     */
    suspend fun ensurePersisted(food: Food): Food =
        if (food.id != 0L) food else cacheRemote(food)

    // ------------------------------------------------------------ upkeep

    /** Called occasionally by the maintenance worker. Never touches your own foods. */
    suspend fun pruneCache(maxAgeDays: Long = 90): Int {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxAgeDays)
        return foodDao.pruneStaleCache(cutoff)
    }

    suspend fun countAll(): Int = foodDao.count()
}
