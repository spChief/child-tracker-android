package com.demkin.childtracker.data.database

import androidx.room.*
import com.demkin.childtracker.data.model.LocationData
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM location_data ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationData>>

    @Query("SELECT * FROM location_data WHERE isSent = 0 ORDER BY timestamp ASC")
    fun getUnsentLocations(): Flow<List<LocationData>>

    @Query("SELECT * FROM location_data WHERE isSent = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsentLocations(limit: Int): List<LocationData>

    @Query("SELECT COUNT(*) FROM location_data WHERE isSent = 0")
    suspend fun getUnsentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationData>)

    @Update
    suspend fun updateLocation(location: LocationData)

    @Query("UPDATE location_data SET isSent = 1 WHERE id IN (:ids)")
    suspend fun markAsSent(ids: List<Long>)

    @Query("DELETE FROM location_data WHERE isSent = 1 AND timestamp < :cutoffTime")
    suspend fun deleteOldSentLocations(cutoffTime: Long)

    @Query("DELETE FROM location_data WHERE id = :id")
    suspend fun deleteLocation(id: Long)

    @Query("DELETE FROM location_data")
    suspend fun deleteAllLocations()
}
