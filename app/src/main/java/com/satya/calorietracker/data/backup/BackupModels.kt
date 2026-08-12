package com.satya.calorietracker.data.backup

import com.satya.calorietracker.data.db.ExerciseEntity
import com.satya.calorietracker.data.db.FoodEntity
import com.satya.calorietracker.data.db.SessionWithSets
import com.satya.calorietracker.data.db.WorkoutSessionEntity
import com.satya.calorietracker.data.db.WorkoutSetEntity
import com.satya.calorietracker.data.db.LogEntryEntity
import com.satya.calorietracker.data.db.RecipeEntity
import com.satya.calorietracker.data.db.RecipeIngredientEntity
import com.satya.calorietracker.data.db.WaterEntryEntity
import com.satya.calorietracker.data.db.WeightEntryEntity
import com.satya.calorietracker.data.prefs.UserPreferences
import kotlinx.serialization.Serializable

/**
 * The on-disk backup shape. Deliberately a plain mirror of the database so a backup is
 * readable in any text editor and can be hand-edited if something ever goes wrong.
 *
 * Nothing here leaves the device unless you explicitly save the file somewhere.
 */
@Serializable
data class BackupFile(
    val app: String = APP_TAG,
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: Long = 0L,
    val preferences: UserPreferences? = null,
    val foods: List<BackupFood> = emptyList(),
    val logEntries: List<BackupLogEntry> = emptyList(),
    val weights: List<BackupWeight> = emptyList(),
    val water: List<BackupWater> = emptyList(),
    val recipes: List<BackupRecipe> = emptyList(),
    /** Added in schema v2. Absent in v1 backups, which still import fine. */
    val exercises: List<BackupExercise> = emptyList(),
    val workouts: List<BackupWorkout> = emptyList()
) {
    companion object {
        const val APP_TAG = "CalorieTracker"
        const val SCHEMA_VERSION = 2
    }
}

@Serializable
data class BackupFood(
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val sourceId: String = "CUSTOM",
    val providerId: String? = null,
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
    val createdAt: Long = 0L
)

@Serializable
data class BackupLogEntry(
    val date: String,
    val timestamp: Long,
    val mealTypeId: String,
    val customMealName: String? = null,
    val name: String,
    val brand: String? = null,
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
    val notes: String? = null,
    val imageUrl: String? = null,
    /** Barcode or name key, used to relink to a food on import. */
    val foodKey: String? = null
)

@Serializable
data class BackupWeight(
    val date: String,
    val timestamp: Long,
    val weightKg: Double,
    val notes: String? = null
)

@Serializable
data class BackupWater(
    val date: String,
    val timestamp: Long,
    val amountMl: Double
)

@Serializable
data class BackupRecipe(
    val name: String,
    val servings: Double = 1.0,
    val notes: String? = null,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val ingredients: List<BackupIngredient> = emptyList()
)

@Serializable
data class BackupExercise(
    val name: String,
    val category: String,
    val equipment: String,
    val primaryMuscle: String,
    val tracksWeight: Boolean = true,
    val tracksReps: Boolean = true,
    val tracksDuration: Boolean = false,
    val tracksDistance: Boolean = false,
    val isFavorite: Boolean = false
)

@Serializable
data class BackupWorkout(
    val date: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val name: String,
    val notes: String? = null,
    val sets: List<BackupWorkoutSet> = emptyList()
)

@Serializable
data class BackupWorkoutSet(
    /** Matched back to the library by name on import, same as foods. */
    val exerciseName: String,
    val setNumber: Int = 1,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val notes: String? = null,
    val timestamp: Long = 0L
)

@Serializable
data class BackupIngredient(
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
    val sodium: Double = 0.0
)

// --------------------------------------------------------------------- mapping

fun FoodEntity.toBackup() = BackupFood(
    name = name, brand = brand, barcode = barcode, sourceId = sourceId,
    providerId = providerId, providerRef = providerRef, per = per, perUnitId = perUnitId,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    fiber = fiber, sugar = sugar, sodium = sodium,
    servingSize = servingSize, servingLabel = servingLabel, imageUrl = imageUrl,
    isFavorite = isFavorite, isPinned = isPinned, isCustom = isCustom,
    useCount = useCount, lastUsedAt = lastUsedAt, createdAt = createdAt
)

fun BackupFood.toEntity() = FoodEntity(
    name = name, brand = brand, barcode = barcode, sourceId = sourceId,
    providerId = providerId, providerRef = providerRef, per = per, perUnitId = perUnitId,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    fiber = fiber, sugar = sugar, sodium = sodium,
    servingSize = servingSize, servingLabel = servingLabel, imageUrl = imageUrl,
    isFavorite = isFavorite, isPinned = isPinned, isCustom = isCustom,
    useCount = useCount, lastUsedAt = lastUsedAt, createdAt = createdAt
)

