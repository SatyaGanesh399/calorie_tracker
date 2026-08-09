package com.satya.calorietracker.util

import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.domain.units.UnitConverter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Small display helpers used by both the Compose UI and the Glance widgets. */
object Format {

    fun kcal(value: Double): String = "%,d".format(Locale.getDefault(), value.roundToInt())

    fun kcal(value: Int): String = "%,d".format(Locale.getDefault(), value)

    /** 12.0 -> "12", 12.34 -> "12.3" */
    fun grams(value: Double): String =
        if (abs(value) >= 100 || value % 1.0 == 0.0) value.roundToInt().toString()
        else String.format(Locale.getDefault(), "%.1f", value)

    fun gramsWithUnit(value: Double): String = "${grams(value)} g"

    fun decimal(value: Double, places: Int = 1): String =
        String.format(Locale.getDefault(), "%.${places}f", value)

    fun percent(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

    // ------------------------------------------------------------- weight

    fun weight(kg: Double, system: UnitSystem, withUnit: Boolean = true): String {
        val (value, unit) = when (system) {
            UnitSystem.METRIC -> kg to "kg"
            UnitSystem.IMPERIAL -> UnitConverter.kgToLb(kg) to "lb"
        }
        val text = String.format(Locale.getDefault(), "%.1f", value)
        return if (withUnit) "$text $unit" else text
    }

    fun weightDelta(kgDelta: Double, system: UnitSystem): String {
        if (abs(kgDelta) < 0.05) return "No change"
        val arrow = if (kgDelta < 0) "↓" else "↑"
        return "$arrow ${weight(abs(kgDelta), system)}"
    }

    fun weightUnitLabel(system: UnitSystem) = if (system == UnitSystem.METRIC) "kg" else "lb"

    // -------------------------------------------------------------- water

    fun water(ml: Double, system: UnitSystem): String = when (system) {
        UnitSystem.IMPERIAL -> {
            val oz = ml / 29.5735295625
            "${String.format(Locale.getDefault(), "%.0f", oz)} oz"
        }
        UnitSystem.METRIC ->
            if (ml >= 1000) "${String.format(Locale.getDefault(), "%.2f", ml / 1000.0).trimEnd('0').trimEnd('.')} L"
            else "${ml.roundToInt()} ml"
    }

    // ------------------------------------------------------------- height

    fun height(cm: Double, system: UnitSystem): String = when (system) {
        UnitSystem.METRIC -> "${cm.roundToInt()} cm"
        UnitSystem.IMPERIAL -> {
            val (ft, inch) = UnitConverter.cmToFeetInches(cm)
            "$ft' ${inch.roundToInt()}\""
        }
    }

    /** Clamps a progress fraction so a wild over-eat day doesn't break the layout. */
    fun progress(consumed: Double, target: Double): Float =
        if (target <= 0.0) 0f else (consumed / target).coerceIn(0.0, 1.0).toFloat()
}
