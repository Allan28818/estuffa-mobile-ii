package com.example.esttufa.model

data class PlantClassificationResponse(
    val predicted_class: String?,
    val confidence: Double?,
    val class_name: String?
)
