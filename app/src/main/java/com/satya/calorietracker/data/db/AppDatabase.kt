package com.satya.calorietracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
        RecipeIngredientEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun logDao(): LogDao
    abstract fun weightDao(): WeightDao
    abstract fun waterDao(): WaterDao
    abstract fun recipeDao(): RecipeDao

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
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Seed the offline food catalog the first time the DB is created,
                        // off the main thread so first launch stays fast.
                        scope.launch(Dispatchers.IO) {
                            instance?.foodDao()?.insertAll(SeedFoods.entities())
                        }
                    }
                })
                // Personal app, single schema version so far. When you add a migration,
                // register it here instead of relying on destructive fallback.
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
