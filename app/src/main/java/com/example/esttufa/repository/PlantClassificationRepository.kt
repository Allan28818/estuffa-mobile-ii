package com.example.esttufa.repository

import com.example.esttufa.model.ApiService
import com.example.esttufa.model.PlantClassificationResponse
import com.example.esttufa.model.RetrofitClient
import okhttp3.MultipartBody

class PlantClassificationRepository(
    private val apiService: ApiService = RetrofitClient.api
) {
    suspend fun predict(
        imagePart: MultipartBody.Part,
        model: String = "decision_tree"
    ): Result<PlantClassificationResponse> =
        runCatching {
            apiService.predictPlantClassification(model, imagePart)
        }
}
