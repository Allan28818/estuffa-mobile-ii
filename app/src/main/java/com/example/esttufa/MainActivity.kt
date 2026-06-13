package com.example.esttufa

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.esttufa.databinding.ActivityMainBinding
import com.example.esttufa.viewmodel.LoginUiState
import com.example.esttufa.viewmodel.LoginViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (viewModel.hasAuthenticatedUser()) {
            navigateToHome()
            return
        }

        observeLoginState()
        setupActions()
    }

    private fun setupActions() {
        binding.btnLogin.setOnClickListener {
            submitLogin()
        }

        binding.btnCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun submitLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etSenha.text?.toString().orEmpty()

        binding.tilEmail.error = null
        binding.tilSenha.error = null

        var isValid = true
        if (email.isBlank()) {
            binding.tilEmail.error = "Informe seu e-mail."
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Informe um e-mail válido."
            isValid = false
        }

        if (password.isBlank()) {
            binding.tilSenha.error = "Informe sua senha."
            isValid = false
        }

        if (isValid) {
            viewModel.login(email, password)
        }
    }

    private fun observeLoginState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                LoginUiState.Idle -> setLoading(false)
                LoginUiState.Loading -> setLoading(true)
                LoginUiState.Success -> navigateToHome()
                is LoginUiState.Error -> {
                    setLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
