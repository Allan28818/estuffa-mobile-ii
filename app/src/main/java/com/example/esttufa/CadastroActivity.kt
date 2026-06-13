package com.example.esttufa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityCadastroBinding
import com.example.esttufa.viewmodel.CadastroUiState
import com.example.esttufa.viewmodel.CadastroViewModel

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private val viewModel: CadastroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnCadastrar.setOnClickListener {
            registerUser()
        }

        binding.btnLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        clearFieldErrors()

        val nome = binding.etNome.text?.toString()?.trim().orEmpty()
        val sobrenome = binding.etSobrenome.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val senha = binding.etSenha.text?.toString().orEmpty()
        val confirmarSenha = binding.etConfirmarSenha.text?.toString().orEmpty()

        if (!validateFields(nome, sobrenome, email, senha, confirmarSenha)) {
            return
        }

        viewModel.register(nome, sobrenome, email, senha)
    }

    private fun validateFields(
        nome: String,
        sobrenome: String,
        email: String,
        senha: String,
        confirmarSenha: String
    ): Boolean {
        var isValid = true

        if (nome.isBlank()) {
            binding.itlNome.error = "Informe o nome"
            isValid = false
        }
        if (sobrenome.isBlank()) {
            binding.tilSobrenome.error = "Informe o sobrenome"
            isValid = false
        }
        if (email.isBlank()) {
            binding.tilEmail.error = "Informe o e-mail"
            isValid = false
        }
        if (senha.isBlank()) {
            binding.tilSenha.error = "Informe a senha"
            isValid = false
        }
        if (confirmarSenha.isBlank()) {
            binding.tilConfirmarSenha.error = "Confirme a senha"
            isValid = false
        } else if (senha != confirmarSenha) {
            binding.tilConfirmarSenha.error = "As senhas não coincidem"
            isValid = false
        }

        return isValid
    }

    private fun clearFieldErrors() {
        binding.itlNome.error = null
        binding.tilSobrenome.error = null
        binding.tilEmail.error = null
        binding.tilSenha.error = null
        binding.tilConfirmarSenha.error = null
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                CadastroUiState.Idle -> setLoading(false)
                CadastroUiState.Loading -> setLoading(true)
                CadastroUiState.Success -> {
                    setLoading(false)
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                is CadastroUiState.Error -> {
                    setLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnCadastrar.isEnabled = !isLoading
    }
}
