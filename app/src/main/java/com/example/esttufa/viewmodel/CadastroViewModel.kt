package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

sealed class CadastroUiState {
    object Idle : CadastroUiState()
    object Loading : CadastroUiState()
    object Success : CadastroUiState()
    data class Error(val message: String) : CadastroUiState()
}

class CadastroViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableLiveData<CadastroUiState>(CadastroUiState.Idle)
    val uiState: LiveData<CadastroUiState> = _uiState

    fun register(
        nome: String,
        sobrenome: String,
        email: String,
        senha: String
    ) {
        if (_uiState.value == CadastroUiState.Loading) {
            return
        }

        _uiState.value = CadastroUiState.Loading

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    _uiState.value = CadastroUiState.Error(
                        "Não foi possível concluir o cadastro."
                    )
                    return@addOnSuccessListener
                }

                val profileUpdates = userProfileChangeRequest {
                    displayName = "$nome $sobrenome".trim()
                }

                user.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        _uiState.value = CadastroUiState.Success
                    }
                    .addOnFailureListener { error ->
                        _uiState.value = CadastroUiState.Error(
                            error.message ?: "Não foi possível atualizar o perfil."
                        )
                    }
            }
            .addOnFailureListener { error ->
                _uiState.value = CadastroUiState.Error(
                    error.message ?: "Não foi possível realizar o cadastro."
                )
            }
    }
}
