package com.satya.calorietracker.ui.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.db.RecipeWithIngredients
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.data.repository.RecipeRepository
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.ui.containerViewModelFactory
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class MealsTab(val label: String) {
    FAVORITES("Favourites"),
    MY_FOODS("My foods"),
    RECIPES("Recipes"),
    RECENT("Recent")
}

class MealsViewModel(
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository,
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    val favorites: StateFlow<List<Food>> = foodRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myFoods: StateFlow<List<Food>> = foodRepository.observeCustom()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recent: StateFlow<List<Food>> = foodRepository.observeRecent(60)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recipes: StateFlow<List<RecipeWithIngredients>> = recipeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(food: Food) {
        viewModelScope.launch { foodRepository.setFavorite(food.id, !food.isFavorite) }
    }

    fun togglePin(food: Food) {
        viewModelScope.launch { foodRepository.setPinned(food.id, !food.isPinned) }
    }

    fun deleteFood(food: Food) {
        viewModelScope.launch { foodRepository.deleteFood(food.id) }
    }

    fun deleteRecipe(recipe: RecipeWithIngredients) {
        viewModelScope.launch { recipeRepository.delete(recipe.recipe.id) }
    }

    fun toggleRecipeFavorite(recipe: RecipeWithIngredients) {
        viewModelScope.launch {
            recipeRepository.setFavorite(recipe.recipe.id, !recipe.recipe.isFavorite)
        }
    }

    /** One tap: log a default serving of this food into the meal that fits the clock. */
    fun quickLog(food: Food, date: LocalDate = DateUtils.today()) {
        viewModelScope.launch {
            diaryRepository.logFood(
                food = food,
                quantity = 1.0,
                servingSize = food.defaultAmount(),
                unit = if (food.servingSize != null) MeasureUnit.SERVING else food.perUnit,
                mealType = MealType.suggestedFor(java.time.LocalTime.now().hour),
                date = date
            )
        }
    }

    fun quickLogRecipe(recipe: RecipeWithIngredients, date: LocalDate = DateUtils.today()) {
        viewModelScope.launch {
            diaryRepository.logRecipe(
                recipe = recipe,
                servings = 1.0,
                mealType = MealType.suggestedFor(java.time.LocalTime.now().hour),
                date = date
            )
        }
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            MealsViewModel(
                foodRepository = container.foodRepository,
                recipeRepository = container.recipeRepository,
                diaryRepository = container.diaryRepository
            )
        }
    }
}
