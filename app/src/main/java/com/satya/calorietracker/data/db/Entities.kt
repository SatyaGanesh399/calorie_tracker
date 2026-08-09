package com.satya.calorietracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Catalog of known foods: built-in seeds, cached API results, custom foods. */
@Entity(
    tableName = "foods",
    indices = [
        Index("name"),
        Index("barcode"),
        Index("lastUsedAt"),
        Index("isFavorite"),
        Index(value = ["providerId", "providerRef"], unique = true)
    ]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    /** FoodSource id: OPEN_FOOD_FACTS / USDA / LOCAL / CUSTOM / RECIPE / QUICK_ADD */
    val sourceId: String,
    /** Which provider produced this row, null for user-created foods. */
    val providerId: String? = null,
    /** The provider's own identifier, used with providerId as a dedupe key. */
    val providerRef: String? = null,

    val per: Double = 100.0,
    val perUnitId: String = "g",

    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0,

    val servingSize: Double? = null,
    val servingLabel: String? = null,
    val imageUrl: String? = null,

    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isCustom: Boolean = false,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0L,
    /** When this cached copy was fetched, so stale API rows can be refreshed. */
    val cachedAt: Long = 0L
)

/** One row of the food diary. Nutrition is stored absolute so history never changes. */
@Entity(
    tableName = "log_entries",
    indices = [Index("date"), Index("timestamp"), Index("foodId"), Index(value = ["date", "mealTypeId"])]
)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val timestamp: Long,
    val mealTypeId: String,
    val customMealName: String? = null,
    val foodId: Long? = null,
    val recipeId: Long? = null,

    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,

    val quantity: Double = 1.0,
    val servingSize: Double = 100.0,
    val unitId: String = "g",

    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0,

    val notes: String? = null
)

@Entity(tableName = "weight_entries", indices = [Index(value = ["date"], unique = true), Index("timestamp")])
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val timestamp: Long,
    /** Always kilograms. Display conversion happens in the UI layer. */
    val weightKg: Double,
    val notes: String? = null
)

@Entity(tableName = "water_entries", indices = [Index("date"), Index("timestamp")])
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val timestamp: Long,
    /** Always millilitres. */
    val amountMl: Double
)

@Entity(tableName = "recipes", indices = [Index("name")])
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val servings: Double = 1.0,
    val notes: String? = null,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Entity(
    tableName = "recipe_ingredients",
    indices = [Index("recipeId")],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recipeId: Long,
    val foodId: Long? = null,
    val name: String,
    val quantity: Double = 1.0,
    val servingSize: Double = 100.0,
    val unitId: String = "g",

    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0,

    val position: Int = 0
)

// ---------------------------------------------------------------- projections

/** Aggregated nutrition for a single day, produced by a SUM() query. */
data class DailyTotalsRow(
    val date: String,
    @ColumnInfo(name = "calories") val calories: Double = 0.0,
    @ColumnInfo(name = "protein") val protein: Double = 0.0,
    @ColumnInfo(name = "carbs") val carbs: Double = 0.0,
    @ColumnInfo(name = "fat") val fat: Double = 0.0,
    @ColumnInfo(name = "fiber") val fiber: Double = 0.0,
    @ColumnInfo(name = "sugar") val sugar: Double = 0.0,
    @ColumnInfo(name = "sodium") val sodium: Double = 0.0,
    @ColumnInfo(name = "entryCount") val entryCount: Int = 0
)

/** Calories grouped by meal, for the dashboard meal list. */
data class MealTotalsRow(
    val mealTypeId: String,
    val customMealName: String?,
    val calories: Double,
    val entryCount: Int
)

data class DateAmountRow(
    val date: String,
    val amount: Double
)
