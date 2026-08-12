package com.satya.calorietracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations.
 *
 * Room verifies the resulting schema against what it expects and throws if they differ,
 * so the SQL below has to match Room's own generated form exactly: backticked
 * identifiers, `INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL` for generated keys,
 * INTEGER for Boolean/Int/Long, REAL for Double, TEXT for String, and NOT NULL on
 * every non-nullable Kotlin property. Kotlin default values are *not* SQL defaults,
 * so no DEFAULT clauses appear here.
 *
 * There is deliberately no destructive fallback anywhere in this file. If a migration
 * is ever wrong the app should fail loudly on launch rather than quietly deleting
 * months of logged meals.
 */
object Migrations {

    /**
     * v1 → v2: adds the workout tracker. Purely additive — not one existing table is
     * touched, so food, weight, water and recipe history carries over untouched.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercises` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `equipment` TEXT NOT NULL,
                    `primaryMuscle` TEXT NOT NULL,
                    `tracksWeight` INTEGER NOT NULL,
                    `tracksReps` INTEGER NOT NULL,
                    `tracksDuration` INTEGER NOT NULL,
                    `tracksDistance` INTEGER NOT NULL,
                    `isCustom` INTEGER NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    `useCount` INTEGER NOT NULL,
                    `lastUsedAt` INTEGER,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_name` ON `exercises` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_category` ON `exercises` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_lastUsedAt` ON `exercises` (`lastUsedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_isFavorite` ON `exercises` (`isFavorite`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `date` TEXT NOT NULL,
                    `startedAt` INTEGER NOT NULL,
                    `endedAt` INTEGER,
                    `name` TEXT NOT NULL,
                    `notes` TEXT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_date` ON `workout_sessions` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_startedAt` ON `workout_sessions` (`startedAt`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_sets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `exerciseId` INTEGER NOT NULL,
                    `exerciseName` TEXT NOT NULL,
                    `setNumber` INTEGER NOT NULL,
                    `weightKg` REAL NOT NULL,
                    `reps` INTEGER NOT NULL,
                    `durationSeconds` INTEGER,
                    `distanceMeters` REAL,
                    `rpe` REAL,
                    `isWarmup` INTEGER NOT NULL,
                    `notes` TEXT,
                    `timestamp` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_sessionId` ON `workout_sets` (`sessionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId` ON `workout_sets` (`exerciseId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_timestamp` ON `workout_sets` (`timestamp`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
