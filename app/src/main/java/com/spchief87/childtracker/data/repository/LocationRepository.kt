package com.spchief87.childtracker.data.repository

import android.content.Context
import com.spchief87.childtracker.data.api.LocationApiService
import com.spchief87.childtracker.data.database.LocationDao
import com.spchief87.childtracker.data.model.LocationData
import com.spchief87.childtracker.data.model.LocationBatchRequest
import com.spchief87.childtracker.utils.DeviceIdManager
import com.spchief87.childtracker.utils.LocationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val apiService: LocationApiService,
    @ApplicationContext private val context: Context
) {

    fun getAllLocations(): Flow<List<LocationData>> = locationDao.getAllLocations()

    suspend fun getLastLocations(limit: Int): List<LocationData> = locationDao.getLastLocations(limit)

    fun getUnsentLocations(): Flow<List<LocationData>> = locationDao.getUnsentLocations()

    suspend fun insertLocation(location: LocationData): Long {
        return locationDao.insertLocation(location)
    }

    /**
     * Вставляет локацию только если она изменилась по сравнению с последней
     * и автоматически синхронизирует её
     */
    suspend fun insertLocationIfChanged(location: LocationData, minDistance: Double = 10.0): Boolean {
        val lastLocation = locationDao.getLastLocation()

        // Если это первая локация или локация изменилась значительно
        val shouldInsert = lastLocation == null || LocationUtils.hasLocationChanged(
            location.latitude, location.longitude,
            lastLocation.latitude, lastLocation.longitude,
            minDistance
        )

        if (shouldInsert) {
            locationDao.insertLocation(location)

            // Автоматически синхронизируем все неотправленные локации
            sendUnsentLocations()

            return true
        }

        return false
    }

    suspend fun insertLocations(locations: List<LocationData>) {
        locationDao.insertLocations(locations)
    }

    suspend fun getUnsentCount(): Int {
        return locationDao.getUnsentCount()
    }

    fun getUnsentCountFlow(): Flow<Int> = locationDao.getUnsentCountFlow()

        suspend fun sendUnsentLocations(): Boolean {
        return try {
            val unsentLocations = locationDao.getUnsentLocations(50) // Отправляем по 50 записей за раз

            if (unsentLocations.isEmpty()) {
                return true
            }

            val deviceId = DeviceIdManager.getDeviceId(context)
            val apiRequests = unsentLocations.map { it.toApiRequest(deviceId) }
            val batchRequest = LocationBatchRequest(locations = apiRequests)
            val response = apiService.sendLocationsBatch(batchRequest)

            if (response.isSuccessful) {
                val ids = unsentLocations.map { it.id }
                locationDao.markAsSent(ids)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendSingleLocation(location: LocationData): Boolean {
        return try {
            val deviceId = DeviceIdManager.getDeviceId(context)
            val response = apiService.sendLocation(location.toApiRequest(deviceId))
            if (response.isSuccessful) {
                locationDao.markAsSent(listOf(location.id))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteOldSentLocations() {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 дней
        locationDao.deleteOldSentLocations(cutoffTime)
    }

    suspend fun deleteAllLocations() {
        locationDao.deleteAllLocations()
    }
}
