package com.exchangecalc.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inputAmount: String,
    val convertedAmount: String,
    val fromCurrencyCode: String,
    val toCurrencyCode: String,
    val exchangeRate: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = ""
)
