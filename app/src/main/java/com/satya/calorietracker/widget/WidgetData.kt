package com.satya.calorietracker.widget

import android.content.Context
import com.satya.calorietracker.appContainer
import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.Nutrients
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.first

/** Everything any of the six widgets could need, fetched in one pass. */
data class WidgetSnapshot(
    val totals: Nutrients = Nutrients.ZERO,
    val goals: NutritionGoals = NutritionGoals.DEFAULT,
    val waterMl: Double = 0.0,
    val weightKg: Double? = null,
    val weeklyWeightChangeKg: Double? = null,
    val entryCount: Int = 0,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val quickWaterMl: Int = 250,
    val failed: Boolean = false
) {
    val caloriesRemaining: Double get() = goals.calories - totals.calories
    val calorieProgress: Float
        get() = if (goals.calories <= 0) 0f else (totals.calories / goals.calories).coerceIn(0.0, 1.0).toFloat()
    val waterProgress: Float
        get() = if (goals.waterMl <= 0) 0f else (waterMl / goals.waterMl).coerceIn(0.0, 1.0).toFloat()
}

/**
 * Widgets read straight from Room and DataStore. They are only rebuilt when something
 * actually changed (see [WidgetUpdater]), so there's no polling and no background service.
 */
object WidgetDataLoader {

    suspend fun load(context: Context): WidgetSnapshot = try {
        val container = context.appContainer
        val today = DateUtils.today()
        val prefs = container.preferencesRepository.preferences.first()
        val totals = container.diaryRepository.totalsFor(today)
        val entries = container.diaryRepository.entriesFor(today)

        WidgetSnapshot(
            totals = totals,
            goals = prefs.goals,
            waterMl = container.waterRepository.totalFor(today),
            weightKg = container.weightRepository.latest()?.weightKg,
            weeklyWeightChangeKg = container.weightRepository.changeOverDays(7),
            entryCount = entries.size,
            unitSystem = prefs.unitSystem,
            quickWaterMl = prefs.quickWaterAmountsMl.firstOrNull() ?: 250
        )
    } catch (e: Exception) {
        // A widget that shows placeholder numbers beats a widget that shows a crash.
        WidgetSnapshot(failed = true)
    }
}
