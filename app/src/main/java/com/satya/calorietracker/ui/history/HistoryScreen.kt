package com.satya.calorietracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.domain.model.LoggedFood
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.EmptyState
import com.satya.calorietracker.ui.components.MacroRow
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.components.StatValue
import com.satya.calorietracker.ui.home.EntryRow
import com.satya.calorietracker.ui.theme.CarbAmber
import com.satya.calorietracker.ui.theme.FatBlue
import com.satya.calorietracker.ui.theme.FiberGreen
import com.satya.calorietracker.ui.theme.GoalClose
import com.satya.calorietracker.ui.theme.GoalGood
import com.satya.calorietracker.ui.theme.GoalOver
import com.satya.calorietracker.ui.theme.ProteinRed
import com.satya.calorietracker.ui.theme.goalStatusColor
import com.satya.calorietracker.util.DateUtils
import com.satya.calorietracker.util.Format
import java.time.LocalDate

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSelectDate: (LocalDate) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onToday: () -> Unit,
    onEntryClick: (LoggedFood) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onToday) { Text("Today") }
            }
        }

        item {
            AppCard {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onShiftMonth(-1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            text = DateUtils.monthYear(state.month.atDay(1)),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onShiftMonth(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    MonthGrid(state = state, onSelectDate = onSelectDate)
                    Spacer(Modifier.height(12.dp))
                    Legend()
                }
            }
        }

        item {
            SectionHeader(DateUtils.prettyDate(state.selected))
        }

        item {
            AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatValue(
                            value = Format.kcal(state.selectedTotals.calories),
                            label = "of ${Format.kcal(state.goals.calories)} kcal"
                        )
                        StatValue(
                            value = Format.water(state.selectedWaterMl, state.unitSystem),
                            label = "Water"
                        )
                        StatValue(
                            value = state.selectedWeightKg?.let {
                                Format.weight(it, state.unitSystem)
                            } ?: "—",
                            label = "Weight"
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(14.dp))
                    MacroRow("Protein", state.selectedTotals.protein, state.goals.protein, "g", ProteinRed)
                    Spacer(Modifier.height(10.dp))
                    MacroRow("Carbs", state.selectedTotals.carbs, state.goals.carbs, "g", CarbAmber)
                    Spacer(Modifier.height(10.dp))
                    MacroRow("Fat", state.selectedTotals.fat, state.goals.fat, "g", FatBlue)
                    Spacer(Modifier.height(10.dp))
                    MacroRow("Fibre", state.selectedTotals.fiber, state.goals.fiber, "g", FiberGreen)
                }
            }
        }

        if (state.selectedEntries.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.EventBusy,
                    title = "Nothing logged",
                    message = "No food recorded on ${DateUtils.fullDate(state.selected)}."
                )
            }
        } else {
            item { SectionHeader("Foods logged") }
            item {
                AppCard {
                    Column {
                        state.selectedEntries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Column {
                                Text(
                                    "${entry.mealType.emoji} ${entry.mealLabel} · ${DateUtils.millisToTimeLabel(entry.timestamp)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                EntryRow(entry = entry, onClick = { onEntryClick(entry) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(state: HistoryUiState, onSelectDate: (LocalDate) -> Unit) {
    val firstOfMonth = state.month.atDay(1)
    val leadingBlanks = (firstOfMonth.dayOfWeek.value + 6) % 7
    val daysInMonth = state.month.lengthOfMonth()
    val today = DateUtils.today()

    Column {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        var dayCounter = 1
        val totalCells = leadingBlanks + daysInMonth
        val rows = (totalCells + 6) / 7

        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val cellIndex = row * 7 + column
                    if (cellIndex < leadingBlanks || dayCounter > daysInMonth) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = state.month.atDay(dayCounter)
                        val summary = state.days[date]
                        val isSelected = date == state.selected
                        val isFuture = date > today

                        DayCell(
                            day = dayCounter,
                            calories = summary?.calories ?: 0.0,
                            target = state.goals.calories.toDouble(),
                            hasData = (summary?.entryCount ?: 0) > 0,
                            isSelected = isSelected,
                            isToday = date == today,
                            isFuture = isFuture,
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    calories: Double,
    target: Double,
    hasData: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = if (hasData) goalStatusColor(calories, target) else null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    statusColor != null -> statusColor.copy(alpha = 0.16f)
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected) Modifier.border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (statusColor != null) {
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LegendDot(GoalGood, "On target")
        LegendDot(GoalClose, "A bit off")
        LegendDot(GoalOver, "Well over/under")
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.height(0.dp))
        Text(
            "  $label",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
