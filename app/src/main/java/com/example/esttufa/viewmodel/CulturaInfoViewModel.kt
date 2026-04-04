package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.model.IrrigationResponse
import com.example.esttufa.repository.IrrigationRepository
import kotlinx.coroutines.launch

sealed class CulturaInfoUiState {
    object Loading : CulturaInfoUiState()
    data class Success(val data: IrrigationResponse) : CulturaInfoUiState()
    data class Error(val message: String) : CulturaInfoUiState()
}

class CulturaInfoViewModel(
    private val repository: IrrigationRepository = IrrigationRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<CulturaInfoUiState>()
    val uiState: LiveData<CulturaInfoUiState> = _uiState

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
}