package com.satya.calorietracker.data.remote.usda

import android.content.Context
import com.satya.calorietracker.BuildConfig
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
 * Optional second provider. Disabled until a key is present in local.properties:
 *
 *     USDA_API_KEY=your_key_here
 *
 * Get one free at https://fdc.nal.usda.gov/api-key-signup.html.
 * See the README "Security" note about client-side keys before using a key you care about.
 */
class UsdaProvider(
    context: Context,
    private val networkMonitor: NetworkMonitor,
    private val apiKey: String = BuildConfig.USDA_API_KEY
) : FoodDataProvider {

    private val appContext = context.applicationContext

    private val api: UsdaApi by lazy {
        NetworkModule.retrofit(appContext, UsdaApi.BASE_URL).create(UsdaApi::class.java)
    }

    override val id = "usda_fdc"
    override val displayName = "USDA FoodData Central"
    override val description =
        "US government food composition database. Excellent for generic and whole foods. Needs a free API key."
    override val requiresNetwork = true
    override val supportsBarcode = true

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun search(query: String, page: Int): ProviderResult<List<Food>> {
        if (!isConfigured()) return ProviderResult.Failure(FailureReason.UNAUTHORIZED, "No USDA API key")
        if (query.isBlank()) return ProviderResult.Empty
        if (!networkMonitor.isOnline()) return ProviderResult.Offline

        return guarded {
            val response = withContext(Dispatchers.IO) {
                api.search(apiKey = apiKey, query = query.trim(), page = page)
            }
            val foods = response.foods.mapNotNull { it.toFood() }
            if (foods.isEmpty()) ProviderResult.Empty else ProviderResult.Success(foods)
        }
    }

    override suspend fun lookupBarcode(barcode: String): ProviderResult<Food?> {
        if (!isConfigured()) return ProviderResult.Failure(FailureReason.UNAUTHORIZED, "No USDA API key")
        if (!networkMonitor.isOnline()) return ProviderResult.Offline

        return guarded {
            val response = withContext(Dispatchers.IO) {
                api.search(apiKey = apiKey, query = barcode.trim(), pageSize = 5, dataType = "Branded")
            }
            val match = response.foods.firstOrNull { it.gtinUpc?.trimStart('0') == barcode.trimStart('0') }
                ?: response.foods.firstOrNull()
            val food = match?.toFood()
            if (food == null) ProviderResult.Empty else ProviderResult.Success(food)
        }
    }

    private inline fun <T> guarded(block: () -> ProviderResult<T>): ProviderResult<T> =
        try {
            block()
        } catch (e: SocketTimeoutException) {
            ProviderResult.Failure(FailureReason.TIMEOUT, e.message.orEmpty())
        } catch (e: IOException) {
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
            ProviderResult.Failure(FailureReason.PARSE, e.message.orEmpty())
        }

    private fun UsdaFood.toFood(): Food? {
        if (description.isBlank()) return null

        fun value(id: Int): Double = nutrients.firstOrNull { it.nutrientId == id }?.value ?: 0.0

        val nutrition = Nutrients(
            calories = value(UsdaApi.ENERGY_KCAL),
            protein = value(UsdaApi.PROTEIN),
            carbs = value(UsdaApi.CARBS),
            fat = value(UsdaApi.FAT),
            fiber = value(UsdaApi.FIBER),
            sugar = value(UsdaApi.SUGARS),
            sodium = value(UsdaApi.SODIUM)
        ).withDerivedCaloriesIfMissing()

        if (nutrition.calories <= 0.0 && nutrition.protein <= 0.0 &&
            nutrition.carbs <= 0.0 && nutrition.fat <= 0.0
        ) return null

        val liquid = servingSizeUnit?.lowercase() in setOf("ml", "mlt", "l")

        return Food(
            name = description.lowercase().replaceFirstChar { it.uppercase() }.take(120),
            brand = (brandName ?: brandOwner)?.trim()?.takeIf { it.isNotBlank() },
            barcode = gtinUpc?.takeIf { it.isNotBlank() },
            sourceId = FoodSource.USDA.id,
            providerId = id,
            per = 100.0,
            perUnitId = if (liquid) MeasureUnit.MILLILITRE.id else MeasureUnit.GRAM.id,
            nutrients = nutrition,
            servingSize = servingSize?.takeIf { it > 0 },
            servingLabel = householdServingFullText?.trim()?.takeIf { it.isNotBlank() }
        )
    }
}
