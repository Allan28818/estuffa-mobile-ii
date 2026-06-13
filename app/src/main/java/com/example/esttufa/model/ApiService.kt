package com.example.esttufa.model

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {

    @GET("irrigation-time/sensor-simulated/{crop}")
    suspend fun getIrrigationTime(
        @Path("crop") crop: String,
        @Query("version") version: String
    ): IrrigationResponse

    @GET("stoves/list")
    suspend fun getCulturas(): CulturaResponse

    @POST("stoves")
    suspend fun createStove(@Body body: CreateStoveRequest): StoveResponse

    @GET("stoves")
    suspend fun getStoves(): StoveListResponse

    @GET("stoves/{stove_id}")
    suspend fun getStove(@Path("stove_id") stoveId: String): StoveResponse

    @PUT("stoves/{stove_id}")
    suspend fun updateStove(
        @Path("stove_id") stoveId: String,
        @Body body: UpdateStoveRequest
    ): StoveResponse

    @DELETE("stoves/{stove_id}")
    suspend fun deleteStove(@Path("stove_id") stoveId: String)
}
