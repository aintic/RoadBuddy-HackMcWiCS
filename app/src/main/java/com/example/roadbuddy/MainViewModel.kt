package com.example.roadbuddy

import android.util.Log
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Provides UI State logic
 * Lifecycle aware class to manage data that survives configuration changes
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Startup)
    val uiState: StateFlow<UiState> = _uiState

    private val _heartRateAvailable = MutableStateFlow(DataTypeAvailability.UNKNOWN)
    val heartRateAvailable: StateFlow<DataTypeAvailability> = _heartRateAvailable

    private val _heartRateBpm = MutableStateFlow(0.0)
    val heartRateBpm: StateFlow<Double> = _heartRateBpm

    /**
     * Check if the watch has heart rate capabilities, then switch UI State accordingly
     */
    init {
        viewModelScope.launch {
            _uiState.value = if (healthServicesManager.hasHeartRateCapability()) {
                UiState.HeartRateAvailable
            } else {
                UiState.HeartRateNotAvailable
            }
        }
    }

    /**
     * Update message regarding heart rate availability if it changes
     * Update the value of current heart rate collected from Health Services Manager flow
     */
    @ExperimentalCoroutinesApi
    suspend fun measureHeartRate() {
        healthServicesManager.heartRateMeasureFlow().collect {
            when (it) {
                is MeasureMessage.MeasureAvailability -> {
                    Log.d(TAG, "Availability changed: ${it.availability}")
                    _heartRateAvailable.value = it.availability
                }
                is MeasureMessage.MeasureData -> {
                    val bpm = it.data.last().value
                    Log.d(TAG, "Data update: $bpm")
                    _heartRateBpm.value = bpm
                }
            }
        }
    }
}

/**
 * Ui State objects
 */
sealed class UiState {
    object Startup : UiState()
    object HeartRateAvailable : UiState()
    object HeartRateNotAvailable : UiState()
    object Driving : UiState()
}
