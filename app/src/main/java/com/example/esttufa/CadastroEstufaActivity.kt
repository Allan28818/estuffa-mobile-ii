package com.example.esttufa

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityCadastroEstufaBinding
import com.example.esttufa.viewmodel.CadastroEstufaUiState
import com.example.esttufa.viewmodel.CadastroEstufaViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class CadastroEstufaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCadastroEstufaBinding
    private val viewModel: CadastroEstufaViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreSelectedCoordinate(savedInstanceState)
        binding = ActivityCadastroEstufaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapEstufa.onCreate(savedInstanceState)

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

    private fun setupMap() {
        updateSelectedCoordinateText()
        binding.mapEstufa.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.setOnMapClickListener { latLng ->
            selectCoordinate(latLng, animateCamera = true)
        }

        val initialTarget = selectedLatLng ?: DEFAULT_CAMERA_TARGET
        val initialZoom = if (selectedLatLng == null) DEFAULT_CAMERA_ZOOM else SELECTED_CAMERA_ZOOM
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialTarget, initialZoom))
        selectedLatLng?.let { selectCoordinate(it, animateCamera = false) }
    }

    private fun selectCoordinate(latLng: LatLng, animateCamera: Boolean) {
        selectedLatLng = latLng
        selectedMarker?.remove()
        selectedMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Coordenada selecionada")
        )

        if (animateCamera) {
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, SELECTED_CAMERA_ZOOM)
            )
        }

        updateSelectedCoordinateText()
    }

    private fun updateSelectedCoordinateText() {
        val latLng = selectedLatLng
        binding.tvSelectedCoordinates.text = if (latLng == null) {
            "Nenhuma coordenada selecionada"
        } else {
            String.format(
                Locale.US,
                "Lat: %.6f | Lng: %.6f",
                latLng.latitude,
                latLng.longitude
            )
        }
    }

    private fun restoreSelectedCoordinate(savedInstanceState: Bundle?) {
        val hasSelectedCoordinate = savedInstanceState?.getBoolean(
            KEY_HAS_SELECTED_COORDINATE
        ) ?: false
        if (hasSelectedCoordinate) {
            selectedLatLng = LatLng(
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
        binding.mapEstufa.onStart()
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
        binding.mapEstufa.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        binding.mapEstufa.onDestroy()
        googleMap = null
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapEstufa.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedLatLng?.let { latLng ->
            outState.putBoolean(KEY_HAS_SELECTED_COORDINATE, true)
            outState.putDouble(KEY_SELECTED_LATITUDE, latLng.latitude)
            outState.putDouble(KEY_SELECTED_LONGITUDE, latLng.longitude)
        }
        super.onSaveInstanceState(outState)
        binding.mapEstufa.onSaveInstanceState(outState)
    }

    private companion object {
        const val DEFAULT_CAMERA_ZOOM = 4f
        const val SELECTED_CAMERA_ZOOM = 15f
        const val KEY_HAS_SELECTED_COORDINATE = "has_selected_coordinate"
        const val KEY_SELECTED_LATITUDE = "selected_latitude"
        const val KEY_SELECTED_LONGITUDE = "selected_longitude"
        val DEFAULT_CAMERA_TARGET = LatLng(-15.7801, -47.9292)
    }
}
