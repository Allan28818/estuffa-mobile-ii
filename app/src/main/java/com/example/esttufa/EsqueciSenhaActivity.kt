package com.example.esttufa

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityEsqueciSenhaBinding
import com.example.esttufa.viewmodel.EsqueciSenhaUiState
import com.example.esttufa.viewmodel.EsqueciSenhaViewModel

class EsqueciSenhaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEsqueciSenhaBinding
    private val viewModel: EsqueciSenhaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEsqueciSenhaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActions()
        observeResetState()
    }

    private fun setupActions() {
        binding.btnEnviar.setOnClickListener {
            submitResetRequest()
        }
    }

    private fun submitResetRequest() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        binding.tilEmail.error = null

        when {
            email.isBlank() -> binding.tilEmail.error = getString(
                R.string.forgot_password_email_required
            )
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                binding.tilEmail.error = getString(
                    R.string.forgot_password_email_invalid
                )
            else -> viewModel.sendResetEmail(email)
        }
    }

    private fun observeResetState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                EsqueciSenhaUiState.Idle -> setLoading(false)
                EsqueciSenhaUiState.Loading -> setLoading(true)
                EsqueciSenhaUiState.Success -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        R.string.forgot_password_success,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                is EsqueciSenhaUiState.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnEnviar.isEnabled = !isLoading
    }
}
