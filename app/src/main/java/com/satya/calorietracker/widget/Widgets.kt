package com.satya.calorietracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.satya.calorietracker.MainActivity
import com.satya.calorietracker.util.Format

// =============================================================== 1. Calories

class CaloriesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            val style = rememberWidgetStyle()
            val compact = style.compact || LocalSize.current.height < 90.dp

            WidgetCard(style = style, onClick = openApp(MainActivity.ACTION_ADD_FOOD)) {
                WidgetTitle("🔥 Calories", style)
                Spacer(GlanceModifier.height(6.dp))
                WidgetBigValue(
                    "${Format.kcal(snapshot.totals.calories)} / ${Format.kcal(snapshot.goals.calories)}",
                    style,
                    size = if (compact) 18 else 22
                )
                Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))
                WidgetBar(snapshot.calorieProgress, WidgetStyle.CALORIE, style)
                if (!compact) {
                    Spacer(GlanceModifier.height(8.dp))
                    val remaining = snapshot.caloriesRemaining
                    WidgetCaption(
                        if (remaining >= 0) "${Format.kcal(remaining)} kcal remaining"
                        else "${Format.kcal(-remaining)} kcal over",
                        style
                    )
                }
            }
        }
    }
}

class CaloriesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaloriesWidget()
}

// ================================================================= 2. Weight

class WeightWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            val style = rememberWidgetStyle()
            val compact = style.compact || LocalSize.current.height < 80.dp

            WidgetCard(style = style, onClick = openApp(MainActivity.ACTION_ADD_WEIGHT)) {
                WidgetTitle("⚖️ Weight", style)
                Spacer(GlanceModifier.height(6.dp))
                WidgetBigValue(
                    snapshot.weightKg?.let { Format.weight(it, snapshot.unitSystem) } ?: "—",
                    style,
                    size = if (compact) 18 else 24
                )
                if (!compact) {
                    Spacer(GlanceModifier.height(6.dp))
                    WidgetCaption(
                        snapshot.weeklyWeightChangeKg?.let {
                            "${Format.weightDelta(it, snapshot.unitSystem)} this week"
                        } ?: "Tap to log a weigh-in",
                        style
                    )
                }
            }
        }
    }
}

class WeightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeightWidget()
}

// ================================================================== 3. Water

class WaterWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            val style = rememberWidgetStyle()
            val compact = style.compact || LocalSize.current.height < 100.dp

            WidgetCard(style = style, onClick = openApp(MainActivity.ACTION_ADD_WATER)) {
                WidgetTitle("💧 Water", style)
                Spacer(GlanceModifier.height(6.dp))
                WidgetBigValue(
                    "${Format.water(snapshot.waterMl, snapshot.unitSystem)} / ${Format.water(snapshot.goals.waterMl.toDouble(), snapshot.unitSystem)}",
                    style,
                    size = if (compact) 16 else 20
                )
                Spacer(GlanceModifier.height(8.dp))
                WidgetBar(snapshot.waterProgress, WidgetStyle.WATER, style)
                if (!compact) {
                    Spacer(GlanceModifier.height(10.dp))
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetPillButton(
                            label = "+${snapshot.quickWaterMl} ml",
                            style = style,
                            onClick = actionRunCallback<AddWaterAction>()
                        )
                        Spacer(GlanceModifier.height(0.dp))
                    }
                }
            }
        }
    }
}

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}

// ============================================================== 4. Nutrition

class NutritionWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            val style = rememberWidgetStyle()
            val tall = LocalSize.current.height >= 140.dp && !style.compact

            WidgetCard(style = style, onClick = openApp(MainActivity.ACTION_ADD_FOOD)) {
                WidgetTitle("🥗 Today's nutrition", style)
                Spacer(GlanceModifier.height(8.dp))
                WidgetStatRow(
                    label = "Protein",
                    value = "${Format.grams(snapshot.totals.protein)} / ${snapshot.goals.protein} g",
                    progress = Format.progress(snapshot.totals.protein, snapshot.goals.protein.toDouble()),
                    color = WidgetStyle.PROTEIN,
                    style = style
                )
                Spacer(GlanceModifier.height(8.dp))
                WidgetStatRow(
                    label = "Carbs",
                    value = "${Format.grams(snapshot.totals.carbs)} / ${snapshot.goals.carbs} g",
                    progress = Format.progress(snapshot.totals.carbs, snapshot.goals.carbs.toDouble()),
                    color = WidgetStyle.CARBS,
                    style = style
                )
                Spacer(GlanceModifier.height(8.dp))
                WidgetStatRow(
                    label = "Fat",
                    value = "${Format.grams(snapshot.totals.fat)} / ${snapshot.goals.fat} g",
                    progress = Format.progress(snapshot.totals.fat, snapshot.goals.fat.toDouble()),
                    color = WidgetStyle.FAT,
                    style = style
                )
                if (tall) {
                    Spacer(GlanceModifier.height(8.dp))
                    WidgetStatRow(
                        label = "Fibre",
                        value = "${Format.grams(snapshot.totals.fiber)} / ${snapshot.goals.fiber} g",
                        progress = Format.progress(snapshot.totals.fiber, snapshot.goals.fiber.toDouble()),
                        color = WidgetStyle.CALORIE,
                        style = style
                    )
                }
            }
        }
    }
}

class NutritionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NutritionWidget()
}

// ============================================================== 5. Quick add

class QuickAddWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val style = rememberWidgetStyle()

            WidgetCard(style = style, onClick = openApp(MainActivity.ACTION_HOME)) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickTile("🍽", "Food", style, MainActivity.ACTION_ADD_FOOD)
                    Spacer(GlanceModifier.width(6.dp))
                    QuickTile("💧", "Water", style, MainActivity.ACTION_ADD_WATER)
                    Spacer(GlanceModifier.width(6.dp))
                    QuickTile("⚖️", "Weight", style, MainActivity.ACTION_ADD_WEIGHT)
                    Spacer(GlanceModifier.width(6.dp))
                    QuickTile("📷", "Scan", style, MainActivity.ACTION_SCAN)
                }
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

// ================================================================ 6. Summary

class SummaryWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataLoader.load(context)
        provideContent {
            val style = rememberWidgetStyle()
            val tall = LocalSize.current.height >= 160.dp && !style.compact

            WidgetCard(style = style, onClick = openApp(MainActivity.ACTION_HOME)) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = "Today",
                        style = TextStyle(
                            color = style.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    WidgetCaption("${snapshot.entryCount} items", style)
                }

                Spacer(GlanceModifier.height(10.dp))
                SummaryLine("🔥", "${Format.kcal(snapshot.totals.calories)} kcal", style)
                SummaryLine("🥩", "${Format.grams(snapshot.totals.protein)} g protein", style)
                SummaryLine("💧", Format.water(snapshot.waterMl, snapshot.unitSystem), style)
                SummaryLine(
                    "⚖️",
                    snapshot.weightKg?.let { Format.weight(it, snapshot.unitSystem) } ?: "No weigh-in",
                    style
                )

                Spacer(GlanceModifier.height(10.dp))
                WidgetBar(snapshot.calorieProgress, WidgetStyle.CALORIE, style)
                if (tall) {
                    Spacer(GlanceModifier.height(6.dp))
                    WidgetCaption(
                        "${(snapshot.calorieProgress * 100).toInt()}% of your calorie target",
                        style
                    )
                }
            }
        }
    }
}

class SummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}

// ================================================================== helpers

@Composable
private fun SummaryLine(emoji: String, text: String, style: WidgetStyle) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$emoji  ",
            style = TextStyle(color = style.onBackground, fontSize = 13.sp)
        )
        Text(
            text = text,
            style = TextStyle(color = style.onBackground, fontSize = 13.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun QuickTile(
    emoji: String,
    label: String,
    style: WidgetStyle,
    action: String
) {
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .padding(4.dp)
            .background(style.trackProvider)
            .cornerRadius(14.dp)
            .clickable(openApp(action)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, style = TextStyle(fontSize = 18.sp, color = style.onBackground))
        Text(
            text = label,
            style = TextStyle(color = style.onBackground, fontSize = 11.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun WidgetPillButton(
    label: String,
    style: WidgetStyle,
    onClick: androidx.glance.action.Action
) {
    Box(
        modifier = GlanceModifier
            .background(style.accentProvider)
            .cornerRadius(14.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.White),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
