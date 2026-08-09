package com.satya.calorietracker.data.repository

import com.satya.calorietracker.data.db.DailyTotalsRow
import com.satya.calorietracker.data.db.LogDao
import com.satya.calorietracker.data.db.MealTotalsRow
import com.satya.calorietracker.data.db.RecipeWithIngredients
import com.satya.calorietracker.data.db.perServingNutrients
import com.satya.calorietracker.data.db.toDomain
import com.satya.calorietracker.data.db.toEntity
import com.satya.calorietracker.data.db.toNutrients
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.domain.model.LoggedFood
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.domain.model.MeasureUnit
import com.satya.calorietracker.domain.model.Nutrients
import com.satya.calorietracker.domain.units.UnitConverter
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** The food diary. Every write bumps the widgets. */
class DiaryRepository(
    private val logDao: LogDao,
    private val foodRepository: FoodRepository,
    private val notifier: DataChangeNotifier = DataChangeNotifier.NONE
) {

    // --------------------------------------------------------------- reads

    fun observeEntries(date: LocalDate): Flow<List<LoggedFood>> =
        logDao.observeForDate(DateUtils.iso(date)).map { list -> list.map { it.toDomain() } }

    fun observeTotals(date: LocalDate): Flow<Nutrients> =
        logDao.observeDailyTotals(DateUtils.iso(date)).map { it.toNutrientsSafe() }

    fun observeEntryCount(date: LocalDate): Flow<Int> =
        logDao.observeDailyTotals(DateUtils.iso(date)).map { it.entryCount }

    fun observeMealTotals(date: LocalDate): Flow<List<MealTotalsRow>> =
        logDao.observeMealTotals(DateUtils.iso(date))

    fun observeTotalsBetween(start: LocalDate, end: LocalDate): Flow<List<DailyTotalsRow>> =
        logDao.observeTotalsBetween(DateUtils.iso(start), DateUtils.iso(end))

    fun observeLoggedDates(start: LocalDate, end: LocalDate): Flow<List<String>> =
        logDao.observeLoggedDates(DateUtils.iso(start), DateUtils.iso(end))

    fun observeRecentlyLogged(limit: Int = 20): Flow<List<LoggedFood>> =
        logDao.observeRecentlyLogged(limit).map { list -> list.map { it.toDomain() } }

    suspend fun totalsFor(date: LocalDate): Nutrients =
        logDao.getDailyTotals(DateUtils.iso(date)).toNutrientsSafe()

    suspend fun totalsBetween(start: LocalDate, end: LocalDate): List<DailyTotalsRow> =
        logDao.getTotalsBetween(DateUtils.iso(start), DateUtils.iso(end))

    suspend fun entriesFor(date: LocalDate): List<LoggedFood> =
        logDao.getForDate(DateUtils.iso(date)).map { it.toDomain() }

    suspend fun getEntry(id: Long): LoggedFood? = logDao.getById(id)?.toDomain()

    suspend fun earliestLoggedDate(): LocalDate? = logDao.earliestDate()?.let { DateUtils.parse(it) }

    // -------------------------------------------------------------- writes

    /**
     * The hot path: log a food. Nutrition is computed once and stored absolute, so
     * editing the food definition later never rewrites history.
     */
    suspend fun logFood(
        food: Food,
        quantity: Double,
        servingSize: Double,
        unit: MeasureUnit,
        mealType: MealType,
        customMealName: String? = null,
        date: LocalDate = DateUtils.today(),
        timestamp: Long = DateUtils.nowMillis(),
        notes: String? = null
    ): Long {
        val persisted = foodRepository.ensurePersisted(food)
        val nutrients = UnitConverter.nutrientsFor(persisted, quantity, servingSize, unit)

        val entry = LoggedFood(
            date = DateUtils.iso(date),
            timestamp = timestamp,
            mealTypeId = mealType.id,
            customMealName = customMealName,
            foodId = persisted.id.takeIf { it != 0L },
            name = persisted.name,
            brand = persisted.brand,
            quantity = quantity,
            servingSize = servingSize,
            unitId = unit.id,
            nutrients = nutrients,
            notes = notes,
            imageUrl = persisted.imageUrl
        )

        val id = logDao.insert(entry.toEntity())
        persisted.id.takeIf { it != 0L }?.let { foodRepository.markUsed(it) }
        notifier.onDataChanged()
        return id
    }

    /** "I ate about 350 kcal" — no food record needed. */
    suspend fun quickAdd(
        calories: Double,
        mealType: MealType,
        protein: Double = 0.0,
        carbs: Double = 0.0,
        fat: Double = 0.0,
        label: String = "Quick add",
        date: LocalDate = DateUtils.today(),
        notes: String? = null
    ): Long {
        val entry = LoggedFood(
            date = DateUtils.iso(date),
            timestamp = DateUtils.nowMillis(),
            mealTypeId = mealType.id,
            name = label,
            quantity = 1.0,
            servingSize = 1.0,
            unitId = MeasureUnit.SERVING.id,
            nutrients = Nutrients(calories = calories, protein = protein, carbs = carbs, fat = fat),
            notes = notes
        )
        val id = logDao.insert(entry.toEntity())
        notifier.onDataChanged()
        return id
    }

    /** Log [servings] portions of a saved recipe. */
    suspend fun logRecipe(
        recipe: RecipeWithIngredients,
        servings: Double,
        mealType: MealType,
        date: LocalDate = DateUtils.today(),
        notes: String? = null
    ): Long {
        val nutrients = recipe.perServingNutrients() * servings
        val entry = LoggedFood(
            date = DateUtils.iso(date),
            timestamp = DateUtils.nowMillis(),
            mealTypeId = mealType.id,
            recipeId = recipe.recipe.id,
            name = recipe.recipe.name,
            quantity = servings,
            servingSize = 1.0,
            unitId = MeasureUnit.SERVING.id,
            nutrients = nutrients,
            notes = notes,
            imageUrl = recipe.recipe.imageUrl
        )
        val id = logDao.insert(entry.toEntity())
        notifier.onDataChanged()
        return id
    }

    /** Repeat an earlier entry as-is — the two-tap path for foods you eat every day. */
    suspend fun repeat(entry: LoggedFood, date: LocalDate = DateUtils.today(), mealType: MealType? = null): Long {
        val copy = entry.copy(
            id = 0L,
            date = DateUtils.iso(date),
            timestamp = DateUtils.nowMillis(),
            mealTypeId = (mealType ?: entry.mealType).id
        )
        val id = logDao.insert(copy.toEntity())
        entry.foodId?.let { foodRepository.markUsed(it) }
        notifier.onDataChanged()
        return id
    }

    /**
     * Edit an existing row. If quantity / serving / unit changed we recompute nutrition
     * from the underlying food; if that food is gone we scale what we already stored.
     */
    suspend fun updateEntry(
        entry: LoggedFood,
        recomputeFrom: Food? = null
    ) {
        val updated = if (recomputeFrom != null) {
            entry.copy(
                nutrients = UnitConverter.nutrientsFor(
                    recomputeFrom, entry.quantity, entry.servingSize, entry.unit
                )
            )
        } else {
            entry
        }
        logDao.update(updated.toEntity())
        notifier.onDataChanged()
    }

    /** Rescale a stored entry when its source food no longer exists. */
    fun rescale(entry: LoggedFood, oldQuantity: Double, newQuantity: Double): LoggedFood {
        if (oldQuantity <= 0.0) return entry
        return entry.copy(quantity = newQuantity, nutrients = entry.nutrients * (newQuantity / oldQuantity))
    }

    suspend fun deleteEntry(id: Long) {
        logDao.deleteById(id)
        notifier.onDataChanged()
    }

    suspend fun deleteDay(date: LocalDate) {
        logDao.deleteForDate(DateUtils.iso(date))
        notifier.onDataChanged()
    }

    private fun DailyTotalsRow.toNutrientsSafe(): Nutrients = toNutrients()
}
