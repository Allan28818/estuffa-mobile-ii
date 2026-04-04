package com.example.esttufa.model

data class IrrigationResponse(
    val version: String,
    val crop: String,
    val moisture: Double,
    val temperature: Double,
    val light: Double,
    val irrigation_time: Double,
    val class_name: String
)