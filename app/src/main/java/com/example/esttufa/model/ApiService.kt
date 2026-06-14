package com.example.esttufa.model

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {

    @Multipart
    @POST("plant-classification/predict")
    suspend fun predictPlantClassification(
        @Query("model") model: String = "decision_tree",
        @Part image: MultipartBody.Part
    ): PlantClassificationResponse

    @GET("hearth-beat")
    suspend fun hearthBeat()

    @GET("irrigation-time/sensor-simulated/{crop}")
    suspend fun getIrrigationTime(
        @Path("crop") crop: String,
        @Query("version") version: String
    ): IrrigationResponse

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
