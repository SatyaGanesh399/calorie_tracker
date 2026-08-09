package com.satya.calorietracker.data.db

import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.LoggedFood
import com.satya.calorietracker.domain.model.Nutrients

fun FoodEntity.toDomain(): Food = Food(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    sourceId = sourceId,
    providerId = providerId,
    per = per,
    perUnitId = perUnitId,
    nutrients = Nutrients(calories, protein, carbs, fat, fiber, sugar, sodium),
    servingSize = servingSize,
    servingLabel = servingLabel,
    imageUrl = imageUrl,
    isFavorite = isFavorite,
    isPinned = isPinned,
    isCustom = isCustom,
    useCount = useCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt
)

fun Food.toEntity(providerRef: String? = null, cachedAt: Long = 0L): FoodEntity = FoodEntity(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    sourceId = sourceId,
    providerId = providerId,
    providerRef = providerRef ?: barcode,
    per = per,
    perUnitId = perUnitId,
    calories = nutrients.calories,
    protein = nutrients.protein,
    carbs = nutrients.carbs,
    fat = nutrients.fat,
    fiber = nutrients.fiber,
    sugar = nutrients.sugar,
    sodium = nutrients.sodium,
    servingSize = servingSize,
    servingLabel = servingLabel,
    imageUrl = imageUrl,
    isFavorite = isFavorite,
    isPinned = isPinned,
    isCustom = isCustom,
    useCount = useCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    cachedAt = cachedAt
)

fun LogEntryEntity.toDomain(): LoggedFood = LoggedFood(
    id = id,
    date = date,
    timestamp = timestamp,
    mealTypeId = mealTypeId,
    customMealName = customMealName,
    foodId = foodId,
    recipeId = recipeId,
    name = name,
    brand = brand,
    quantity = quantity,
    servingSize = servingSize,
    unitId = unitId,
    nutrients = Nutrients(calories, protein, carbs, fat, fiber, sugar, sodium),
    notes = notes,
    imageUrl = imageUrl
)

fun LoggedFood.toEntity(): LogEntryEntity = LogEntryEntity(
    id = id,
    date = date,
    timestamp = timestamp,
    mealTypeId = mealTypeId,
    customMealName = customMealName,
    foodId = foodId,
    recipeId = recipeId,
    name = name,
    brand = brand,
    imageUrl = imageUrl,
    quantity = quantity,
    servingSize = servingSize,
    unitId = unitId,
    calories = nutrients.calories,
    protein = nutrients.protein,
    carbs = nutrients.carbs,
    fat = nutrients.fat,
    fiber = nutrients.fiber,
    sugar = nutrients.sugar,
    sodium = nutrients.sodium,
    notes = notes
)

fun DailyTotalsRow.toNutrients(): Nutrients =
    Nutrients(calories, protein, carbs, fat, fiber, sugar, sodium)

fun RecipeIngredientEntity.toNutrients(): Nutrients =
    Nutrients(calories, protein, carbs, fat, fiber, sugar, sodium)

fun RecipeWithIngredients.totalNutrients(): Nutrients =
    ingredients.fold(Nutrients.ZERO) { acc, ing -> acc + ing.toNutrients() }

fun RecipeWithIngredients.perServingNutrients(): Nutrients =
    totalNutrients() / recipe.servings.coerceAtLeast(0.0001)
