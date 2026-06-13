package com.example.esttufa.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val user = firebaseAuth.currentUser ?: return chain.proceed(chain.request())
        val token = runCatching {
            Tasks.await(user.getIdToken(false)).token
        }.getOrNull()

        if (token.isNullOrBlank()) {
            return chain.proceed(chain.request())
        }

        val authenticatedRequest = chain.request()
            .newBuilder()
            .header(AUTHORIZATION_HEADER, "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
