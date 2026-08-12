package com.satya.calorietracker.ui.workout

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.data.db.ExerciseEntity
import com.satya.calorietracker.data.db.SessionWithSets
import com.satya.calorietracker.data.db.WorkoutSetEntity
import com.satya.calorietracker.data.repository.inputMode
import com.satya.calorietracker.data.repository.subtitle
import com.satya.calorietracker.domain.model.SetInputMode
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.domain.units.UnitConverter
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.EmptyState
import com.satya.calorietracker.ui.components.NumberField
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.components.StatValue
import com.satya.calorietracker.util.DateUtils
import com.satya.calorietracker.util.Format
import kotlin.math.roundToInt

@Composable
fun WorkoutsScreen(
    state: WorkoutUiState,
    onStartWorkout: () -> Unit,
    onFinishWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit,
    onAddExercise: () -> Unit,
    onAddSet: (ExerciseEntity, Double, Int, Int?, Double?, Boolean) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRemoveExercise: (ExerciseEntity) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onRepeatSession: (SessionWithSets) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var showDiscardConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 40.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Workouts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item { WeekSummaryCard(state) }

        if (state.hasActiveSession) {
            item {
                ActiveSessionHeader(
                    state = state,
                    onFinish = onFinishWorkout,
                    onDiscard = { showDiscardConfirm = true }
                )
            }

            items(
                count = state.activeExercises.size,
                key = { state.activeExercises[it].exercise.id }
            ) { index ->
                val entry = state.activeExercises[index]
                ExerciseCard(
                    entry = entry,
                    unitSystem = state.unitSystem,
                    onAddSet = { w, r, d, dist, warm -> onAddSet(entry.exercise, w, r, d, dist, warm) },
                    onDeleteSet = onDeleteSet,
                    onRemove = { onRemoveExercise(entry.exercise) }
                )
            }

            item {
                OutlinedButton(
                    onClick = onAddExercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
            }
        } else {
            item {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start a workout", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (state.recentSessions.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.FitnessCenter,
                        title = "No workouts yet",
                        message = "Start one, pick your exercises, and log each set as you go. Weights and reps build into strength charts over time."
                    )
                }
            } else {
                item { SectionHeader("Recent workouts") }
                items(
                    count = state.recentSessions.size,
                    key = { state.recentSessions[it].session.id }
                ) { index ->
                    val session = state.recentSessions[index]
                    PastSessionCard(
                        session = session,
                        unitSystem = state.unitSystem,
                        onRepeat = { onRepeatSession(session) },
                        onDelete = { onDeleteSession(session.session.id) }
                    )
                }
            }
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard this workout?") },
            text = { Text("Every set you've logged in this session will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDiscardWorkout()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Keep going") }
            }
        )
    }
}

// ------------------------------------------------------------------ summary

@Composable
private fun WeekSummaryCard(state: WorkoutUiState) {
    AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatValue("${state.weekSessionCount}", "Sessions this week")
            StatValue(formatVolume(state.weekVolumeKg, state.unitSystem), "Volume this week")
            StatValue(
                if (state.hasActiveSession) "${state.activeSetCount}" else "—",
                "Sets today"
            )
        }
    }
}

@Composable
private fun ActiveSessionHeader(
    state: WorkoutUiState,
    onFinish: () -> Unit,
    onDiscard: () -> Unit
) {
    val session = state.activeSession?.session ?: return
    val minutes = ((DateUtils.nowMillis() - session.startedAt) / 60_000).coerceAtLeast(0)

    AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "In progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                session.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Started ${DateUtils.millisToTimeLabel(session.startedAt)} · ${minutes} min · " +
                    "${state.activeSetCount} sets · ${formatVolume(state.activeVolumeKg, state.unitSystem)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Finish") }
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
            }
        }
    }
}

// ------------------------------------------------------------ exercise card

