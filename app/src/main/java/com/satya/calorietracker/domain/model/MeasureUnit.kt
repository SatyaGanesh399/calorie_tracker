package com.satya.calorietracker.domain.model

/** What kind of quantity a unit measures. Conversion is only allowed inside a dimension. */
enum class UnitDimension { MASS, VOLUME, COUNT }

/**
 * Every unit the app understands.
 *
 * [factorToBase] converts an amount in this unit to the dimension's base unit:
 *  - MASS   base = gram
 *  - VOLUME base = millilitre
 *  - COUNT  base = "one serving of this food", so the factor is applied to the food's
 *           own serving size at calculation time (see UnitConverter).
 */
enum class MeasureUnit(
    val id: String,
    val label: String,
    val plural: String,
    val dimension: UnitDimension,
    val factorToBase: Double
) {
    GRAM("g", "g", "g", UnitDimension.MASS, 1.0),
    KILOGRAM("kg", "kg", "kg", UnitDimension.MASS, 1000.0),
    MILLIGRAM("mg", "mg", "mg", UnitDimension.MASS, 0.001),
    OUNCE("oz", "oz", "oz", UnitDimension.MASS, 28.349523125),
    POUND("lb", "lb", "lb", UnitDimension.MASS, 453.59237),

    MILLILITRE("ml", "ml", "ml", UnitDimension.VOLUME, 1.0),
    LITRE("l", "L", "L", UnitDimension.VOLUME, 1000.0),
    FLUID_OUNCE("floz", "fl oz", "fl oz", UnitDimension.VOLUME, 29.5735295625),
    CUP("cup", "cup", "cups", UnitDimension.VOLUME, 240.0),
    TABLESPOON("tbsp", "tbsp", "tbsp", UnitDimension.VOLUME, 15.0),
    TEASPOON("tsp", "tsp", "tsp", UnitDimension.VOLUME, 5.0),

    PIECE("piece", "piece", "pieces", UnitDimension.COUNT, 1.0),
    SERVING("serving", "serving", "servings", UnitDimension.COUNT, 1.0),
    SLICE("slice", "slice", "slices", UnitDimension.COUNT, 1.0),
    BOTTLE("bottle", "bottle", "bottles", UnitDimension.COUNT, 1.0),
    PACKET("packet", "packet", "packets", UnitDimension.COUNT, 1.0),
    SCOOP("scoop", "scoop", "scoops", UnitDimension.COUNT, 1.0),
    CUSTOM("custom", "custom", "custom", UnitDimension.COUNT, 1.0);

    val isCount: Boolean get() = dimension == UnitDimension.COUNT

    companion object {
        fun fromId(id: String?): MeasureUnit =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: entries.firstOrNull { it.label.equals(id, ignoreCase = true) }
                ?: GRAM

        val MASS_UNITS = entries.filter { it.dimension == UnitDimension.MASS }
        val VOLUME_UNITS = entries.filter { it.dimension == UnitDimension.VOLUME }
        val COUNT_UNITS = entries.filter { it.dimension == UnitDimension.COUNT }

        /** Units offered in the water tracker. */
        val WATER_UNITS = listOf(MILLILITRE, LITRE, FLUID_OUNCE)

        /**
         * The units it makes sense to offer for a food whose nutrition is defined
         * per [base] (g or ml). Count units are always offered because a food can
         * always be logged as "1 serving".
         */
        fun optionsFor(base: MeasureUnit): List<MeasureUnit> = when (base.dimension) {
            UnitDimension.VOLUME -> VOLUME_UNITS + COUNT_UNITS
            else -> MASS_UNITS + COUNT_UNITS
        }
    }
}

/** Metric vs imperial display preference. Storage is always metric. */
enum class UnitSystem(val id: String, val label: String) {
    METRIC("METRIC", "Metric (kg, g, ml)"),
    IMPERIAL("IMPERIAL", "Imperial (lb, oz, fl oz)");

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: METRIC
    }
}
