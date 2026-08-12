package com.satya.calorietracker.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.data.db.ExerciseEntity
import com.satya.calorietracker.data.repository.subtitle
import com.satya.calorietracker.domain.model.Equipment
import com.satya.calorietracker.domain.model.ExerciseCategory
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.NumberField

/**
 * Exercise picker. Opens straight into the search field — with ~250 exercises,
 * typing three letters is faster than any amount of scrolling — with category chips
 * and a recently-used row for the things you actually train.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    state: ExercisePickerState,
    sheetState: SheetState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSelect: (ExerciseEntity) -> Unit,
    onToggleFavorite: (ExerciseEntity) -> Unit,
    onCreateCustom: (String, ExerciseCategory, Equipment, String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Add exercise",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search 250+ exercises") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ChoiceChip(
                        selected = state.category == "ALL",
                        label = "All",
                        onClick = { onCategoryChange("ALL") }
                    )
                }
                items(ExerciseCategory.entries.toList()) { category ->
                    ChoiceChip(
                        selected = state.category == category.id,
                        label = "${category.emoji} ${category.label}",
                        onClick = { onCategoryChange(category.id) }
                    )
                }
            }

            if (state.recent.isNotEmpty() && state.query.isBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Recently trained",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recent, key = { it.id }) { exercise ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelect(exercise) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.results, key = { it.id }) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        onClick = { onSelect(exercise) },
                        onToggleFavorite = { onToggleFavorite(exercise) }
                    )
                }

                item {
                    TextButton(
                        onClick = { showCreate = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.query.isBlank()) "Create a custom exercise"
                            else "Create \"${state.query}\""
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateExerciseDialog(
            initialName = state.query,
            onDismiss = { showCreate = false },
            onCreate = { name, category, equipment, muscle ->
                showCreate = false
                onCreateCustom(name, category, equipment, muscle)
            }
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                exercise.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                if (exercise.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favourite",
                modifier = Modifier.size(18.dp),
                tint = if (exercise.isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateExerciseDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onCreate: (String, ExerciseCategory, Equipment, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var muscle by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.FULL_BODY) }
    var equipment by remember { mutableStateOf(Equipment.BARBELL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ExerciseCategory.entries.toList()) { option ->
                        ChoiceChip(
                            selected = category == option,
                            label = option.label,
                            onClick = { category = option }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Equipment", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Equipment.entries.toList()) { option ->
                        ChoiceChip(
                            selected = equipment == option,
                            label = option.label,
                            onClick = { equipment = option }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = muscle,
                    onValueChange = { muscle = it },
                    label = { Text("Main muscle (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), category, equipment, muscle.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Create and add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
