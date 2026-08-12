package com.satya.calorietracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.db.ExerciseEntity
import com.satya.calorietracker.data.db.SessionWithSets
import com.satya.calorietracker.data.db.WorkoutSetEntity
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.repository.WorkoutRepository
import com.satya.calorietracker.domain.model.Equipment
import com.satya.calorietracker.domain.model.ExerciseCategory
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.ui.containerViewModelFactory
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One exercise inside the session being edited, with the sets logged for it so far. */
data class SessionExercise(
    val exercise: ExerciseEntity,
    val sets: List<WorkoutSetEntity> = emptyList(),
    val lastTime: WorkoutSetEntity? = null
) {
    val workingSets: List<WorkoutSetEntity> get() = sets.filterNot { it.isWarmup }
    val volumeKg: Double get() = sets.sumOf { it.volumeKg }
    val bestSet: WorkoutSetEntity? get() = workingSets.maxByOrNull { it.weightKg }
}

data class WorkoutUiState(
    val activeSession: SessionWithSets? = null,
    val activeExercises: List<SessionExercise> = emptyList(),
    val todaySessions: List<SessionWithSets> = emptyList(),
    val recentSessions: List<SessionWithSets> = emptyList(),
    val weekSessionCount: Int = 0,
    val weekVolumeKg: Double = 0.0,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val loading: Boolean = true
) {
    val hasActiveSession: Boolean get() = activeSession != null
    val activeVolumeKg: Double get() = activeExercises.sumOf { it.volumeKg }
    val activeSetCount: Int get() = activeExercises.sumOf { it.sets.size }
}

