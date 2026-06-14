package com.example.esttufa.model

data class UserProfile(
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val phone: String?,
    val location: String?,
    val currentPlanId: String,
    val renewalDate: String?
)
