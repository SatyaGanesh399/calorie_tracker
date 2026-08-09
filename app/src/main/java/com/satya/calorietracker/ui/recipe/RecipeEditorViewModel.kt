package com.satya.calorietracker.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.db.RecipeEntity
import com.satya.calorietracker.data.db.RecipeIngredientEntity
import com.satya.calorietracker.data.db.toNutrients
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.data.repository.RecipeRepository
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.domain.model.Nutrients
import com.satya.calorietracker.ui.containerViewModelFactory
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RecipeEditorUiState(
    val recipeId: Long = 0L,
    val name: String = "",
    val servingsText: String = "1",
    val notes: String = "",
    val ingredients: List<RecipeIngredientEntity> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Food> = emptyList(),
    val searching: Boolean = false,
    val searchMessage: String? = null,
    val saved: Boolean = false,
    val error: String? = null,
    val loading: Boolean = false
) {
    val servings: Double get() = servingsText.toDoubleOrNull()?.coerceAtLeast(0.0001) ?: 1.0

    val total: Nutrients
        get() = ingredients.fold(Nutrients.ZERO) { acc, ing -> acc + ing.toNutrients() }

    val perServing: Nutrients get() = total / servings

    val canSave: Boolean get() = name.isNotBlank() && ingredients.isNotEmpty()
}

/**
 * Recipes are stored with their ingredients' nutrition already resolved, so a recipe
 * total never silently changes because a source food was edited later.
 */
class RecipeEditorViewModel(
    private val recipeRepository: RecipeRepository,
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeEditorUiState())
    val state: StateFlow<RecipeEditorUiState> = _state.asStateFlow()

    fun load(recipeId: Long) {
        if (recipeId == 0L) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val recipe = recipeRepository.getById(recipeId)
            if (recipe == null) {
                _state.value = _state.value.copy(loading = false, error = "That recipe no longer exists.")
                return@launch
            }
            _state.value = RecipeEditorUiState(
                recipeId = recipe.recipe.id,
                name = recipe.recipe.name,
                servingsText = recipe.recipe.servings.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                },
                notes = recipe.recipe.notes.orEmpty(),
                ingredients = recipe.ingredients.sortedBy { it.position },
                loading = false
            )
        }
    }

    fun setName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun setServings(text: String) {
        _state.value = _state.value.copy(servingsText = text)
    }

    fun setNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    // ------------------------------------------------------- ingredient search

    fun onSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList(), searchMessage = null)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                searchResults = foodRepository.searchLocal(query),
                searching = true
            )
            val aggregated = foodRepository.search(query)
            _state.value = _state.value.copy(
                searchResults = aggregated.foods,
                searching = false,
                searchMessage = aggregated.message
            )
        }
    }

    fun addIngredient(food: Food, quantity: Double, servingSize: Double, unit: MeasureUnit) {
        viewModelScope.launch {
            val persisted = foodRepository.ensurePersisted(food)
            val ingredient = recipeRepository.ingredientFrom(
                food = persisted,
                quantity = quantity,
                servingSize = servingSize,
                unit = unit,
                position = _state.value.ingredients.size
            )
            _state.value = _state.value.copy(
                ingredients = _state.value.ingredients + ingredient,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun removeIngredient(index: Int) {
        val list = _state.value.ingredients.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _state.value = _state.value.copy(
                ingredients = list.mapIndexed { i, ing -> ing.copy(position = i) }
            )
        }
    }

    // --------------------------------------------------------------- persist

    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        if (!current.canSave) {
            _state.value = current.copy(error = "A recipe needs a name and at least one ingredient.")
            return
        }
        viewModelScope.launch {
            try {
                val id = recipeRepository.save(
                    RecipeEntity(
                        id = current.recipeId,
                        name = current.name.trim(),
                        servings = current.servings,
                        notes = current.notes.takeIf { it.isNotBlank() }
                    ),
                    current.ingredients
                )
                _state.value = current.copy(saved = true, recipeId = id, error = null)
                onSaved(id)
            } catch (e: Exception) {
                _state.value = current.copy(error = "Couldn't save: ${e.message ?: "database error"}")
            }
        }
    }

    fun logServings(servings: Double, mealType: MealType, date: LocalDate = DateUtils.today(), onDone: () -> Unit) {
        val id = _state.value.recipeId
        if (id == 0L) return
        viewModelScope.launch {
            recipeRepository.getById(id)?.let { recipe ->
                diaryRepository.logRecipe(recipe, servings, mealType, date)
                onDone()
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            RecipeEditorViewModel(
                recipeRepository = container.recipeRepository,
                foodRepository = container.foodRepository,
                diaryRepository = container.diaryRepository
            )
        }
    }
}
