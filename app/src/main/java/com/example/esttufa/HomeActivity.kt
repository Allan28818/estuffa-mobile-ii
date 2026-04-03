package com.example.esttufa

import com.example.esttufa.adapter.CulturaAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityHomeBinding
import com.example.esttufa.viewmodel.HomeViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        viewModel.loadCulturas()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { carregando ->
            binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
        }

        viewModel.culturas.observe(this) { lista ->
            val adapter = CulturaAdapter(this, lista)
            binding.lvCulturas.adapter = adapter

            binding.lvCulturas.setOnItemClickListener { _, _, position, _ ->
                val cultura = lista[position]
                val intent = Intent(this, CulturaInfoActivity::class.java)
                intent.putExtra("cultura", cultura.nome)
                startActivity(intent)
            }
        }

        viewModel.isEmpty.observe(this) { vazio ->
            binding.llEmptyState.visibility = if (vazio) View.VISIBLE else View.GONE
            binding.lvCulturas.visibility   = if (vazio) View.GONE   else View.VISIBLE
        }
    }
}