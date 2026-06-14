package com.example.esttufa.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.esttufa.R
import com.example.esttufa.model.SettingsItem

class SettingsAdapter(
    context: Context,
    items: List<SettingsItem>
) : ArrayAdapter<SettingsItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_setting, parent, false)
        val holder = view.tag as? ViewHolder ?: ViewHolder(view).also {
            view.tag = it
        }
        val item = getItem(position) ?: return view

        holder.icon.setImageResource(item.iconResId)
        holder.icon.contentDescription = item.label
        holder.label.text = item.label
        holder.label.setTextColor(
            context.getColor(
                if (item.isDestructive) R.color.error_red else R.color.text_primary
            )
        )
        holder.chevron.contentDescription = context.getString(
            R.string.settings_chevron_description,
            item.label
        )

        return view
    }

    private class ViewHolder(view: View) {
        val icon: ImageView = view.findViewById(R.id.ivSettingIcon)
        val label: TextView = view.findViewById(R.id.tvSettingLabel)
        val chevron: ImageView = view.findViewById(R.id.ivSettingChevron)
    }
}
