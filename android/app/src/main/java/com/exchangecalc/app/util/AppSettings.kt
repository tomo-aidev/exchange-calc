package com.exchangecalc.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.exchangecalc.app.model.Currency

class AppSettings private constructor() {
    private var prefs: SharedPreferences? = null

    var defaultFromCurrency by mutableStateOf(Currency.USD)
        private set

    var defaultToCurrency by mutableStateOf(Currency.JPY)
        private set

    var darkModeEnabled by mutableStateOf(false)
        private set

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_FROM_CURRENCY = "default_from_currency"
        private const val KEY_TO_CURRENCY = "default_to_currency"
        private const val KEY_DARK_MODE = "dark_mode_enabled"

        @Volatile
        private var INSTANCE: AppSettings? = null

        fun getInstance(): AppSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettings().also { INSTANCE = it }
            }
        }

        fun init(context: Context) {
            val instance = getInstance()
            instance.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            instance.loadSettings()
        }
    }

    private fun loadSettings() {
        val p = prefs ?: return
        val fromCode = p.getString(KEY_FROM_CURRENCY, "USD") ?: "USD"
        defaultFromCurrency = Currency.fromCode(fromCode) ?: Currency.USD

        val toCode = p.getString(KEY_TO_CURRENCY, "JPY") ?: "JPY"
        defaultToCurrency = Currency.fromCode(toCode) ?: Currency.JPY

        darkModeEnabled = p.getBoolean(KEY_DARK_MODE, false)
    }

    fun updateFromCurrency(currency: Currency) {
        defaultFromCurrency = currency
        prefs?.edit()?.putString(KEY_FROM_CURRENCY, currency.code)?.apply()
    }

    fun updateToCurrency(currency: Currency) {
        defaultToCurrency = currency
        prefs?.edit()?.putString(KEY_TO_CURRENCY, currency.code)?.apply()
    }

    fun updateDarkMode(enabled: Boolean) {
        darkModeEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, enabled)?.apply()
    }
}
