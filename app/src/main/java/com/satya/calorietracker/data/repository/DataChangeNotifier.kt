package com.satya.calorietracker.data.repository

/**
 * Repositories fire this after any write that a home-screen widget could be showing.
 * Keeping it as an interface means the data layer never imports the widget package,
 * and tests can pass a no-op.
 */
fun interface DataChangeNotifier {
    suspend fun onDataChanged()

    companion object {
        val NONE = DataChangeNotifier { }
    }
}
