package com.example.esttufa.warming

import com.example.esttufa.model.RetrofitClient
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object ApiWarmingHelper {
    private val alreadyWarmed = AtomicBoolean(false)

    fun warmUp() {
        if (!alreadyWarmed.compareAndSet(false, true)) return

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching { RetrofitClient.api.hearthBeat() }
        }
    }
}
