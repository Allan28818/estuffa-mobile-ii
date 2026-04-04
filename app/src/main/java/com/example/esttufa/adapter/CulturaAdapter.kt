package com.example.esttufa.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.esttufa.R
import com.example.esttufa.model.Cultura

class CulturaAdapter(
    private val context: Context,
    private val culturas: List<Cultura>
) : ArrayAdapter<Cultura>(context, 0, culturas) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_cultura, parent, false)

        val cultura = culturas[position]

        val nome = view.findViewById<TextView>(R.id.tvCulturaNome)
        val imagem = view.findViewById<ImageView>(R.id.ivCultura)

        nome.text = when (cultura.id) {
            "lettuce" -> "Alface"
            "arugula" -> "Rúcula"
            "tomato"  -> "Tomate"
            else -> cultura.name
        }

        Glide.with(context)
            .load(cultura.url)
            .into(imagem)

        return view
    }
}