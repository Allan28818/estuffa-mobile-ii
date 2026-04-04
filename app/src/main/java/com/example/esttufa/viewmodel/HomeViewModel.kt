package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.model.Cultura
import com.example.esttufa.repository.CulturaRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = CulturaRepository()

    private val _culturas = MutableLiveData<List<Cultura>>()
    val culturas: LiveData<List<Cultura>> = _culturas

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadCulturas() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getCulturas()
            val lista = result.getOrDefault(emptyList())

            _culturas.value = lista
            _isEmpty.value = lista.isEmpty()

            _isLoading.value = false
        }
    }
}