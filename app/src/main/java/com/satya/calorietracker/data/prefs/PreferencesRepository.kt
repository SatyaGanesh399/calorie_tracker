package com.satya.calorietracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.satya.calorietracker.domain.model.NutritionGoals
import com.satya.calorietracker.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * All settings live in a single JSON blob. That keeps every write atomic (no half-applied
 * goal change), makes export/import trivial, and means adding a setting never needs a
 * migration — new fields just take their default.
 */
class PreferencesRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val key = stringPreferencesKey("user_preferences_json")

    val preferences: Flow<UserPreferences> = store.data
        .catch { e ->
            // A corrupt or unreadable prefs file must never block the app.
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[key]?.let { raw ->
                runCatching { json.decodeFromString<UserPreferences>(raw) }
                    .getOrElse { UserPreferences.DEFAULT }
            } ?: UserPreferences.DEFAULT
        }

    suspend fun current(): UserPreferences = preferences.first()

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.edit { prefs ->
            val existing = prefs[key]
                ?.let { runCatching { json.decodeFromString<UserPreferences>(it) }.getOrNull() }
                ?: UserPreferences.DEFAULT
            prefs[key] = json.encodeToString(transform(existing))
        }
    }

    // -------------------------------------------------------- focused setters

    suspend fun setGoals(goals: NutritionGoals) = update { it.copy(goals = goals) }

    suspend fun setProfile(profile: UserProfile) = update { it.copy(profile = profile) }

    suspend fun setUnitSystemId(id: String) = update { it.copy(unitSystemId = id) }

    suspend fun setThemeModeId(id: String) = update { it.copy(themeModeId = id) }

    suspend fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    suspend fun setAccentId(id: String) = update { it.copy(accentId = id) }

    suspend fun setReminders(reminders: List<Reminder>) = update { it.copy(reminders = reminders) }

    suspend fun upsertReminder(reminder: Reminder) = update { prefs ->
        val updated = prefs.reminders.toMutableList()
        val index = updated.indexOfFirst { it.typeId == reminder.typeId }
        if (index >= 0) updated[index] = reminder else updated += reminder
        prefs.copy(reminders = updated)
    }

    suspend fun setProviderEnabled(providerId: String, enabled: Boolean) = update { prefs ->
        val ids = prefs.enabledProviderIds.toMutableSet()
        if (enabled) ids += providerId else ids -= providerId
        prefs.copy(enabledProviderIds = ids)
    }

    suspend fun addCustomMeal(name: String) = update { prefs ->
        val trimmed = name.trim()
        if (trimmed.isBlank() || prefs.customMeals.any { it.equals(trimmed, true) }) prefs
        else prefs.copy(customMeals = prefs.customMeals + trimmed)
    }

    suspend fun removeCustomMeal(name: String) = update { prefs ->
        prefs.copy(customMeals = prefs.customMeals.filterNot { it.equals(name, true) })
    }

    suspend fun setQuickWaterAmounts(amounts: List<Int>) =
        update { it.copy(quickWaterAmountsMl = amounts.filter { ml -> ml > 0 }.distinct().sorted()) }

    suspend fun setOnboardingComplete(complete: Boolean) =
        update { it.copy(onboardingComplete = complete) }

    suspend fun setGoalWeight(kg: Double) = update {
        it.copy(goalWeightKg = kg, profile = it.profile.copy(goalWeightKg = kg))
    }

    suspend fun setStartWeightIfAbsent(kg: Double) = update {
        if (it.startWeightKg == null) it.copy(startWeightKg = kg) else it
    }

    suspend fun setCurrentWeight(kg: Double) = update {
        it.copy(profile = it.profile.copy(weightKg = kg))
    }

    suspend fun replaceAll(preferences: UserPreferences) = update { preferences }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}
