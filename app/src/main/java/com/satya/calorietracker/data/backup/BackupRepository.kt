package com.satya.calorietracker.data.backup

import androidx.room.withTransaction
import com.satya.calorietracker.data.db.AppDatabase
import com.satya.calorietracker.data.db.FoodDao
import com.satya.calorietracker.data.db.LogDao
import com.satya.calorietracker.data.db.RecipeDao
import com.satya.calorietracker.data.db.WaterDao
import com.satya.calorietracker.data.db.WeightDao
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.repository.DataChangeNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Result of restoring a backup, surfaced verbatim in a dialog. */
sealed interface ImportResult {
    data class Success(
        val foods: Int,
        val logEntries: Int,
        val weights: Int,
        val water: Int,
        val recipes: Int,
        val preferencesRestored: Boolean
    ) : ImportResult {
        val total: Int get() = foods + logEntries + weights + water + recipes
    }

    data class Failure(val message: String) : ImportResult
}

enum class ImportMode { MERGE, REPLACE }

/**
 * Local export / import. No network, no account, no upload — the file goes exactly
 * where you point the system file picker and nowhere else.
 */
class BackupRepository(
    private val database: AppDatabase,
    private val foodDao: FoodDao,
    private val logDao: LogDao,
    private val weightDao: WeightDao,
    private val waterDao: WaterDao,
    private val recipeDao: RecipeDao,
    private val prefs: PreferencesRepository,
    private val notifier: DataChangeNotifier = DataChangeNotifier.NONE
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ------------------------------------------------------------- export

    suspend fun exportJson(includePreferences: Boolean = true): String = withContext(Dispatchers.IO) {
        val foods = foodDao.getAll()
        val foodKeyById = foods.associate { it.id to (it.barcode ?: "${it.name}|${it.brand.orEmpty()}") }

        val backup = BackupFile(
            exportedAt = System.currentTimeMillis(),
            preferences = if (includePreferences) prefs.current() else null,
            foods = foods.filter { it.isCustom || it.isFavorite || it.useCount > 0 }.map { it.toBackup() },
            logEntries = logDao.getAll().map { it.toBackup(foodKeyById[it.foodId]) },
            weights = weightDao.getAll().map { it.toBackup() },
            water = waterDao.getAll().map { it.toBackup() },
            recipes = recipeDao.getAll().map { it.recipe.toBackup(it.ingredients) }
        )
        json.encodeToString(backup)
    }

    /**
     * Spreadsheet-friendly export. One row per diary entry, plus separate blocks for
     * weight and water so a single file covers everything a person actually charts.
     */
    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()

        sb.appendLine("# Food log")
        sb.appendLine("date,time,meal,food,brand,quantity,serving_size,unit,calories,protein_g,carbs_g,fat_g,fiber_g,sugar_g,sodium_mg,notes")
        logDao.getAll().sortedBy { it.timestamp }.forEach { e ->
            sb.appendLine(
                listOf(
                    e.date,
                    com.satya.calorietracker.util.DateUtils.millisToTimeLabel(e.timestamp),
                    e.customMealName ?: e.mealTypeId,
                    e.name, e.brand.orEmpty(),
                    e.quantity.fmt(), e.servingSize.fmt(), e.unitId,
                    e.calories.fmt(), e.protein.fmt(), e.carbs.fmt(), e.fat.fmt(),
                    e.fiber.fmt(), e.sugar.fmt(), e.sodium.fmt(),
                    e.notes.orEmpty()
                ).joinToString(",") { it.csv() }
            )
        }

        sb.appendLine()
        sb.appendLine("# Weight")
        sb.appendLine("date,weight_kg,notes")
        weightDao.getAll().forEach { w ->
            sb.appendLine(listOf(w.date, w.weightKg.fmt(), w.notes.orEmpty()).joinToString(",") { it.csv() })
        }

        sb.appendLine()
        sb.appendLine("# Water")
        sb.appendLine("date,time,amount_ml")
        waterDao.getAll().forEach { w ->
            sb.appendLine(
                listOf(
                    w.date,
                    com.satya.calorietracker.util.DateUtils.millisToTimeLabel(w.timestamp),
                    w.amountMl.fmt()
                ).joinToString(",") { it.csv() }
            )
        }

        sb.appendLine()
        sb.appendLine("# My foods (per 100 g/ml)")
        sb.appendLine("name,brand,barcode,unit,calories,protein_g,carbs_g,fat_g,fiber_g,sugar_g,sodium_mg,serving_size,serving_label")
        foodDao.getAll().filter { it.isCustom }.forEach { f ->
            sb.appendLine(
                listOf(
                    f.name, f.brand.orEmpty(), f.barcode.orEmpty(), f.perUnitId,
                    f.calories.fmt(), f.protein.fmt(), f.carbs.fmt(), f.fat.fmt(),
                    f.fiber.fmt(), f.sugar.fmt(), f.sodium.fmt(),
                    f.servingSize?.fmt().orEmpty(), f.servingLabel.orEmpty()
                ).joinToString(",") { it.csv() }
            )
        }

        sb.toString()
    }

    // ------------------------------------------------------------- import

    suspend fun importJson(raw: String, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val backup = try {
            json.decodeFromString<BackupFile>(raw)
        } catch (e: Exception) {
            return@withContext ImportResult.Failure(
                "That doesn't look like a Calorie Tracker backup. Pick the .json file this app exported."
            )
        }

        if (backup.app != BackupFile.APP_TAG) {
            return@withContext ImportResult.Failure("This backup was made by a different app.")
        }
        if (backup.schemaVersion > BackupFile.SCHEMA_VERSION) {
            return@withContext ImportResult.Failure(
                "This backup was made by a newer version of the app. Update first, then import."
            )
        }

        try {
            database.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    logDao.deleteAll()
                    weightDao.deleteAll()
                    waterDao.deleteAll()
                    recipeDao.deleteAll()
                    // Foods are kept: the seed catalog is worth more than a clean slate.
                }

                // Foods first, so log entries can be relinked to them.
                val keyToId = mutableMapOf<String, Long>()
                foodDao.getAll().forEach { existing ->
                    keyToId[existing.barcode ?: "${existing.name}|${existing.brand.orEmpty()}"] = existing.id
                }
                backup.foods.forEach { f ->
                    val key = f.barcode ?: "${f.name}|${f.brand.orEmpty()}"
                    if (!keyToId.containsKey(key)) {
                        keyToId[key] = foodDao.insert(f.toEntity())
                    }
                }

                logDao.insertAll(backup.logEntries.map { it.toEntity(it.foodKey?.let(keyToId::get)) })
                weightDao.insertAll(backup.weights.map { it.toEntity() })
                waterDao.insertAll(backup.water.map { it.toEntity() })

                backup.recipes.forEach { r ->
                    val recipeId = recipeDao.insertRecipe(r.toEntity())
                    recipeDao.insertIngredients(
                        r.ingredients.mapIndexed { index, ing -> ing.toEntity(recipeId, index) }
                    )
                }
            }

            backup.preferences?.let { prefs.replaceAll(it) }
            notifier.onDataChanged()

            ImportResult.Success(
                foods = backup.foods.size,
                logEntries = backup.logEntries.size,
                weights = backup.weights.size,
                water = backup.water.size,
                recipes = backup.recipes.size,
                preferencesRestored = backup.preferences != null
            )
        } catch (e: Exception) {
            ImportResult.Failure("Import failed: ${e.message ?: "unknown database error"}. Nothing was changed.")
        }
    }

    // -------------------------------------------------------- destructive

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            logDao.deleteAll()
            weightDao.deleteAll()
            waterDao.deleteAll()
            recipeDao.deleteAll()
            foodDao.deleteAll()
        }
        prefs.clear()
        notifier.onDataChanged()
    }

}

private fun Double.fmt(): String =
    if (this % 1.0 == 0.0) toLong().toString() else String.format(java.util.Locale.US, "%.2f", this)

private fun String.csv(): String =
    if (contains(',') || contains('"') || contains('\n')) "\"" + replace("\"", "\"\"") + "\"" else this
