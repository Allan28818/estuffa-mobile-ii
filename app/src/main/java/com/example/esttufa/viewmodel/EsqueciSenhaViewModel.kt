package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

sealed class EsqueciSenhaUiState {
    object Idle : EsqueciSenhaUiState()
    object Loading : EsqueciSenhaUiState()
    object Success : EsqueciSenhaUiState()
    data class Error(val message: String) : EsqueciSenhaUiState()
}

class EsqueciSenhaViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState =
        MutableLiveData<EsqueciSenhaUiState>(EsqueciSenhaUiState.Idle)
    val uiState: LiveData<EsqueciSenhaUiState> = _uiState

    fun sendResetEmail(email: String) {
        if (_uiState.value == EsqueciSenhaUiState.Loading) return

        _uiState.value = EsqueciSenhaUiState.Loading
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _uiState.value = EsqueciSenhaUiState.Success
            }
            .addOnFailureListener { exception ->
                _uiState.value = EsqueciSenhaUiState.Error(
                    readableMessage(exception)
                )
            }
    }

    private fun readableMessage(exception: Exception): String =
        when (exception) {
            is FirebaseAuthInvalidUserException ->
                "Não encontramos uma conta com este e-mail."
            is FirebaseTooManyRequestsException ->
                "Muitas tentativas. Aguarde um momento e tente novamente."
            is FirebaseNetworkException ->
                "Sem conexão com a internet. Verifique sua rede."
            else ->
                "Não foi possível enviar o e-mail de recuperação. Tente novamente."
        }
}