data class ExercisePickerState(
    val query: String = "",
    val category: String = "ALL",
    val results: List<ExerciseEntity> = emptyList(),
    val recent: List<ExerciseEntity> = emptyList(),
    val loading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    /**
     * Exercises the user has picked but not yet logged a set for. Sets are what make an
     * exercise real in the database, so without this an exercise would vanish between
     * choosing it and typing the first number.
     */
    private val _pendingExercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())

    /** Cached "last time you did this" rows, so each exercise card can show a target. */
    private val _lastPerformance = MutableStateFlow<Map<Long, WorkoutSetEntity>>(emptyMap())

    private val _picker = MutableStateFlow(ExercisePickerState())
    val picker: StateFlow<ExercisePickerState> = _picker.asStateFlow()

    private val _refresh = MutableStateFlow(0)

    val state: StateFlow<WorkoutUiState> = combine(
        workoutRepository.observeActiveSession(),
        workoutRepository.observeRecentSessions(30),
        _pendingExercises,
        _lastPerformance,
        combine(preferencesRepository.preferences, _refresh) { prefs, _ -> prefs }
    ) { active, recent, pending, lastPerf, prefs ->
        val weekStart = DateUtils.today().minusDays(6)
        val weekVolume = workoutRepository.volumeBetween(weekStart).sumOf { it.volumeKg }
        val weekSessions = recent.count { DateUtils.parse(it.session.date) >= weekStart }

        WorkoutUiState(
            activeSession = active,
            activeExercises = buildExercises(active, pending, lastPerf),
            todaySessions = recent.filter { it.session.date == DateUtils.todayIso() },
            recentSessions = recent.filter { it.session.endedAt != null },
            weekSessionCount = weekSessions,
            weekVolumeKg = weekVolume,
            unitSystem = prefs.unitSystem,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    /**
     * Merges exercises that already have sets with ones just picked, preserving the
     * order they were added to the session.
     */
    private suspend fun buildExercises(
        active: SessionWithSets?,
        pending: List<ExerciseEntity>,
        lastPerf: Map<Long, WorkoutSetEntity>
    ): List<SessionExercise> {
        if (active == null) return emptyList()

        val byExercise = active.sets.groupBy { it.exerciseId }
        val loggedIds = byExercise.keys

        val logged = byExercise.entries
            .sortedBy { (_, sets) -> sets.minOf { it.timestamp } }
            .mapNotNull { (id, sets) ->
                val exercise = workoutRepository.getExercise(id) ?: return@mapNotNull null
                SessionExercise(exercise, sets.sortedBy { it.setNumber }, lastPerf[id])
            }

        val notYetLogged = pending
            .filterNot { it.id in loggedIds }
            .map { SessionExercise(it, emptyList(), lastPerf[it.id]) }

        return logged + notYetLogged
    }

    // ------------------------------------------------------------- session

    fun startWorkout() {
        viewModelScope.launch {
            workoutRepository.startSession()
            _pendingExercises.value = emptyList()
        }
    }

    fun finishWorkout() {
        val sessionId = state.value.activeSession?.session?.id ?: return
        viewModelScope.launch {
            // An empty workout is a mis-tap, not a training session worth keeping.
            if (state.value.activeSetCount == 0) workoutRepository.deleteSession(sessionId)
            else workoutRepository.finishSession(sessionId)
            _pendingExercises.value = emptyList()
        }
    }

    fun discardWorkout() {
        val sessionId = state.value.activeSession?.session?.id ?: return
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
            _pendingExercises.value = emptyList()
        }
    }

    fun renameSession(name: String, notes: String?) {
        val sessionId = state.value.activeSession?.session?.id ?: return
        viewModelScope.launch { workoutRepository.renameSession(sessionId, name, notes) }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { workoutRepository.deleteSession(sessionId) }
    }

    /** Repeat a past session's exercise selection as a fresh workout. */
    fun repeatSession(session: SessionWithSets) {
        viewModelScope.launch {
            workoutRepository.startSession(name = session.session.name)
            val exercises = session.sets
                .map { it.exerciseId }
                .distinct()
                .mapNotNull { workoutRepository.getExercise(it) }
            _pendingExercises.value = exercises
            exercises.forEach { cacheLastPerformance(it.id) }
        }
    }

    // ---------------------------------------------------------------- sets

    fun addSet(
        exercise: ExerciseEntity,
        weightKg: Double,
        reps: Int,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
        isWarmup: Boolean = false
    ) {
        val sessionId = state.value.activeSession?.session?.id ?: return
        viewModelScope.launch {
            workoutRepository.addSet(
                sessionId = sessionId,
                exercise = exercise,
                weightKg = weightKg,
                reps = reps,
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
                isWarmup = isWarmup
            )
        }
    }

    fun updateSet(set: WorkoutSetEntity) {
        viewModelScope.launch { workoutRepository.updateSet(set) }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch { workoutRepository.deleteSet(setId) }
    }

    fun removeExerciseFromSession(exercise: ExerciseEntity) {
        _pendingExercises.value = _pendingExercises.value.filterNot { it.id == exercise.id }
        val sets = state.value.activeExercises.firstOrNull { it.exercise.id == exercise.id }?.sets.orEmpty()
        viewModelScope.launch { sets.forEach { workoutRepository.deleteSet(it.id) } }
    }

    // --------------------------------------------------------- exercise picker

    fun openPicker() {
        viewModelScope.launch {
            _picker.value = _picker.value.copy(loading = true)
            _picker.value = ExercisePickerState(
                results = workoutRepository.searchExercises(""),
                recent = workoutRepository.recentExercises(),
                loading = false
            )
        }
    }

    fun onPickerQuery(query: String) {
        _picker.value = _picker.value.copy(query = query)
        viewModelScope.launch {
            val results =
                if (query.isBlank() && _picker.value.category != "ALL") {
                    workoutRepository.exercisesByCategory(_picker.value.category)
                } else {
                    workoutRepository.searchExercises(query)
                }
            _picker.value = _picker.value.copy(results = results)
        }
    }

    fun onPickerCategory(category: String) {
        _picker.value = _picker.value.copy(category = category)
        viewModelScope.launch {
            val results = if (_picker.value.query.isBlank()) {
                workoutRepository.exercisesByCategory(category)
            } else {
                workoutRepository.searchExercises(_picker.value.query)
                    .filter { category == "ALL" || it.category == category }
            }
            _picker.value = _picker.value.copy(results = results)
        }
    }

    /** Adds the exercise to the current workout, starting one if needed. */
    fun selectExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            if (state.value.activeSession == null) workoutRepository.startSession()
            if (_pendingExercises.value.none { it.id == exercise.id }) {
                _pendingExercises.value = _pendingExercises.value + exercise
            }
            cacheLastPerformance(exercise.id)
        }
    }

    fun createCustomExercise(
        name: String,
        category: ExerciseCategory,
        equipment: Equipment,
        muscle: String,
        onCreated: (ExerciseEntity) -> Unit
    ) {
        viewModelScope.launch {
            val id = workoutRepository.createCustomExercise(name, category, equipment, muscle)
            workoutRepository.getExercise(id)?.let { created ->
                onCreated(created)
                selectExercise(created)
            }
            onPickerQuery(_picker.value.query)
        }
    }

    fun toggleExerciseFavorite(exercise: ExerciseEntity) {
        viewModelScope.launch {
            workoutRepository.setExerciseFavorite(exercise.id, !exercise.isFavorite)
            onPickerQuery(_picker.value.query)
        }
    }

    private suspend fun cacheLastPerformance(exerciseId: Long) {
        val last = workoutRepository.lastPerformance(exerciseId) ?: return
        _lastPerformance.value = _lastPerformance.value + (exerciseId to last)
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            WorkoutViewModel(
                workoutRepository = container.workoutRepository,
                preferencesRepository = container.preferencesRepository
            )
        }
    }
}
