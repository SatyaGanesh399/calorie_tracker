package com.satya.calorietracker.data.remote.openfoodfacts

import android.content.Context
import com.satya.calorietracker.data.remote.FailureReason
import com.satya.calorietracker.data.remote.FoodDataProvider
import com.satya.calorietracker.data.remote.NetworkModule
import com.satya.calorietracker.data.remote.NetworkMonitor
import com.satya.calorietracker.data.remote.ProviderResult
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.FoodSource
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.domain.model.Nutrients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Open Food Facts — an open, crowd-sourced product database with the best barcode
 * coverage of any free source and no key requirement. This is the default provider.
 */
class OpenFoodFactsProvider(
    context: Context,
    private val networkMonitor: NetworkMonitor
) : FoodDataProvider {

    private val appContext = context.applicationContext

    private val api: OpenFoodFactsApi by lazy {
        NetworkModule.retrofit(appContext, OpenFoodFactsApi.BASE_URL)
            .create(OpenFoodFactsApi::class.java)
    }

    override val id = "open_food_facts"
    override val displayName = "Open Food Facts"
    override val description =
        "Free, open, community-maintained product database. No account or key needed. Best for packaged foods and barcodes."
    override val requiresNetwork = true
    override val supportsBarcode = true

    override suspend fun search(query: String, page: Int): ProviderResult<List<Food>> {
        if (query.isBlank()) return ProviderResult.Empty
        if (!networkMonitor.isOnline()) return ProviderResult.Offline

        return runCatchingProvider {
            val response = withContext(Dispatchers.IO) {
                api.search(terms = query.trim(), page = page)
            }
            val foods = response.products.mapNotNull { it.toFood() }
            if (foods.isEmpty()) ProviderResult.Empty else ProviderResult.Success(foods)
        }
    }

    override suspend fun lookupBarcode(barcode: String): ProviderResult<Food?> {
        if (barcode.isBlank()) return ProviderResult.Empty
        if (!networkMonitor.isOnline()) return ProviderResult.Offline

        return runCatchingProvider {
            val response = withContext(Dispatchers.IO) { api.product(barcode.trim()) }
            val food = response.product?.toFood()
            if (response.status != 1 || food == null) ProviderResult.Empty
            else ProviderResult.Success(food)
        }
    }

    // ------------------------------------------------------------- internals

    private inline fun <T> runCatchingProvider(block: () -> ProviderResult<T>): ProviderResult<T> =
        try {
            block()
        } catch (e: SocketTimeoutException) {
            ProviderResult.Failure(FailureReason.TIMEOUT, e.message.orEmpty())
        } catch (e: IOException) {
            // No route, DNS failure, connection dropped mid-flight.
            if (!networkMonitor.isOnline()) ProviderResult.Offline
            else ProviderResult.Failure(FailureReason.SERVER, e.message.orEmpty())
        } catch (e: HttpException) {
            val reason = when (e.code()) {
                401, 403 -> FailureReason.UNAUTHORIZED
                429 -> FailureReason.RATE_LIMITED
                in 500..599 -> FailureReason.SERVER
                else -> FailureReason.UNKNOWN
            }
            ProviderResult.Failure(reason, "HTTP ${e.code()}")
        } catch (e: Exception) {
            // Malformed JSON, unexpected types — never let a bad payload crash the app.
            ProviderResult.Failure(FailureReason.PARSE, e.message.orEmpty())
        }

    private fun OffProduct.toFood(): Food? {
        val label = listOfNotNull(productNameEn, productName, genericName)
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: return null

        val n = nutriments ?: return null

        val kcal = n.energyKcal100g
            ?: n.energyKj100g?.let { it / KJ_PER_KCAL }
            ?: n.energy100g?.let { it / KJ_PER_KCAL }
            ?: 0.0

        // OFF stores sodium in grams; we keep milligrams. Fall back to salt / 2.5.
        val sodiumMg = n.sodium100g?.times(1000)
            ?: n.salt100g?.div(2.5)?.times(1000)
            ?: 0.0

        val nutrients = Nutrients(
            calories = kcal,
            protein = n.proteins100g ?: 0.0,
            carbs = n.carbohydrates100g ?: 0.0,
            fat = n.fat100g ?: 0.0,
            fiber = n.fiber100g ?: 0.0,
            sugar = n.sugars100g ?: 0.0,
            sodium = sodiumMg
        ).withDerivedCaloriesIfMissing()

        // Everything zero means the product exists but nobody has filled in nutrition.
        if (nutrients.calories <= 0.0 && nutrients.protein <= 0.0 &&
            nutrients.carbs <= 0.0 && nutrients.fat <= 0.0
        ) return null

        val isLiquid = looksLikeLiquid()

        return Food(
            name = label.take(120),
            brand = brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() },
            barcode = code,
            sourceId = FoodSource.OPEN_FOOD_FACTS.id,
            providerId = id,
            per = 100.0,
            perUnitId = if (isLiquid) MeasureUnit.MILLILITRE.id else MeasureUnit.GRAM.id,
            nutrients = nutrients,
            servingSize = servingQuantity?.takeIf { it > 0 },
            servingLabel = servingSize?.trim()?.takeIf { it.isNotBlank() },
            imageUrl = imageFrontSmallUrl ?: imageSmallUrl
        )
    }

    private fun OffProduct.looksLikeLiquid(): Boolean {
        val unit = productQuantityUnit?.lowercase()?.trim()
        if (unit == "ml" || unit == "l" || unit == "cl") return true
        val hints = listOfNotNull(quantity, servingSize).joinToString(" ").lowercase()
        return Regex("\\b\\d+\\s*(ml|cl|l|litre|liter)\\b").containsMatchIn(hints)
    }

    private companion object {
        const val KJ_PER_KCAL = 4.184
    }
}
