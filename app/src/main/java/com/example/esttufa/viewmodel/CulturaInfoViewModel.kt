package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.model.IrrigationResponse
import com.example.esttufa.repository.IrrigationRepository
import com.example.esttufa.repository.PlantClassificationRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

sealed class CulturaInfoUiState {
    object Loading : CulturaInfoUiState()
    data class Success(val data: IrrigationResponse) : CulturaInfoUiState()
    data class Error(val message: String) : CulturaInfoUiState()
}

sealed class ClassificationUiState {
    object Idle : ClassificationUiState()
    object Loading : ClassificationUiState()
    data class Success(val className: String) : ClassificationUiState()
    data class Error(val message: String) : ClassificationUiState()
}

class CulturaInfoViewModel(
    private val repository: IrrigationRepository = IrrigationRepository(),
    private val classificationRepository: PlantClassificationRepository = PlantClassificationRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<CulturaInfoUiState>()
    val uiState: LiveData<CulturaInfoUiState> = _uiState

    private val _classificationState =
        MutableLiveData<ClassificationUiState>(ClassificationUiState.Idle)
    val classificationState: LiveData<ClassificationUiState> = _classificationState

    fun load(cropEn: String, version: String) {
        viewModelScope.launch {
            _uiState.value = CulturaInfoUiState.Loading

            val result = repository.getIrrigationTime(cropEn, version)

            _uiState.value = result.fold(
                onSuccess = { CulturaInfoUiState.Success(it) },
                onFailure = { CulturaInfoUiState.Error(it.message ?: "Erro desconhecido") }
            )
        }
    }

    fun classifyImage(imagePart: MultipartBody.Part) {
        if (_classificationState.value == ClassificationUiState.Loading) return

        _classificationState.value = ClassificationUiState.Loading

        viewModelScope.launch {
            classificationRepository.predict(imagePart).fold(
                onSuccess = { response ->
                    _classificationState.value = response.resolvedClassName()?.let {
                        ClassificationUiState.Success(it)
                    } ?: ClassificationUiState.Error("Classe nao identificada")
                },
                onFailure = {
                    _classificationState.value =
                        ClassificationUiState.Error(it.message ?: "Erro desconhecido")
                }
            )
        }
    }
}
