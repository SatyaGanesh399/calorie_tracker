package com.satya.calorietracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val MEALS = "meals"
    const val PROGRESS = "progress"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val SEARCH = "search"
    const val SCANNER = "scanner"
    const val QUICK_ADD = "quick_add"
    const val CUSTOM_FOOD = "custom_food"
    const val FOOD_DETAIL = "food_detail"
    const val EDIT_ENTRY = "edit_entry"
    const val RECIPE_EDITOR = "recipe_editor"

    const val SETTINGS_PROFILE = "settings/profile"
    const val SETTINGS_GOALS = "settings/goals"
    const val SETTINGS_UNITS = "settings/units"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_DATA = "settings/data"
    const val SETTINGS_PROVIDERS = "settings/providers"
    const val SETTINGS_PRIVACY = "settings/privacy"

    // ---- builders (keeps route strings in one place) ----
    fun search(mealId: String, date: String) = "$SEARCH?meal=$mealId&date=$date"
    fun scanner(mealId: String, date: String) = "$SCANNER?meal=$mealId&date=$date"
    fun quickAdd(mealId: String, date: String) = "$QUICK_ADD?meal=$mealId&date=$date"
    fun customFood(foodId: Long = 0L, barcode: String = "", name: String = "") =
        "$CUSTOM_FOOD?foodId=$foodId&barcode=${barcode.encode()}&name=${name.encode()}"

    private fun String.encode(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
    fun foodDetail(foodId: Long, mealId: String, date: String) =
        "$FOOD_DETAIL/$foodId?meal=$mealId&date=$date"
    fun editEntry(entryId: Long) = "$EDIT_ENTRY/$entryId"
    fun recipeEditor(recipeId: Long = 0L) = "$RECIPE_EDITOR?recipeId=$recipeId"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    MEALS(Routes.MEALS, "Meals", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    PROGRESS(Routes.PROGRESS, "Progress", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    HISTORY(Routes.HISTORY, "History", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
