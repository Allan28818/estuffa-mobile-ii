package com.example.esttufa.local

import android.content.Context

class PreferencesHelper(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isDarkThemeEnabled(): Boolean =
        preferences.getBoolean(KEY_DARK_THEME_ENABLED, false)

    fun saveDarkThemeEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_DARK_THEME_ENABLED, enabled)
            .apply()
    }

    fun getTemperatureUnit(): TemperatureUnit {
        val value = preferences.getString(KEY_TEMPERATURE_UNIT, TemperatureUnit.CELSIUS.value)
        return TemperatureUnit.fromValue(value)
    }

    fun saveTemperatureUnit(unit: TemperatureUnit) {
        preferences.edit()
            .putString(KEY_TEMPERATURE_UNIT, unit.value)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "esttufa_preferences"
        private const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"
        private const val KEY_TEMPERATURE_UNIT = "temperature_unit"
    }
}

enum class TemperatureUnit(val value: String) {
    CELSIUS("C"),
    FAHRENHEIT("F");

    companion object {
        fun fromValue(value: String?): TemperatureUnit =
            entries.firstOrNull { it.value == value } ?: CELSIUS
    }
}
