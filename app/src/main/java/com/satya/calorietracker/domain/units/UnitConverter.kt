package com.satya.calorietracker.domain.units

import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.domain.model.Nutrients
import com.satya.calorietracker.domain.model.UnitDimension

/**
 * All unit maths lives here so the rest of the app never guesses.
 *
 * Rules:
 *  - MASS <-> MASS and VOLUME <-> VOLUME convert exactly.
 *  - MASS <-> VOLUME uses a density of 1 g/ml. That is exact for water and close
 *    enough for milk, juice and soft drinks, which is where it actually gets used.
 *    The UI never silently mixes dimensions except in that fallback.
 *  - COUNT units (piece / serving / slice / ...) resolve through the food's own
 *    serving size, falling back to its reference amount (usually 100 g).
 */
object UnitConverter {

    /** Convert [amount] from one unit to another within the same dimension. */
    fun convert(amount: Double, from: MeasureUnit, to: MeasureUnit): Double {
        if (from == to) return amount
        if (from.isCount || to.isCount) return amount
        val base = amount * from.factorToBase          // grams or millilitres
        return base / to.factorToBase                   // density 1 across dimensions
    }

    fun canConvert(from: MeasureUnit, to: MeasureUnit): Boolean =
        from == to || (!from.isCount && !to.isCount)

    /**
     * How much of the food's reference unit (g or ml) does [quantity] [unit] represent?
     *
     * Examples:
     *  - 1.5 × 100 g of a per-100 g food -> 150 g
     *  - 2 scoops of a food with servingSize = 37 g -> 74 g
     */
    fun amountInReferenceUnit(
        food: Food,
        quantity: Double,
        servingSize: Double,
        unit: MeasureUnit
    ): Double {
        if (quantity <= 0.0) return 0.0
        return if (unit.isCount) {
            val oneServing = food.servingSize ?: food.per
            quantity * oneServing * unit.factorToBase
        } else {
            convert(quantity * servingSize, unit, food.perUnit)
        }
    }

    /** Nutrition for an arbitrary portion of a food. */
    fun nutrientsFor(
        food: Food,
        quantity: Double,
        servingSize: Double,
        unit: MeasureUnit
    ): Nutrients {
        val reference = food.per.takeIf { it > 0.0 } ?: 100.0
        val amount = amountInReferenceUnit(food, quantity, servingSize, unit)
        return food.nutrients * (amount / reference)
    }

    // ---------------------------------------------------------------- water

    fun toMillilitres(amount: Double, unit: MeasureUnit): Double = when (unit.dimension) {
        UnitDimension.VOLUME -> amount * unit.factorToBase
        UnitDimension.MASS -> amount * unit.factorToBase   // density 1
        UnitDimension.COUNT -> amount * 250.0              // a "glass"
    }

    fun fromMillilitres(ml: Double, unit: MeasureUnit): Double = when (unit.dimension) {
        UnitDimension.VOLUME, UnitDimension.MASS -> ml / unit.factorToBase
        UnitDimension.COUNT -> ml / 250.0
    }

    // --------------------------------------------------------------- weight

    fun kgToLb(kg: Double): Double = kg / MeasureUnit.POUND.factorToBase * 1000.0
    fun lbToKg(lb: Double): Double = lb * MeasureUnit.POUND.factorToBase / 1000.0

    fun cmToFeetInches(cm: Double): Pair<Int, Double> {
        val totalInches = cm / 2.54
        val feet = (totalInches / 12).toInt()
        return feet to (totalInches - feet * 12)
    }

    fun feetInchesToCm(feet: Int, inches: Double): Double = (feet * 12 + inches) * 2.54
}
