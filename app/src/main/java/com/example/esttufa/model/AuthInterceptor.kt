package com.example.esttufa.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val onUnauthorized: () -> Unit = {}
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val user = firebaseAuth.currentUser
        val token = runCatching {
            user?.let { Tasks.await(it.getIdToken(true)).token }
        }.getOrNull()

        val requestWithAuthentication = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "Bearer $token")
                .build()
        }

        val response = chain.proceed(requestWithAuthentication)
        if (response.code == HTTP_UNAUTHORIZED) {
            onUnauthorized()
        }

        return response
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val HTTP_UNAUTHORIZED = 401
    }
}
