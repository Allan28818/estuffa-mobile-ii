package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.R
import com.example.esttufa.model.SettingsItem
import com.example.esttufa.model.UserProfile
import com.example.esttufa.repository.UserRepository
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<ProfileUiState>()
    val uiState: LiveData<ProfileUiState> = _uiState

    val settingsItems = listOf(
        SettingsItem("notifications", R.drawable.ic_notifications, "Notificações"),
        SettingsItem("language", R.drawable.ic_language, "Idioma"),
        SettingsItem("support", R.drawable.ic_support, "Suporte"),
        SettingsItem("terms", R.drawable.ic_terms, "Termos de uso"),
        SettingsItem("logout", R.drawable.ic_logout, "Sair", isDestructive = true)
    )

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            repository.getProfile()
                .onSuccess { _uiState.value = ProfileUiState.Success(it) }
                .onFailure {
                    _uiState.value = ProfileUiState.Error(
                        it.message ?: "Erro ao carregar perfil. Tente novamente."
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
    }
}
