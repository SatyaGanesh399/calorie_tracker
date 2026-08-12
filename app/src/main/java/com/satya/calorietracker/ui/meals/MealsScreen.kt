package com.satya.calorietracker.ui.meals

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.satya.calorietracker.data.db.RecipeWithIngredients
import com.satya.calorietracker.data.db.perServingNutrients
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.EmptyState
import com.satya.calorietracker.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(
    onBack: () -> Unit,
    favorites: List<Food>,
    myFoods: List<Food>,
    recent: List<Food>,
    recipes: List<RecipeWithIngredients>,
    onFoodClick: (Food) -> Unit,
    onQuickLog: (Food) -> Unit,
    onToggleFavorite: (Food) -> Unit,
    onTogglePin: (Food) -> Unit,
    onEditFood: (Food) -> Unit,
    onDeleteFood: (Food) -> Unit,
    onRecipeClick: (RecipeWithIngredients) -> Unit,
    onQuickLogRecipe: (RecipeWithIngredients) -> Unit,
    onEditRecipe: (RecipeWithIngredients) -> Unit,
    onDeleteRecipe: (RecipeWithIngredients) -> Unit,
    onCreateFood: () -> Unit,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var tab by remember { mutableStateOf(MealsTab.FAVORITES) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My food library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { barPadding ->
    Box(modifier.fillMaxSize().padding(barPadding)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Everything you eat often, one tap away.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MealsTab.entries.toList()) { t ->
                    ChoiceChip(selected = tab == t, label = t.label, onClick = { tab = t })
                }
            }

            Spacer(Modifier.height(10.dp))

            when (tab) {
                MealsTab.FAVORITES -> FoodList(
                    foods = favorites,
                    emptyTitle = "No favourites yet",
                    emptyMessage = "Tap the star on any food and it lands here, ready to log in one tap.",
                    onFoodClick = onFoodClick,
                    onQuickLog = onQuickLog,
                    onToggleFavorite = onToggleFavorite,
                    onTogglePin = onTogglePin,
                    onEdit = onEditFood,
                    onDelete = onDeleteFood,
                    contentPadding = contentPadding
                )

                MealsTab.MY_FOODS -> FoodList(
                    foods = myFoods,
                    emptyTitle = "No custom foods",
                    emptyMessage = "Type a nutrition label in once — homemade curry, your protein powder — and it's yours forever.",
                    emptyAction = "Create a food" to onCreateFood,
                    onFoodClick = onFoodClick,
                    onQuickLog = onQuickLog,
                    onToggleFavorite = onToggleFavorite,
                    onTogglePin = onTogglePin,
                    onEdit = onEditFood,
                    onDelete = onDeleteFood,
                    contentPadding = contentPadding
                )

                MealsTab.RECENT -> FoodList(
                    foods = recent,
                    emptyTitle = "Nothing logged yet",
                    emptyMessage = "Foods you log show up here so repeating a meal takes two taps.",
                    onFoodClick = onFoodClick,
                    onQuickLog = onQuickLog,
                    onToggleFavorite = onToggleFavorite,
                    onTogglePin = onTogglePin,
                    onEdit = onEditFood,
                    onDelete = onDeleteFood,
                    contentPadding = contentPadding
                )

                MealsTab.RECIPES -> RecipeList(
                    recipes = recipes,
                    onClick = onRecipeClick,
                    onQuickLog = onQuickLogRecipe,
                    onEdit = onEditRecipe,
                    onDelete = onDeleteRecipe,
                    onCreate = onCreateRecipe,
                    contentPadding = contentPadding
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = if (tab == MealsTab.RECIPES) onCreateRecipe else onCreateFood,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(if (tab == MealsTab.RECIPES) "New recipe" else "New food") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp)
        )
    }
    }
}

@Composable
private fun FoodList(
    foods: List<Food>,
    emptyTitle: String,
    emptyMessage: String,
    onFoodClick: (Food) -> Unit,
    onQuickLog: (Food) -> Unit,
    onToggleFavorite: (Food) -> Unit,
    onTogglePin: (Food) -> Unit,
    onEdit: (Food) -> Unit,
    onDelete: (Food) -> Unit,
    contentPadding: PaddingValues,
    emptyAction: Pair<String, () -> Unit>? = null
) {
    if (foods.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.MenuBook,
            title = emptyTitle,
            message = emptyMessage,
            actionLabel = emptyAction?.first,
            onAction = emptyAction?.second
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(foods, key = { it.id }) { food ->
            FoodLibraryRow(
                food = food,
                onClick = { onFoodClick(food) },
                onQuickLog = { onQuickLog(food) },
                onToggleFavorite = { onToggleFavorite(food) },
                onTogglePin = { onTogglePin(food) },
                onEdit = { onEdit(food) },
                onDelete = { onDelete(food) }
            )
        }
    }
}

@Composable
private fun FoodLibraryRow(
    food: Food,
    onClick: () -> Unit,
    onQuickLog: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (food.isPinned) {
            Icon(
                Icons.Filled.PushPin,
                contentDescription = "Pinned",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            Text(
                food.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (food.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favourite",
                tint = if (food.isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onQuickLog),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Log now",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (food.isPinned) "Unpin" else "Pin to top") },
                    onClick = { menuOpen = false; onTogglePin() }
                )
                if (food.isCustom) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeList(
    recipes: List<RecipeWithIngredients>,
    onClick: (RecipeWithIngredients) -> Unit,
    onQuickLog: (RecipeWithIngredients) -> Unit,
    onEdit: (RecipeWithIngredients) -> Unit,
    onDelete: (RecipeWithIngredients) -> Unit,
    onCreate: () -> Unit,
    contentPadding: PaddingValues
) {
    if (recipes.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.MenuBook,
            title = "No recipes yet",
            message = "Add the ingredients once, say how many servings it makes, and log a portion whenever you cook it.",
            actionLabel = "Create a recipe",
            onAction = onCreate
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(recipes, key = { it.recipe.id }) { recipe ->
            var menuOpen by remember { mutableStateOf(false) }
            val perServing = recipe.perServingNutrients()

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
                        "${Format.kcal(perServing.calories)} kcal / serving · makes ${recipe.recipe.servings.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onQuickLog(recipe) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Log a serving",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { menuOpen = false; onEdit(recipe) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menuOpen = false; onDelete(recipe) }
                        )
                    }
                }
            }
        }
    }
}
