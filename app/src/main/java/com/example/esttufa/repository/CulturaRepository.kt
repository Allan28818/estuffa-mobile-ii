package com.example.esttufa.repository

import com.example.esttufa.model.Cultura
import com.example.esttufa.model.RetrofitClient

class CulturaRepository {

    suspend fun getCulturas(): Result<List<Cultura>> {
        return try {
            val response = RetrofitClient.api.getCulturas()
            Result.success(response.stoves)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}