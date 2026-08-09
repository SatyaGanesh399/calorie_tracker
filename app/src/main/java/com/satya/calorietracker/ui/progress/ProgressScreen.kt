package com.satya.calorietracker.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.data.repository.StatsRange
import com.satya.calorietracker.data.repository.WeightEntry
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.domain.units.UnitConverter
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.BarChart
import com.satya.calorietracker.ui.components.ChartPoint
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.DeltaText
import com.satya.calorietracker.ui.components.EmptyState
import com.satya.calorietracker.ui.components.LineChart
import com.satya.calorietracker.ui.components.NumberField
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.components.StatValue
import com.satya.calorietracker.ui.theme.CalorieOrange
import com.satya.calorietracker.ui.theme.CarbAmber
import com.satya.calorietracker.ui.theme.FatBlue
import com.satya.calorietracker.ui.theme.FiberGreen
import com.satya.calorietracker.ui.theme.ProteinRed
import com.satya.calorietracker.ui.theme.WaterBlue
import com.satya.calorietracker.ui.theme.WeightViolet
import com.satya.calorietracker.util.DateUtils
import com.satya.calorietracker.util.Format

enum class ProgressTab(val label: String) {
    WEIGHT("Weight"),
    NUTRITION("Nutrition"),
    WATER("Water")
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onRangeChange: (StatsRange) -> Unit,
    onLogWeight: (Double) -> Unit,
    onDeleteWeight: (Long) -> Unit,
    onSetGoalWeight: (Double) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var tab by remember { mutableStateOf(ProgressTab.WEIGHT) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Progress",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ProgressTab.entries.toList()) { t ->
                        ChoiceChip(selected = tab == t, label = t.label, onClick = { tab = t })
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StatsRange.entries.toList()) { r ->
                        ChoiceChip(
                            selected = state.range == r,
                            label = r.label,
                            onClick = { onRangeChange(r) }
                        )
                    }
                }
            }

            when (tab) {
                ProgressTab.WEIGHT -> weightSection(
                    state = state,
                    onSetGoal = { showGoalDialog = true },
                    onDelete = onDeleteWeight
                )
                ProgressTab.NUTRITION -> nutritionSection(state)
                ProgressTab.WATER -> waterSection(state)
            }
        }

        if (tab == ProgressTab.WEIGHT) {
            ExtendedFloatingActionButton(
                onClick = { showWeightDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Log weight") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp)
            )
        }
    }

    if (showWeightDialog) {
        WeightInputDialog(
            title = "Log weight",
            initialKg = state.weightSummary.current ?: 70.0,
            unitSystem = state.unitSystem,
            onConfirm = {
                onLogWeight(it)
                showWeightDialog = false
            },
            onDismiss = { showWeightDialog = false }
        )
    }

    if (showGoalDialog) {
        WeightInputDialog(
            title = "Goal weight",
            initialKg = state.goalWeightKg ?: state.weightSummary.current ?: 70.0,
            unitSystem = state.unitSystem,
            onConfirm = {
                onSetGoalWeight(it)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }
}

// -------------------------------------------------------------------- weight

private fun androidx.compose.foundation.lazy.LazyListScope.weightSection(
    state: ProgressUiState,
    onSetGoal: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val summary = state.weightSummary
    val system = state.unitSystem

    item {
        AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                if (summary.current == null) {
                    Text(
                        "No weigh-ins yet. Log one and the chart starts here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        Format.weight(summary.current, system),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    summary.weeklyChange?.let {
                        Spacer(Modifier.height(4.dp))
                        DeltaText(
                            delta = if (system == UnitSystem.METRIC) it else UnitConverter.kgToLb(it),
                            unit = Format.weightUnitLabel(system),
                            goodWhenNegative = (summary.goal ?: summary.current) <= summary.current
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    LineChart(
                        points = state.weightEntries.map {
                            ChartPoint(
                                label = DateUtils.shortDate(it.date),
                                value = if (system == UnitSystem.METRIC) it.weightKg.toFloat()
                                else UnitConverter.kgToLb(it.weightKg).toFloat()
                            )
                        },
                        lineColor = WeightViolet,
                        goalValue = summary.goal?.let {
                            if (system == UnitSystem.METRIC) it.toFloat()
                            else UnitConverter.kgToLb(it).toFloat()
                        },
                        valueFormatter = { Format.decimal(it.toDouble(), 1) }
                    )
                }
            }
        }
    }

    item {
        AppCard {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatValue(
                        value = summary.start?.let { Format.weight(it, system, withUnit = false) } ?: "—",
                        label = "Start"
                    )
                    StatValue(
                        value = summary.current?.let { Format.weight(it, system, withUnit = false) } ?: "—",
                        label = "Current"
                    )
                    StatValue(
                        value = summary.goal?.let { Format.weight(it, system, withUnit = false) } ?: "—",
                        label = "Goal"
                    )
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatValue(
                        value = summary.totalChange?.let { Format.weightDelta(it, system) } ?: "—",
                        label = "Total change"
                    )
                    StatValue(
                        value = summary.monthlyChange?.let { Format.weightDelta(it, system) } ?: "—",
                        label = "Last 30 days"
                    )
                    StatValue(
                        value = summary.remainingToGoal?.let { Format.weight(kotlin.math.abs(it), system) } ?: "—",
                        label = "To goal"
                    )
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onSetGoal) { Text("Set goal weight") }
            }
        }
    }

    item { SectionHeader("Weigh-in history") }

    if (state.weightEntries.isEmpty()) {
        item {
            EmptyState(
                icon = Icons.Outlined.MonitorWeight,
                title = "Nothing logged",
                message = "Weigh yourself at the same time of day — first thing in the morning works best — and the trend becomes reliable within a couple of weeks."
            )
        }
    } else {
        items(state.weightEntries.sortedByDescending { it.date }, key = { it.id }) { entry ->
            WeightRow(entry = entry, system = state.unitSystem, onDelete = { onDelete(entry.id) })
        }
    }
}

