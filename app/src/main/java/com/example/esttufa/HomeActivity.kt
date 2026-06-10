package com.example.esttufa

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.adapter.CulturaAdapter
import com.example.esttufa.databinding.ActivityHomeBinding
import com.example.esttufa.viewmodel.HomeViewModel

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()

        // Chamada inicial para carregar dados (via ViewModel)
        viewModel.loadCulturas()
    }

    private fun setupUI() {
        // Formata o texto de boas-vindas para suportar HTML (negrito no nome)
        binding.tvBoasVindas.text = Html.fromHtml(
            getString(R.string.boas_vindas),
            Html.FROM_HTML_MODE_COMPACT
        )

        // Configura o clique do botão flutuante para abrir a tela de cadastro de estufa
        binding.fabAddEsttufa.setOnClickListener {
            val intent = Intent(this, CadastroEstufaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        // Observa o estado de carregamento
        viewModel.isLoading.observe(this) { carregando ->
            binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
        }

        // Observa a lista de culturas
        viewModel.culturas.observe(this) { lista ->
            val adapter = CulturaAdapter(this, lista)
            binding.lvCulturas.adapter = adapter

            binding.lvCulturas.setOnItemClickListener { _, _, position, _ ->
                val cultura = lista[position]
                val intent = Intent(this, CulturaInfoActivity::class.java)
                intent.putExtra("cultura", cultura.id)
                startActivity(intent)
            }
        }

        // Observa se a lista está vazia para alternar a visibilidade
        viewModel.isEmpty.observe(this) { vazio ->
            if (vazio) {
                binding.llEmptyState.visibility = View.VISIBLE
                binding.lvCulturas.visibility = View.GONE
            } else {
                binding.llEmptyState.visibility = View.GONE
                binding.lvCulturas.visibility = View.VISIBLE
            }
        }
    }
}