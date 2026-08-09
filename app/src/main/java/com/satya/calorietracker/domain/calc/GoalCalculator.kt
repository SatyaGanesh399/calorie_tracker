package com.satya.calorietracker.domain.calc

import com.satya.calorietracker.domain.model.Gender
import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.UserProfile
import kotlin.math.roundToInt

/**
 * Mifflin-St Jeor BMR -> TDEE -> calorie target, plus a sensible macro split.
 * The result is only ever a *suggestion*: the user can override every field.
 */
object GoalCalculator {

    /** Basal metabolic rate, kcal/day. */
    fun bmr(profile: UserProfile): Double {
        val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
        return when (profile.gender) {
            Gender.MALE -> base + 5
            Gender.FEMALE -> base - 161
            Gender.OTHER -> base - 78   // midpoint, avoids forcing a choice
        }
    }

    /** Total daily energy expenditure, kcal/day. */
    fun tdee(profile: UserProfile): Double = bmr(profile) * profile.activity.multiplier

    /**
     * Calorie target after applying the weight goal, floored at a safe minimum
     * (1200 kcal for women / 1500 for everyone else) so the calculator can never
     * suggest something unhealthy.
     */
    fun calorieTarget(profile: UserProfile): Int {
        val raw = tdee(profile) + profile.goal.kcalDelta
        val floor = if (profile.gender == Gender.FEMALE) 1200.0 else 1500.0
        return raw.coerceAtLeast(floor).roundToInt()
    }

    /**
     * Macros. Protein is set from body weight (1.6 g/kg cutting, 1.4 g/kg otherwise),
     * fat at 25 % of energy, carbs fill the remainder. Fibre follows the 14 g / 1000 kcal
     * guideline. Water is 35 ml per kg of body weight, rounded to the nearest 250 ml.
     */
    fun suggestedGoals(profile: UserProfile): NutritionGoals {
        val kcal = calorieTarget(profile)
        val cutting = profile.goal.kcalDelta < 0
        val proteinG = (profile.weightKg * if (cutting) 1.6 else 1.4).roundToInt()
        val fatG = (kcal * 0.25 / 9).roundToInt()
        val remaining = kcal - proteinG * 4 - fatG * 9
        val carbG = (remaining / 4.0).coerceAtLeast(50.0).roundToInt()
        val fiberG = (kcal / 1000.0 * 14).roundToInt()
        val waterMl = ((profile.weightKg * 35) / 250).roundToInt() * 250

        return NutritionGoals(
            calories = kcal,
            protein = proteinG,
            carbs = carbG,
            fat = fatG,
            fiber = fiberG,
            waterMl = waterMl.coerceIn(1500, 5000)
        )
    }

    /** Body mass index, for the profile summary. */
    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun bmiLabel(bmi: Double): String = when {
        bmi <= 0 -> "—"
        bmi < 18.5 -> "Underweight"
        bmi < 25 -> "Healthy"
        bmi < 30 -> "Overweight"
        else -> "Obese"
    }
}
