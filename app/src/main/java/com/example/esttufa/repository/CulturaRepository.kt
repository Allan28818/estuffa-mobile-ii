package com.example.esttufa.repository

import com.example.esttufa.model.RetrofitClient
import com.example.esttufa.model.StoveResponse

class CulturaRepository {

    suspend fun getStoves(): Result<List<StoveResponse>> =
        runCatching {
            RetrofitClient.api.getStoves().stoves
        }
}
