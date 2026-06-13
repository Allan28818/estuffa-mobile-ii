package com.example.esttufa.repository

import com.example.esttufa.model.ApiService
import com.example.esttufa.model.CreateStoveRequest
import com.example.esttufa.model.RetrofitClient
import com.example.esttufa.model.StoveResponse
import com.example.esttufa.model.UpdateStoveRequest

class StoveRepository(
    private val apiService: ApiService = RetrofitClient.api
) {

    suspend fun createStove(name: String, crop: String): Result<StoveResponse> =
        runCatching {
            apiService.createStove(CreateStoveRequest(name, crop))
        }

    suspend fun getStoves(): Result<List<StoveResponse>> =
        runCatching {
            apiService.getStoves().stoves
        }

    suspend fun getStove(stoveId: String): Result<StoveResponse> =
        runCatching {
            apiService.getStove(stoveId)
        }

    suspend fun updateStove(
        stoveId: String,
        name: String?,
        crop: String?
    ): Result<StoveResponse> =
        runCatching {
            apiService.updateStove(stoveId, UpdateStoveRequest(name, crop))
        }

    suspend fun deleteStove(stoveId: String): Result<Unit> =
        runCatching {
            apiService.deleteStove(stoveId)
        }
}
