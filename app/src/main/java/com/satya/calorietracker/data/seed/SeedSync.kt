package com.satya.calorietracker.data.seed

import com.satya.calorietracker.data.db.ExerciseDao
import com.satya.calorietracker.data.db.FoodDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tops up the bundled catalogs on an existing install.
 *
 * The database only seeds itself once, at creation, so without this an app that was
 * installed before a new batch of foods or exercises was added would never see them —
 * and the obvious "fix" of wiping and re-seeding would take your logged meals with it.
 *
 * Matching is done on **name** rather than on a generated key, so renaming the key
 * format (or tidying it up later) can never produce duplicates. It's idempotent:
 * running it on every launch is harmless, it just inserts nothing.
 */
class SeedSync(
    private val foodDao: FoodDao,
    private val exerciseDao: ExerciseDao
) {

    data class Result(val foodsAdded: Int, val exercisesAdded: Int) {
        val anythingAdded: Boolean get() = foodsAdded > 0 || exercisesAdded > 0
    }

    suspend fun run(): Result = withContext(Dispatchers.IO) {
        Result(
            foodsAdded = syncFoods(),
            exercisesAdded = syncExercises()
        )
    }

    private suspend fun syncFoods(): Int {
        val existing = foodDao.seededNames().toHashSet()
        val missing = SeedFoods.entities().filterNot { it.name in existing }
        if (missing.isEmpty()) return 0
        foodDao.insertAll(missing)
        return missing.size
    }

    private suspend fun syncExercises(): Int {
        val existing = exerciseDao.seededNames().toHashSet()
        val missing = SeedExercises.entities().filterNot { it.name in existing }
        if (missing.isEmpty()) return 0
        exerciseDao.insertAll(missing)
        return missing.size
    }
}
