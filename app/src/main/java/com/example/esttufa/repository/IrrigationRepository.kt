package com.example.esttufa.repository

import com.example.esttufa.model.IrrigationResponse
import com.example.esttufa.model.RetrofitClient

class IrrigationRepository {

    suspend fun getIrrigationTime(
        crop: String,
        version: String
    ): Result<IrrigationResponse> {
        return try {
            val response = RetrofitClient.api.getIrrigationTime(crop, version)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}