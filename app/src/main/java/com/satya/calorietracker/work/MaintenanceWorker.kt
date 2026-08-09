package com.satya.calorietracker.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.satya.calorietracker.appContainer
import java.util.concurrent.TimeUnit

/**
 * The app's only background job. Runs at most once a week, when the phone is idle and
 * charging, and does exactly two cheap things: trim the food cache and refresh widgets.
 *
 * Deliberately not a foreground service, not a periodic sync, and not anything that
 * would show up in a battery report.
 */
class MaintenanceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val container = applicationContext.appContainer
        container.foodRepository.pruneCache(maxAgeDays = 90)
        container.widgetUpdater.updateAll()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

object MaintenanceScheduler {

    private const val WORK_NAME = "calorie_tracker_maintenance"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(7, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
