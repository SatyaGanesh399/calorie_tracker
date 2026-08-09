package com.satya.calorietracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.satya.calorietracker.MainActivity
import com.satya.calorietracker.R
import com.satya.calorietracker.data.prefs.ReminderType

object NotificationHelper {

    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_reminders_description)
            enableVibration(true)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    /**
     * Posts a reminder. Silently does nothing if the user hasn't granted notification
     * permission — a denied permission is a preference, not an error.
     */
    fun showReminder(context: Context, type: ReminderType) {
        if (!canPostNotifications(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ACTION, type.reminderAction())
        }
        val pending = PendingIntent.getActivity(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(type.title)
            .setContentText(type.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(type.ordinal + 100, notification)
        }
    }

    private fun ReminderType.reminderAction(): String = when (this) {
        ReminderType.WATER -> MainActivity.ACTION_ADD_WATER
        ReminderType.WEIGHT -> MainActivity.ACTION_ADD_WEIGHT
        else -> MainActivity.ACTION_ADD_FOOD
    }
}
