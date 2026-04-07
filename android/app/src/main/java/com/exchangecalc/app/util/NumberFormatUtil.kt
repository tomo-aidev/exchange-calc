package com.exchangecalc.app.util

import com.exchangecalc.app.model.Currency
import java.math.BigDecimal
import java.text.DecimalFormat

object NumberFormatUtil {
    fun formatCurrency(value: BigDecimal, currency: Currency): String {
        val pattern = if (currency.decimalPlaces == 0) "#,##0" else "#,##0.${"0".repeat(currency.decimalPlaces)}"
        val formatter = DecimalFormat(pattern)
        return "${currency.symbol}${formatter.format(value)}"
    }

    fun formatRate(value: Double): String {
        val formatter = DecimalFormat("#,##0.0000")
        return formatter.format(value)
    }
}
