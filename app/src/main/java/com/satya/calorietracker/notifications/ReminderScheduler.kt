package com.satya.calorietracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.satya.calorietracker.data.prefs.Reminder
import com.satya.calorietracker.util.DateUtils
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reminders use inexact alarms on purpose.
 *
 * `setExactAndAllowWhileIdle` would need SCHEDULE_EXACT_ALARM, which Android 13+ treats
 * as a sensitive permission and which users are right to be suspicious of. A logging
 * nudge that lands within a few minutes of 13:30 is fine, so we use the inexact,
 * permission-free variant and let the system batch it.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun rescheduleAll(reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            cancel(reminder)
            if (reminder.enabled && reminder.daysOfWeek.isNotEmpty()) schedule(reminder)
        }
    }

    fun schedule(reminder: Reminder) {
        val manager = alarmManager ?: return
        val triggerAt = nextTrigger(reminder) ?: return

        runCatching {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent(reminder, create = true) ?: return
            )
        }
    }

    fun cancel(reminder: Reminder) {
        val manager = alarmManager ?: return
        pendingIntent(reminder, create = false)?.let { manager.cancel(it) }
    }

    /** The next matching day-of-week at the reminder's time, strictly in the future. */
    private fun nextTrigger(reminder: Reminder): Long? {
        if (reminder.daysOfWeek.isEmpty()) return null
        val now = LocalDateTime.now()
        for (offset in 0..7) {
            val candidate = now.toLocalDate()
                .plusDays(offset.toLong())
                .atTime(reminder.hour, reminder.minute)
            if (candidate.isAfter(now) && candidate.dayOfWeek.value in reminder.daysOfWeek) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun pendingIntent(reminder: Reminder, create: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_TYPE_ID, reminder.typeId)
        }
        var flags = PendingIntent.FLAG_IMMUTABLE
        flags = flags or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, reminder.alarmId, intent, flags)
    }

    // ------------------------------------------------------------ daily reset

    /**
     * Fires just after local midnight so the widgets flip to the new day without the
     * app needing to be open. Nothing is deleted — each day simply has its own rows.
     */
    fun scheduleDailyReset() {
        val manager = alarmManager ?: return
        val intent = Intent(context, DailyResetReceiver::class.java).apply {
            action = DailyResetReceiver.ACTION_NEW_DAY
        }
        val pending = PendingIntent.getBroadcast(
            context,
            DAILY_RESET_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + DateUtils.millisUntilNextMidnight() + 5_000L

        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    companion object {
        private const val DAILY_RESET_REQUEST_CODE = 9001

        /** True when the OS would let us use exact alarms — informational only. */
        fun canScheduleExact(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                    ?.canScheduleExactAlarms() == true
            } else true
    }
}
