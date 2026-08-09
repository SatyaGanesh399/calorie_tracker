package com.satya.calorietracker.data.remote

import com.satya.calorietracker.domain.model.Food

/**
 * The single seam between the app and any nutrition database.
 *
 * Nothing above this interface knows whether a food came from Open Food Facts, USDA,
 * the bundled catalog or a file on disk. Adding a provider means implementing this
 * and registering it in [FoodProviderRegistry] — no UI, ViewModel or DAO changes.
 */
interface FoodDataProvider {

    /** Stable id persisted alongside cached foods. Never change it for a live provider. */
    val id: String

    val displayName: String

    /** Shown in Settings > Food database. */
    val description: String

    val requiresNetwork: Boolean

    val supportsBarcode: Boolean

    /** False when the provider is switched off or missing its configuration (e.g. an API key). */
    fun isConfigured(): Boolean = true

    suspend fun search(query: String, page: Int = 1): ProviderResult<List<Food>>

    suspend fun lookupBarcode(barcode: String): ProviderResult<Food?>
}

/**
 * Explicit result type so callers can tell "nothing matched" apart from "we're offline"
 * apart from "the API is down" — each of which gets a different message in the UI.
 */
sealed interface ProviderResult<out T> {

    data class Success<T>(val data: T) : ProviderResult<T>

    /** The provider worked but had nothing for this query. */
    data object Empty : ProviderResult<Nothing>

    /** No usable network connection. */
    data object Offline : ProviderResult<Nothing>

    /** Provider rejected us — rate limit, bad key, 5xx, malformed payload, timeout. */
    data class Failure(val reason: FailureReason, val message: String) : ProviderResult<Nothing>

    companion object {
        fun <T> of(data: T?): ProviderResult<T> =
            if (data == null) Empty else Success(data)
    }
}

enum class FailureReason(val userMessage: String) {
    RATE_LIMITED("That food database is busy right now. Try again in a minute."),
    UNAUTHORIZED("This food database needs an API key. Add one in Settings > Food database."),
    SERVER("The food database isn't responding. Your saved foods still work."),
    TIMEOUT("The lookup took too long. Check your connection and try again."),
    PARSE("We got an answer we couldn't read. You can add this food manually."),
    UNKNOWN("Something went wrong looking that up. You can add this food manually.")
}

fun <T> ProviderResult<T>.dataOrNull(): T? = (this as? ProviderResult.Success)?.data

fun ProviderResult<*>.userMessage(): String? = when (this) {
    is ProviderResult.Failure -> reason.userMessage
    ProviderResult.Offline -> "You're offline. Showing your saved foods only."
    ProviderResult.Empty -> null
    is ProviderResult.Success -> null
}
