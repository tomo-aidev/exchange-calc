package com.exchangecalc.app.model

data class CachedRates(
    val rates: Map<String, Double>,
    val lastUpdated: Long,
    val fetchedAt: Long
)
