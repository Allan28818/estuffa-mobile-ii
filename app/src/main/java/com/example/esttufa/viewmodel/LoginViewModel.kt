package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    fun hasAuthenticatedUser(): Boolean = auth.currentUser != null

    fun login(email: String, password: String) {
        if (_uiState.value == LoginUiState.Loading) return

        _uiState.value = LoginUiState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _uiState.value = LoginUiState.Success
            }
            .addOnFailureListener { exception ->
                _uiState.value = LoginUiState.Error(readableMessage(exception))
            }
    }

    private fun readableMessage(exception: Exception): String =
        when (exception) {
            is FirebaseAuthInvalidCredentialsException ->
                "E-mail ou senha inválidos."
            is FirebaseAuthInvalidUserException ->
                if (exception.errorCode == "ERROR_USER_DISABLED") {
                    "Esta conta foi desativada."
                } else {
                    "Não encontramos uma conta com este e-mail."
                }
            is FirebaseTooManyRequestsException ->
                "Muitas tentativas. Aguarde um momento e tente novamente."
            is FirebaseNetworkException ->
                "Sem conexão com a internet. Verifique sua rede."
            else ->
                "Não foi possível entrar. Tente novamente."
        }
}
