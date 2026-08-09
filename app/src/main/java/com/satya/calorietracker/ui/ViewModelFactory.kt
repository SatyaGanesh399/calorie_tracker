package com.satya.calorietracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.satya.calorietracker.CalorieTrackerApp
import com.satya.calorietracker.di.AppContainer

/**
 * One helper instead of a DI framework: every ViewModel gets its dependencies from the
 * single [AppContainer] hanging off the Application.
 */
inline fun <reified VM : ViewModel> containerViewModelFactory(
    crossinline create: (AppContainer) -> VM
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalorieTrackerApp
        create(app.container)
    }
}
