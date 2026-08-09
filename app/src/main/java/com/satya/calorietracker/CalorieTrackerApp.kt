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
            ReminderScheduler(this@CalorieTrackerApp).rescheduleAll(prefs.reminders)
            ReminderScheduler(this@CalorieTrackerApp).scheduleDailyReset()
            MaintenanceScheduler.schedule(this@CalorieTrackerApp)
        }
    }
}

/** Convenience accessor used by ViewModel factories, widgets and receivers. */
val Context.appContainer: AppContainer
    get() = (applicationContext as CalorieTrackerApp).container
