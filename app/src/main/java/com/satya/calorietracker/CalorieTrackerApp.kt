package com.satya.calorietracker

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.satya.calorietracker.di.AppContainer
import com.satya.calorietracker.notifications.NotificationHelper
import com.satya.calorietracker.notifications.ReminderScheduler
import com.satya.calorietracker.work.MaintenanceScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CalorieTrackerApp : Application(), Configuration.Provider {

    /** Lives as long as the process. Used for DB seeding, prefs caching, widget pushes. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.INFO else android.util.Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, applicationScope)

        NotificationHelper.createChannels(this)

        applicationScope.launch {
            // Re-arm anything time-based. Cheap, and keeps reminders correct after an update.
            val prefs = container.preferencesRepository.preferences.first()
            val scheduler = ReminderScheduler(this@CalorieTrackerApp)
            scheduler.rescheduleAll(prefs.reminders)
            scheduler.scheduleDailyReset()
            MaintenanceScheduler.schedule(this@CalorieTrackerApp)

            // Pick up foods and exercises added since this app was installed. Only
            // inserts what's missing, so nothing you've logged is ever touched.
            runCatching { container.seedSync.run() }
        }
    }
}

/** Convenience accessor used by ViewModel factories, widgets and receivers. */
val Context.appContainer: AppContainer
    get() = (applicationContext as CalorieTrackerApp).container
