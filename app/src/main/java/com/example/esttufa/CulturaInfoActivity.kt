package com.example.esttufa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.core.content.FileProvider
import com.example.esttufa.databinding.ActivityCulturaInfoBinding
import com.example.esttufa.viewmodel.ClassificationUiState
import com.example.esttufa.viewmodel.CulturaInfoUiState
import com.example.esttufa.viewmodel.CulturaInfoViewModel
import java.io.File
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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
    private var isIrrigationLoading = false
    private var isClassificationLoading = false
    private var pendingCameraUri: Uri? = null

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
            launchCamera()
        } else {
            Toast.makeText(this, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para tirar foto
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { wasSaved ->
        val capturedImageUri = pendingCameraUri
        pendingCameraUri = null

        if (wasSaved && capturedImageUri != null) {
            processImageResult(capturedImageUri)
        } else if (wasSaved) {
            showClassificationError("Não foi possível acessar a foto capturada.")
        }
    }

    // Launcher para escolher da galeria
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processImageResult(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCulturaInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pendingCameraUri = savedInstanceState
            ?.getString(KEY_PENDING_CAMERA_URI)
            ?.let(Uri::parse)

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
                launchCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        runCatching {
            createCameraImageUri()
        }.onSuccess { imageUri ->
            pendingCameraUri = imageUri
            takePictureLauncher.launch(imageUri)
        }.onFailure {
            showClassificationError("Não foi possível preparar a câmera.")
        }
    }

    private fun createCameraImageUri(): Uri {
        val imageDirectory = File(cacheDir, CAMERA_IMAGE_DIRECTORY).apply {
            check(isDirectory || mkdirs()) {
                "Não foi possível criar o diretório temporário de imagens"
            }
        }
        val imageFile = File.createTempFile(CAMERA_FILE_PREFIX, ".jpg", imageDirectory)

        return FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile
        )
    }

    private fun processImageResult(uri: Uri) {
        binding.tvInstrucaoFoto.visibility = View.GONE
        binding.cvFotoPreview.visibility = View.VISIBLE
        binding.ivFotoResult.setImageDrawable(null)
        binding.ivFotoResult.setImageURI(uri)

        val imageBytes = runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            } ?: error("Não foi possível abrir a imagem")
        }.getOrElse {
            showClassificationError("Não foi possível ler a foto capturada.")
            return
        }

        val mediaType = contentResolver.getType(uri)
            ?.takeIf { it.startsWith("image/") }
            ?: "image/jpeg"
        val requestBody = imageBytes.toRequestBody(mediaType.toMediaType())
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            if (mediaType == "image/png") "photo.png" else "photo.jpg",
            requestBody
        )
        viewModel.classifyImage(imagePart)
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
                is CulturaInfoUiState.Loading -> {
                    isIrrigationLoading = true
                    updateProgressVisibility()
                }
                is CulturaInfoUiState.Success -> {
                    isIrrigationLoading = false
                    updateProgressVisibility()
                    val data = state.data
                    binding.tvTemperatura.text  = "${"%.0f".format(data.temperature)}°C"
                    binding.tvUmidade.text      = "${"%.0f".format(data.moisture)}%"
                    binding.tvLuminosidade.text = "${"%.0f".format(data.light)}"
                    binding.tvIrrigacao.text    = "${"%.2f".format(data.irrigation_time)} seg"
                    binding.tvClassName.text    = data.class_name
                    timeLeftInMillis = (data.irrigation_time * 1000).toLong()
                }
                is CulturaInfoUiState.Error -> {
                    isIrrigationLoading = false
                    updateProgressVisibility()
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.classificationState.observe(this) { state ->
            when (state) {
                ClassificationUiState.Idle -> Unit
                ClassificationUiState.Loading -> {
                    isClassificationLoading = true
                    binding.cvFotoReconhecida.visibility = View.GONE
                    binding.cvFotoNaoReconhecida.visibility = View.GONE
                    updateProgressVisibility()
                }
                is ClassificationUiState.Success -> {
                    isClassificationLoading = false
                    binding.tvNomeCulturaResult.text = state.className
                    binding.cvFotoReconhecida.visibility = View.VISIBLE
                    binding.cvFotoNaoReconhecida.visibility = View.GONE
                    updateProgressVisibility()
                }
                is ClassificationUiState.Error -> {
                    isClassificationLoading = false
                    showClassificationError(state.message)
                    updateProgressVisibility()
                }
            }
        }
    }

    private fun showClassificationError(message: String) {
        binding.cvFotoReconhecida.visibility = View.GONE
        binding.cvFotoNaoReconhecida.visibility = View.VISIBLE
        binding.tvFotoErro.text = message
    }

    private fun updateProgressVisibility() {
        binding.progressBar.visibility = if (
            isIrrigationLoading || isClassificationLoading
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.fabCamera.isEnabled = !isClassificationLoading
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_PENDING_CAMERA_URI, pendingCameraUri?.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val CAMERA_IMAGE_DIRECTORY = "captured_images"
        private const val CAMERA_FILE_PREFIX = "classification_"
        private const val KEY_PENDING_CAMERA_URI = "pending_camera_uri"
    }
}
