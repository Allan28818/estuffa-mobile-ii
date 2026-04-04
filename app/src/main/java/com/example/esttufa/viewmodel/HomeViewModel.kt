package com.example.esttufa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esttufa.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.esttufa.model.Cultura

class HomeViewModel : ViewModel() {

    private val _culturas = MutableLiveData<List<Cultura>>()
    val culturas: LiveData<List<Cultura>> = _culturas

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadCulturas() {
        viewModelScope.launch {
            _isLoading.value = true

            // simula chamada de rede — futuramente vira Retrofit
            delay(1000)

            val lista = listOf(
                Cultura("Tomate", R.drawable.img_tomate),
                Cultura("Alface", R.drawable.img_alface),
                Cultura("Rúcula", R.drawable.img_rucula)
            )

            _culturas.value = lista
            _isEmpty.value = lista.isEmpty()
            _isLoading.value = false
        }
    }
}