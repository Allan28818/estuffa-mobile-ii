package com.example.esttufa.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.example.esttufa.R
import com.example.esttufa.model.StoveResponse

class CulturaAdapter(
    context: Context,
    private val stoves: List<StoveResponse>,
    private val onStoveClick: (StoveResponse) -> Unit,
    private val onDeleteClick: (StoveResponse) -> Unit
) : ArrayAdapter<StoveResponse>(context, 0, stoves) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_cultura, parent, false)

        val stove = stoves[position]

        val nome = view.findViewById<TextView>(R.id.tvCulturaNome)
        val imagem = view.findViewById<ImageView>(R.id.ivCultura)
        val deleteButton = view.findViewById<ImageButton>(R.id.btnDeleteStove)

        nome.text = stove.name
        view.setOnClickListener {
            onStoveClick(stove)
        }
        deleteButton.setOnClickListener {
            onDeleteClick(stove)
        }
        imagem.setImageResource(
            when (stove.crop.lowercase()) {
                "lettuce" -> R.drawable.img_alface
                "arugula" -> R.drawable.img_rucula
                "tomato" -> R.drawable.img_tomate
                else -> R.drawable.img_alface
            }
        )

        return view
    }
}
