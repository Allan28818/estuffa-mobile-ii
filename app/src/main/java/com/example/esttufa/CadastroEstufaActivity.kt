package com.example.esttufa

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityCadastroEstufaBinding
import com.example.esttufa.viewmodel.CadastroEstufaUiState
import com.example.esttufa.viewmodel.CadastroEstufaViewModel
import java.util.Locale
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class CadastroEstufaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroEstufaBinding
    private val viewModel: CadastroEstufaViewModel by viewModels()
    private var selectedGeoPoint: GeoPoint? = null
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreSelectedCoordinate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        binding = ActivityCadastroEstufaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupMap()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        val culturas = arrayOf("Alface", "Tomate", "Rúcula")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            culturas
        )
        binding.actvCultura.setAdapter(adapter)

        binding.btnCriarEstufa.setOnClickListener {
            val nome = binding.etNomeEstufa.text?.toString().orEmpty().trim()
            val cultura = binding.actvCultura.text?.toString().orEmpty().trim()

            if (validateFields(nome, cultura)) {
                viewModel.createStove(nome, cultura)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMap() {
        updateSelectedCoordinateText()
        binding.mapEstufa.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> view.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> view.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        binding.mapEstufa.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapEstufa.setMultiTouchControls(true)
        binding.mapEstufa.minZoomLevel = MIN_MAP_ZOOM

        binding.mapEstufa.overlays.add(
            MapEventsOverlay(
                object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                        selectCoordinate(point, animateCamera = true)
                        return true
                    }

                    override fun longPressHelper(point: GeoPoint): Boolean = false
                }
            )
        )

        val initialTarget = selectedGeoPoint ?: DEFAULT_CAMERA_TARGET
        val initialZoom = if (selectedGeoPoint == null) DEFAULT_CAMERA_ZOOM else SELECTED_CAMERA_ZOOM
        binding.mapEstufa.controller.setZoom(initialZoom)
        binding.mapEstufa.controller.setCenter(initialTarget)
        selectedGeoPoint?.let { selectCoordinate(it, animateCamera = false) }
    }

    private fun selectCoordinate(geoPoint: GeoPoint, animateCamera: Boolean) {
        selectedGeoPoint = geoPoint
        selectedMarker?.let(binding.mapEstufa.overlays::remove)
        selectedMarker = Marker(binding.mapEstufa).apply {
            position = geoPoint
            title = "Coordenada selecionada"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.mapEstufa.overlays.add(selectedMarker)

        if (animateCamera) {
            binding.mapEstufa.controller.animateTo(geoPoint)
            binding.mapEstufa.controller.setZoom(SELECTED_CAMERA_ZOOM)
        }

        updateSelectedCoordinateText()
        binding.mapEstufa.invalidate()
    }

    private fun updateSelectedCoordinateText() {
        val geoPoint = selectedGeoPoint
        binding.tvSelectedCoordinates.text = if (geoPoint == null) {
            "Nenhuma coordenada selecionada"
        } else {
            String.format(
                Locale.US,
                "Lat: %.6f | Lng: %.6f",
                geoPoint.latitude,
                geoPoint.longitude
            )
        }
    }

    private fun restoreSelectedCoordinate(savedInstanceState: Bundle?) {
        val hasSelectedCoordinate = savedInstanceState?.getBoolean(
            KEY_HAS_SELECTED_COORDINATE
        ) ?: false
        if (hasSelectedCoordinate) {
            selectedGeoPoint = GeoPoint(
                savedInstanceState.getDouble(KEY_SELECTED_LATITUDE),
                savedInstanceState.getDouble(KEY_SELECTED_LONGITUDE)
            )
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                CadastroEstufaUiState.Idle -> binding.btnCriarEstufa.isEnabled = true
                CadastroEstufaUiState.Loading -> binding.btnCriarEstufa.isEnabled = false
                is CadastroEstufaUiState.Success -> {
                    Toast.makeText(
                        this,
                        "Estufa criada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is CadastroEstufaUiState.Error -> {
                    binding.btnCriarEstufa.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateFields(nome: String, cultura: String): Boolean {
        binding.tilNomeEstufa.error = if (nome.isEmpty()) {
            "Informe o nome da estufa."
        } else {
            null
        }
        binding.tilCultura.error = if (cultura.isEmpty()) {
            "Selecione uma cultura."
        } else {
            null
        }

        return nome.isNotEmpty() && cultura.isNotEmpty()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapEstufa.onResume()
    }

    override fun onPause() {
        binding.mapEstufa.onPause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        binding.mapEstufa.onDetach()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedGeoPoint?.let { geoPoint ->
            outState.putBoolean(KEY_HAS_SELECTED_COORDINATE, true)
            outState.putDouble(KEY_SELECTED_LATITUDE, geoPoint.latitude)
            outState.putDouble(KEY_SELECTED_LONGITUDE, geoPoint.longitude)
        }
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val MIN_MAP_ZOOM = 3.0
        const val DEFAULT_CAMERA_ZOOM = 15.0
        const val SELECTED_CAMERA_ZOOM = 15.0
        const val KEY_HAS_SELECTED_COORDINATE = "has_selected_coordinate"
        const val KEY_SELECTED_LATITUDE = "selected_latitude"
        const val KEY_SELECTED_LONGITUDE = "selected_longitude"
        val DEFAULT_CAMERA_TARGET = GeoPoint(-15.7801, -47.9292)
    }
}
