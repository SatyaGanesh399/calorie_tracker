package com.satya.calorietracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.backup.BackupRepository
import com.satya.calorietracker.data.backup.ImportMode
import com.satya.calorietracker.data.backup.ImportResult
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.data.prefs.Reminder
import com.satya.calorietracker.data.prefs.UserPreferences
import com.satya.calorietracker.data.remote.FoodProviderRegistry
import com.satya.calorietracker.data.remote.ProviderStatus
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.domain.calc.GoalCalculator
import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.UserProfile
import com.satya.calorietracker.ui.containerViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DataOpResult {
    data class Exported(val path: String, val format: String) : DataOpResult
    data class Imported(val summary: String) : DataOpResult
    data class Failed(val message: String) : DataOpResult
    data object Cleared : DataOpResult
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository,
    private val foodRepository: FoodRepository,
    private val registry: FoodProviderRegistry,
    private val onRemindersChanged: (List<Reminder>) -> Unit
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences.DEFAULT)

    private val _dataOp = MutableStateFlow<DataOpResult?>(null)
    val dataOp: StateFlow<DataOpResult?> = _dataOp.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun providerStatuses(): List<ProviderStatus> = registry.statuses()

    // ------------------------------------------------------------- profile

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            preferencesRepository.setProfile(profile)
            val prefs = preferencesRepository.current()
            if (prefs.autoGoalsFromProfile) {
                preferencesRepository.setGoals(GoalCalculator.suggestedGoals(profile))
            }
        }
    }

    fun updateGoals(goals: NutritionGoals) {
        viewModelScope.launch {
            preferencesRepository.setGoals(goals)
            // A manual edit means the user has taken over; stop overwriting them.
            preferencesRepository.update { it.copy(autoGoalsFromProfile = false) }
        }
    }

    fun recalculateGoals() {
        viewModelScope.launch {
            val prefs = preferencesRepository.current()
            preferencesRepository.setGoals(GoalCalculator.suggestedGoals(prefs.profile))
            preferencesRepository.update { it.copy(autoGoalsFromProfile = true) }
        }
    }

    fun setUnitSystem(id: String) {
        viewModelScope.launch { preferencesRepository.setUnitSystemId(id) }
    }

    fun setThemeMode(id: String) {
        viewModelScope.launch { preferencesRepository.setThemeModeId(id) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun setAccent(id: String) {
        viewModelScope.launch { preferencesRepository.setAccentId(id) }
    }

    fun setProviderEnabled(providerId: String, enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setProviderEnabled(providerId, enabled) }
    }

    fun addCustomMeal(name: String) {
        viewModelScope.launch { preferencesRepository.addCustomMeal(name) }
    }

    fun removeCustomMeal(name: String) {
        viewModelScope.launch { preferencesRepository.removeCustomMeal(name) }
    }

    fun setQuickWaterAmounts(amounts: List<Int>) {
        viewModelScope.launch { preferencesRepository.setQuickWaterAmounts(amounts) }
    }

    // ----------------------------------------------------------- reminders

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            preferencesRepository.upsertReminder(reminder)
            onRemindersChanged(preferencesRepository.current().reminders)
        }
    }

    fun disableAllReminders() {
        viewModelScope.launch {
            val updated = preferencesRepository.current().reminders.map { it.copy(enabled = false) }
            preferencesRepository.setReminders(updated)
            onRemindersChanged(updated)
        }
    }

    // ---------------------------------------------------------------- data

    fun exportJson(context: Context, uri: Uri) {
        runExport(context, uri, "JSON") { backupRepository.exportJson() }
    }

    fun exportCsv(context: Context, uri: Uri) {
        runExport(context, uri, "CSV") { backupRepository.exportCsv() }
    }

    private fun runExport(context: Context, uri: Uri, format: String, produce: suspend () -> String) {
        viewModelScope.launch {
            _busy.value = true
            _dataOp.value = try {
                val content = produce()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: error("Couldn't open that location for writing.")
                }
                DataOpResult.Exported(uri.lastPathSegment ?: "backup", format)
            } catch (e: Exception) {
                DataOpResult.Failed("Export failed: ${e.message ?: "unknown error"}")
            }
            _busy.value = false
        }
    }

    fun importJson(context: Context, uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            _busy.value = true
            _dataOp.value = try {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Couldn't read that file.")
                }
                when (val result = backupRepository.importJson(raw, mode)) {
                    is ImportResult.Success -> {
                        onRemindersChanged(preferencesRepository.current().reminders)
                        DataOpResult.Imported(
                            "Restored ${result.logEntries} diary entries, ${result.workouts} workouts, " +
                                "${result.weights} weigh-ins, ${result.water} water logs, " +
                                "${result.foods} foods and ${result.recipes} recipes."
                        )
                    }
                    is ImportResult.Failure -> DataOpResult.Failed(result.message)
                }
            } catch (e: Exception) {
                DataOpResult.Failed("Import failed: ${e.message ?: "unknown error"}")
            }
            _busy.value = false
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _busy.value = true
            backupRepository.clearAllData()
            onRemindersChanged(emptyList())
            _dataOp.value = DataOpResult.Cleared
            _busy.value = false
        }
    }

    fun pruneCache() {
        viewModelScope.launch {
            val removed = foodRepository.pruneCache()
            _dataOp.value = DataOpResult.Imported("Removed $removed cached foods you never used.")
        }
    }

    fun clearDataOp() {
        _dataOp.value = null
    }

    companion object {
        fun factory(onRemindersChanged: (List<Reminder>) -> Unit) =
            containerViewModelFactory { container ->
                SettingsViewModel(
                    preferencesRepository = container.preferencesRepository,
                    backupRepository = container.backupRepository,
                    foodRepository = container.foodRepository,
                    registry = container.providerRegistry,
                    onRemindersChanged = onRemindersChanged
                )
            }
    }
}
