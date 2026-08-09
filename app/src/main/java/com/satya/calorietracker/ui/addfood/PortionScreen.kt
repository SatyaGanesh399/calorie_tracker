package com.satya.calorietracker.ui.addfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.InfoBanner
import com.satya.calorietracker.ui.components.MacroSplitBar
import com.satya.calorietracker.ui.components.NumberField
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.theme.CarbAmber
import com.satya.calorietracker.ui.theme.FatBlue
import com.satya.calorietracker.ui.theme.ProteinRed
import com.satya.calorietracker.util.DateUtils
import com.satya.calorietracker.util.Format
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortionScreen(
    state: PortionUiState,
    onQuantityChange: (String) -> Unit,
    onServingSizeChange: (String) -> Unit,
    onUnitChange: (MeasureUnit) -> Unit,
    onMealChange: (MealType, String?) -> Unit,
    onDateChange: (java.time.LocalDate) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onScale: (Double) -> Unit,
    onToggleFavorite: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit entry" else "Add food") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.food != null) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favourite",
                                tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (state.isEdit) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(Modifier.padding(16.dp)) {
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = if (state.isEdit) "Save changes"
                        else "Add ${Format.kcal(state.computed.calories)} kcal",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            state.error?.let {
                InfoBanner(
                    message = it,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    actionLabel = "OK",
                    onAction = onDismissError
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(state.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            state.subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.servingHint?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Label serving: $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(18.dp))

            // ---------------------------------------------------- portion
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!state.unit.isCount) {
                    NumberField(
                        value = state.servingSizeText,
                        onValueChange = onServingSizeChange,
                        label = "Serving size",
                        modifier = Modifier.weight(1f)
                    )
                }
                UnitDropdown(
                    unit = state.unit,
                    options = state.availableUnits,
                    onSelect = onUnitChange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            NumberField(
                value = state.quantityText,
                onValueChange = onQuantityChange,
                label = if (state.unit.isCount) "How many ${state.unit.plural}?" else "Quantity",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5 to "½", 2.0 to "×2", 3.0 to "×3").forEach { (factor, label) ->
                    OutlinedButton(onClick = { onScale(factor) }) { Text(label) }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ------------------------------------------------- nutrition
            NutritionPanel(state)

            Spacer(Modifier.height(20.dp))

            SectionHeader("Meal")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MealType.BUILT_INS) { meal ->
                    ChoiceChip(
                        selected = state.mealType == meal && state.customMealName == null,
                        label = "${meal.emoji} ${meal.displayName}",
                        onClick = { onMealChange(meal, null) }
                    )
                }
                items(state.customMeals) { name ->
                    ChoiceChip(
                        selected = state.customMealName == name,
                        label = name,
                        onClick = { onMealChange(MealType.CUSTOM, name) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionHeader("When")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(DateUtils.prettyDate(state.date))
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(DateUtils.millisToTimeLabel(state.timestamp))
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optional)") },
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.startOfDayMillis(state.date)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateChange(
                            Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val time = DateUtils.millisToLocalDateTime(state.timestamp)
        val timeState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this entry?") },
            text = { Text("It will be removed from ${DateUtils.prettyDate(state.date).lowercase()}'s log.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NutritionPanel(state: PortionUiState) {
    val n = state.computed
    val (p, c, f) = n.macroSplit()

    AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Calories", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Format.kcal(n.calories)} kcal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            MacroSplitBar(
                proteinPct = p, carbsPct = c, fatPct = f,
                proteinColor = ProteinRed, carbsColor = CarbAmber, fatColor = FatBlue
            )
            Spacer(Modifier.height(14.dp))

            NutrientLine("Protein", n.protein, "g")
            NutrientLine("Carbohydrates", n.carbs, "g")
            NutrientLine("Fat", n.fat, "g")
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            NutrientLine("Fibre", n.fiber, "g")
            NutrientLine("Sugar", n.sugar, "g")
            NutrientLine("Sodium", n.sodium, "mg")
        }
    }
}

@Composable
private fun NutrientLine(label: String, value: Double, unit: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${Format.grams(value)} $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdown(
    unit: MeasureUnit,
    options: List<MeasureUnit>,
    onSelect: (MeasureUnit) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Unit"
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            value = unit.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.label}  ·  ${option.dimension.name.lowercase()}") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
