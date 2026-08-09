package com.satya.calorietracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.db.DailyTotalsRow
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.data.repository.WaterRepository
import com.satya.calorietracker.data.repository.WeightRepository
import com.satya.calorietracker.domain.model.LoggedFood
import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.Nutrients
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
import java.time.YearMonth

data class DaySummary(
    val date: LocalDate,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val entryCount: Int
)

data class HistoryUiState(
    val month: YearMonth = YearMonth.now(),
    val days: Map<LocalDate, DaySummary> = emptyMap(),
    val selected: LocalDate = DateUtils.today(),
    val selectedEntries: List<LoggedFood> = emptyList(),
    val selectedTotals: Nutrients = Nutrients.ZERO,
    val selectedWaterMl: Double = 0.0,
    val selectedWeightKg: Double? = null,
    val goals: NutritionGoals = NutritionGoals.DEFAULT,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val diaryRepository: DiaryRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    private val _selected = MutableStateFlow(DateUtils.today())
    val selected: StateFlow<LocalDate> = _selected.asStateFlow()

    val state: StateFlow<HistoryUiState> =
        combine(_month, _selected, preferencesRepository.preferences) { month, selected, prefs ->
            Triple(month, selected, prefs)
        }.flatMapLatest { (month, selected, prefs) ->
            val start = month.atDay(1)
            val end = month.atEndOfMonth()

            combine(
                diaryRepository.observeTotalsBetween(start, end),
                diaryRepository.observeEntries(selected),
                diaryRepository.observeTotals(selected),
                waterRepository.observeTotal(selected)
            ) { monthTotals, entries, totals, water ->
                HistoryUiState(
                    month = month,
                    days = monthTotals.associate { it.toSummary() },
                    selected = selected,
                    selectedEntries = entries,
                    selectedTotals = totals,
                    selectedWaterMl = water,
                    selectedWeightKg = weightRepository.forDate(selected)?.weightKg,
                    goals = prefs.goals,
                    unitSystem = prefs.unitSystem,
                    loading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private fun DailyTotalsRow.toSummary(): Pair<LocalDate, DaySummary> {
        val day = DateUtils.parse(date)
        return day to DaySummary(day, calories, protein, carbs, fat, fiber, entryCount)
    }

    fun selectDate(date: LocalDate) {
        _selected.value = date
        if (YearMonth.from(date) != _month.value) _month.value = YearMonth.from(date)
    }

    fun shiftMonth(months: Long) {
        _month.value = _month.value.plusMonths(months)
    }

    fun goToToday() {
        _month.value = YearMonth.now()
        _selected.value = DateUtils.today()
    }

    fun deleteEntry(entry: LoggedFood) {
        viewModelScope.launch { diaryRepository.deleteEntry(entry.id) }
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            HistoryViewModel(
                diaryRepository = container.diaryRepository,
                waterRepository = container.waterRepository,
                weightRepository = container.weightRepository,
                preferencesRepository = container.preferencesRepository
            )
        }
    }
}
