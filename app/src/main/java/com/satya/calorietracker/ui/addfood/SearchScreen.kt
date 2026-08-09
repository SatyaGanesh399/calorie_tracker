package com.satya.calorietracker.ui.addfood

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.satya.calorietracker.data.db.RecipeWithIngredients
import com.satya.calorietracker.data.db.perServingNutrients
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.EmptyState
import com.satya.calorietracker.ui.components.InfoBanner
import com.satya.calorietracker.ui.components.LoadingLine
import com.satya.calorietracker.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    favorites: List<Food>,
    recent: List<Food>,
    myFoods: List<Food>,
    recipes: List<RecipeWithIngredients>,
    mealLabel: String,
    onQueryChange: (String) -> Unit,
    onTabChange: (SearchTab) -> Unit,
    onFoodClick: (Food) -> Unit,
    onFoodQuickAdd: (Food) -> Unit,
    onToggleFavorite: (Food) -> Unit,
    onRecipeClick: (RecipeWithIngredients) -> Unit,
    onScan: () -> Unit,
    onCreateCustom: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Straight into typing — the search screen exists to be typed in.
        runCatching { focusRequester.requestFocus() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to $mealLabel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Scan barcode")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search food, brand or barcode") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SearchTab.entries.toList()) { tab ->
                    ChoiceChip(
                        selected = state.tab == tab,
                        label = tab.label,
                        onClick = { onTabChange(tab) }
                    )
                }
            }

            if (state.searching) {
                Spacer(Modifier.height(8.dp))
                LoadingLine()
            }

            state.message?.let { message ->
                Spacer(Modifier.height(10.dp))
                InfoBanner(
                    message = message,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    actionLabel = if (state.offline) null else "Retry",
                    onAction = if (state.offline) null else onRetry
                )
            }

            Spacer(Modifier.height(8.dp))

            val listToShow: List<Food> = when (state.tab) {
                SearchTab.ALL -> if (state.query.isBlank()) recent else state.results
                SearchTab.FAVORITES -> favorites.filterQuery(state.query)
                SearchTab.RECENT -> recent.filterQuery(state.query)
                SearchTab.MY_FOODS -> myFoods.filterQuery(state.query)
                SearchTab.RECIPES -> emptyList()
            }

            if (state.tab == SearchTab.RECIPES) {
                RecipeResults(
                    recipes = if (state.query.isBlank()) recipes else recipes.filter {
                        it.recipe.name.contains(state.query, ignoreCase = true)
                    },
                    onClick = onRecipeClick
                )
            } else if (listToShow.isEmpty()) {
                when {
                    state.query.isBlank() && state.tab == SearchTab.ALL -> EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "Start typing",
                        message = "Search 3 million+ products, or scan a barcode. Everything you look up is saved so it works offline next time."
                    )
                    state.searching -> Box(Modifier.fillMaxSize())
                    else -> EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No matches",
                        message = "Nothing found for \"${state.query}\". You can add it yourself in about 20 seconds.",
                        actionLabel = "Create a custom food",
                        onAction = onCreateCustom
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(listToShow, key = { "${it.id}-${it.name}-${it.barcode ?: ""}" }) { food ->
                        FoodResultRow(
                            food = food,
                            onClick = { onFoodClick(food) },
                            onQuickAdd = { onFoodQuickAdd(food) },
                            onToggleFavorite = { onToggleFavorite(food) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Can't find it? Create a custom food →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onCreateCustom)
                                .padding(vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun List<Food>.filterQuery(query: String): List<Food> =
    if (query.isBlank()) this
    else filter {
        it.name.contains(query, true) || it.brand?.contains(query, true) == true
    }

@Composable
private fun FoodResultRow(
    food: Food,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (!food.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = food.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
            } else {
                Text(
                    text = food.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            Spacer(Modifier.height(2.dp))
            Text(
                text = food.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "P ${Format.grams(food.nutrients.protein)} g · C ${Format.grams(food.nutrients.carbs)} g · F ${Format.grams(food.nutrients.fat)} g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (food.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (food.isFavorite) "Remove favourite" else "Add favourite",
                tint = if (food.isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onQuickAdd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RecipeResults(
    recipes: List<RecipeWithIngredients>,
    onClick: (RecipeWithIngredients) -> Unit
) {
    if (recipes.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = "No recipes yet",
            message = "Build a recipe once — say your usual chicken rice bowl — and log the whole thing in one tap after that."
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(recipes, key = { it.recipe.id }) { recipe ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClick(recipe) }
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(recipe.recipe.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${recipe.ingredients.size} ingredients · ${recipe.recipe.servings.toInt()} servings",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${Format.kcal(recipe.perServingNutrients().calories)} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
