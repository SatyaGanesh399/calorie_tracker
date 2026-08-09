package com.satya.calorietracker.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.satya.calorietracker.data.prefs.Reminder
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.InfoBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    reminders: List<Reminder>,
    onUpdate: (Reminder) -> Unit,
    onDisableAll: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var pendingEnable by remember { mutableStateOf<Reminder?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
        if (granted) pendingEnable?.let { onUpdate(it.copy(enabled = true)) }
        pendingEnable = null
    }

    fun requestEnable(reminder: Reminder) {
        if (hasPermission) {
            onUpdate(reminder.copy(enabled = true))
        } else {
            pendingEnable = reminder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    SettingsDetailScaffold(title = "Reminders", onBack = onBack) { modifier ->
        Column(modifier) {
            Text(
                "All reminders are off until you turn one on. Each fires once at the time you pick — there's no repeat nagging.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (permissionDenied && !hasPermission) {
                Spacer(Modifier.height(14.dp))
                InfoBanner(
                    message = "Notifications are blocked for this app, so reminders can't appear. You can turn them on in Android Settings › Apps › Calorie Tracker › Notifications.",
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            AppCard {
                Column {
                    reminders.forEachIndexed { index, reminder ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${reminder.type.title}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${reminder.timeLabel} · ${reminder.daysLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    reminder.type.body,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = reminder.enabled,
                                onCheckedChange = { checked ->
                                    if (checked) requestEnable(reminder)
                                    else onUpdate(reminder.copy(enabled = false))
                                }
                            )
                        }
                        TextButton(onClick = { editing = reminder }) { Text("Change time & days") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onDisableAll, modifier = Modifier.fillMaxWidth()) {
                Text("Turn all reminders off")
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    editing?.let { reminder ->
        ReminderEditDialog(
            reminder = reminder,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                onUpdate(updated)
                editing = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditDialog(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onConfirm: (Reminder) -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = reminder.hour,
        initialMinute = reminder.minute,
        is24Hour = true
    )
    var days by remember { mutableStateOf(reminder.daysOfWeek) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(reminder.type.label) },
        text = {
            Column {
                TimePicker(state = timeState)
                Spacer(Modifier.height(12.dp))
                Text("Repeat on", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Reminder.DAY_NAMES.forEachIndexed { index, name ->
                        val day = index + 1
                        ChoiceChip(
                            selected = day in days,
                            label = name.take(1),
                            onClick = {
                                days = if (day in days) days - day else days + day
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        reminder.copy(
                            hour = timeState.hour,
                            minute = timeState.minute,
                            daysOfWeek = days.ifEmpty { Reminder.ALL_DAYS }
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
