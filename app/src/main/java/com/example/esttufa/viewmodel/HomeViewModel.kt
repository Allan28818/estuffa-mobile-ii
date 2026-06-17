package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.model.StoveResponse
import com.example.esttufa.repository.StoveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class HomeViewModel : ViewModel() {

    private val repository = StoveRepository()

    private val _stoves = MutableLiveData<List<StoveResponse>>()
    val stoves: LiveData<List<StoveResponse>> = _stoves

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun loadStoves() {
        if (_isLoading.value == true) return

        viewModelScope.launch {
            _isLoading.value = true

            val result = withContext(Dispatchers.IO) {
                repository.getStoves()
            }

            result.fold(
                onSuccess = ::updateStoves,
                onFailure = {
                    if (_stoves.value == null) {
                        updateStoves(emptyList())
                    }
                    _message.value = "Nao foi possivel carregar suas estufas."
                }
            )

            _isLoading.value = false
        }
    }

    fun deleteStove(stove: StoveResponse) {
        if (_isLoading.value == true) return

        viewModelScope.launch {
            _isLoading.value = true

            val result = withContext(Dispatchers.IO) {
                repository.deleteStove(stove.id)
            }

            result.fold(
                onSuccess = {
                    val updatedStoves = _stoves.value.orEmpty()
                        .filterNot { it.id == stove.id }
                    updateStoves(updatedStoves)
                    _message.value = "Estufa removida."
                },
                onFailure = {
                    _message.value = deleteErrorMessage(it)
                }
            )

            _isLoading.value = false
        }
    }

    fun consumeMessage() {
        _message.value = ""
    }

    private fun updateStoves(stoves: List<StoveResponse>) {
        _stoves.value = stoves
        _isEmpty.value = stoves.isEmpty()
    }

    private fun deleteErrorMessage(error: Throwable): String =
        when {
            error is HttpException && error.code() == HTTP_NOT_FOUND ->
                "Essa estufa nao foi encontrada."
            error is HttpException && error.code() in HTTP_AUTHORIZATION_ERRORS ->
                "Sua sessao expirou. Faca login novamente."
            else -> "Nao foi possivel remover a estufa."
        }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        val HTTP_AUTHORIZATION_ERRORS = setOf(401, 403)
    }
}
