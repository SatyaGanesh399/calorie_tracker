package com.satya.calorietracker.ui.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.LoggedFood
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.domain.model.Nutrients
import com.satya.calorietracker.domain.units.UnitConverter
import com.satya.calorietracker.ui.containerViewModelFactory
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PortionUiState(
    val food: Food? = null,
    val entry: LoggedFood? = null,
    val title: String = "",
    val subtitle: String? = null,
    val quantityText: String = "1",
    val servingSizeText: String = "100",
    val unit: MeasureUnit = MeasureUnit.GRAM,
    val availableUnits: List<MeasureUnit> = MeasureUnit.MASS_UNITS + MeasureUnit.COUNT_UNITS,
    val mealType: MealType = MealType.SNACK,
    val customMealName: String? = null,
    val customMeals: List<String> = emptyList(),
    val date: LocalDate = DateUtils.today(),
    val timestamp: Long = DateUtils.nowMillis(),
    val notes: String = "",
    val computed: Nutrients = Nutrients.ZERO,
    val isFavorite: Boolean = false,
    val isEdit: Boolean = false,
    val loading: Boolean = true,
    val saved: Boolean = false,
    val error: String? = null
) {
    val quantity: Double get() = quantityText.toDoubleOrNull() ?: 0.0
    val servingSize: Double get() = servingSizeText.toDoubleOrNull() ?: 0.0
    val canSave: Boolean get() = quantity > 0 && (unit.isCount || servingSize > 0)
    val servingHint: String?
        get() = food?.servingLabel?.takeIf { it.isNotBlank() }
}

/**
 * Shared by "add a portion of this food" and "edit this diary row" — they are the same
 * form with a different save action, so they are the same ViewModel.
 */
