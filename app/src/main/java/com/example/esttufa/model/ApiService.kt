package com.example.esttufa.model

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("irrigation-time/sensor-simulated/{crop}")
    suspend fun getIrrigationTime(
        @Path("crop") crop: String,
        @Query("version") version: String
    ): IrrigationResponse

    @GET("stoves/list")
    suspend fun getCulturas(): CulturaResponse
}