@Composable
private fun WeightRow(entry: WeightEntry, system: UnitSystem, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(DateUtils.prettyDate(entry.date), style = MaterialTheme.typography.bodyMedium)
            entry.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            Format.weight(entry.weightKg, system),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete weigh-in")
        }
    }
}

// ----------------------------------------------------------------- nutrition

private fun androidx.compose.foundation.lazy.LazyListScope.nutritionSection(state: ProgressUiState) {
    val stats = state.calorieStats
    val macros = state.macroStats

    item {
        AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                Text("Calories", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                BarChart(
                    points = stats.series.map {
                        ChartPoint(DateUtils.shortDate(it.date), it.value.toFloat())
                    },
                    barColor = CalorieOrange,
                    target = state.goals.calories.toFloat()
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatValue(Format.kcal(stats.dailyAverage), "Daily avg")
                    StatValue(Format.kcal(stats.weeklyAverage), "Weekly avg")
                    StatValue(Format.kcal(stats.monthlyAverage), "Monthly avg")
                }
            }
        }
    }

    item {
        AppCard {
            Column {
                Text("Highest & lowest", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatValue(
                        value = stats.highest?.let { Format.kcal(it.value) } ?: "—",
                        label = stats.highest?.let { DateUtils.shortDate(it.date) } ?: "Highest day"
                    )
                    StatValue(
                        value = stats.lowest?.let { Format.kcal(it.value) } ?: "—",
                        label = stats.lowest?.let { DateUtils.shortDate(it.date) } ?: "Lowest day"
                    )
                    StatValue(
                        value = "${stats.daysTracked}/${stats.daysInRange}",
                        label = "Days tracked"
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (state.streak > 0) "🔥 ${state.streak}-day logging streak"
                    else "Log something today to start a streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    item {
        AppCard {
            Column {
                Text("Average nutrition per day", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                AverageRow("Protein", macros.avgProtein, state.goals.protein, ProteinRed)
                AverageRow("Carbohydrates", macros.avgCarbs, state.goals.carbs, CarbAmber)
                AverageRow("Fat", macros.avgFat, state.goals.fat, FatBlue)
                AverageRow("Fibre", macros.avgFiber, state.goals.fiber, FiberGreen)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sugar", style = MaterialTheme.typography.bodyMedium)
                    Text("${Format.grams(macros.avgSugar)} g", style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sodium", style = MaterialTheme.typography.bodyMedium)
                    Text("${Format.grams(macros.avgSodium)} mg", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AverageRow(label: String, average: Double, target: Int, color: androidx.compose.ui.graphics.Color) {
    Column(Modifier.padding(vertical = 5.dp)) {
        com.satya.calorietracker.ui.components.MacroRow(
            label = label,
            consumed = average,
            target = target,
            unit = "g",
            color = color
        )
    }
}

// --------------------------------------------------------------------- water

private fun androidx.compose.foundation.lazy.LazyListScope.waterSection(state: ProgressUiState) {
    item {
        AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column {
                Text("Water", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                BarChart(
                    points = state.waterSeries.map {
                        ChartPoint(DateUtils.shortDate(it.date), it.value.toFloat())
                    },
                    barColor = WaterBlue,
                    target = state.goals.waterMl.toFloat()
                )
                Spacer(Modifier.height(14.dp))
                val avg = if (state.waterSeries.isEmpty()) 0.0
                else state.waterSeries.sumOf { it.value } / state.waterSeries.size
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatValue(Format.water(avg, state.unitSystem), "Daily avg")
                    StatValue(
                        Format.water(state.goals.waterMl.toDouble(), state.unitSystem),
                        "Target"
                    )
                    StatValue(
                        "${state.waterSeries.count { it.value >= state.goals.waterMl }}",
                        "Days on target"
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------- dialog

@Composable
private fun WeightInputDialog(
    title: String,
    initialKg: Double,
    unitSystem: UnitSystem,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val displayValue = if (unitSystem == UnitSystem.METRIC) initialKg else UnitConverter.kgToLb(initialKg)
    var text by remember { mutableStateOf(Format.decimal(displayValue, 1)) }
    val unitLabel = Format.weightUnitLabel(unitSystem)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                NumberField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Weight",
                    suffix = unitLabel,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "One weigh-in per day — logging again replaces today's value.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = text.toDoubleOrNull() ?: return@TextButton
                    val kg = if (unitSystem == UnitSystem.METRIC) value else UnitConverter.lbToKg(value)
                    if (kg in 20.0..400.0) onConfirm(kg)
                },
                enabled = (text.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
