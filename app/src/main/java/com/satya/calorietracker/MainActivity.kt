package com.satya.calorietracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.satya.calorietracker.notifications.ReminderScheduler
import com.satya.calorietracker.ui.navigation.CalorieTrackerRoot
import com.satya.calorietracker.ui.navigation.LaunchAction
import com.satya.calorietracker.ui.theme.CalorieTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingAction by mutableStateOf(LaunchAction.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingAction = intent.toLaunchAction()

        setContent {
            val prefs by appContainer.preferencesState.collectAsStateWithLifecycle()

            CalorieTrackerTheme(
                themeMode = prefs.themeMode,
                accent = prefs.accent,
                dynamicColor = prefs.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalorieTrackerRoot(
                        launchAction = pendingAction,
                        onLaunchActionHandled = { pendingAction = LaunchAction.NONE },
                        onRemindersChanged = { reminders ->
                            lifecycleScope.launch {
                                ReminderScheduler(this@MainActivity).rescheduleAll(reminders)
                            }
                        }
                    )
                }
            }
        }
    }

    /** Widgets and notifications reuse the single task, so new intents arrive here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAction = intent.toLaunchAction()
    }

    private fun Intent.toLaunchAction(): LaunchAction =
        when (getStringExtra(EXTRA_ACTION)) {
            ACTION_ADD_FOOD -> LaunchAction.ADD_FOOD
            ACTION_ADD_WATER -> LaunchAction.ADD_WATER
            ACTION_ADD_WEIGHT -> LaunchAction.ADD_WEIGHT
            ACTION_SCAN -> LaunchAction.SCAN
            else -> LaunchAction.NONE
        }

    companion object {
        const val EXTRA_ACTION = "com.satya.calorietracker.extra.ACTION"

        const val ACTION_HOME = "HOME"
        const val ACTION_ADD_FOOD = "ADD_FOOD"
        const val ACTION_ADD_WATER = "ADD_WATER"
        const val ACTION_ADD_WEIGHT = "ADD_WEIGHT"
        const val ACTION_SCAN = "SCAN"
    }
}
