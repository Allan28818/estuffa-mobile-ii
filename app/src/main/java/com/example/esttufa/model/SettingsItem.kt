package com.example.esttufa.model

data class SettingsItem(
    val id: String,
    val iconResId: Int,
    val label: String,
    val isDestructive: Boolean = false
)
