package com.example.esttufa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.esttufa.databinding.ActivityCulturaInfoBinding
import com.example.esttufa.viewmodel.CulturaInfoUiState
import com.example.esttufa.viewmodel.CulturaInfoViewModel
import java.util.*

class CulturaInfoActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityCulturaInfoBinding
    private val viewModel: CulturaInfoViewModel by viewModels()

    // Sensores e Hardware
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var vibrator: Vibrator? = null

    // Estado da Irrigação
    private var isPopupOpen = false
    private var isWatering = false
    private var timeLeftInMillis: Long = 0L
    private var isFinished = false

    // Timer
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isWatering && timeLeftInMillis > 0) {
                timeLeftInMillis -= 100
                updateTimerUI()
                if (timeLeftInMillis <= 0) {
                    onIrrigationFinished()
                } else {
                    timerHandler.postDelayed(this, 100)
                }
            }
        }
    }

    // --- Lógica de Câmera e Galeria ---

    // Launcher para permissão de câmera
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para tirar foto
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            processImageResult(bitmap, null)
        }
    }

    // Launcher para escolher da galeria
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processImageResult(null, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCulturaInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val culturaEn = intent.getStringExtra("cultura") ?: "lettuce"
        setupUI(culturaEn)
        observeViewModel()
        viewModel.load(culturaEn, "V1")
    }

    private fun setupUI(culturaEn: String) {
        val (nomePt, imagemRes) = when (culturaEn.lowercase()) {
            "lettuce" -> "Alface" to R.drawable.img_alface
            "arugula" -> "Rúcula" to R.drawable.img_rucula
            "tomato"  -> "Tomate" to R.drawable.img_tomate
            else      -> "Cultura" to R.drawable.img_alface
        }
        binding.ivCulturaHeader.setImageResource(imagemRes)
        binding.tvCulturaInfoTitle.text = "Estufa de $nomePt"
        binding.ivBack.setOnClickListener { finish() }

        binding.cvIrrigacao.setOnClickListener { if (!isPopupOpen) openIrrigationPopup() }
        binding.btnOverlayClose.setOnClickListener { closeIrrigationPopup() }
        binding.btnPopupStop.setOnClickListener { stopIrrigationProcess() }
        binding.fabCamera.setOnClickListener { showImageSourceDialog() }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Tirar Foto", "Escolher da Galeria")
        AlertDialog.Builder(this)
            .setTitle("Atualizar foto da cultura")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePictureLauncher.launch(null)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun processImageResult(bitmap: Bitmap?, uri: Uri?) {
        val isRecognized = Random().nextInt(10) < 7
        binding.tvInstrucaoFoto.visibility = View.GONE

        if (isRecognized) {
            binding.cvFotoReconhecida.visibility = View.VISIBLE
            binding.cvFotoNaoReconhecida.visibility = View.GONE
            if (bitmap != null) binding.ivFotoResult.setImageBitmap(bitmap)
            else if (uri != null) binding.ivFotoResult.setImageURI(uri)

            val culturaTitle = binding.tvCulturaInfoTitle.text.toString()
            binding.tvNomeCulturaResult.text = culturaTitle.replace("Estufa de ", "")
        } else {
            binding.cvFotoReconhecida.visibility = View.GONE
            binding.cvFotoNaoReconhecida.visibility = View.VISIBLE
        }
    }

    private fun openIrrigationPopup() {
        isPopupOpen = true
        isFinished = false
        binding.clIrrigationPopup.visibility = View.VISIBLE
        binding.btnPopupStop.visibility = View.GONE
        binding.llWaterParticles.visibility = View.INVISIBLE
        updateTimerUI()
    }

    private fun closeIrrigationPopup() {
        stopWatering()
        stopVibration()
        isPopupOpen = false
        binding.clIrrigationPopup.visibility = View.GONE
    }

    private fun stopIrrigationProcess() {
        stopVibration()
        binding.btnPopupStop.visibility = View.GONE
        Toast.makeText(this, "Irrigação concluída!", Toast.LENGTH_SHORT).show()
        closeIrrigationPopup()
    }

    private fun updateTimerUI() {
        val seconds = timeLeftInMillis / 1000.0
        binding.tvPopupTimer.text = String.format(Locale.US, "%.2fs", seconds)
    }

    private fun startWatering() {
        if (!isWatering && !isFinished) {
            isWatering = true
            binding.llWaterParticles.visibility = View.VISIBLE
            timerHandler.post(timerRunnable)
        }
    }

    private fun stopWatering() {
        isWatering = false
        binding.llWaterParticles.visibility = View.INVISIBLE
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun onIrrigationFinished() {
        isFinished = true
        stopWatering()
        timeLeftInMillis = 0
        updateTimerUI()
        binding.btnPopupStop.visibility = View.VISIBLE
        startContinuousVibration()
    }

    private fun startContinuousVibration() {
        val pattern = longArrayOf(0, 500, 200)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopVibration() { vibrator?.cancel() }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CulturaInfoUiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is CulturaInfoUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val data = state.data
                    binding.tvTemperatura.text  = "${"%.0f".format(data.temperature)}°C"
                    binding.tvUmidade.text      = "${"%.0f".format(data.moisture)}%"
                    binding.tvLuminosidade.text = "${"%.0f".format(data.light)}"
                    binding.tvIrrigacao.text    = "${"%.2f".format(data.irrigation_time)} seg"
                    binding.tvClassName.text    = data.class_name
                    timeLeftInMillis = (data.irrigation_time * 1000).toLong()
                }
                is CulturaInfoUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isPopupOpen || isFinished) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val xAxis = event.values[0]
            if (xAxis > 7.0) startWatering() else stopWatering()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopWatering()
        stopVibration()
    }
}