package com.example.esttufa

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.esttufa.databinding.ActivityCadastroEstufaBinding

class CadastroEstufaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroEstufaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroEstufaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // Configura o botão de voltar
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Configura o Dropdown de Culturas (Mock de opções)
        val culturas = arrayOf("Alface", "Tomate", "Rúcula")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, culturas)
        binding.actvCultura.setAdapter(adapter)

        // Configura o botão de criar
        binding.btnCriarEstufa.setOnClickListener {
            val nome = binding.etNomeEstufa.text.toString()
            val cultura = binding.actvCultura.text.toString()

            if (nome.isNotEmpty() && cultura.isNotEmpty()) {
                /*
                   LOGICA DE CADASTRO (FIRESTORE):
                   1. Obter o UID do usuário logado (Firebase Auth).
                   2. Criar um objeto 'Estufa' ou HashMap com:
                      - nome_estufa: String
                      - cultura_tipo: String (id ou nome)
                      - data_criacao: Timestamp
                      - status: "Ativo"
                   3. Salvar na coleção: db.collection("usuarios").document(uid).collection("estufas").add(objeto)
                   4. No sucesso, fechar a tela e atualizar a Home.
                */
                Toast.makeText(this, "Estufa '$nome' criada com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}