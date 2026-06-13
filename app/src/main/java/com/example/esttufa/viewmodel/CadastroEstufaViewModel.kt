package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.model.StoveResponse
import com.example.esttufa.repository.StoveRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class CadastroEstufaUiState {
    object Idle : CadastroEstufaUiState()
    object Loading : CadastroEstufaUiState()
    data class Success(val stove: StoveResponse) : CadastroEstufaUiState()
    data class Error(val message: String) : CadastroEstufaUiState()
}

class CadastroEstufaViewModel(
    private val repository: StoveRepository = StoveRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<CadastroEstufaUiState>(
        CadastroEstufaUiState.Idle
    )
    val uiState: LiveData<CadastroEstufaUiState> = _uiState

    fun createStove(name: String, cropInPortuguese: String) {
        val crop = CROP_VALUES[cropInPortuguese]
        if (crop == null) {
            _uiState.value = CadastroEstufaUiState.Error(
                "Selecione uma cultura válida."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = CadastroEstufaUiState.Loading
            _uiState.value = repository.createStove(name.trim(), crop).fold(
                onSuccess = { CadastroEstufaUiState.Success(it) },
                onFailure = { CadastroEstufaUiState.Error(errorMessage(it)) }
            )
        }
    }

    private fun errorMessage(error: Throwable): String =
        when {
            error is HttpException && error.code() == HTTP_UNPROCESSABLE_ENTITY ->
                "Não foi possível criar a estufa. Verifique o nome e a cultura."
            else -> error.message ?: "Não foi possível criar a estufa."
        }

    private companion object {
        const val HTTP_UNPROCESSABLE_ENTITY = 422

        val CROP_VALUES = mapOf(
            "Alface" to "lettuce",
            "Tomate" to "tomato",
            "Rúcula" to "arugula"
        )
    }
}
