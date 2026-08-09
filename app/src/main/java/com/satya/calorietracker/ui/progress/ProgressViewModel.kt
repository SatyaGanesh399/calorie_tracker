package com.satya.calorietracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.repository.CalorieStats
import com.satya.calorietracker.data.repository.DayPoint
import com.satya.calorietracker.data.repository.MacroStats
import com.satya.calorietracker.data.repository.StatsRange
import com.satya.calorietracker.data.repository.StatsRepository
import com.satya.calorietracker.data.repository.WaterRepository
import com.satya.calorietracker.data.repository.WeightEntry
import com.satya.calorietracker.data.repository.WeightRepository
import com.satya.calorietracker.data.repository.WeightSummary
import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.ui.containerViewModelFactory
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProgressUiState(
    val range: StatsRange = StatsRange.MONTH,
    val weightEntries: List<WeightEntry> = emptyList(),
    val weightSummary: WeightSummary = WeightSummary(),
    val waterSeries: List<DayPoint> = emptyList(),
    val calorieStats: CalorieStats = CalorieStats(),
    val macroStats: MacroStats = MacroStats(),
    val streak: Int = 0,
    val goals: NutritionGoals = NutritionGoals.DEFAULT,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val goalWeightKg: Double? = null,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val weightRepository: WeightRepository,
    private val waterRepository: WaterRepository,
    private val statsRepository: StatsRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _range = MutableStateFlow(StatsRange.MONTH)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    /** Bumped after a write so the derived (suspend-computed) sections refresh. */
    private val _refresh = MutableStateFlow(0)

    val state: StateFlow<ProgressUiState> =
        combine(_range, preferencesRepository.preferences, _refresh) { range, prefs, _ ->
            Triple(range, prefs, Unit)
        }.flatMapLatest { (range, prefs, _) ->
            val start = statsRepository.rangeStart(range)
            combine(
                weightRepository.observeBetween(start, DateUtils.today()),
                weightRepository.observeAll()
            ) { inRange, all ->
                ProgressUiState(
                    range = range,
                    weightEntries = inRange.ifEmpty { all.takeLast(2) },
                    weightSummary = weightRepository.summary(prefs.goalWeightKg),
                    waterSeries = statsRepository.waterTotals(range),
                    calorieStats = statsRepository.calorieStats(range),
                    macroStats = statsRepository.macroStats(range),
                    streak = statsRepository.currentStreak(),
                    goals = prefs.goals,
                    unitSystem = prefs.unitSystem,
                    goalWeightKg = prefs.goalWeightKg,
                    loading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun setRange(range: StatsRange) {
        _range.value = range
    }

    fun logWeight(weightKg: Double, date: LocalDate = DateUtils.today(), notes: String? = null) {
        viewModelScope.launch {
            weightRepository.log(weightKg, date, notes)
            _refresh.value++
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            weightRepository.delete(id)
            _refresh.value++
        }
    }

    fun setGoalWeight(kg: Double) {
        viewModelScope.launch {
            preferencesRepository.setGoalWeight(kg)
            _refresh.value++
        }
    }

    fun addWater(ml: Double, date: LocalDate = DateUtils.today()) {
        viewModelScope.launch {
            waterRepository.add(ml, date)
            _refresh.value++
        }
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            ProgressViewModel(
                weightRepository = container.weightRepository,
                waterRepository = container.waterRepository,
                statsRepository = container.statsRepository,
                preferencesRepository = container.preferencesRepository
            )
        }
    }
}
