package com.satya.calorietracker.ui.addfood

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.MealType

/**
 * The "+ Add Food" sheet. Everything here is one tap from the dashboard, and the
 * recent-foods strip at the top makes repeating yesterday's breakfast a two-tap job.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(
    mealType: MealType,
    customMealName: String?,
    recentFoods: List<Food>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onFavorites: () -> Unit,
    onRecent: () -> Unit,
    onMyFoods: () -> Unit,
    onQuickAdd: () -> Unit,
    onCreateCustom: () -> Unit,
    onRecipes: () -> Unit,
    onRepeatFood: (Food) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Add to ${customMealName ?: mealType.displayName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pick up where you left off, or search something new.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (recentFoods.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Recently logged",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentFoods.take(10), key = { it.id }) { food ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { onRepeatFood(food) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    food.name.take(20),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                                Text(
                                    "${food.nutrients.calories.toInt()} kcal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            AddFoodOption(Icons.Outlined.Search, "Search food", "Millions of foods and products", onSearch)
            AddFoodOption(Icons.Outlined.CameraAlt, "Scan barcode", "Point the camera at a packet", onScan)
            AddFoodOption(Icons.Outlined.Star, "Favourites", "Your starred foods", onFavorites)
            AddFoodOption(Icons.Outlined.History, "Recent", "Everything you've logged before", onRecent)
            AddFoodOption(Icons.Outlined.MenuBook, "My foods", "Foods you created yourself", onMyFoods)
            AddFoodOption(Icons.Outlined.MenuBook, "Recipes", "Your saved meals and recipes", onRecipes)
            AddFoodOption(Icons.Outlined.Bolt, "Quick add calories", "When you only know the number", onQuickAdd)
            AddFoodOption(Icons.Outlined.Create, "Create a custom food", "Type the nutrition label in once", onCreateCustom)
        }
    }
}

@Composable
private fun AddFoodOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
