package com.satya.calorietracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.data.prefs.AccentColor
import com.satya.calorietracker.data.prefs.UserPreferences
import com.satya.calorietracker.data.remote.ProviderStatus
import com.satya.calorietracker.domain.calc.GoalCalculator
import com.satya.calorietracker.domain.model.ActivityLevel
import com.satya.calorietracker.domain.model.Gender
import com.satya.calorietracker.domain.model.GoalType
import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.ThemeMode
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.domain.model.UserProfile
import com.satya.calorietracker.domain.units.UnitConverter
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.InfoBanner
import com.satya.calorietracker.ui.components.NumberField
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.components.SettingRow
import com.satya.calorietracker.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        content(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        )
    }
}

// ------------------------------------------------------------------- profile

@Composable
fun ProfileSettingsScreen(
    preferences: UserPreferences,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val profile = preferences.profile
    val metric = preferences.unitSystem == UnitSystem.METRIC

    var age by remember { mutableStateOf(profile.age.toString()) }
    var gender by remember { mutableStateOf(profile.gender) }
    var height by remember {
        mutableStateOf(
            if (metric) profile.heightCm.toInt().toString()
            else Format.decimal(profile.heightCm / 2.54, 1)
        )
    }
    var weight by remember {
        mutableStateOf(
            Format.weight(profile.weightKg, preferences.unitSystem, withUnit = false)
        )
    }
    var goalWeight by remember {
        mutableStateOf(
            Format.weight(preferences.goalWeightKg, preferences.unitSystem, withUnit = false)
        )
    }
    var activity by remember { mutableStateOf(profile.activity) }
    var goal by remember { mutableStateOf(profile.goal) }

    fun buildProfile(): UserProfile {
        val h = height.toDoubleOrNull() ?: profile.heightCm
        val w = weight.toDoubleOrNull() ?: profile.weightKg
        val gw = goalWeight.toDoubleOrNull() ?: preferences.goalWeightKg
        return UserProfile(
            age = age.toIntOrNull()?.coerceIn(10, 110) ?: profile.age,
            genderId = gender.id,
            heightCm = if (metric) h else h * 2.54,
            weightKg = if (metric) w else UnitConverter.lbToKg(w),
            goalWeightKg = if (metric) gw else UnitConverter.lbToKg(gw),
            activityId = activity.id,
            goalId = goal.id
        )
    }

    val preview = GoalCalculator.suggestedGoals(buildProfile())

    SettingsDetailScaffold(
        title = "Profile",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = { onSave(buildProfile()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp)
            ) { Text("Save profile") }
        }
    ) { modifier ->
        Column(modifier) {
            Text(
                "Used only to suggest a calorie target. Nothing leaves this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(age, { age = it }, "Age", Modifier.weight(1f), allowDecimal = false)
                NumberField(
                    height, { height = it }, "Height", Modifier.weight(1f),
                    suffix = if (metric) "cm" else "in"
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    weight, { weight = it }, "Current weight", Modifier.weight(1f),
                    suffix = Format.weightUnitLabel(preferences.unitSystem)
                )
                NumberField(
                    goalWeight, { goalWeight = it }, "Goal weight", Modifier.weight(1f),
                    suffix = Format.weightUnitLabel(preferences.unitSystem)
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Sex (for the BMR formula)")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Gender.entries.toList()) { g ->
                    ChoiceChip(selected = gender == g, label = g.label, onClick = { gender = g })
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Activity level")
            Column {
                ActivityLevel.entries.forEach { level ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = activity == level, onClick = { activity = level })
                        Column(Modifier.padding(start = 4.dp)) {
                            Text(level.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                level.description,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Goal")
            Column {
                GoalType.entries.forEach { g ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = goal == g, onClick = { goal = g })
                        Text(g.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Text("Suggested targets", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${Format.kcal(preview.calories)} kcal · ${preview.protein} g protein · ${preview.carbs} g carbs · ${preview.fat} g fat",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "You can override any of these under Goals.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

// --------------------------------------------------------------------- goals

@Composable
fun GoalsSettingsScreen(
    preferences: UserPreferences,
    onSave: (NutritionGoals) -> Unit,
    onRecalculate: () -> Unit,
    onBack: () -> Unit
) {
    val goals = preferences.goals
    var calories by remember { mutableStateOf(goals.calories.toString()) }
    var protein by remember { mutableStateOf(goals.protein.toString()) }
    var carbs by remember { mutableStateOf(goals.carbs.toString()) }
    var fat by remember { mutableStateOf(goals.fat.toString()) }
    var fiber by remember { mutableStateOf(goals.fiber.toString()) }
    var water by remember { mutableStateOf(goals.waterMl.toString()) }

    val macroKcal = (protein.toIntOrNull() ?: 0) * 4 +
        (carbs.toIntOrNull() ?: 0) * 4 +
        (fat.toIntOrNull() ?: 0) * 9
    val calorieTarget = calories.toIntOrNull() ?: 0
    val mismatch = calorieTarget > 0 && kotlin.math.abs(macroKcal - calorieTarget) > calorieTarget * 0.1

    SettingsDetailScaffold(
        title = "Goals",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = {
                    onSave(
                        NutritionGoals(
                            calories = calories.toIntOrNull() ?: goals.calories,
                            protein = protein.toIntOrNull() ?: goals.protein,
                            carbs = carbs.toIntOrNull() ?: goals.carbs,
                            fat = fat.toIntOrNull() ?: goals.fat,
                            fiber = fiber.toIntOrNull() ?: goals.fiber,
                            waterMl = water.toIntOrNull() ?: goals.waterMl
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp)
            ) { Text("Save goals") }
        }
    ) { modifier ->
        Column(modifier) {
            Text(
                "Every value here is yours to set. The calculator is only ever a starting point.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            NumberField(calories, { calories = it }, "Daily calories", Modifier.fillMaxWidth(), suffix = "kcal", allowDecimal = false)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(protein, { protein = it }, "Protein", Modifier.weight(1f), suffix = "g", allowDecimal = false)
                NumberField(carbs, { carbs = it }, "Carbs", Modifier.weight(1f), suffix = "g", allowDecimal = false)
                NumberField(fat, { fat = it }, "Fat", Modifier.weight(1f), suffix = "g", allowDecimal = false)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(fiber, { fiber = it }, "Fibre", Modifier.weight(1f), suffix = "g", allowDecimal = false)
                NumberField(water, { water = it }, "Water", Modifier.weight(1f), suffix = "ml", allowDecimal = false)
            }

            if (mismatch) {
                Spacer(Modifier.height(14.dp))
                InfoBanner(
                    message = "Your macros add up to $macroKcal kcal, which doesn't match your $calorieTarget kcal target. That's fine if it's deliberate."
                )
            }

            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onRecalculate, modifier = Modifier.fillMaxWidth()) {
                Text("Recalculate from my profile")
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

// --------------------------------------------------------------------- units

@Composable
fun UnitsSettingsScreen(
    preferences: UserPreferences,
    onSelect: (UnitSystem) -> Unit,
    onBack: () -> Unit
) {
    SettingsDetailScaffold(title = "Units", onBack = onBack) { modifier ->
        Column(modifier) {
            Text(
                "Data is always stored in metric — this only changes how it's shown, so you can switch back and forth without losing anything.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            AppCard {
                Column {
                    UnitSystem.entries.forEach { system ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.unitSystem == system,
                                onClick = { onSelect(system) }
                            )
                            Text(system.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            AppCard {
                Column {
                    Text("Preview", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Weight: ${Format.weight(78.4, preferences.unitSystem)}")
                    Text("Height: ${Format.height(175.0, preferences.unitSystem)}")
                    Text("Water: ${Format.water(1750.0, preferences.unitSystem)}")
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

// ---------------------------------------------------------------- appearance

@Composable
fun AppearanceSettingsScreen(
    preferences: UserPreferences,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onAccent: (AccentColor) -> Unit,
    onBack: () -> Unit
) {
    SettingsDetailScaffold(title = "Appearance", onBack = onBack) { modifier ->
        Column(modifier) {
            SectionHeader("Theme")
            AppCard {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = { onThemeMode(mode) }
                            )
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            AppCard {
                SettingRow(
                    title = "Use wallpaper colours",
                    subtitle = "Material You dynamic colour (Android 12+)",
                    trailing = {
                        Switch(checked = preferences.dynamicColor, onCheckedChange = onDynamicColor)
                    }
                )
            }

            if (!preferences.dynamicColor) {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Accent colour")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AccentColor.entries.toList()) { accent ->
                        ChoiceChip(
                            selected = preferences.accent == accent,
                            label = accent.label,
                            onClick = { onAccent(accent) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ----------------------------------------------------------------- providers

@Composable
fun ProvidersSettingsScreen(
    statuses: List<ProviderStatus>,
    enabledIds: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    SettingsDetailScaffold(title = "Food database", onBack = onBack) { modifier ->
        Column(modifier) {
            Text(
                "Search asks the phone first, then whichever online sources you enable. Turning everything off still leaves you with your own foods and the built-in list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            statuses.forEach { status ->
                AppCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(status.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    status.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (status.requiresNetwork) {
                                Switch(
                                    checked = status.id in enabledIds,
                                    enabled = status.configured,
                                    onCheckedChange = { onToggle(status.id, it) }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = buildString {
                                append(if (status.requiresNetwork) "Needs internet" else "Works offline")
                                append(" · ")
                                append(if (status.supportsBarcode) "Barcodes supported" else "No barcode lookup")
                                append(" · ")
                                append(
                                    when {
                                        !status.configured -> "Not configured (API key missing)"
                                        !status.reachable -> "Currently unreachable"
                                        else -> "Ready"
                                    }
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            InfoBanner(
                message = "To add a new source, implement FoodDataProvider and register it in AppContainer. Nothing else in the app needs to change."
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

// ------------------------------------------------------------------- privacy

@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    SettingsDetailScaffold(title = "Privacy & permissions", onBack = onBack) { modifier ->
        Column(modifier) {
            AppCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Text(
                        "Your data stays on this phone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "There is no account, no login, no server and no analytics. Everything you log lives in a database on this device. The only way data leaves is if you export it yourself.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Permissions this app asks for")

            PermissionExplainer(
                "Camera",
                "Only to read barcodes. Frames are analysed on-device and never stored or sent anywhere. Deny it and everything else keeps working."
            )
            PermissionExplainer(
                "Notifications",
                "Only if you switch a reminder on. Android 13+ asks the first time you enable one."
            )
            PermissionExplainer(
                "Internet",
                "Only when you search a food or scan a barcode, to ask Open Food Facts. Results are cached so the same food works offline afterwards."
            )
            PermissionExplainer(
                "Run at startup",
                "Only to re-arm your reminders after a reboot. Nothing runs in the background otherwise."
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader("Permissions this app never asks for")
            Text(
                "Location · Contacts · Microphone · Storage · Phone · Call logs · Any advertising or analytics SDK.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            InfoBanner(
                message = "Barcode searches send only the barcode number to Open Food Facts. Text searches send only what you typed. Neither includes anything about you."
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PermissionExplainer(title: String, body: String) {
    AppCard {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}
