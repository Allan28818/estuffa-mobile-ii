package com.example.esttufa

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityCulturaInfoBinding
import com.example.esttufa.viewmodel.CulturaInfoUiState
import com.example.esttufa.viewmodel.CulturaInfoViewModel

class CulturaInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCulturaInfoBinding
    private val viewModel: CulturaInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCulturaInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val culturaEn = intent.getStringExtra("cultura") ?: ""

        val culturaPt = when (culturaEn.lowercase()) {
            "lettuce" -> "Alface"
            "arugula" -> "Rúcula"
            "tomato"  -> "Tomate"
            else      -> culturaEn
        }

        val artigo = when (culturaPt) {
            "Alface", "Rúcula" -> "da"
            "Tomate" -> "do"
            else -> "de"
        }

        binding.tvCulturaTitle.text = "Cultura $artigo ${culturaPt.lowercase()}"
        binding.ivBack.setOnClickListener { finish() }

        observeViewModel()
        viewModel.load(culturaEn, "V1")
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CulturaInfoUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.llCards.visibility     = View.GONE
                    binding.tvError.visibility     = View.GONE
                }
                is CulturaInfoUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.llCards.visibility     = View.VISIBLE
                    binding.tvError.visibility     = View.GONE

                    val data = state.data
                    binding.tvTemperatura.text  = "${"%.1f".format(data.temperature)}°C"
                    binding.tvUmidade.text      = "${"%.1f".format(data.moisture)}%"
                    binding.tvLuminosidade.text = "${"%.0f".format(data.light)}"
                    binding.tvIrrigacao.text    = "${"%.2f".format(data.irrigation_time)} seg"
                    binding.tvClassName.text    = data.class_name
                }
                is CulturaInfoUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.llCards.visibility     = View.GONE
                    binding.tvError.visibility     = View.VISIBLE
                    binding.tvError.text           = state.message
                }
            }
        }
    }
}