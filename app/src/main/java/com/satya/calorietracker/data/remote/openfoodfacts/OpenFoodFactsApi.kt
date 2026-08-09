package com.satya.calorietracker.data.remote.openfoodfacts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts REST endpoints. No API key, no account, no rate-limit header —
 * they only ask that clients send a descriptive User-Agent, which NetworkModule does.
 */
interface OpenFoodFactsApi {

    @GET("api/v2/product/{barcode}.json")
    suspend fun product(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = FIELDS
    ): OffProductResponse

    @GET("cgi/search.pl")
    suspend fun search(
        @Query("search_terms") terms: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("fields") fields: String = FIELDS
    ): OffSearchResponse

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
        const val FIELDS =
            "code,product_name,product_name_en,generic_name,brands,quantity," +
                "serving_size,serving_quantity,product_quantity_unit," +
                "image_front_small_url,image_small_url,nutriments"
    }
}

@Serializable
data class OffProductResponse(
    val status: Int = 0,
    @SerialName("status_verbose") val statusVerbose: String? = null,
    val code: String? = null,
    val product: OffProduct? = null
)

@Serializable
data class OffSearchResponse(
    val count: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 0,
    val products: List<OffProduct> = emptyList()
)

@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_en") val productNameEn: String? = null,
    @SerialName("generic_name") val genericName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
    @SerialName("product_quantity_unit") val productQuantityUnit: String? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null,
    @SerialName("image_small_url") val imageSmallUrl: String? = null,
    val nutriments: OffNutriments? = null
)

/**
 * Only the per-100 g/ml fields are declared. Open Food Facts also ships `_serving`
 * and `_value` variants, which `ignoreUnknownKeys` discards.
 */
@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("energy_100g") val energy100g: Double? = null,
    @SerialName("energy-kj_100g") val energyKj100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("sugars_100g") val sugars100g: Double? = null,
    @SerialName("sodium_100g") val sodium100g: Double? = null,
    @SerialName("salt_100g") val salt100g: Double? = null
)