@Composable
private fun ExerciseCard(
    entry: SessionExercise,
    unitSystem: UnitSystem,
    onAddSet: (Double, Int, Int?, Double?, Boolean) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val mode = entry.exercise.inputMode
    val unitLabel = Format.weightUnitLabel(unitSystem)

    // Pre-filled from last time, because repeating last session's numbers is the
    // overwhelmingly common case and retyping them at the gym is friction.
    val lastWeightDisplay = entry.lastTime?.weightKg?.let { toDisplayWeight(it, unitSystem) }
    var weight by remember(entry.exercise.id) {
        mutableStateOf(lastWeightDisplay?.let { trimNumber(it) } ?: "")
    }
    var reps by remember(entry.exercise.id) {
        mutableStateOf(entry.lastTime?.reps?.takeIf { it > 0 }?.toString() ?: "")
    }
    var minutes by remember(entry.exercise.id) { mutableStateOf("") }
    var distance by remember(entry.exercise.id) { mutableStateOf("") }
    var warmup by remember(entry.exercise.id) { mutableStateOf(false) }

    AppCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        entry.exercise.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Exercise options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Remove from workout") },
                            onClick = { menuOpen = false; onRemove() }
                        )
                    }
                }
            }

            entry.lastTime?.let { last ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Last time: ${describeSet(last, unitSystem)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (entry.sets.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                entry.sets.forEachIndexed { index, set ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SetRow(
                        index = index + 1,
                        set = set,
                        unitSystem = unitSystem,
                        onDelete = { onDeleteSet(set.id) }
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Volume ${formatVolume(entry.volumeKg, unitSystem)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------------------------ set entry row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (mode) {
                    SetInputMode.WEIGHT_REPS -> {
                        NumberField(weight, { weight = it }, "Weight", Modifier.weight(1f), suffix = unitLabel)
                        NumberField(reps, { reps = it }, "Reps", Modifier.weight(1f), allowDecimal = false)
                    }
                    SetInputMode.REPS_ONLY -> {
                        NumberField(weight, { weight = it }, "Added", Modifier.weight(1f), suffix = unitLabel)
                        NumberField(reps, { reps = it }, "Reps", Modifier.weight(1f), allowDecimal = false)
                    }
                    SetInputMode.DURATION -> {
                        NumberField(weight, { weight = it }, "Added", Modifier.weight(1f), suffix = unitLabel)
                        NumberField(minutes, { minutes = it }, "Minutes", Modifier.weight(1f))
                    }
                    SetInputMode.DURATION_DISTANCE -> {
                        NumberField(minutes, { minutes = it }, "Minutes", Modifier.weight(1f))
                        NumberField(distance, { distance = it }, "Distance", Modifier.weight(1f), suffix = "km")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mode == SetInputMode.WEIGHT_REPS || mode == SetInputMode.REPS_ONLY) {
                    TextButton(onClick = { warmup = !warmup }) {
                        Text(if (warmup) "Warm-up ✓" else "Warm-up")
                    }
                }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(
                    onClick = {
                        val kg = weight.toDoubleOrNull()?.let { fromDisplayWeight(it, unitSystem) } ?: 0.0
                        val repCount = reps.toIntOrNull() ?: 0
                        val seconds = minutes.toDoubleOrNull()?.let { (it * 60).roundToInt() }
                        val metres = distance.toDoubleOrNull()?.let { it * 1000 }

                        val valid = when (mode) {
                            SetInputMode.WEIGHT_REPS, SetInputMode.REPS_ONLY -> repCount > 0
                            SetInputMode.DURATION -> (seconds ?: 0) > 0
                            SetInputMode.DURATION_DISTANCE -> (seconds ?: 0) > 0 || (metres ?: 0.0) > 0
                        }
                        if (valid) {
                            onAddSet(kg, repCount, seconds, metres, warmup)
                            warmup = false
                        }
                    }
                ) { Text("Add set") }
            }
        }
    }
}

@Composable
private fun SetRow(
    index: Int,
    set: WorkoutSetEntity,
    unitSystem: UnitSystem,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (set.isWarmup) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (set.isWarmup) "W" else "$index",
                style = MaterialTheme.typography.labelMedium,
                color = if (set.isWarmup) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            describeSet(set, unitSystem),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (set.estimatedOneRepMax > 0) {
            Text(
                "e1RM ${Format.decimal(toDisplayWeight(set.estimatedOneRepMax, unitSystem), 1)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete set",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ------------------------------------------------------------------ history

@Composable
private fun PastSessionCard(
    session: SessionWithSets,
    unitSystem: UnitSystem,
    onRepeat: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AppCard(onClick = { expanded = !expanded }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(session.session.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append(DateUtils.prettyDate(DateUtils.parse(session.session.date)))
                            session.durationMinutes?.let { append(" · ${it} min") }
                            append(" · ${session.sets.size} sets")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatVolume(session.totalVolumeKg, unitSystem),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Session options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Do this workout again") },
                            onClick = { menuOpen = false; onRepeat() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                session.exerciseNames.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) 10 else 2
            )

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    session.sets.groupBy { it.exerciseName }.forEach { (name, sets) ->
                        Text(
                            name,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            sets.joinToString("   ") { describeSetCompact(it, unitSystem) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ helpers

private fun toDisplayWeight(kg: Double, system: UnitSystem): Double =
    if (system == UnitSystem.METRIC) kg else UnitConverter.kgToLb(kg)

private fun fromDisplayWeight(value: Double, system: UnitSystem): Double =
    if (system == UnitSystem.METRIC) value else UnitConverter.lbToKg(value)

private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else Format.decimal(value, 1)

private fun formatVolume(kg: Double, system: UnitSystem): String {
    if (kg <= 0) return "—"
    val value = toDisplayWeight(kg, system)
    val unit = Format.weightUnitLabel(system)
    return if (value >= 1000) "${Format.decimal(value / 1000, 1)}t" else "${value.roundToInt()} $unit"
}

private fun describeSet(set: WorkoutSetEntity, system: UnitSystem): String = buildString {
    val unit = Format.weightUnitLabel(system)
    if (set.weightKg > 0) append("${trimNumber(toDisplayWeight(set.weightKg, system))} $unit")
    if (set.reps > 0) {
        if (isNotEmpty()) append(" × ")
        append("${set.reps} reps")
    }
    set.durationSeconds?.takeIf { it > 0 }?.let {
        if (isNotEmpty()) append(" · ")
        append(if (it >= 60) "${it / 60} min" else "$it s")
    }
    set.distanceMeters?.takeIf { it > 0 }?.let {
        if (isNotEmpty()) append(" · ")
        append("${Format.decimal(it / 1000, 2)} km")
    }
    if (isEmpty()) append("Logged")
}

private fun describeSetCompact(set: WorkoutSetEntity, system: UnitSystem): String = when {
    set.weightKg > 0 && set.reps > 0 ->
        "${trimNumber(toDisplayWeight(set.weightKg, system))}×${set.reps}"
    set.reps > 0 -> "${set.reps}"
    set.durationSeconds != null -> "${(set.durationSeconds ?: 0) / 60}m"
    else -> "—"
}