class PortionViewModel(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PortionUiState())
    val state: StateFlow<PortionUiState> = _state.asStateFlow()

    /** Entry point for "log this food". */
    fun loadFood(foodId: Long, mealType: MealType, date: LocalDate, customMealName: String? = null) {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val food = foodRepository.getById(foodId)
            if (food == null) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "That food is no longer saved on this phone."
                )
                return@launch
            }

            val defaultUnit = if (food.servingSize != null) MeasureUnit.SERVING else food.perUnit
            val serving = food.servingSize ?: food.per

            _state.value = PortionUiState(
                food = food,
                title = food.name,
                subtitle = food.brand,
                quantityText = "1",
                servingSizeText = trim(serving),
                unit = defaultUnit,
                availableUnits = MeasureUnit.optionsFor(food.perUnit),
                mealType = mealType,
                customMealName = customMealName,
                customMeals = prefs.customMeals,
                date = date,
                isFavorite = food.isFavorite,
                loading = false
            ).recompute()
        }
    }

    /** Entry point for "edit this diary row". */
    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val entry = diaryRepository.getEntry(entryId)
            if (entry == null) {
                _state.value = _state.value.copy(loading = false, error = "That entry has been deleted.")
                return@launch
            }
            val food = entry.foodId?.let { foodRepository.getById(it) }

            _state.value = PortionUiState(
                food = food,
                entry = entry,
                title = entry.name,
                subtitle = entry.brand,
                quantityText = trim(entry.quantity),
                servingSizeText = trim(entry.servingSize),
                unit = entry.unit,
                availableUnits = food?.let { MeasureUnit.optionsFor(it.perUnit) }
                    ?: (MeasureUnit.MASS_UNITS + MeasureUnit.VOLUME_UNITS + MeasureUnit.COUNT_UNITS),
                mealType = entry.mealType,
                customMealName = entry.customMealName,
                customMeals = prefs.customMeals,
                date = DateUtils.parse(entry.date),
                timestamp = entry.timestamp,
                notes = entry.notes.orEmpty(),
                computed = entry.nutrients,
                isFavorite = food?.isFavorite ?: false,
                isEdit = true,
                loading = false
            )
        }
    }

    // ------------------------------------------------------------ mutations

    fun setQuantity(text: String) {
        _state.value = _state.value.copy(quantityText = text).recompute()
    }

    fun setServingSize(text: String) {
        _state.value = _state.value.copy(servingSizeText = text).recompute()
    }

    fun setUnit(unit: MeasureUnit) {
        val current = _state.value
        val food = current.food

        // Convert the serving amount so switching g -> oz keeps the same real portion.
        val newServing = when {
            unit.isCount -> food?.servingSize ?: food?.per ?: 1.0
            current.unit.isCount -> food?.servingSize ?: food?.per ?: 100.0
            else -> UnitConverter.convert(current.servingSize, current.unit, unit)
        }

        _state.value = current.copy(
            unit = unit,
            servingSizeText = trim(newServing)
        ).recompute()
    }

    fun setMeal(mealType: MealType, customName: String? = null) {
        _state.value = _state.value.copy(mealType = mealType, customMealName = customName)
    }

    fun setDate(date: LocalDate) {
        _state.value = _state.value.copy(date = date)
    }

    fun setTime(hour: Int, minute: Int) {
        val current = _state.value
        val millis = DateUtils.toMillis(current.date, java.time.LocalTime.of(hour, minute))
        _state.value = current.copy(timestamp = millis)
    }

    fun setNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun toggleFavorite() {
        val food = _state.value.food ?: return
        viewModelScope.launch {
            val persisted = foodRepository.ensurePersisted(food)
            val newValue = !persisted.isFavorite
            foodRepository.setFavorite(persisted.id, newValue)
            _state.value = _state.value.copy(
                food = persisted.copy(isFavorite = newValue),
                isFavorite = newValue
            )
        }
    }

    /** Multiply the portion — the "×2" style shortcut buttons. */
    fun scaleQuantity(factor: Double) {
        val current = _state.value.quantity.takeIf { it > 0 } ?: 1.0
        setQuantity(trim(current * factor))
    }

    // ---------------------------------------------------------------- save

    fun save(onDone: () -> Unit) {
        val current = _state.value
        if (!current.canSave) {
            _state.value = current.copy(error = "Enter an amount greater than zero.")
            return
        }

        viewModelScope.launch {
            try {
                if (current.isEdit && current.entry != null) {
                    val updated = current.entry.copy(
                        date = DateUtils.iso(current.date),
                        timestamp = current.timestamp,
                        mealTypeId = current.mealType.id,
                        customMealName = current.customMealName,
                        quantity = current.quantity,
                        servingSize = current.servingSize,
                        unitId = current.unit.id,
                        notes = current.notes.takeIf { it.isNotBlank() },
                        nutrients = current.computed
                    )
                    diaryRepository.updateEntry(updated, recomputeFrom = current.food)
                } else {
                    val food = current.food ?: return@launch
                    diaryRepository.logFood(
                        food = food,
                        quantity = current.quantity,
                        servingSize = current.servingSize,
                        unit = current.unit,
                        mealType = current.mealType,
                        customMealName = current.customMealName,
                        date = current.date,
                        timestamp = current.timestamp,
                        notes = current.notes.takeIf { it.isNotBlank() }
                    )
                }
                _state.value = _state.value.copy(saved = true, error = null)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Couldn't save that: ${e.message ?: "database error"}"
                )
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            diaryRepository.deleteEntry(entry.id)
            onDone()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ------------------------------------------------------------ internals

    private fun PortionUiState.recompute(): PortionUiState {
        val food = food
        val computed = when {
            food != null -> UnitConverter.nutrientsFor(food, quantity, servingSize, unit)
            entry != null && entry.quantity > 0 ->
                entry.nutrients * (quantity / entry.quantity)
            else -> Nutrients.ZERO
        }
        return copy(computed = computed)
    }

    private fun trim(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

    companion object {
        val Factory = containerViewModelFactory { container ->
            PortionViewModel(
                foodRepository = container.foodRepository,
                diaryRepository = container.diaryRepository,
                preferencesRepository = container.preferencesRepository
            )
        }
    }
}
