package com.demkin.childtracker.data.api

import com.demkin.childtracker.data.model.LocationApiRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LocationApiService {

    @POST("location")
    suspend fun sendLocation(@Body location: LocationApiRequest): Response<Unit>

    @POST("locations/batch")
    suspend fun sendLocationsBatch(@Body locations: List<LocationApiRequest>): Response<Unit>
}
