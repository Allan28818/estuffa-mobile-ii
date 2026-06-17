package com.example.esttufa

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.text.Html
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.adapter.CulturaAdapter
import com.example.esttufa.databinding.ActivityHomeBinding
import com.example.esttufa.viewmodel.HomeViewModel
import com.example.esttufa.warming.ApiWarmingHelper
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.sqrt

class HomeActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sensorManager: SensorManager
    private val viewModel: HomeViewModel by viewModels()
    private var hasCompletedFirstResume = false
    private var accelerometer: Sensor? = null
    private var lastShakeAtMillis = 0L

    private val createStoveLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadStoves()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiWarmingHelper.warmUp()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupShakeRefresh()
        setupUI()
        observeViewModel()
        viewModel.loadStoves()
    }

    private fun setupShakeRefresh() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun setupUI() {
        val displayName = FirebaseAuth.getInstance()
            .currentUser
            ?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: "usuário"

        binding.tvBoasVindas.text = Html.fromHtml(
            "Olá, <b>${Html.escapeHtml(displayName)}</b> 👋",
            Html.FROM_HTML_MODE_COMPACT
        )

        binding.fabAddEsttufa.setOnClickListener {
            createStoveLauncher.launch(
                Intent(this, CadastroEstufaActivity::class.java)
            )
        }

        binding.llHeader.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.stoves.observe(this) { stoves ->
            binding.lvCulturas.adapter = CulturaAdapter(this, stoves)
            binding.lvCulturas.setOnItemClickListener { _, _, position, _ ->
                val stove = stoves[position]
                val intent = Intent(this, CulturaInfoActivity::class.java).apply {
                    putExtra("cultura", stove.crop)
                    putExtra("crop", stove.crop)
                    putExtra("stove_id", stove.id)
                    putExtra("stove_name", stove.name)
                }
                startActivity(intent)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.llEmptyState.visibility = if (isEmpty) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.lvCulturas.visibility = if (isEmpty) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerShakeRefresh()
        if (hasCompletedFirstResume) {
            viewModel.loadStoves()
        } else {
            hasCompletedFirstResume = true
        }
    }

    override fun onPause() {
        unregisterShakeRefresh()
        super.onPause()
    }

    private fun registerShakeRefresh() {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private fun unregisterShakeRefresh() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val gravityX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gravityY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gravityZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(
            (gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ).toDouble()
        )

        if (gForce <= SHAKE_THRESHOLD_GRAVITY) return

        val currentTimeMillis = SystemClock.elapsedRealtime()
        if (currentTimeMillis - lastShakeAtMillis < SHAKE_DEBOUNCE_MS) return

        lastShakeAtMillis = currentTimeMillis
        viewModel.loadStoves()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7
        private const val SHAKE_DEBOUNCE_MS = 1_200L
    }
}
