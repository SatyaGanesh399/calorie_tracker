package com.satya.calorietracker.di

import android.content.Context
import com.satya.calorietracker.data.backup.BackupRepository
import com.satya.calorietracker.data.db.AppDatabase
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.prefs.UserPreferences
import com.satya.calorietracker.data.remote.FoodProviderRegistry
import com.satya.calorietracker.data.remote.LocalFoodDatabaseProvider
import com.satya.calorietracker.data.remote.NetworkMonitor
import com.satya.calorietracker.data.remote.openfoodfacts.OpenFoodFactsProvider
import com.satya.calorietracker.data.remote.usda.UsdaProvider
import com.satya.calorietracker.data.repository.DataChangeNotifier
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.data.repository.RecipeRepository
import com.satya.calorietracker.data.repository.StatsRepository
import com.satya.calorietracker.data.repository.WaterRepository
import com.satya.calorietracker.data.repository.WeightRepository
import com.satya.calorietracker.data.repository.WorkoutRepository
import com.satya.calorietracker.data.seed.SeedSync
import com.satya.calorietracker.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * Hand-rolled dependency container.
 *
 * A personal app with one process and no build variants doesn't need Hilt or Koin —
 * this is ~60 lines, has zero annotation processing, and every wiring decision is
 * visible in one place.
 */
class AppContainer(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.get(appContext, scope) }

    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(appContext) }

    /**
     * A hot snapshot of settings. The provider registry and the widgets need synchronous
     * access to "which providers are on", and blocking on DataStore for that would be silly.
     */
    val preferencesState: StateFlow<UserPreferences> by lazy {
        preferencesRepository.preferences.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences.DEFAULT
        )
    }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(appContext) }

    val widgetUpdater: WidgetUpdater by lazy { WidgetUpdater(appContext) }

    private val notifier: DataChangeNotifier by lazy {
        DataChangeNotifier { widgetUpdater.updateAll() }
    }

    // ------------------------------------------------------------- providers

    private val localProvider by lazy { LocalFoodDatabaseProvider(database.foodDao()) }

    private val remoteProviders by lazy {
        listOf(
            OpenFoodFactsProvider(appContext, networkMonitor),
            UsdaProvider(appContext, networkMonitor)
        )
    }

    val providerRegistry: FoodProviderRegistry by lazy {
        FoodProviderRegistry(
            local = localProvider,
            remote = remoteProviders,
            networkMonitor = networkMonitor,
            enabledIds = { preferencesState.value.enabledProviderIds }
        )
    }

    // ---------------------------------------------------------- repositories

    val foodRepository: FoodRepository by lazy {
        FoodRepository(database.foodDao(), providerRegistry)
    }

    val diaryRepository: DiaryRepository by lazy {
        DiaryRepository(database.logDao(), foodRepository, notifier)
    }

    val weightRepository: WeightRepository by lazy {
        WeightRepository(database.weightDao(), preferencesRepository, notifier)
    }

    val waterRepository: WaterRepository by lazy {
        WaterRepository(database.waterDao(), notifier)
    }

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepository(database.recipeDao(), notifier)
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepository(diaryRepository, waterRepository)
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), database.exerciseDao(), notifier)
    }

    /**
     * Tops up the bundled food and exercise catalogs on an app that was installed
     * before they grew. Safe to run on every launch — it only inserts what's missing.
     */
    val seedSync: SeedSync by lazy {
        SeedSync(database.foodDao(), database.exerciseDao())
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            database = database,
            foodDao = database.foodDao(),
            logDao = database.logDao(),
            weightDao = database.weightDao(),
            waterDao = database.waterDao(),
            recipeDao = database.recipeDao(),
            exerciseDao = database.exerciseDao(),
            workoutDao = database.workoutDao(),
            prefs = preferencesRepository,
            notifier = notifier
        )
    }
}
