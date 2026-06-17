package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.model.StoveResponse
import com.example.esttufa.repository.CulturaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    private val repository = CulturaRepository()

    private val _stoves = MutableLiveData<List<StoveResponse>>()
    val stoves: LiveData<List<StoveResponse>> = _stoves

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadStoves() {
        if (_isLoading.value == true) return

        viewModelScope.launch {
            _isLoading.value = true

            val stoveList = withContext(Dispatchers.IO) {
                repository.getStoves().getOrDefault(emptyList())
            }

            _stoves.value = stoveList
            _isEmpty.value = stoveList.isEmpty()

            _isLoading.value = false
        }
    }
}
