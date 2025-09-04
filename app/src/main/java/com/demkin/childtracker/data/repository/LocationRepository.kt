package com.demkin.childtracker.data.repository

import com.demkin.childtracker.data.api.LocationApiService
import com.demkin.childtracker.data.database.LocationDao
import com.demkin.childtracker.data.model.LocationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val apiService: LocationApiService
) {

    fun getAllLocations(): Flow<List<LocationData>> = locationDao.getAllLocations()

    fun getUnsentLocations(): Flow<List<LocationData>> = locationDao.getUnsentLocations()

    suspend fun insertLocation(location: LocationData): Long {
        return locationDao.insertLocation(location)
    }

    suspend fun insertLocations(locations: List<LocationData>) {
        locationDao.insertLocations(locations)
    }

    suspend fun getUnsentCount(): Int {
        return locationDao.getUnsentCount()
    }

    suspend fun sendUnsentLocations(): Boolean {
        return try {
            val unsentLocations = locationDao.getUnsentLocations(50) // Отправляем по 50 записей за раз

            if (unsentLocations.isEmpty()) {
                return true
            }

            val apiRequests = unsentLocations.map { it.toApiRequest() }
            val response = apiService.sendLocationsBatch(apiRequests)

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
            val response = apiService.sendLocation(location.toApiRequest())
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
