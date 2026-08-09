package com.satya.calorietracker.data.remote

import com.satya.calorietracker.domain.model.Food

/** What Settings > Food database shows for each provider. */
data class ProviderStatus(
    val id: String,
    val name: String,
    val description: String,
    val requiresNetwork: Boolean,
    val supportsBarcode: Boolean,
    val configured: Boolean,
    val enabled: Boolean,
    val reachable: Boolean
)

/** Combined outcome of asking every provider, so the UI can show partial success. */
data class AggregatedSearch(
    val foods: List<Food>,
    val localCount: Int,
    val remoteCount: Int,
    val message: String? = null,
    val offline: Boolean = false
)

/**
 * Fans a query out across the registered providers and merges the answers.
 *
 * Order matters: [local] always answers first so results appear instantly and so an
 * offline phone still gets something useful. Remote providers are then appended,
 * deduplicated against what we already have.
 */
class FoodProviderRegistry(
    private val local: LocalFoodDatabaseProvider,
    private val remote: List<FoodDataProvider>,
    private val networkMonitor: NetworkMonitor,
    private val enabledIds: () -> Set<String> = { remote.map { it.id }.toSet() }
) {

    val all: List<FoodDataProvider> get() = listOf(local) + remote

    fun activeRemote(): List<FoodDataProvider> {
        val enabled = enabledIds()
        return remote.filter { it.id in enabled && it.isConfigured() }
    }

    fun statuses(): List<ProviderStatus> {
        val enabled = enabledIds()
        val online = networkMonitor.isOnline()
        return all.map { p ->
            ProviderStatus(
                id = p.id,
                name = p.displayName,
                description = p.description,
                requiresNetwork = p.requiresNetwork,
                supportsBarcode = p.supportsBarcode,
                configured = p.isConfigured(),
                enabled = !p.requiresNetwork || p.id in enabled,
                reachable = !p.requiresNetwork || online
            )
        }
    }

    /** Local-only pass. Instant, no network, used for the first frame of every search. */
    suspend fun searchLocal(query: String): List<Food> =
        local.search(query).dataOrNull().orEmpty()

    /**
     * Full search. Never throws: any provider that fails is skipped and its message is
     * surfaced only if *nothing* else worked.
     */
    suspend fun search(query: String, page: Int = 1): AggregatedSearch {
        if (query.isBlank()) return AggregatedSearch(emptyList(), 0, 0)

        val localResults = searchLocal(query)
        val merged = localResults.toMutableList()
        val seen = localResults.mapTo(mutableSetOf()) { it.dedupeKey() }

        var failureMessage: String? = null
        var sawOffline = false
        var remoteCount = 0

        for (provider in activeRemote()) {
            when (val result = provider.search(query, page)) {
                is ProviderResult.Success -> {
                    result.data.forEach { food ->
                        if (seen.add(food.dedupeKey())) {
                            merged += food
                            remoteCount++
                        }
                    }
                }
                ProviderResult.Offline -> sawOffline = true
                ProviderResult.Empty -> Unit
                is ProviderResult.Failure -> failureMessage = failureMessage ?: result.reason.userMessage
            }
        }

        val message = when {
            sawOffline && localResults.isEmpty() -> "You're offline — no saved food matches \"$query\". Add it manually and it'll be there next time."
            sawOffline -> "Offline: showing foods saved on this phone."
            remoteCount == 0 && failureMessage != null && localResults.isEmpty() -> failureMessage
            else -> null
        }

        return AggregatedSearch(
            foods = merged,
            localCount = localResults.size,
            remoteCount = remoteCount,
            message = message,
            offline = sawOffline
        )
    }

    /**
     * Barcode lookup: cache first (free and instant, and works offline), then each
     * remote provider that supports barcodes.
     */
    suspend fun lookupBarcode(barcode: String): BarcodeLookup {
        local.lookupBarcode(barcode).dataOrNull()?.let {
            return BarcodeLookup.Found(it, fromCache = true)
        }

        var sawOffline = false
        var failure: String? = null

        for (provider in activeRemote().filter { it.supportsBarcode }) {
            when (val result = provider.lookupBarcode(barcode)) {
                is ProviderResult.Success -> result.data?.let {
                    return BarcodeLookup.Found(it, fromCache = false)
                }
                ProviderResult.Offline -> sawOffline = true
                ProviderResult.Empty -> Unit
                is ProviderResult.Failure -> failure = failure ?: result.reason.userMessage
            }
        }

        return when {
            sawOffline -> BarcodeLookup.Offline(barcode)
            failure != null -> BarcodeLookup.Error(barcode, failure)
            else -> BarcodeLookup.NotFound(barcode)
        }
    }
}

sealed interface BarcodeLookup {
    data class Found(val food: Food, val fromCache: Boolean) : BarcodeLookup
    data class NotFound(val barcode: String) : BarcodeLookup
    data class Offline(val barcode: String) : BarcodeLookup
    data class Error(val barcode: String, val message: String) : BarcodeLookup
}

/** Same barcode, or same name+brand, counts as the same food when merging providers. */
private fun Food.dedupeKey(): String =
    barcode?.takeIf { it.isNotBlank() }
        ?: (name.lowercase().trim() + "|" + (brand?.lowercase()?.trim().orEmpty()))
