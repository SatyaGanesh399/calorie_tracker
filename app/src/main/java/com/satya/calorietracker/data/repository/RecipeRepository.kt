package com.satya.calorietracker.data.repository

import com.satya.calorietracker.data.db.RecipeDao
import com.satya.calorietracker.data.db.RecipeEntity
import com.satya.calorietracker.data.db.RecipeIngredientEntity
import com.satya.calorietracker.data.db.RecipeWithIngredients
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.domain.model.Nutrients
import com.satya.calorietracker.domain.units.UnitConverter
import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val notifier: DataChangeNotifier = DataChangeNotifier.NONE
) {

    fun observeAll(): Flow<List<RecipeWithIngredients>> = recipeDao.observeAll()

    fun observeById(id: Long): Flow<RecipeWithIngredients?> = recipeDao.observeById(id)

    fun observeCount(): Flow<Int> = recipeDao.observeCount()

    suspend fun getById(id: Long): RecipeWithIngredients? = recipeDao.getById(id)

    suspend fun search(query: String): List<RecipeWithIngredients> = recipeDao.search(query, limit = 30)

    suspend fun save(
        recipe: RecipeEntity,
        ingredients: List<RecipeIngredientEntity>
    ): Long {
        val now = System.currentTimeMillis()
        val id = recipeDao.saveRecipe(
            recipe.copy(
                createdAt = if (recipe.createdAt == 0L) now else recipe.createdAt,
                updatedAt = now
            ),
            ingredients
        )
        notifier.onDataChanged()
        return id
    }

    suspend fun delete(id: Long) {
        recipeDao.deleteRecipeById(id)
        notifier.onDataChanged()
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = recipeDao.setFavorite(id, favorite)

    suspend fun exportAll(): List<RecipeWithIngredients> = recipeDao.getAll()

    /**
     * Turn a food + portion into a recipe ingredient, resolving its nutrition once so
     * the recipe total is stable even if the food is later edited.
     */
    fun ingredientFrom(
        food: Food,
        quantity: Double,
        servingSize: Double,
        unit: MeasureUnit,
        position: Int = 0
    ): RecipeIngredientEntity {
        val n: Nutrients = UnitConverter.nutrientsFor(food, quantity, servingSize, unit)
        return RecipeIngredientEntity(
            recipeId = 0L,
            foodId = food.id.takeIf { it != 0L },
            name = food.name,
            quantity = quantity,
            servingSize = servingSize,
            unitId = unit.id,
            calories = n.calories,
            protein = n.protein,
            carbs = n.carbs,
            fat = n.fat,
            fiber = n.fiber,
            sugar = n.sugar,
            sodium = n.sodium,
            position = position
        )
    }
}
