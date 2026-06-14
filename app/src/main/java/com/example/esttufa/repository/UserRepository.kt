package com.example.esttufa.repository

import com.example.esttufa.model.UserProfile
import com.google.firebase.auth.FirebaseAuth

class UserRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getProfile(): Result<UserProfile> = runCatching {
        val user = checkNotNull(firebaseAuth.currentUser) {
            "Usuário não autenticado."
        }

        UserProfile(
            displayName = user.displayName?.takeIf(String::isNotBlank) ?: "Usuário",
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString(),
            phone = null,
            location = null,
            currentPlanId = PlanRepository.getUserCurrentPlanId(),
            renewalDate = "15/07/2026"
        )
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
