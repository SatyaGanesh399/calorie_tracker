package com.satya.calorietracker.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.satya.calorietracker.data.prefs.Reminder
import com.satya.calorietracker.domain.model.MealType
import com.satya.calorietracker.domain.model.ThemeMode
import com.satya.calorietracker.domain.model.UnitSystem
import com.satya.calorietracker.ui.addfood.AddFoodSheet
import com.satya.calorietracker.ui.addfood.CustomFoodScreen
import com.satya.calorietracker.ui.addfood.CustomFoodViewModel
import com.satya.calorietracker.ui.addfood.PortionScreen
import com.satya.calorietracker.ui.addfood.PortionViewModel
import com.satya.calorietracker.ui.addfood.QuickAddScreen
import com.satya.calorietracker.ui.addfood.QuickAddViewModel
import com.satya.calorietracker.ui.addfood.SearchScreen
import com.satya.calorietracker.ui.addfood.SearchViewModel
import com.satya.calorietracker.ui.history.HistoryScreen
import com.satya.calorietracker.ui.history.HistoryViewModel
import com.satya.calorietracker.ui.home.HomeScreen
import com.satya.calorietracker.ui.home.HomeViewModel
import com.satya.calorietracker.ui.meals.MealsScreen
import com.satya.calorietracker.ui.meals.MealsViewModel
import com.satya.calorietracker.ui.progress.ProgressScreen
import com.satya.calorietracker.ui.progress.ProgressViewModel
import com.satya.calorietracker.ui.recipe.RecipeEditorScreen
import com.satya.calorietracker.ui.recipe.RecipeEditorViewModel
import com.satya.calorietracker.ui.scanner.BarcodeScannerScreen
import com.satya.calorietracker.ui.scanner.ScannerViewModel
import com.satya.calorietracker.ui.settings.AppearanceSettingsScreen
import com.satya.calorietracker.ui.settings.DataSettingsScreen
import com.satya.calorietracker.ui.settings.GoalsSettingsScreen
import com.satya.calorietracker.ui.settings.NotificationSettingsScreen
import com.satya.calorietracker.ui.settings.PrivacySettingsScreen
import com.satya.calorietracker.ui.settings.ProfileSettingsScreen
import com.satya.calorietracker.ui.settings.ProvidersSettingsScreen
import com.satya.calorietracker.ui.settings.SettingsScreen
import com.satya.calorietracker.ui.settings.SettingsViewModel
import com.satya.calorietracker.ui.settings.UnitsSettingsScreen
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.launch

