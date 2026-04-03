package com.example.esttufa

import android.os.Bundle
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.esttufa.adapter.CulturaAdapter

data class Cultura(
    val nome: String,
    val imagem: Int
)

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val culturas = listOf(
            Cultura("Tomate", R.drawable.img_tomate),
            Cultura("Alface", R.drawable.img_alface),
            Cultura("Rúcula", R.drawable.img_rucula)
        )

        val listView = findViewById<ListView>(R.id.lvCulturas)

        val adapter = CulturaAdapter(this, culturas)

        listView.adapter = adapter
    }
}