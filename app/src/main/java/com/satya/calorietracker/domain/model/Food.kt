package com.satya.calorietracker.domain.model

import kotlinx.serialization.Serializable

enum class FoodSource(val id: String, val label: String) {
    OPEN_FOOD_FACTS("OPEN_FOOD_FACTS", "Open Food Facts"),
    USDA("USDA", "USDA FoodData Central"),
    LOCAL("LOCAL", "Built-in database"),
    CUSTOM("CUSTOM", "My food"),
    RECIPE("RECIPE", "Recipe"),
    QUICK_ADD("QUICK_ADD", "Quick add");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: LOCAL
    }
}

/**
 * A food *definition* (a catalog item), not something that has been eaten.
 *
 * [nutrients] are expressed per [per] [perUnit] — almost always per 100 g or per 100 ml,
 * which is the convention both Open Food Facts and USDA use.
 */
@Serializable
data class Food(
    val id: Long = 0L,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val sourceId: String = FoodSource.LOCAL.id,
    val providerId: String? = null,
    val per: Double = 100.0,
    val perUnitId: String = MeasureUnit.GRAM.id,
    val nutrients: Nutrients = Nutrients.ZERO,
    /** Default single serving expressed in [perUnit], e.g. 37.0 for a 37 g scoop. */
    val servingSize: Double? = null,
    /** Human label for that serving, e.g. "1 heaping scoop (37 g)". */
    val servingLabel: String? = null,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isCustom: Boolean = false,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = 0L
) {
    val source: FoodSource get() = FoodSource.fromId(sourceId)
    val perUnit: MeasureUnit get() = MeasureUnit.fromId(perUnitId)

    /** "Brand · 165 kcal / 100 g" style subtitle. */
    val subtitle: String
        get() = buildString {
            if (!brand.isNullOrBlank()) append(brand).append(" · ")
            append("${nutrients.calories.toInt()} kcal / ${per.trimZeros()} ${perUnit.label}")
        }

    /** The amount that should be pre-filled when the user opens this food. */
    fun defaultAmount(): Double = servingSize ?: per
}

private fun Double.trimZeros(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

/** A food that has actually been eaten, i.e. a diary row. */
@Serializable
data class LoggedFood(
    val id: Long = 0L,
    val date: String,
    val timestamp: Long,
    val mealTypeId: String = MealType.SNACK.id,
    val customMealName: String? = null,
    val foodId: Long? = null,
    val recipeId: Long? = null,
    val name: String,
    val brand: String? = null,
    val quantity: Double = 1.0,
    val servingSize: Double = 100.0,
    val unitId: String = MeasureUnit.GRAM.id,
    val nutrients: Nutrients = Nutrients.ZERO,
    val notes: String? = null,
    val imageUrl: String? = null
) {
    val mealType: MealType get() = MealType.fromId(mealTypeId)
    val unit: MeasureUnit get() = MeasureUnit.fromId(unitId)

    /** "1.5 × 100 g" or "2 servings". */
    val portionLabel: String
        get() {
            val q = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else "%.2f".format(quantity).trimEnd('0').trimEnd('.')
            return if (unit.isCount) {
                "$q ${if (quantity == 1.0) unit.label else unit.plural}"
            } else {
                val size = if (servingSize % 1.0 == 0.0) servingSize.toInt().toString() else "%.1f".format(servingSize)
                if (quantity == 1.0) "$size ${unit.label}" else "$q × $size ${unit.label}"
            }
        }

    val mealLabel: String get() = customMealName ?: mealType.displayName
}
