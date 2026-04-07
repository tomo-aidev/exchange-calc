package com.exchangecalc.app.model

import java.math.BigDecimal

data class ConversionResult(
    val inputAmount: BigDecimal,
    val convertedAmount: BigDecimal,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val exchangeRate: Double,
    val lastUpdated: Long?
)
