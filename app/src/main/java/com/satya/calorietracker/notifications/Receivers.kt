package com.satya.calorietracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.satya.calorietracker.appContainer
import com.satya.calorietracker.data.prefs.Reminder
import com.satya.calorietracker.data.prefs.ReminderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Posts one reminder, then re-arms itself for the same slot next week. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val typeId = intent.getStringExtra(EXTRA_TYPE_ID) ?: return
        val type = ReminderType.fromId(typeId)

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val prefs = context.appContainer.preferencesRepository.preferences.first()
                val reminder = prefs.reminders.firstOrNull { it.typeId == typeId }

                if (reminder?.enabled == true) {
                    NotificationHelper.showReminder(context, type)
                    // Alarms are one-shot, so book the next occurrence now.
                    ReminderScheduler(context).schedule(reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.satya.calorietracker.REMINDER_FIRE"
        const val EXTRA_TYPE_ID = "type_id"
    }
}

/**
 * Midnight rollover. Nothing is erased: the dashboard simply starts querying the new
 * date, and widgets are refreshed so they show an empty day instead of yesterday's.
 */
class DailyResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NEW_DAY) return

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                context.appContainer.widgetUpdater.updateAll()
                ReminderScheduler(context).scheduleDailyReset()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_NEW_DAY = "com.satya.calorietracker.NEW_DAY"
    }
}

/**
 * Alarms don't survive a reboot, a time change or an app update, so we re-arm
 * everything the moment the system tells us one of those happened.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED
        )
        if (!relevant) return

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val container = context.appContainer
                val reminders: List<Reminder> =
                    container.preferencesRepository.preferences.first().reminders
                val scheduler = ReminderScheduler(context)
                scheduler.rescheduleAll(reminders)
                scheduler.scheduleDailyReset()
                container.widgetUpdater.updateAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
