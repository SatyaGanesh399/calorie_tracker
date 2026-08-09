package com.satya.calorietracker.ui.addfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.db.RecipeWithIngredients
import com.satya.calorietracker.data.repository.DiaryRepository
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.data.repository.RecipeRepository
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.ui.containerViewModelFactory
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class SearchTab(val label: String) {
    ALL("All"),
    FAVORITES("Favourites"),
    RECENT("Recent"),
    MY_FOODS("My foods"),
    RECIPES("Recipes")
}

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.ALL,
    val results: List<Food> = emptyList(),
    val recipes: List<RecipeWithIngredients> = emptyList(),
    val searching: Boolean = false,
    val message: String? = null,
    val offline: Boolean = false,
    val hasSearched: Boolean = false
)

/**
 * Search runs in two passes: the local catalog answers immediately (so the list never
 * sits empty while a request is in flight), then the network result is merged in.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository,
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val favorites: StateFlow<List<Food>> = foodRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recent: StateFlow<List<Food>> = foodRepository.observeRecent(60)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myFoods: StateFlow<List<Food>> = foodRepository.observeCustom()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allRecipes: StateFlow<List<RecipeWithIngredients>> = recipeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var remoteJob: Job? = null

    init {
        _query
            .debounce(320)
            .distinctUntilChanged()
            .onEach { query -> runSearch(query) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _query.value = query
        _state.value = _state.value.copy(query = query)
        if (query.isBlank()) {
            remoteJob?.cancel()
            _state.value = _state.value.copy(
                results = emptyList(),
                recipes = emptyList(),
                searching = false,
                message = null,
                hasSearched = false
            )
        }
    }

    fun setTab(tab: SearchTab) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun retry() {
        viewModelScope.launch { runSearch(_query.value) }
    }

    private suspend fun runSearch(query: String) {
        if (query.isBlank()) return

        // Pass 1 — local, instant.
        val localHits = foodRepository.searchLocal(query)
        val localRecipes = recipeRepository.search(query)
        _state.value = _state.value.copy(
            results = localHits,
            recipes = localRecipes,
            searching = true,
            message = null,
            hasSearched = true
        )

        // Pass 2 — providers.
        remoteJob?.cancel()
        remoteJob = viewModelScope.launch {
            val aggregated = foodRepository.search(query)
            _state.value = _state.value.copy(
                results = aggregated.foods,
                searching = false,
                message = aggregated.message,
                offline = aggregated.offline
            )
        }
    }

    fun toggleFavorite(food: Food) {
        viewModelScope.launch {
            val persisted = foodRepository.ensurePersisted(food)
            foodRepository.setFavorite(persisted.id, !persisted.isFavorite)
        }
    }

    /** One-tap log with the food's default serving — the fastest possible path. */
    fun logDefaultServing(food: Food, mealType: MealType, date: LocalDate = DateUtils.today()) {
        viewModelScope.launch {
            diaryRepository.logFood(
                food = food,
                quantity = 1.0,
                servingSize = food.defaultAmount(),
                unit = if (food.servingSize != null) com.satya.calorietracker.domain.model.MeasureUnit.SERVING
                else food.perUnit,
                mealType = mealType,
                date = date
            )
        }
    }

    fun logRecipe(recipe: RecipeWithIngredients, mealType: MealType, date: LocalDate = DateUtils.today()) {
        viewModelScope.launch {
            diaryRepository.logRecipe(recipe, servings = 1.0, mealType = mealType, date = date)
        }
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            SearchViewModel(
                foodRepository = container.foodRepository,
                recipeRepository = container.recipeRepository,
                diaryRepository = container.diaryRepository
            )
        }
    }
}
