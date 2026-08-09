package com.satya.calorietracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<RecipeIngredientEntity>
)

@Dao
interface RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY isFavorite DESC, name ASC")
    fun observeAll(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeWithIngredients?

    @Transaction
    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    suspend fun search(query: String, limit: Int): List<RecipeWithIngredients>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    suspend fun getAll(): List<RecipeWithIngredients>

    @Query("SELECT COUNT(*) FROM recipes")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsFor(recipeId: Long)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Long)

    @Query("UPDATE recipes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Transaction
    suspend fun saveRecipe(recipe: RecipeEntity, ingredients: List<RecipeIngredientEntity>): Long {
        val id = insertRecipe(recipe)
        val recipeId = if (recipe.id == 0L) id else recipe.id
        deleteIngredientsFor(recipeId)
        insertIngredients(ingredients.mapIndexed { index, ing ->
            ing.copy(id = 0L, recipeId = recipeId, position = index)
        })
        return recipeId
    }

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()
}
