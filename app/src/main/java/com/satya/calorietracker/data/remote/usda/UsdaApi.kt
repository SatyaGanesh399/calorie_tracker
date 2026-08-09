package com.satya.calorietracker.data.remote.usda

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * USDA FoodData Central. Free, but needs an API key (data.gov key, issued instantly).
 * Strongest for generic / whole foods where Open Food Facts is thin.
 */
interface UsdaApi {

    @GET("fdc/v1/foods/search")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("pageNumber") page: Int = 1,
        @Query("pageSize") pageSize: Int = 25,
        @Query("dataType") dataType: String = "Foundation,SR Legacy,Branded"
    ): UsdaSearchResponse

    companion object {
        const val BASE_URL = "https://api.nal.usda.gov/"

        // FoodData Central nutrient ids
        const val ENERGY_KCAL = 1008
        const val PROTEIN = 1003
        const val FAT = 1004
        const val CARBS = 1005
        const val FIBER = 1079
        const val SUGARS = 2000
        const val SODIUM = 1093
    }
}

@Serializable
data class UsdaSearchResponse(
    val totalHits: Int = 0,
    val currentPage: Int = 1,
    val foods: List<UsdaFood> = emptyList()
)

@Serializable
data class UsdaFood(
    val fdcId: Long = 0L,
    val description: String = "",
    val dataType: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val gtinUpc: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val householdServingFullText: String? = null,
    @SerialName("foodNutrients") val nutrients: List<UsdaNutrient> = emptyList()
)

@Serializable
data class UsdaNutrient(
    val nutrientId: Int = 0,
    val nutrientName: String? = null,
    val unitName: String? = null,
    val value: Double? = null
)