/** What the app should do when it's opened from a widget or a notification. */
enum class LaunchAction { NONE, ADD_FOOD, ADD_WATER, ADD_WEIGHT, SCAN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieTrackerRoot(
    launchAction: LaunchAction,
    onLaunchActionHandled: () -> Unit,
    onRemindersChanged: (List<Reminder>) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevel = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val lastDeleted by homeViewModel.lastDeleted.collectAsStateWithLifecycle()

    var addFoodTarget by remember { mutableStateOf<Pair<MealType, String?>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // ------------------------------------------------- widget / notification entry
    LaunchedEffect(launchAction) {
        when (launchAction) {
            LaunchAction.ADD_FOOD -> {
                navController.navigateTopLevel(Routes.HOME)
                addFoodTarget = MealType.suggestedFor(java.time.LocalTime.now().hour) to null
            }
            LaunchAction.SCAN -> navController.navigate(
                Routes.scanner(
                    MealType.suggestedFor(java.time.LocalTime.now().hour).id,
                    DateUtils.todayIso()
                )
            )
            LaunchAction.ADD_WATER -> navController.navigateTopLevel(Routes.HOME)
            LaunchAction.ADD_WEIGHT -> navController.navigateTopLevel(Routes.PROGRESS)
            LaunchAction.NONE -> Unit
        }
        if (launchAction != LaunchAction.NONE) onLaunchActionHandled()
    }

    // ------------------------------------------------------------- undo snackbar
    LaunchedEffect(lastDeleted) {
        val deleted = lastDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Removed ${deleted.name}",
            actionLabel = "Undo",
            withDismissAction = true
        )
        if (result == SnackbarResult.ActionPerformed) homeViewModel.undoDelete()
        else homeViewModel.clearUndo()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = topLevel != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateTopLevel(destination.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                ExtendedFloatingActionButton(
                    onClick = {
                        addFoodTarget =
                            MealType.suggestedFor(java.time.LocalTime.now().hour) to null
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add food") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
        ) {
            // ------------------------------------------------------------- HOME
            composable(Routes.HOME) {
                HomeScreen(
                    state = homeState,
                    onSelectDate = homeViewModel::selectDate,
                    onShiftDate = homeViewModel::shiftDate,
                    onAddFood = { meal, custom -> addFoodTarget = meal to custom },
                    onEditEntry = { navController.navigate(Routes.editEntry(it.id)) },
                    onDeleteEntry = homeViewModel::deleteEntry,
                    onRepeatFood = { food ->
                        homeViewModel.repeatFood(
                            food,
                            MealType.suggestedFor(java.time.LocalTime.now().hour)
                        )
                        scope.launch { snackbarHostState.showSnackbar("Added ${food.name}") }
                    },
                    onAddWater = homeViewModel::addWater,
                    onUndoWater = homeViewModel::undoWater,
                    onOpenWeight = { navController.navigateTopLevel(Routes.PROGRESS) },
                    onOpenWater = { navController.navigateTopLevel(Routes.PROGRESS) },
                    contentPadding = padding
                )
            }

            // ------------------------------------------------------------ MEALS
            composable(Routes.MEALS) {
                val mealsViewModel: MealsViewModel = viewModel(factory = MealsViewModel.Factory)
                val favorites by mealsViewModel.favorites.collectAsStateWithLifecycle()
                val myFoods by mealsViewModel.myFoods.collectAsStateWithLifecycle()
                val recent by mealsViewModel.recent.collectAsStateWithLifecycle()
                val recipes by mealsViewModel.recipes.collectAsStateWithLifecycle()

                MealsScreen(
                    favorites = favorites,
                    myFoods = myFoods,
                    recent = recent,
                    recipes = recipes,
                    onFoodClick = { food ->
                        navController.navigate(
                            Routes.foodDetail(
                                food.id,
                                MealType.suggestedFor(java.time.LocalTime.now().hour).id,
                                DateUtils.todayIso()
                            )
                        )
                    },
                    onQuickLog = {
                        mealsViewModel.quickLog(it)
                        scope.launch { snackbarHostState.showSnackbar("Logged ${it.name}") }
                    },
                    onToggleFavorite = mealsViewModel::toggleFavorite,
                    onTogglePin = mealsViewModel::togglePin,
                    onEditFood = { navController.navigate(Routes.customFood(it.id)) },
                    onDeleteFood = mealsViewModel::deleteFood,
                    onRecipeClick = { navController.navigate(Routes.recipeEditor(it.recipe.id)) },
                    onQuickLogRecipe = {
                        mealsViewModel.quickLogRecipe(it)
                        scope.launch { snackbarHostState.showSnackbar("Logged ${it.recipe.name}") }
                    },
                    onEditRecipe = { navController.navigate(Routes.recipeEditor(it.recipe.id)) },
                    onDeleteRecipe = mealsViewModel::deleteRecipe,
                    onCreateFood = { navController.navigate(Routes.customFood()) },
                    onCreateRecipe = { navController.navigate(Routes.recipeEditor()) },
                    contentPadding = padding
                )
            }

            // --------------------------------------------------------- PROGRESS
            composable(Routes.PROGRESS) {
                val progressViewModel: ProgressViewModel =
                    viewModel(factory = ProgressViewModel.Factory)
                val state by progressViewModel.state.collectAsStateWithLifecycle()

                ProgressScreen(
                    state = state,
                    onRangeChange = progressViewModel::setRange,
                    onLogWeight = { progressViewModel.logWeight(it) },
                    onDeleteWeight = progressViewModel::deleteWeight,
                    onSetGoalWeight = progressViewModel::setGoalWeight,
                    contentPadding = padding
                )
            }

            // ---------------------------------------------------------- HISTORY
            composable(Routes.HISTORY) {
                val historyViewModel: HistoryViewModel =
                    viewModel(factory = HistoryViewModel.Factory)
                val state by historyViewModel.state.collectAsStateWithLifecycle()

                HistoryScreen(
                    state = state,
                    onSelectDate = historyViewModel::selectDate,
                    onShiftMonth = historyViewModel::shiftMonth,
                    onToday = historyViewModel::goToToday,
                    onEntryClick = { navController.navigate(Routes.editEntry(it.id)) },
                    contentPadding = padding
                )
            }

            // --------------------------------------------------------- SETTINGS
            composable(Routes.SETTINGS) {
                val settingsViewModel: SettingsViewModel =
                    viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
                val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

                SettingsScreen(
                    preferences = prefs,
                    onOpenProfile = { navController.navigate(Routes.SETTINGS_PROFILE) },
                    onOpenGoals = { navController.navigate(Routes.SETTINGS_GOALS) },
                    onOpenUnits = { navController.navigate(Routes.SETTINGS_UNITS) },
                    onOpenNotifications = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                    onOpenAppearance = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                    onOpenData = { navController.navigate(Routes.SETTINGS_DATA) },
                    onOpenProviders = { navController.navigate(Routes.SETTINGS_PROVIDERS) },
                    onOpenPrivacy = { navController.navigate(Routes.SETTINGS_PRIVACY) },
                    contentPadding = padding
                )
            }

            settingsGraph(navController, onRemindersChanged)
            addFoodGraph(navController)
        }
    }

    // ------------------------------------------------------------- add-food sheet
    addFoodTarget?.let { (meal, customName) ->
        val date = homeState.date
        AddFoodSheet(
            mealType = meal,
            customMealName = customName,
            recentFoods = homeState.recentFoods,
            sheetState = sheetState,
            onDismiss = { addFoodTarget = null },
            onSearch = {
                addFoodTarget = null
                navController.navigate(Routes.search(meal.id, DateUtils.iso(date)))
            },
            onScan = {
                addFoodTarget = null
                navController.navigate(Routes.scanner(meal.id, DateUtils.iso(date)))
            },
            onFavorites = {
                addFoodTarget = null
                navController.navigate(Routes.search(meal.id, DateUtils.iso(date)))
            },
            onRecent = {
                addFoodTarget = null
                navController.navigate(Routes.search(meal.id, DateUtils.iso(date)))
            },
            onMyFoods = {
                addFoodTarget = null
                navController.navigateTopLevel(Routes.MEALS)
            },
            onQuickAdd = {
                addFoodTarget = null
                navController.navigate(Routes.quickAdd(meal.id, DateUtils.iso(date)))
            },
            onCreateCustom = {
                addFoodTarget = null
                navController.navigate(Routes.customFood())
            },
            onRecipes = {
                addFoodTarget = null
                navController.navigateTopLevel(Routes.MEALS)
            },
            onRepeatFood = { food ->
                homeViewModel.repeatFood(food, meal)
                addFoodTarget = null
                scope.launch { snackbarHostState.showSnackbar("Added ${food.name}") }
            }
        )
    }
}

// =========================================================== add-food sub-graph

private fun androidx.navigation.NavGraphBuilder.addFoodGraph(navController: NavHostController) {

    composable(
        route = "${Routes.SEARCH}?meal={meal}&date={date}",
        arguments = listOf(
            navArgument("meal") { type = NavType.StringType; defaultValue = MealType.SNACK.id },
            navArgument("date") { type = NavType.StringType; defaultValue = DateUtils.todayIso() }
        )
    ) { entry ->
        val mealId = entry.arguments?.getString("meal") ?: MealType.SNACK.id
        val dateIso = entry.arguments?.getString("date") ?: DateUtils.todayIso()
        val meal = MealType.fromId(mealId)

        val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
        val state by searchViewModel.state.collectAsStateWithLifecycle()
        val favorites by searchViewModel.favorites.collectAsStateWithLifecycle()
        val recent by searchViewModel.recent.collectAsStateWithLifecycle()
        val myFoods by searchViewModel.myFoods.collectAsStateWithLifecycle()
        val recipes by searchViewModel.allRecipes.collectAsStateWithLifecycle()

        SearchScreen(
            state = state,
            favorites = favorites,
            recent = recent,
            myFoods = myFoods,
            recipes = recipes,
            mealLabel = meal.displayName,
            onQueryChange = searchViewModel::onQueryChange,
            onTabChange = searchViewModel::setTab,
            onFoodClick = { food ->
                navController.navigate(Routes.foodDetail(food.id, mealId, dateIso))
            },
            onFoodQuickAdd = { food ->
                searchViewModel.logDefaultServing(food, meal, DateUtils.parse(dateIso))
                navController.popBackStack()
            },
            onToggleFavorite = searchViewModel::toggleFavorite,
            onRecipeClick = { recipe ->
                searchViewModel.logRecipe(recipe, meal, DateUtils.parse(dateIso))
                navController.popBackStack()
            },
            onScan = { navController.navigate(Routes.scanner(mealId, dateIso)) },
            onCreateCustom = { navController.navigate(Routes.customFood()) },
            onBack = { navController.popBackStack() },
            onRetry = searchViewModel::retry
        )
    }

    composable(
        route = "${Routes.SCANNER}?meal={meal}&date={date}",
        arguments = listOf(
            navArgument("meal") { type = NavType.StringType; defaultValue = MealType.SNACK.id },
            navArgument("date") { type = NavType.StringType; defaultValue = DateUtils.todayIso() }
        )
    ) { entry ->
        val mealId = entry.arguments?.getString("meal") ?: MealType.SNACK.id
        val dateIso = entry.arguments?.getString("date") ?: DateUtils.todayIso()

        val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.Factory)
        val state by scannerViewModel.state.collectAsStateWithLifecycle()

        BarcodeScannerScreen(
            state = state,
            onBarcodeDetected = scannerViewModel::onBarcodeDetected,
            onUseFood = { food ->
                navController.navigate(Routes.foodDetail(food.id, mealId, dateIso)) {
                    popUpTo("${Routes.SCANNER}?meal={meal}&date={date}") { inclusive = true }
                }
            },
            onCreateManually = { barcode ->
                navController.navigate(Routes.customFood(barcode = barcode)) {
                    popUpTo("${Routes.SCANNER}?meal={meal}&date={date}") { inclusive = true }
                }
            },
            onRetry = scannerViewModel::retry,
            onResume = scannerViewModel::resumeScanning,
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        route = "${Routes.FOOD_DETAIL}/{foodId}?meal={meal}&date={date}",
        arguments = listOf(
            navArgument("foodId") { type = NavType.LongType },
            navArgument("meal") { type = NavType.StringType; defaultValue = MealType.SNACK.id },
            navArgument("date") { type = NavType.StringType; defaultValue = DateUtils.todayIso() }
        )
    ) { entry ->
        val foodId = entry.arguments?.getLong("foodId") ?: 0L
        val mealId = entry.arguments?.getString("meal") ?: MealType.SNACK.id
        val dateIso = entry.arguments?.getString("date") ?: DateUtils.todayIso()

        val portionViewModel: PortionViewModel = viewModel(factory = PortionViewModel.Factory)
        val state by portionViewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(foodId) {
            portionViewModel.loadFood(foodId, MealType.fromId(mealId), DateUtils.parse(dateIso))
        }

        PortionScreen(
            state = state,
            onQuantityChange = portionViewModel::setQuantity,
            onServingSizeChange = portionViewModel::setServingSize,
            onUnitChange = portionViewModel::setUnit,
            onMealChange = portionViewModel::setMeal,
            onDateChange = portionViewModel::setDate,
            onTimeChange = portionViewModel::setTime,
            onNotesChange = portionViewModel::setNotes,
            onScale = portionViewModel::scaleQuantity,
            onToggleFavorite = portionViewModel::toggleFavorite,
            onSave = { portionViewModel.save { navController.popBackStack() } },
            onDelete = { portionViewModel.delete { navController.popBackStack() } },
            onBack = { navController.popBackStack() },
            onDismissError = portionViewModel::clearError
        )
    }

    composable(
        route = "${Routes.EDIT_ENTRY}/{entryId}",
        arguments = listOf(navArgument("entryId") { type = NavType.LongType })
    ) { entry ->
        val entryId = entry.arguments?.getLong("entryId") ?: 0L
        val portionViewModel: PortionViewModel = viewModel(factory = PortionViewModel.Factory)
        val state by portionViewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(entryId) { portionViewModel.loadEntry(entryId) }

        PortionScreen(
            state = state,
            onQuantityChange = portionViewModel::setQuantity,
            onServingSizeChange = portionViewModel::setServingSize,
            onUnitChange = portionViewModel::setUnit,
            onMealChange = portionViewModel::setMeal,
            onDateChange = portionViewModel::setDate,
            onTimeChange = portionViewModel::setTime,
            onNotesChange = portionViewModel::setNotes,
            onScale = portionViewModel::scaleQuantity,
            onToggleFavorite = portionViewModel::toggleFavorite,
            onSave = { portionViewModel.save { navController.popBackStack() } },
            onDelete = { portionViewModel.delete { navController.popBackStack() } },
            onBack = { navController.popBackStack() },
            onDismissError = portionViewModel::clearError
        )
    }

    composable(
        route = "${Routes.QUICK_ADD}?meal={meal}&date={date}",
        arguments = listOf(
            navArgument("meal") { type = NavType.StringType; defaultValue = MealType.SNACK.id },
            navArgument("date") { type = NavType.StringType; defaultValue = DateUtils.todayIso() }
        )
    ) { entry ->
        val mealId = entry.arguments?.getString("meal") ?: MealType.SNACK.id
        val dateIso = entry.arguments?.getString("date") ?: DateUtils.todayIso()
        val quickAddViewModel: QuickAddViewModel = viewModel(factory = QuickAddViewModel.Factory)

        QuickAddScreen(
            initialMeal = MealType.fromId(mealId),
            date = DateUtils.parse(dateIso),
            onAdd = { calories, meal, protein, carbs, fat, label ->
                quickAddViewModel.add(
                    calories = calories,
                    mealType = meal,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    label = label,
                    date = DateUtils.parse(dateIso)
                ) { navController.popBackStack() }
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        route = "${Routes.CUSTOM_FOOD}?foodId={foodId}&barcode={barcode}&name={name}",
        arguments = listOf(
            navArgument("foodId") { type = NavType.LongType; defaultValue = 0L },
            navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
            navArgument("name") { type = NavType.StringType; defaultValue = "" }
        )
    ) { entry ->
        val foodId = entry.arguments?.getLong("foodId") ?: 0L
        val barcode = entry.arguments?.getString("barcode").orEmpty()
        val name = entry.arguments?.getString("name").orEmpty()

        val customFoodViewModel: CustomFoodViewModel =
            viewModel(factory = CustomFoodViewModel.Factory)
        val state by customFoodViewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(foodId) { customFoodViewModel.load(foodId) }

        CustomFoodScreen(
            state = state,
            prefillBarcode = barcode.ifBlank { null },
            prefillName = name.ifBlank { null },
            onSave = { food -> customFoodViewModel.save(food) { navController.popBackStack() } },
            onDelete = { id -> customFoodViewModel.delete(id) { navController.popBackStack() } },
            onBack = { navController.popBackStack() },
            onDismissError = customFoodViewModel::clearError
        )
    }

    composable(
        route = "${Routes.RECIPE_EDITOR}?recipeId={recipeId}",
        arguments = listOf(navArgument("recipeId") { type = NavType.LongType; defaultValue = 0L })
    ) { entry ->
        val recipeId = entry.arguments?.getLong("recipeId") ?: 0L
        val recipeViewModel: RecipeEditorViewModel =
            viewModel(factory = RecipeEditorViewModel.Factory)
        val state by recipeViewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(recipeId) { recipeViewModel.load(recipeId) }

        RecipeEditorScreen(
            state = state,
            onNameChange = recipeViewModel::setName,
            onServingsChange = recipeViewModel::setServings,
            onNotesChange = recipeViewModel::setNotes,
            onSearchQuery = recipeViewModel::onSearchQuery,
            onAddIngredient = recipeViewModel::addIngredient,
            onRemoveIngredient = recipeViewModel::removeIngredient,
            onSave = { recipeViewModel.save { navController.popBackStack() } },
            onBack = { navController.popBackStack() },
            onDismissError = recipeViewModel::clearError
        )
    }
}

// =========================================================== settings sub-graph

private fun androidx.navigation.NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    onRemindersChanged: (List<Reminder>) -> Unit
) {
    composable(Routes.SETTINGS_PROFILE) {
        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
        val prefs by vm.preferences.collectAsStateWithLifecycle()
        ProfileSettingsScreen(
            preferences = prefs,
            onSave = {
                vm.updateProfile(it)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS_GOALS) {
        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
        val prefs by vm.preferences.collectAsStateWithLifecycle()
        GoalsSettingsScreen(
            preferences = prefs,
            onSave = {
                vm.updateGoals(it)
                navController.popBackStack()
            },
            onRecalculate = vm::recalculateGoals,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS_UNITS) {
        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
        val prefs by vm.preferences.collectAsStateWithLifecycle()
        UnitsSettingsScreen(
            preferences = prefs,
            onSelect = { system: UnitSystem -> vm.setUnitSystem(system.id) },
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS_APPEARANCE) {
        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
        val prefs by vm.preferences.collectAsStateWithLifecycle()
        AppearanceSettingsScreen(
            preferences = prefs,
            onThemeMode = { mode: ThemeMode -> vm.setThemeMode(mode.id) },
            onDynamicColor = vm::setDynamicColor,
            onAccent = { accent -> vm.setAccent(accent.id) },
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS_NOTIFICATIONS) {
        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
        val prefs by vm.preferences.collectAsStateWithLifecycle()
        NotificationSettingsScreen(
            reminders = prefs.reminders,
            onUpdate = vm::updateReminder,
            onDisableAll = vm::disableAllReminders,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS_PROVIDERS) {
        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
        val prefs by vm.preferences.collectAsStateWithLifecycle()
        ProvidersSettingsScreen(
            statuses = vm.providerStatuses(),
            enabledIds = prefs.enabledProviderIds,
            onToggle = vm::setProviderEnabled,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS_DATA) {
        val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(onRemindersChanged))
        val busy by vm.busy.collectAsStateWithLifecycle()
        val result by vm.dataOp.collectAsStateWithLifecycle()
        val context = androidx.compose.ui.platform.LocalContext.current

        DataSettingsScreen(
            busy = busy,
            result = result,
            onExportJson = { uri -> vm.exportJson(context, uri) },
            onExportCsv = { uri -> vm.exportCsv(context, uri) },
            onImport = { uri, mode -> vm.importJson(context, uri, mode) },
            onClearAll = vm::clearAllData,
            onPruneCache = vm::pruneCache,
            onDismissResult = vm::clearDataOp,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Routes.SETTINGS_PRIVACY) {
        PrivacySettingsScreen(onBack = { navController.popBackStack() })
    }
}

/** Bottom-nav behaviour: single instance, restore state, never grow the back stack. */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
