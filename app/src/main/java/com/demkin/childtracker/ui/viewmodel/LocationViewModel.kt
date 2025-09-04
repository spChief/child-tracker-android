package com.demkin.childtracker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.demkin.childtracker.data.repository.LocationRepository
import com.demkin.childtracker.service.LocationTrackingService
import com.demkin.childtracker.worker.LocationSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    application: Application,
    private val locationRepository: LocationRepository,
    private val sharedPreferences: SharedPreferences
) : AndroidViewModel(application) {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _unsentCount = MutableStateFlow(0)
    val unsentCount: StateFlow<Int> = _unsentCount.asStateFlow()

    init {
        _isTracking.value = sharedPreferences.getBoolean("tracking_enabled", false)
        updateUnsentCount()
    }

    fun toggleTracking() {
        val context = getApplication<Application>()
        if (_isTracking.value) {
            stopTracking(context)
        } else {
            startTracking(context)
        }
    }

    private fun startTracking(context: Context) {
        LocationTrackingService.startService(context)
        _isTracking.value = true
        sharedPreferences.edit().putBoolean("tracking_enabled", true).apply()

        // Запускаем периодическую синхронизацию
        LocationSyncWorker.enqueuePeriodic(context)
    }

    private fun stopTracking(context: Context) {
        LocationTrackingService.stopService(context)
        _isTracking.value = false
        sharedPreferences.edit().putBoolean("tracking_enabled", false).apply()

        // Останавливаем синхронизацию
        LocationSyncWorker.cancel(context)
    }

    fun syncLocations() {
        val context = getApplication<Application>()
        LocationSyncWorker.enqueue(context)
    }

    private fun updateUnsentCount() {
        viewModelScope.launch {
            locationRepository.getUnsentCount().let { count ->
                _unsentCount.value = count
            }
        }
    }

    fun refreshUnsentCount() {
        updateUnsentCount()
    }
}
