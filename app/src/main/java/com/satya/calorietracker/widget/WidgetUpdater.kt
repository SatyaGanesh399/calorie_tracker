package com.satya.calorietracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.action.ActionParameters
import com.satya.calorietracker.appContainer
import com.satya.calorietracker.util.DateUtils

/**
 * Pushes fresh content to every widget. Called from repositories after a write, from
 * the midnight alarm, and from the weekly maintenance worker — never on a timer.
 *
 * `updateAll` is a no-op when a widget type isn't on the home screen, so this stays
 * cheap even with all six declared.
 */
class WidgetUpdater(private val context: Context) {

    private val widgets: List<GlanceAppWidget> by lazy {
        listOf(
            CaloriesWidget(),
            WeightWidget(),
            WaterWidget(),
            NutritionWidget(),
            QuickAddWidget(),
            SummaryWidget()
        )
    }

    suspend fun updateAll() {
        widgets.forEach { widget ->
            // One failing widget must never stop the others updating.
            runCatching { widget.updateAll(context) }
        }
    }
}

/** "+250 ml" straight from the home screen, without opening the app. */
class AddWaterAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        runCatching {
            val container = context.appContainer
            val prefs = container.preferencesState.value
            val amount = prefs.quickWaterAmountsMl.firstOrNull() ?: 250
            container.waterRepository.add(amount.toDouble(), DateUtils.today())
        }
    }
}
