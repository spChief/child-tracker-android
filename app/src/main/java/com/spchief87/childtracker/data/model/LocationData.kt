package com.spchief87.childtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "location_data")
data class LocationData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSent: Boolean = false,
    val provider: String? = null
) {
    fun toApiRequest(deviceId: String): LocationApiRequest {
        return LocationApiRequest(
            deviceId = deviceId,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            altitude = altitude,
            speed = speed,
            bearing = bearing,
            timestamp = timestamp,
            provider = provider
        )
    }
}

data class LocationApiRequest(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long,
    val provider: String? = null
)

data class LocationBatchRequest(
    val locations: List<LocationApiRequest>
)
