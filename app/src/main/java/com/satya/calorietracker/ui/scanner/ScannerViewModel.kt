package com.satya.calorietracker.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satya.calorietracker.data.remote.BarcodeLookup
import com.satya.calorietracker.data.repository.FoodRepository
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.ui.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Scanning : ScanState
    data class LookingUp(val barcode: String) : ScanState
    data class Found(val food: Food, val fromCache: Boolean) : ScanState
    data class NotFound(val barcode: String) : ScanState
    data class Offline(val barcode: String) : ScanState
    data class Error(val barcode: String, val message: String) : ScanState
}

class ScannerViewModel(
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /** Barcodes already handled this session, so a steady camera doesn't fire repeatedly. */
    private var lastHandled: String? = null

    fun onBarcodeDetected(barcode: String) {
        if (barcode.isBlank()) return
        if (barcode == lastHandled) return
        if (_state.value !is ScanState.Scanning) return

        lastHandled = barcode
        _state.value = ScanState.LookingUp(barcode)

        viewModelScope.launch {
            _state.value = when (val result = foodRepository.lookupBarcode(barcode)) {
                is BarcodeLookup.Found -> ScanState.Found(result.food, result.fromCache)
                is BarcodeLookup.NotFound -> ScanState.NotFound(barcode)
                is BarcodeLookup.Offline -> ScanState.Offline(barcode)
                is BarcodeLookup.Error -> ScanState.Error(barcode, result.message)
            }
        }
    }

    /** Called when the user dismisses the result sheet and wants to scan something else. */
    fun resumeScanning() {
        lastHandled = null
        _state.value = ScanState.Scanning
    }

    fun retry() {
        val barcode = when (val s = _state.value) {
            is ScanState.NotFound -> s.barcode
            is ScanState.Offline -> s.barcode
            is ScanState.Error -> s.barcode
            is ScanState.LookingUp -> s.barcode
            else -> null
        } ?: return
        lastHandled = null
        _state.value = ScanState.Scanning
        onBarcodeDetected(barcode)
    }

    companion object {
        val Factory = containerViewModelFactory { container ->
            ScannerViewModel(container.foodRepository)
        }
    }
}
