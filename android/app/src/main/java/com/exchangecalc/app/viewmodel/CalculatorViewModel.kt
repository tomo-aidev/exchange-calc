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

    var manualRateEnabled by mutableStateOf(false)
        private set

    var manualRateText by mutableStateOf("")
        private set

    val inputAmount: BigDecimal
        get() = inputText.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private val effectiveRate: Double?
        get() {
            if (manualRateEnabled) {
                val manual = manualRateText.toDoubleOrNull()
                if (manual != null && manual > 0) return manual
            }
            val fromRate = exchangeRateService.currentRates[fromCurrency.code] ?: return null
            val toRate = exchangeRateService.currentRates[toCurrency.code] ?: return null
            if (fromRate == 0.0) return null
            return toRate / fromRate
        }

    val conversionResult: ConversionResult?
        get() {
            val amount = inputAmount
            if (amount <= BigDecimal.ZERO) return null
            val rate = effectiveRate ?: return null
            val convertedAmount = amount.multiply(BigDecimal.valueOf(rate))
                .setScale(toCurrency.decimalPlaces, RoundingMode.HALF_UP)
            return ConversionResult(
                inputAmount = amount,
                convertedAmount = convertedAmount,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                exchangeRate = rate,
                lastUpdated = lastUpdated
            )
        }

    var rateDisplay by mutableStateOf<String?>(null)
        private set

    var lastUpdatedDisplay by mutableStateOf<String?>(null)
        private set

    var lastUpdated by mutableStateOf<Long?>(exchangeRateService.lastUpdated)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var hasRates by mutableStateOf(exchangeRateService.hasRates)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    val remainingCount: Int get() = usageLimiter.remainingCount
    val isProUnlocked: Boolean get() = PurchaseManager.getInstance().isProUnlocked

    val displayText: String
        get() = if (hasDecimalPoint && !inputText.contains(".")) inputText + "." else inputText

    val histories = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun updateRateState() {
        hasRates = exchangeRateService.hasRates
        isLoading = exchangeRateService.isLoading
        lastUpdated = exchangeRateService.lastUpdated
        lastError = exchangeRateService.lastError
        updateRateDisplay()
    }

    private fun updateRateDisplay() {
        val rate = effectiveRate
        rateDisplay = if (rate != null) {
            "1 ${fromCurrency.code} = ${"%.4f".format(rate)} ${toCurrency.code}"
        } else null

        lastUpdatedDisplay = if (manualRateEnabled && manualRateText.toDoubleOrNull() != null) {
            null // Manual rate - no timestamp
        } else {
            val ts = exchangeRateService.lastUpdated
            if (ts != null) {
                val sdf = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(ts))
            } else null
        }
    }

    fun toggleManualRate(enabled: Boolean) {
        manualRateEnabled = enabled
        if (enabled) {
            val rate = effectiveRate
            if (rate != null) {
                manualRateText = "%.4f".format(rate)
            }
        }
        updateRateDisplay()
    }

    fun setManualRate(rate: Double) {
        manualRateText = "%.4f".format(rate)
        updateRateDisplay()
    }

    init {
        viewModelScope.launch {
            isLoading = true
            exchangeRateService.fetchRatesIfNeeded()
            updateRateState()
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
        updateRateDisplay()
    }

    fun selectFromCurrency(currency: Currency) {
        if (currency == toCurrency) return
        fromCurrency = currency
        inputText = "0"
        hasDecimalPoint = false
        showFromCurrencyPicker = false
        updateRateDisplay()
    }

    fun selectToCurrency(currency: Currency) {
        if (currency == fromCurrency) return
        toCurrency = currency
        showToCurrencyPicker = false
        updateRateDisplay()
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
        viewModelScope.launch {
            isLoading = true
            exchangeRateService.fetchRatesIfNeeded()
            updateRateState()
        }
    }

    fun forceRefreshRates() {
        viewModelScope.launch {
            isLoading = true
            exchangeRateService.forceRefresh()
            updateRateState()
        }
    }

    // Paywall

    fun dismissPaywall() { showPaywall = false }
    fun onPurchased() { showPaywall = false }
    fun dismissSavedFeedback() { showSavedFeedback = false }
}
