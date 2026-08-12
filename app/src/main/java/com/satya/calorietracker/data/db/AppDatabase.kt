package com.satya.calorietracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.satya.calorietracker.data.seed.SeedExercises
import com.satya.calorietracker.data.seed.SeedFoods
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FoodEntity::class,
        LogEntryEntity::class,
        WeightEntryEntity::class,
        WaterEntryEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun logDao(): LogDao
    abstract fun weightDao(): WeightDao
    abstract fun waterDao(): WaterDao
    abstract fun recipeDao(): RecipeDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DB_NAME = "calorie_tracker.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context, scope: CoroutineScope): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext, scope).also { instance = it }
            }

        private fun build(context: Context, scope: CoroutineScope): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(*Migrations.ALL)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Seed the offline catalogs the first time the DB is created,
                        // off the main thread so first launch stays fast. Existing
                        // installs are topped up by SeedSync instead.
                        scope.launch(Dispatchers.IO) {
                            instance?.let {
                                it.foodDao().insertAll(SeedFoods.entities())
                                it.exerciseDao().insertAll(SeedExercises.entities())
                            }
                        }
                    }
                })
                // Downgrades can only happen if you sideload an older APK; there is no
                // sensible way to un-migrate, so start clean rather than crash-loop.
                // Upgrades always go through a real migration and never lose data.
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()

        /** Used by "Clear all data" in Settings. */
        fun closeAndDelete(context: Context) {
            synchronized(this) {
                instance?.close()
                instance = null
                context.applicationContext.deleteDatabase(DB_NAME)
            }
        }
    }
}
