package com.satya.calorietracker.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.data.db.toNutrients
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.ui.addfood.UnitDropdown
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.InfoBanner
import com.satya.calorietracker.ui.components.LoadingLine
import com.satya.calorietracker.ui.components.NumberField
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    state: RecipeEditorUiState,
    onNameChange: (String) -> Unit,
    onServingsChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSearchQuery: (String) -> Unit,
    onAddIngredient: (Food, Double, Double, MeasureUnit) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit
) {
    var pickerFood by remember { mutableStateOf<Food?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.recipeId == 0L) "New recipe" else "Edit recipe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Save recipe") }
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
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Recipe name") },
                placeholder = { Text("Chicken rice bowl") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            NumberField(
                value = state.servingsText,
                onValueChange = onServingsChange,
                label = "How many servings does this make?",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("Ingredients")

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQuery,
                label = { Text("Add an ingredient") },
                placeholder = { Text("Search a food…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.searching) {
                Spacer(Modifier.height(6.dp))
                LoadingLine()
            }

            state.searchMessage?.let {
                Spacer(Modifier.height(8.dp))
                InfoBanner(message = it)
            }

            if (state.searchResults.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(state.searchResults.take(25), key = { "${it.id}-${it.name}" }) { food ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { pickerFood = food }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        food.subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.ingredients.isEmpty()) {
                Text(
                    "No ingredients yet. Search above to add the first one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AppCard {
                    Column {
                        state.ingredients.forEachIndexed { index, ing ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(ing.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        buildString {
                                            val q = if (ing.quantity % 1.0 == 0.0) ing.quantity.toInt().toString()
                                            else Format.decimal(ing.quantity, 2)
                                            val unit = MeasureUnit.fromId(ing.unitId)
                                            if (unit.isCount) append("$q ${unit.plural}")
                                            else append("${Format.grams(ing.quantity * ing.servingSize)} ${unit.label}")
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${Format.kcal(ing.toNutrients().calories)} kcal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(onClick = { onRemoveIngredient(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recipe total", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${Format.kcal(state.total.calories)} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Per serving", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${Format.kcal(state.perServing.calories)} kcal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "P ${Format.grams(state.perServing.protein)} g · C ${Format.grams(state.perServing.carbs)} g · F ${Format.grams(state.perServing.fat)} g · Fibre ${Format.grams(state.perServing.fiber)} g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text("Method / notes (optional)") },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(30.dp))
        }
    }

    pickerFood?.let { food ->
        IngredientAmountDialog(
            food = food,
            onDismiss = { pickerFood = null },
            onConfirm = { quantity, servingSize, unit ->
                onAddIngredient(food, quantity, servingSize, unit)
                pickerFood = null
            }
        )
    }
}

@Composable
private fun IngredientAmountDialog(
    food: Food,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, MeasureUnit) -> Unit
) {
    var amount by remember { mutableStateOf((food.servingSize ?: food.per).let {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
    }) }
    var unit by remember { mutableStateOf(food.perUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column {
                Text(
                    food.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount",
                        modifier = Modifier.weight(1f)
                    )
                    UnitDropdown(
                        unit = unit,
                        options = MeasureUnit.optionsFor(food.perUnit),
                        onSelect = { unit = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value > 0) {
                        if (unit.isCount) onConfirm(value, food.servingSize ?: food.per, unit)
                        else onConfirm(1.0, value, unit)
                    }
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
