package com.exchangecalc.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exchangecalc.app.data.AppDatabase
import com.exchangecalc.app.data.ConversionHistory
import com.exchangecalc.app.model.ConversionResult
import com.exchangecalc.app.model.Currency
import com.exchangecalc.app.service.ExchangeRateService
import com.exchangecalc.app.util.AppSettings
import com.exchangecalc.app.util.PurchaseManager
import com.exchangecalc.app.util.UsageLimiter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val historyDao = db.historyDao()
    private val usageLimiter = UsageLimiter.getInstance(application)
    private val exchangeRateService = ExchangeRateService.getInstance(application)
    private val settings = AppSettings.getInstance()

    var inputText by mutableStateOf("0")
        private set

    var hasDecimalPoint by mutableStateOf(false)
        private set

    var fromCurrency by mutableStateOf(settings.defaultFromCurrency)
        private set

    var toCurrency by mutableStateOf(settings.defaultToCurrency)
        private set

    var showPaywall by mutableStateOf(false)
        private set

    var showSavedFeedback by mutableStateOf(false)
        private set

    var showFromCurrencyPicker by mutableStateOf(false)
        private set

    var showToCurrencyPicker by mutableStateOf(false)
        private set

    val inputAmount: BigDecimal
        get() = inputText.toBigDecimalOrNull() ?: BigDecimal.ZERO

    val conversionResult: ConversionResult?
        get() {
            val amount = inputAmount
            if (amount <= BigDecimal.ZERO) return null
            return exchangeRateService.convert(amount, fromCurrency, toCurrency)
        }

    val rateDisplay: String?
        get() {
            val fromRate = exchangeRateService.currentRates[fromCurrency.code] ?: return null
            val toRate = exchangeRateService.currentRates[toCurrency.code] ?: return null
            if (fromRate == 0.0) return null
            val rate = toRate / fromRate
            return "1 ${fromCurrency.code} = ${"%.4f".format(rate)} ${toCurrency.code}"
        }

    val lastUpdated: Long? get() = exchangeRateService.lastUpdated
    val isLoading: Boolean get() = exchangeRateService.isLoading
    val hasRates: Boolean get() = exchangeRateService.hasRates
    val lastError: String? get() = exchangeRateService.lastError
    val remainingCount: Int get() = usageLimiter.remainingCount
    val isProUnlocked: Boolean get() = PurchaseManager.getInstance().isProUnlocked

    val displayText: String
        get() = if (hasDecimalPoint && !inputText.contains(".")) inputText + "." else inputText

    val histories = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            exchangeRateService.fetchRatesIfNeeded()
        }
    }

    // Keypad Actions

    fun appendDigit(digit: String) {
        if (!usageLimiter.canUse()) {
            showPaywall = true
            return
        }
        if (inputText.length >= 12) return

        if (inputText.contains(".")) {
            val dotIndex = inputText.indexOf(".")
            val decimalCount = inputText.length - dotIndex - 1
            if (decimalCount >= fromCurrency.decimalPlaces) return
        }

        inputText = if (inputText == "0" && digit != "0" && !hasDecimalPoint) {
            digit
        } else if (inputText == "0" && digit == "0" && !hasDecimalPoint) {
            return
        } else {
            inputText + digit
        }
    }

    fun appendDoubleZero() {
        if (!usageLimiter.canUse()) {
            showPaywall = true
            return
        }
        if (inputText == "0" && !hasDecimalPoint) return
        if (inputText.length >= 11) return

        if (inputText.contains(".")) {
            val dotIndex = inputText.indexOf(".")
            val decimalCount = inputText.length - dotIndex - 1
            val canAdd = fromCurrency.decimalPlaces - decimalCount
            when {
                canAdd >= 2 -> inputText += "00"
                canAdd == 1 -> inputText += "0"
                else -> return
            }
        } else {
            inputText += "00"
        }
    }

    fun appendDecimalPoint() {
        if (!usageLimiter.canUse()) {
            showPaywall = true
            return
        }
        if (fromCurrency.decimalPlaces == 0) return
        if (hasDecimalPoint) return
        hasDecimalPoint = true
        inputText += "."
    }

    fun deleteLastCharacter() {
        if (inputText.length <= 1) {
            clear()
            return
        }
        if (inputText.last() == '.') {
            hasDecimalPoint = false
        }
        inputText = inputText.dropLast(1)
    }

    fun clear() {
        if (inputText != "0") {
            usageLimiter.recordUsage()
        }
        inputText = "0"
        hasDecimalPoint = false
    }

    // Currency Actions

    fun swapCurrencies() {
        val temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
        inputText = "0"
        hasDecimalPoint = false
    }

    fun selectFromCurrency(currency: Currency) {
        if (currency == toCurrency) return
        fromCurrency = currency
        inputText = "0"
        hasDecimalPoint = false
        showFromCurrencyPicker = false
    }

    fun selectToCurrency(currency: Currency) {
        if (currency == fromCurrency) return
        toCurrency = currency
        showToCurrencyPicker = false
    }

    fun openFromCurrencyPicker() { showFromCurrencyPicker = true }
    fun openToCurrencyPicker() { showToCurrencyPicker = true }
    fun dismissFromCurrencyPicker() { showFromCurrencyPicker = false }
    fun dismissToCurrencyPicker() { showToCurrencyPicker = false }

    // History

    fun saveToHistory() {
        val result = conversionResult ?: return
        if (result.inputAmount <= BigDecimal.ZERO) return

        viewModelScope.launch {
            historyDao.insert(
                ConversionHistory(
                    inputAmount = result.inputAmount.toPlainString(),
                    convertedAmount = result.convertedAmount.toPlainString(),
                    fromCurrencyCode = result.fromCurrency.code,
                    toCurrencyCode = result.toCurrency.code,
                    exchangeRate = result.exchangeRate
                )
            )
            showSavedFeedback = true
        }
    }

    fun restoreFromHistory(history: ConversionHistory) {
        val from = Currency.fromCode(history.fromCurrencyCode) ?: return
        val to = Currency.fromCode(history.toCurrencyCode) ?: return
        fromCurrency = from
        toCurrency = to
        inputText = "0"
        hasDecimalPoint = false
        for (char in history.inputAmount) {
            if (char == '.') {
                appendDecimalPoint()
            } else if (char.isDigit()) {
                val saved = inputText
                inputText = if (inputText == "0") char.toString() else inputText + char
            }
        }
    }

    fun deleteHistory(history: ConversionHistory) {
        viewModelScope.launch { historyDao.delete(history) }
    }

    fun deleteAllHistory() {
        viewModelScope.launch { historyDao.deleteAll() }
    }

    // Rate Refresh

    fun refreshRates() {
        viewModelScope.launch { exchangeRateService.fetchRatesIfNeeded() }
    }

    fun forceRefreshRates() {
        viewModelScope.launch { exchangeRateService.forceRefresh() }
    }

    // Paywall

    fun dismissPaywall() { showPaywall = false }
    fun onPurchased() { showPaywall = false }
    fun dismissSavedFeedback() { showSavedFeedback = false }
}
