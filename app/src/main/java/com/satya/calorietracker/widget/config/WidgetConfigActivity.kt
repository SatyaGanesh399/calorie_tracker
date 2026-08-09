package com.satya.calorietracker.widget.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.satya.calorietracker.appContainer
import com.satya.calorietracker.data.prefs.AccentColor
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.ChoiceChip
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.components.SettingRow
import com.satya.calorietracker.ui.theme.CalorieTrackerTheme
import com.satya.calorietracker.widget.WidgetPrefKeys
import com.satya.calorietracker.widget.WidgetTheme
import kotlinx.coroutines.launch

/**
 * Launched by the launcher when a widget is added (and again from "Reconfigure" on
 * Android 12+). Everything has a sensible default, so pressing back still leaves a
 * working widget.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Assume cancelled until the user confirms, per the widget contract.
        setResult(Activity.RESULT_CANCELED, resultIntent())

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val prefs by appContainer.preferencesState.collectAsStateWithLifecycle()

            CalorieTrackerTheme(
                themeMode = prefs.themeMode,
                accent = prefs.accent,
                dynamicColor = prefs.dynamicColor
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WidgetConfigContent(
                        defaultAccent = prefs.accent,
                        onDone = ::applyAndFinish
                    )
                }
            }
        }
    }

    private fun applyAndFinish(
        theme: WidgetTheme,
        accent: AccentColor,
        compact: Boolean,
        transparent: Boolean
    ) {
        lifecycleScope.launch {
            runCatching {
                val manager = GlanceAppWidgetManager(this@WidgetConfigActivity)
                val glanceId = manager.getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@WidgetConfigActivity, glanceId) { state ->
                    state[WidgetPrefKeys.THEME] = theme.id
                    state[WidgetPrefKeys.ACCENT] = accent.id
                    state[WidgetPrefKeys.COMPACT] = compact
                    state[WidgetPrefKeys.TRANSPARENT] = transparent
                }
                appContainer.widgetUpdater.updateAll()
            }
            setResult(Activity.RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@Composable
private fun WidgetConfigContent(
    defaultAccent: AccentColor,
    onDone: (WidgetTheme, AccentColor, Boolean, Boolean) -> Unit
) {
    var theme by remember { mutableStateOf(WidgetTheme.SYSTEM) }
    var accent by remember { mutableStateOf(defaultAccent) }
    var compact by remember { mutableStateOf(false) }
    var transparent by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Button(
                onClick = { onDone(theme, accent, compact, transparent) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp)
            ) { Text("Add widget") }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Widget options",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "You can resize the widget on the home screen as usual, and come back here any time via your launcher's widget options.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("Theme")
            AppCard {
                Column {
                    WidgetTheme.entries.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = theme == option, onClick = { theme = option })
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Accent")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccentColor.entries.toList()) { option ->
                    ChoiceChip(
                        selected = accent == option,
                        label = option.label,
                        onClick = { accent = option }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            AppCard {
                Column {
                    SettingRow(
                        title = "Compact mode",
                        subtitle = "Bigger numbers, fewer lines — good for small sizes",
                        trailing = { Switch(checked = compact, onCheckedChange = { compact = it }) }
                    )
                    SettingRow(
                        title = "Transparent background",
                        subtitle = "Let your wallpaper show through",
                        trailing = {
                            Switch(checked = transparent, onCheckedChange = { transparent = it })
                        }
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}