fun LogEntryEntity.toBackup(foodKey: String?) = BackupLogEntry(
    date = date, timestamp = timestamp, mealTypeId = mealTypeId, customMealName = customMealName,
    name = name, brand = brand, quantity = quantity, servingSize = servingSize, unitId = unitId,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    fiber = fiber, sugar = sugar, sodium = sodium, notes = notes, imageUrl = imageUrl,
    foodKey = foodKey
)

fun BackupLogEntry.toEntity(foodId: Long?) = LogEntryEntity(
    date = date, timestamp = timestamp, mealTypeId = mealTypeId, customMealName = customMealName,
    foodId = foodId, name = name, brand = brand, imageUrl = imageUrl,
    quantity = quantity, servingSize = servingSize, unitId = unitId,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    fiber = fiber, sugar = sugar, sodium = sodium, notes = notes
)

fun WeightEntryEntity.toBackup() = BackupWeight(date, timestamp, weightKg, notes)

fun BackupWeight.toEntity() = WeightEntryEntity(
    date = date, timestamp = timestamp, weightKg = weightKg, notes = notes
)

fun WaterEntryEntity.toBackup() = BackupWater(date, timestamp, amountMl)

fun BackupWater.toEntity() = WaterEntryEntity(date = date, timestamp = timestamp, amountMl = amountMl)

fun RecipeEntity.toBackup(ingredients: List<RecipeIngredientEntity>) = BackupRecipe(
    name = name, servings = servings, notes = notes, imageUrl = imageUrl,
    isFavorite = isFavorite, createdAt = createdAt,
    ingredients = ingredients.map {
        BackupIngredient(
            name = it.name, quantity = it.quantity, servingSize = it.servingSize, unitId = it.unitId,
            calories = it.calories, protein = it.protein, carbs = it.carbs, fat = it.fat,
            fiber = it.fiber, sugar = it.sugar, sodium = it.sodium
        )
    }
)

fun BackupRecipe.toEntity() = RecipeEntity(
    name = name, servings = servings, notes = notes, imageUrl = imageUrl,
    isFavorite = isFavorite, createdAt = createdAt, updatedAt = createdAt
)

fun BackupIngredient.toEntity(recipeId: Long, position: Int) = RecipeIngredientEntity(
    recipeId = recipeId, name = name, quantity = quantity, servingSize = servingSize, unitId = unitId,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    fiber = fiber, sugar = sugar, sodium = sodium, position = position
)


// ------------------------------------------------------- workouts (schema v2)

fun ExerciseEntity.toBackup() = BackupExercise(
    name = name, category = category, equipment = equipment, primaryMuscle = primaryMuscle,
    tracksWeight = tracksWeight, tracksReps = tracksReps,
    tracksDuration = tracksDuration, tracksDistance = tracksDistance,
    isFavorite = isFavorite
)

fun BackupExercise.toEntity(now: Long) = ExerciseEntity(
    name = name, category = category, equipment = equipment, primaryMuscle = primaryMuscle,
    tracksWeight = tracksWeight, tracksReps = tracksReps,
    tracksDuration = tracksDuration, tracksDistance = tracksDistance,
    isCustom = true, isFavorite = isFavorite, createdAt = now
)

fun SessionWithSets.toBackup() = BackupWorkout(
    date = session.date,
    startedAt = session.startedAt,
    endedAt = session.endedAt,
    name = session.name,
    notes = session.notes,
    sets = sets.map {
        BackupWorkoutSet(
            exerciseName = it.exerciseName,
            setNumber = it.setNumber,
            weightKg = it.weightKg,
            reps = it.reps,
            durationSeconds = it.durationSeconds,
            distanceMeters = it.distanceMeters,
            rpe = it.rpe,
            isWarmup = it.isWarmup,
            notes = it.notes,
            timestamp = it.timestamp
        )
    }
)

fun BackupWorkout.toEntity() = WorkoutSessionEntity(
    date = date, startedAt = startedAt, endedAt = endedAt, name = name, notes = notes
)

fun BackupWorkoutSet.toEntity(sessionId: Long, exerciseId: Long) = WorkoutSetEntity(
    sessionId = sessionId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    setNumber = setNumber,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
    rpe = rpe,
    isWarmup = isWarmup,
    notes = notes,
    timestamp = timestamp
)
