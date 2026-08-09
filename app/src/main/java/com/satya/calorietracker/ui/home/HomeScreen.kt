package com.satya.calorietracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.LoggedFood
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.MacroRow
import com.satya.calorietracker.ui.components.MiniRing
import com.satya.calorietracker.ui.components.ProgressRing
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.components.StatBar
import com.satya.calorietracker.ui.theme.CarbAmber
import com.satya.calorietracker.ui.theme.FatBlue
import com.satya.calorietracker.ui.theme.FiberGreen
import com.satya.calorietracker.ui.theme.ProteinRed
import com.satya.calorietracker.ui.theme.WaterBlue
import com.satya.calorietracker.ui.theme.WeightViolet
import com.satya.calorietracker.util.DateUtils
import com.satya.calorietracker.util.Format
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onShiftDate: (Long) -> Unit,
    onAddFood: (MealType, String?) -> Unit,
    onEditEntry: (LoggedFood) -> Unit,
    onDeleteEntry: (LoggedFood) -> Unit,
    onRepeatFood: (Food) -> Unit,
    onAddWater: (Int) -> Unit,
    onUndoWater: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenWater: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            HomeHeader(
                date = state.date,
                onShiftDate = onShiftDate,
                onSelectDate = onSelectDate
            )
        }

        item(key = "calories") {
            CalorieHeroCard(state = state)
        }

        item(key = "macros") {
            MacroCard(state = state)
        }

        item(key = "water_weight") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WaterCard(
                    consumedMl = state.waterMl,
                    goalMl = state.goals.waterMl,
                    unitSystem = state.unitSystem,
                    quickAmounts = state.quickWaterAmounts,
                    onAdd = onAddWater,
                    onUndo = onUndoWater,
                    onOpen = onOpenWater,
                    modifier = Modifier.weight(1f)
                )
                WeightCard(
                    state = state,
                    onOpen = onOpenWeight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.recentFoods.isNotEmpty()) {
            item(key = "recent_header") {
                SectionHeader(title = "Log again")
            }
            item(key = "recent") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.recentFoods, key = { it.id }) { food ->
                        QuickRepeatChip(food = food, onClick = { onRepeatFood(food) })
                    }
                }
            }
        }

        item(key = "meals_header") {
            SectionHeader(
                title = if (state.isToday) "Today's meals" else "Meals",
                action = null
            )
        }

        items(state.meals, key = { it.key }) { meal ->
            MealCard(
                meal = meal,
                onAdd = { onAddFood(meal.mealType, meal.customName) },
                onEditEntry = onEditEntry,
                onDeleteEntry = onDeleteEntry
            )
        }
    }
}

// --------------------------------------------------------------------- header

@Composable
private fun HomeHeader(
    date: LocalDate,
    onShiftDate: (Long) -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${DateUtils.greeting()} 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateUtils.fullDate(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onShiftDate(-1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
            }
            IconButton(
                onClick = { onShiftDate(1) },
                enabled = date < DateUtils.today()
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
            }
        }

        Spacer(Modifier.height(10.dp))
        WeekStrip(selected = date, onSelect = onSelectDate)
    }
}

/** The seven-day strip at the top. Future days are dimmed but still tappable. */
@Composable
private fun WeekStrip(selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val days = remember(selected) { DateUtils.weekOf(selected) }
    val today = DateUtils.today()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { day ->
            val isSelected = day == selected
            val isFuture = day > today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                    .clickable(enabled = !isFuture) { onSelect(day) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = DateUtils.dayLetter(day),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------ hero card

@Composable
private fun CalorieHeroCard(state: HomeUiState) {
    AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥 Calories", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${(state.calorieProgress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            ProgressRing(progress = state.calorieProgress, size = 190.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Format.kcal(state.caloriesConsumed),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of ${Format.kcal(state.goals.calories)} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val remaining = state.caloriesRemaining
            Text(
                text = if (remaining >= 0) "${Format.kcal(remaining)} kcal remaining"
                else "${Format.kcal(-remaining)} kcal over",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (remaining >= 0) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MacroCard(state: HomeUiState) {
    AppCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroDial("Protein", state.totals.protein, state.goals.protein, ProteinRed)
                MacroDial("Carbs", state.totals.carbs, state.goals.carbs, CarbAmber)
                MacroDial("Fat", state.totals.fat, state.goals.fat, FatBlue)
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            MacroRow(
                label = "Fibre",
                consumed = state.totals.fiber,
                target = state.goals.fiber,
                unit = "g",
                color = FiberGreen
            )
        }
    }
}

@Composable
private fun MacroDial(label: String, consumed: Double, target: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MiniRing(
            progress = Format.progress(consumed, target.toDouble()),
            color = color,
            size = 62.dp,
            label = Format.grams(consumed)
        )
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            "of ${target} g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ----------------------------------------------------------- water & weight

@Composable
private fun WaterCard(
    consumedMl: Double,
    goalMl: Int,
    unitSystem: com.satya.calorietracker.domain.model.UnitSystem,
    quickAmounts: List<Int>,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier, onClick = onOpen) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = WaterBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Water", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = Format.water(consumedMl, unitSystem),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "of ${Format.water(goalMl.toDouble(), unitSystem)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            StatBar(progress = Format.progress(consumedMl, goalMl.toDouble()), color = WaterBlue)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quickAmounts.take(2).forEach { amount ->
                    FilledTonalButton(
                        onClick = { onAdd(amount) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+$amount", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            AnimatedVisibility(visible = consumedMl > 0) {
                TextButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Undo", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun WeightCard(
    state: HomeUiState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier, onClick = onOpen) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.MonitorWeight,
                    contentDescription = null,
                    tint = WeightViolet,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Weight", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            val weight = state.latestWeight
            if (weight == null) {
                Text(
                    "Not logged yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onOpen,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Add", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Text(
                    text = Format.weight(weight.weightKg, state.unitSystem),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = DateUtils.prettyDate(weight.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                state.weeklyWeightChange?.let { change ->
                    Text(
                        text = "${Format.weightDelta(change, state.unitSystem)} this week",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------- meal cards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(
    meal: MealSection,
    onAdd: () -> Unit,
    onEditEntry: (LoggedFood) -> Unit,
    onDeleteEntry: (LoggedFood) -> Unit
) {
    AppCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meal.mealType.emoji} ${meal.label}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${Format.kcal(meal.calories)} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to ${meal.label}")
                }
            }

            if (meal.entries.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Nothing logged yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(6.dp))
                meal.entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    SwipeableEntryRow(
                        entry = entry,
                        onClick = { onEditEntry(entry) },
                        onDelete = { onDeleteEntry(entry) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntryRow(
    entry: LoggedFood,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        EntryRow(entry = entry, onClick = onClick)
    }
}

@Composable
fun EntryRow(entry: LoggedFood, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(entry.portionLabel)
                    if (!entry.brand.isNullOrBlank()) append(" · ").append(entry.brand)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Format.kcal(entry.nutrients.calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "P ${Format.grams(entry.nutrients.protein)} · C ${Format.grams(entry.nutrients.carbs)} · F ${Format.grams(entry.nutrients.fat)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickRepeatChip(food: Food, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Column {
                Text(
                    food.name.take(22),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
                Text(
                    "${food.nutrients.calories.roundToInt()} kcal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingIcon = {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(15.dp)
                )
            }
        },
        modifier = Modifier.height(56.dp)
    )
}

@Composable
fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
