package com.spchief87.childtracker.data.api

import com.spchief87.childtracker.data.model.LocationApiRequest
import com.spchief87.childtracker.data.model.LocationBatchRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LocationApiService {

    @POST("location")
    suspend fun sendLocation(@Body location: LocationApiRequest): Response<Unit>

    @POST("locations/batch")
    suspend fun sendLocationsBatch(@Body batchRequest: LocationBatchRequest): Response<Unit>
}
