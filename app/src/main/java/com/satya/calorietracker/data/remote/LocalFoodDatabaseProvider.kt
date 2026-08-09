package com.satya.calorietracker.data.remote

import com.satya.calorietracker.data.db.FoodDao
import com.satya.calorietracker.data.db.toDomain
import com.satya.calorietracker.domain.model.Food

/**
 * The always-available provider: the on-device catalog (bundled seeds, cached API
 * results, custom foods). It is queried first for every search and every barcode,
 * which is what makes the app usable with the radio off.
 */
class LocalFoodDatabaseProvider(private val foodDao: FoodDao) : FoodDataProvider {

    override val id = "local"
    override val displayName = "On this phone"
    override val description =
        "Built-in food list plus everything you've created, favourited or looked up before. Always available, never needs a connection."
    override val requiresNetwork = false
    override val supportsBarcode = true

    override suspend fun search(query: String, page: Int): ProviderResult<List<Food>> {
        if (query.isBlank()) return ProviderResult.Empty
        return try {
            val results = foodDao.search(query.trim(), limit = 60).map { it.toDomain() }
            if (results.isEmpty()) ProviderResult.Empty else ProviderResult.Success(results)
        } catch (e: Exception) {
            ProviderResult.Failure(FailureReason.UNKNOWN, e.message.orEmpty())
        }
    }

    override suspend fun lookupBarcode(barcode: String): ProviderResult<Food?> = try {
        val hit = foodDao.getByBarcode(barcode.trim())?.toDomain()
        if (hit == null) ProviderResult.Empty else ProviderResult.Success(hit)
    } catch (e: Exception) {
        ProviderResult.Failure(FailureReason.UNKNOWN, e.message.orEmpty())
    }
}
