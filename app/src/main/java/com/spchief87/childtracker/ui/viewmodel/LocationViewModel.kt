package com.spchief87.childtracker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spchief87.childtracker.data.model.LocationData
import com.spchief87.childtracker.data.repository.LocationRepository
import com.spchief87.childtracker.service.LocationTrackingService
import com.spchief87.childtracker.worker.LocationSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val _lastLocations = MutableStateFlow<List<LocationData>>(emptyList())
    val lastLocations: StateFlow<List<LocationData>> = _lastLocations.asStateFlow()

    init {
        _isTracking.value = sharedPreferences.getBoolean("tracking_enabled", false)
        observeUnsentCount()
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

    private fun observeUnsentCount() {
        viewModelScope.launch {
            locationRepository.getUnsentCountFlow().collect { count ->
                _unsentCount.value = count
            }
        }
    }

    fun refreshUnsentCount() {
        // Теперь счетчик обновляется автоматически через Flow
        // Этот метод оставляем для совместимости, но он больше не нужен
    }

    fun loadLastLocations(limit: Int = 10) {
        viewModelScope.launch {
            try {
                val locations = locationRepository.getLastLocations(limit)
                _lastLocations.value = locations
            } catch (e: Exception) {
                // В случае ошибки оставляем пустой список
                _lastLocations.value = emptyList()
            }
        }
    }
}
