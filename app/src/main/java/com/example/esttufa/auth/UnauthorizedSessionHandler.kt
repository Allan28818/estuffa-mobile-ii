package com.example.esttufa.auth

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.esttufa.MainActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.atomic.AtomicBoolean

object UnauthorizedSessionHandler {

    private val redirectInProgress = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun handle() {
        FirebaseAuth.getInstance().signOut()

        if (!redirectInProgress.compareAndSet(false, true)) {
            return
        }

        mainHandler.post {
            val context = FirebaseApp.getInstance().applicationContext
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            mainHandler.postDelayed(
                { redirectInProgress.set(false) },
                REDIRECT_DEBOUNCE_MILLIS
            )
        }
    }

    private const val REDIRECT_DEBOUNCE_MILLIS = 1_000L
}
