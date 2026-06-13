package com.example.esttufa.model

data class CreateStoveRequest(
    val name: String,
    val crop: String
)

data class UpdateStoveRequest(
    val name: String? = null,
    val crop: String? = null
)
