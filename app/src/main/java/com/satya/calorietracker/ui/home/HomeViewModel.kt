package com.satya.calorietracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.data.repository.WaterRepository
import com.satya.calorietracker.data.repository.WeightEntry
import com.satya.calorietracker.data.repository.WeightRepository
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.LoggedFood
import com.satya.calorietracker.domain.model.MealType
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

/** A meal card on the dashboard: its entries and their combined calories. */
data class MealSection(
    val mealType: MealType,
    val customName: String? = null,
    val entries: List<LoggedFood> = emptyList()
) {
    val label: String get() = customName ?: mealType.displayName
    val calories: Double get() = entries.sumOf { it.nutrients.calories }
    val key: String get() = customName ?: mealType.id
}

data class HomeUiState(
    val date: LocalDate = DateUtils.today(),
    val goals: NutritionGoals = NutritionGoals.DEFAULT,
    val totals: Nutrients = Nutrients.ZERO,
    val waterMl: Double = 0.0,
    val entries: List<LoggedFood> = emptyList(),
    val meals: List<MealSection> = emptyList(),
    val latestWeight: WeightEntry? = null,
    val weeklyWeightChange: Double? = null,
    val recentFoods: List<Food> = emptyList(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val quickWaterAmounts: List<Int> = listOf(250, 500, 750, 1000),
    val customMeals: List<String> = emptyList(),
    val loading: Boolean = true
) {
    val caloriesConsumed: Double get() = totals.calories
    val caloriesRemaining: Double get() = goals.calories - totals.calories
    val calorieProgress: Float
        get() = if (goals.calories <= 0) 0f else (totals.calories / goals.calories).toFloat()
    val isToday: Boolean get() = date == DateUtils.today()
    val hasAnything: Boolean get() = entries.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val diaryRepository: DiaryRepository,
    private val waterRepository: WaterRepository,
    private val weightRepository: WeightRepository,
    private val foodRepository: FoodRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _date = MutableStateFlow(DateUtils.today())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    /** Set by the swipe-to-delete undo snackbar. */
    private val _lastDeleted = MutableStateFlow<LoggedFood?>(null)
    val lastDeleted: StateFlow<LoggedFood?> = _lastDeleted.asStateFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(_date, preferencesRepository.preferences) { date, prefs -> date to prefs }
            .flatMapLatest { (date, prefs) ->
                combine(
                    diaryRepository.observeEntries(date),
                    diaryRepository.observeTotals(date),
                    waterRepository.observeTotal(date),
                    weightRepository.observeLatest(),
                    foodRepository.observeRecent(12)
                ) { entries, totals, water, weight, recent ->
                    HomeUiState(
                        date = date,
                        goals = prefs.goals,
                        totals = totals,
                        waterMl = water,
                        entries = entries,
                        meals = buildMeals(entries, prefs.customMeals),
                        latestWeight = weight,
                        weeklyWeightChange = weightRepository.changeOverDays(7),
                        recentFoods = recent,
                        unitSystem = prefs.unitSystem,
                        quickWaterAmounts = prefs.quickWaterAmountsMl,
                        customMeals = prefs.customMeals,
                        loading = false
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * Always show the four built-in meals (even empty, so there's an obvious place to
     * tap), then any custom meal that has something in it or that the user defined.
     */
    private fun buildMeals(entries: List<LoggedFood>, customMeals: List<String>): List<MealSection> {
        val builtIns = MealType.BUILT_INS.map { type ->
            MealSection(
                mealType = type,
                entries = entries.filter { it.mealType == type && it.customMealName == null }
            )
        }
        val customNames = (customMeals + entries.mapNotNull { it.customMealName }).distinct()
        val customs = customNames.map { name ->
            MealSection(
                mealType = MealType.CUSTOM,
                customName = name,
                entries = entries.filter { it.customMealName == name }
            )
        }
        return builtIns + customs
    }

    fun selectDate(date: LocalDate) {
        _date.value = date
    }

    fun goToToday() {
        _date.value = DateUtils.today()
    }

    fun shiftDate(days: Long) {
        _date.value = _date.value.plusDays(days)
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch { waterRepository.add(amountMl.toDouble(), _date.value) }
    }

    fun undoWater() {
        viewModelScope.launch { waterRepository.undoLast(_date.value) }
    }

    fun deleteEntry(entry: LoggedFood) {
        viewModelScope.launch {
            diaryRepository.deleteEntry(entry.id)
            _lastDeleted.value = entry
        }
    }

    /** Restores the row the user just swiped away. */
    fun undoDelete() {
        val entry = _lastDeleted.value ?: return
        viewModelScope.launch {
            diaryRepository.repeat(entry.copy(id = 0L), DateUtils.parse(entry.date), entry.mealType)
            _lastDeleted.value = null
        }
    }

    fun clearUndo() {
        _lastDeleted.value = null
    }

    fun repeatFood(food: Food, mealType: MealType) {
        viewModelScope.launch {
            diaryRepository.logFood(
                food = food,
                quantity = 1.0,
                servingSize = food.defaultAmount(),
                unit = food.perUnit,
                mealType = mealType,
                date = _date.value
            )
        }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch { weightRepository.log(weightKg, _date.value) }
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            HomeViewModel(
                diaryRepository = container.diaryRepository,
                waterRepository = container.waterRepository,
                weightRepository = container.weightRepository,
                foodRepository = container.foodRepository,
                preferencesRepository = container.preferencesRepository
            )
        }
    }
}
