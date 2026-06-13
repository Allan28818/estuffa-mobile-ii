package com.example.esttufa

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.adapter.CulturaAdapter
import com.example.esttufa.databinding.ActivityHomeBinding
import com.example.esttufa.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private var hasCompletedFirstResume = false

    private val createStoveLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadStoves()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
        viewModel.loadStoves()
    }

    private fun setupUI() {
        val displayName = FirebaseAuth.getInstance()
            .currentUser
            ?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: "usuário"

        binding.tvBoasVindas.text = Html.fromHtml(
            "Olá, <b>${Html.escapeHtml(displayName)}</b> 👋",
            Html.FROM_HTML_MODE_COMPACT
        )

        binding.fabAddEsttufa.setOnClickListener {
            createStoveLauncher.launch(
                Intent(this, CadastroEstufaActivity::class.java)
            )
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.stoves.observe(this) { stoves ->
            binding.lvCulturas.adapter = CulturaAdapter(this, stoves)
            binding.lvCulturas.setOnItemClickListener { _, _, position, _ ->
                val stove = stoves[position]
                val intent = Intent(this, CulturaInfoActivity::class.java).apply {
                    putExtra("cultura", stove.crop)
                    putExtra("crop", stove.crop)
                    putExtra("stove_id", stove.id)
                    putExtra("stove_name", stove.name)
                }
                startActivity(intent)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.llEmptyState.visibility = if (isEmpty) {
                View.VISIBLE
            } else {
                View.GONE
            }
            binding.lvCulturas.visibility = if (isEmpty) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCompletedFirstResume) {
            viewModel.loadStoves()
        } else {
            hasCompletedFirstResume = true
        }
    }
}
