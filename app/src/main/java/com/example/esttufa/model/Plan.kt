package com.example.esttufa.model

data class Plan(
    val id: String,
    val name: String,
    val priceRange: String,
    val benefits: List<String>,
    val maxVisibleBenefits: Int = 3,
    val isRecommended: Boolean = false,
    val emoji: String
)
