package com.satya.calorietracker.ui.addfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.NumberField
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.containerViewModelFactory
import kotlinx.coroutines.launch
import java.time.LocalDate

class QuickAddViewModel(
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    fun add(
        calories: Double,
        mealType: MealType,
        protein: Double,
        carbs: Double,
        fat: Double,
        label: String,
        date: LocalDate,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            diaryRepository.quickAdd(
                calories = calories,
                mealType = mealType,
                protein = protein,
                carbs = carbs,
                fat = fat,
                label = label.ifBlank { "Quick add" },
                date = date
            )
            onDone()
        }
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            QuickAddViewModel(container.diaryRepository)
        }
    }
}

/**
 * For the meals you can't be bothered to itemise. Calories are required; macros are
 * optional and left blank rather than guessed, so the macro rings stay honest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    initialMeal: MealType,
    date: LocalDate,
    onAdd: (calories: Double, meal: MealType, protein: Double, carbs: Double, fat: Double, label: String) -> Unit,
    onBack: () -> Unit
) {
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(initialMeal) }

    val kcal = calories.toDoubleOrNull() ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick add") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    onAdd(
                        kcal,
                        meal,
                        protein.toDoubleOrNull() ?: 0.0,
                        carbs.toDoubleOrNull() ?: 0.0,
                        fat.toDoubleOrNull() ?: 0.0,
                        label
                    )
                },
                enabled = kcal > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Add ${if (kcal > 0) kcal.toInt().toString() else ""} kcal")
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
            Text(
                "Don't know the exact food? Just log the number.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            NumberField(
                value = calories,
                onValueChange = { calories = it },
                label = "Calories",
                suffix = "kcal",
                allowDecimal = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 200, 350, 500).forEach { preset ->
                    ChoiceChip(
                        selected = calories == preset.toString(),
                        label = "$preset",
                        onClick = { calories = preset.toString() }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Meal")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MealType.BUILT_INS) { m ->
                    ChoiceChip(
                        selected = meal == m,
                        label = "${m.emoji} ${m.displayName}",
                        onClick = { meal = m }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Macros (optional)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = "Protein",
                    suffix = "g",
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = "Carbs",
                    suffix = "g",
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = "Fat",
                    suffix = "g",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (optional)") },
                placeholder = { Text("e.g. Dinner at Ravi's") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Logging to ${com.satya.calorietracker.util.DateUtils.prettyDate(date).lowercase()}.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}
