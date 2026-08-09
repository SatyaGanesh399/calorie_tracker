package com.satya.calorietracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.data.prefs.UserPreferences
import com.satya.calorietracker.domain.calc.GoalCalculator
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.SettingRow
import com.satya.calorietracker.util.Format

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onOpenProfile: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenUnits: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenData: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val enabledReminders = preferences.reminders.count { it.enabled }
    val bmi = GoalCalculator.bmi(preferences.profile.weightKg, preferences.profile.heightCm)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Text("Your targets", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${Format.kcal(preferences.goals.calories)} kcal · " +
                            "${preferences.goals.protein} g protein · " +
                            "${preferences.goals.carbs} g carbs · " +
                            "${preferences.goals.fat} g fat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "BMI ${Format.decimal(bmi, 1)} · ${GoalCalculator.bmiLabel(bmi)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AppCard {
                Column {
                    SettingRow(
                        title = "Profile",
                        subtitle = "${preferences.profile.age} · ${Format.height(preferences.profile.heightCm, preferences.unitSystem)} · ${Format.weight(preferences.profile.weightKg, preferences.unitSystem)}",
                        icon = Icons.Outlined.Person,
                        onClick = onOpenProfile
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = "Goals",
                        subtitle = "Calories, macros, fibre and water",
                        icon = Icons.Outlined.Analytics,
                        onClick = onOpenGoals
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = "Units",
                        subtitle = preferences.unitSystem.label,
                        icon = Icons.Outlined.Straighten,
                        onClick = onOpenUnits
                    )
                }
            }
        }

        item {
            AppCard {
                Column {
                    SettingRow(
                        title = "Reminders",
                        subtitle = if (enabledReminders == 0) "Off — nothing will interrupt you"
                        else "$enabledReminders reminder${if (enabledReminders == 1) "" else "s"} on",
                        icon = Icons.Outlined.Notifications,
                        onClick = onOpenNotifications
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = "Appearance",
                        subtitle = "${preferences.themeMode.label} · ${if (preferences.dynamicColor) "Wallpaper colours" else preferences.accent.label}",
                        icon = Icons.Outlined.DarkMode,
                        onClick = onOpenAppearance
                    )
                }
            }
        }

        item {
            AppCard {
                Column {
                    SettingRow(
                        title = "Food database",
                        subtitle = "Which nutrition sources to use",
                        icon = Icons.Outlined.CloudOff,
                        onClick = onOpenProviders
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = "Data",
                        subtitle = "Export, import, clear",
                        icon = Icons.Outlined.Storage,
                        onClick = onOpenData
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = "Privacy & permissions",
                        subtitle = "What this app collects — spoiler: nothing",
                        icon = Icons.Outlined.PrivacyTip,
                        onClick = onOpenPrivacy
                    )
                }
            }
        }

        item {
            Text(
                text = "Calorie Tracker · personal build\nNo account, no cloud, no analytics.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
    }
}
