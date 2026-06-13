package com.example.esttufa

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityCadastroEstufaBinding
import com.example.esttufa.viewmodel.CadastroEstufaUiState
import com.example.esttufa.viewmodel.CadastroEstufaViewModel

class CadastroEstufaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroEstufaBinding
    private val viewModel: CadastroEstufaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroEstufaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        val culturas = arrayOf("Alface", "Tomate", "Rúcula")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            culturas
        )
        binding.actvCultura.setAdapter(adapter)

        binding.btnCriarEstufa.setOnClickListener {
            val nome = binding.etNomeEstufa.text?.toString().orEmpty().trim()
            val cultura = binding.actvCultura.text?.toString().orEmpty().trim()

            if (validateFields(nome, cultura)) {
                viewModel.createStove(nome, cultura)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                CadastroEstufaUiState.Idle -> binding.btnCriarEstufa.isEnabled = true
                CadastroEstufaUiState.Loading -> binding.btnCriarEstufa.isEnabled = false
                is CadastroEstufaUiState.Success -> {
                    Toast.makeText(
                        this,
                        "Estufa criada com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
                is CadastroEstufaUiState.Error -> {
                    binding.btnCriarEstufa.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateFields(nome: String, cultura: String): Boolean {
        binding.tilNomeEstufa.error = if (nome.isEmpty()) {
            "Informe o nome da estufa."
        } else {
            null
        }
        binding.tilCultura.error = if (cultura.isEmpty()) {
            "Selecione uma cultura."
        } else {
            null
        }

        return nome.isNotEmpty() && cultura.isNotEmpty()
    }
}
