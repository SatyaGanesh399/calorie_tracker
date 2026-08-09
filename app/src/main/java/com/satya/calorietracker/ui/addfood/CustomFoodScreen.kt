package com.satya.calorietracker.ui.addfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.FoodSource
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.domain.model.Nutrients
import com.satya.calorietracker.ui.components.InfoBanner
import com.satya.calorietracker.ui.components.NumberField
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomFoodUiState(
    val existing: Food? = null,
    val loading: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null
)

class CustomFoodViewModel(
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomFoodUiState())
    val state: StateFlow<CustomFoodUiState> = _state.asStateFlow()

    fun load(foodId: Long) {
        if (foodId == 0L) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            _state.value = CustomFoodUiState(existing = foodRepository.getById(foodId), loading = false)
        }
    }

    fun save(food: Food, onSaved: (Long) -> Unit) {
        if (food.name.isBlank()) {
            _state.value = _state.value.copy(error = "Give the food a name so you can find it later.")
            return
        }
        viewModelScope.launch {
            try {
                val id = foodRepository.saveCustomFood(food)
                _state.value = _state.value.copy(savedId = id, error = null)
                onSaved(id)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Couldn't save: ${e.message ?: "database error"}")
            }
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            foodRepository.deleteFood(id)
            onDone()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            CustomFoodViewModel(container.foodRepository)
        }
    }
}

/**
 * Type a nutrition label in once and never again. Everything except the name and
 * calories is optional — a half-filled custom food is more useful than no food.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodScreen(
    state: CustomFoodUiState,
    prefillBarcode: String?,
    prefillName: String?,
    onSave: (Food) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit
) {
    val existing = state.existing

    var name by remember(existing) { mutableStateOf(existing?.name ?: prefillName.orEmpty()) }
    var brand by remember(existing) { mutableStateOf(existing?.brand.orEmpty()) }
    var barcode by remember(existing) { mutableStateOf(existing?.barcode ?: prefillBarcode.orEmpty()) }
    var per by remember(existing) { mutableStateOf((existing?.per ?: 100.0).trimmed()) }
    var unit by remember(existing) { mutableStateOf(existing?.perUnit ?: MeasureUnit.GRAM) }
    var calories by remember(existing) { mutableStateOf(existing?.nutrients?.calories.orBlank()) }
    var protein by remember(existing) { mutableStateOf(existing?.nutrients?.protein.orBlank()) }
    var carbs by remember(existing) { mutableStateOf(existing?.nutrients?.carbs.orBlank()) }
    var fat by remember(existing) { mutableStateOf(existing?.nutrients?.fat.orBlank()) }
    var fiber by remember(existing) { mutableStateOf(existing?.nutrients?.fiber.orBlank()) }
    var sugar by remember(existing) { mutableStateOf(existing?.nutrients?.sugar.orBlank()) }
    var sodium by remember(existing) { mutableStateOf(existing?.nutrients?.sodium.orBlank()) }
    var servingSize by remember(existing) { mutableStateOf(existing?.servingSize.orBlank()) }
    var servingLabel by remember(existing) { mutableStateOf(existing?.servingLabel.orEmpty()) }
    var showDelete by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && (calories.toDoubleOrNull() ?: 0.0) >= 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New food" else "Edit food") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete food")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    onSave(
                        Food(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            brand = brand.trim().takeIf { it.isNotBlank() },
                            barcode = barcode.trim().takeIf { it.isNotBlank() },
                            sourceId = FoodSource.CUSTOM.id,
                            per = per.toDoubleOrNull() ?: 100.0,
                            perUnitId = unit.id,
                            nutrients = Nutrients(
                                calories = calories.toDoubleOrNull() ?: 0.0,
                                protein = protein.toDoubleOrNull() ?: 0.0,
                                carbs = carbs.toDoubleOrNull() ?: 0.0,
                                fat = fat.toDoubleOrNull() ?: 0.0,
                                fiber = fiber.toDoubleOrNull() ?: 0.0,
                                sugar = sugar.toDoubleOrNull() ?: 0.0,
                                sodium = sodium.toDoubleOrNull() ?: 0.0
                            ),
                            servingSize = servingSize.toDoubleOrNull(),
                            servingLabel = servingLabel.trim().takeIf { it.isNotBlank() },
                            isCustom = true,
                            isFavorite = existing?.isFavorite ?: false,
                            createdAt = existing?.createdAt ?: 0L
                        )
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Save food") }
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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food name") },
                placeholder = { Text("Homemade chicken curry") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode (optional)") },
                supportingText = { Text("Adding it means future scans find this food instantly.") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Nutrition is per…")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    value = per,
                    onValueChange = { per = it },
                    label = "Amount",
                    modifier = Modifier.weight(1f)
                )
                UnitDropdown(
                    unit = unit,
                    options = listOf(MeasureUnit.GRAM, MeasureUnit.MILLILITRE),
                    onSelect = { unit = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Nutrition")
            NumberField(
                value = calories,
                onValueChange = { calories = it },
                label = "Calories",
                suffix = "kcal",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(protein, { protein = it }, "Protein", Modifier.weight(1f), suffix = "g")
                NumberField(carbs, { carbs = it }, "Carbs", Modifier.weight(1f), suffix = "g")
                NumberField(fat, { fat = it }, "Fat", Modifier.weight(1f), suffix = "g")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(fiber, { fiber = it }, "Fibre", Modifier.weight(1f), suffix = "g")
                NumberField(sugar, { sugar = it }, "Sugar", Modifier.weight(1f), suffix = "g")
                NumberField(sodium, { sodium = it }, "Sodium", Modifier.weight(1f), suffix = "mg")
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Default serving (optional)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    value = servingSize,
                    onValueChange = { servingSize = it },
                    label = "Serving",
                    suffix = unit.label,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = servingLabel,
                    onValueChange = { servingLabel = it },
                    label = { Text("Label") },
                    placeholder = { Text("1 bowl") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete \"${existing.name}\"?") },
            text = { Text("Entries already in your diary keep their nutrition — only the saved food is removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete(existing.id)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

private fun Double?.orBlank(): String = when {
    this == null -> ""
    this == 0.0 -> ""
    this % 1.0 == 0.0 -> toInt().toString()
    else -> String.format(java.util.Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
}

private fun Double.trimmed(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
