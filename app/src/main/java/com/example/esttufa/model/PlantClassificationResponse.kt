package com.example.esttufa.model

data class PlantClassificationResponse(
    val model: String?,
    val prediction: String?,
    val predicted_class: String?,
    val confidence: Double?,
    val class_name: String?
) {
    fun resolvedClassName(): String? =
        sequenceOf(prediction, predicted_class, class_name)
            .mapNotNull { value -> value?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
}
