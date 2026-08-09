package com.satya.calorietracker.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * A bundle of nutrition values. Always absolute amounts (not per-100g) unless the
 * owning type documents otherwise.
 *
 * Units: calories = kcal, protein/carbs/fat/fiber/sugar = grams, sodium = milligrams.
 * A `null`-like "unknown" value is represented as [UNKNOWN] (-1.0) so we can tell
 * "this food genuinely has 0 g fibre" apart from "the database didn't tell us".
 */
@Serializable
data class Nutrients(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0
) {
    operator fun plus(other: Nutrients) = Nutrients(
        calories = calories + other.calories,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
        fiber = fiber + other.fiber,
        sugar = sugar + other.sugar,
        sodium = sodium + other.sodium
    )

    operator fun times(factor: Double) = Nutrients(
        calories = calories * factor,
        protein = protein * factor,
        carbs = carbs * factor,
        fat = fat * factor,
        fiber = fiber * factor,
        sugar = sugar * factor,
        sodium = sodium * factor
    )

    operator fun div(divisor: Double): Nutrients =
        if (divisor == 0.0) ZERO else times(1.0 / divisor)

    /** kcal derived from macros. Used to sanity-check or fill in missing calorie values. */
    val caloriesFromMacros: Double
        get() = protein * 4 + carbs * 4 + fat * 9

    /** Percentage split of energy between protein / carbs / fat. Sums to ~100. */
    fun macroSplit(): Triple<Int, Int, Int> {
        val total = caloriesFromMacros
        if (total <= 0.0) return Triple(0, 0, 0)
        val p = (protein * 4 / total * 100).roundToInt()
        val c = (carbs * 4 / total * 100).roundToInt()
        return Triple(p, c, (100 - p - c).coerceAtLeast(0))
    }

    /** If a provider gave us macros but no energy, derive it rather than showing 0 kcal. */
    fun withDerivedCaloriesIfMissing(): Nutrients =
        if (calories > 0.0 || caloriesFromMacros <= 0.0) this
        else copy(calories = caloriesFromMacros)

    companion object {
        val ZERO = Nutrients()
        const val UNKNOWN = -1.0
    }
}

/** Daily targets the user is trying to hit. */
@Serializable
data class NutritionGoals(
    val calories: Int = 2000,
    val protein: Int = 120,
    val carbs: Int = 220,
    val fat: Int = 65,
    val fiber: Int = 30,
    val waterMl: Int = 2500
) {
    companion object {
        val DEFAULT = NutritionGoals()
    }
}
