package com.exchangecalc.app.service

import android.content.Context
import android.content.SharedPreferences
import com.exchangecalc.app.model.Currency
import com.exchangecalc.app.model.ConversionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

data class PairRatePoint(val date: String, val rate: Double)

class ExchangeRateService private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("exchange_rates", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private val apiUrl = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"
    private val fallbackUrl = "https://latest.currency-api.pages.dev/v1/currencies/usd.json"
    private val refreshInterval = 24 * 60 * 60 * 1000L

    var currentRates: Map<String, Double> = emptyMap()
        private set
    var lastUpdated: Long? = null
        private set
    var rateDate: String? = null
        private set
    var isLoading: Boolean = false
        private set
    var lastError: String? = null
        private set

    val hasRates: Boolean get() = currentRates.isNotEmpty()

    init {
        loadCachedRates()
    }

    suspend fun fetchRatesIfNeeded() {
        val last = lastUpdated
        if (last != null && System.currentTimeMillis() - last < refreshInterval && hasRates) {
            return
        }
        forceRefresh()
    }

    suspend fun forceRefresh() {
        if (isLoading) return
        isLoading = true
        lastError = null

        val urls = listOf(apiUrl, fallbackUrl)

        for (url in urls) {
            try {
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute()
                }

                if (!response.isSuccessful) continue

                val body = response.body?.string() ?: continue

                val jsonObj = JSONObject(body)
                val date = jsonObj.optString("date", "")
                val usdRates = jsonObj.optJSONObject("usd") ?: continue

                val uppercasedRates = mutableMapOf<String, Double>()
                val keys = usdRates.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    uppercasedRates[key.uppercase()] = usdRates.getDouble(key)
                }
                uppercasedRates["USD"] = 1.0

                currentRates = uppercasedRates
                rateDate = date

                // Parse date to timestamp
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                lastUpdated = try {
                    sdf.parse(date)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }

                saveCachedRates()
                lastError = null
                isLoading = false
                return

            } catch (e: Exception) {
                lastError = e.message
            }
        }

        isLoading = false
    }

    fun convert(amount: BigDecimal, from: Currency, to: Currency): ConversionResult? {
        val fromRate = currentRates[from.code] ?: return null
        val toRate = currentRates[to.code] ?: return null
        if (fromRate == 0.0) return null

        val exchangeRate = toRate / fromRate
        val convertedAmount = amount.multiply(BigDecimal.valueOf(exchangeRate))
            .setScale(to.decimalPlaces, RoundingMode.HALF_UP)

        return ConversionResult(
            inputAmount = amount,
            convertedAmount = convertedAmount,
            fromCurrency = from,
            toCurrency = to,
            exchangeRate = exchangeRate,
            lastUpdated = lastUpdated
        )
    }

    suspend fun fetchPairHistory(from: Currency, to: Currency, days: Int = 90): List<PairRatePoint> {
        val cacheKey = "pairHistory:${from.code}:${to.code}"
        val cacheDateKey = "$cacheKey:date"

        // Check cache
        val cachedDate = prefs.getString(cacheDateKey, null)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        if (cachedDate == today) {
            val cachedJson = prefs.getString(cacheKey, null)
            if (cachedJson != null) {
                try {
                    val arr = JSONArray(cachedJson)
                    val points = mutableListOf<PairRatePoint>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        points.add(PairRatePoint(obj.getString("date"), obj.getDouble("rate")))
                    }
                    return points.sortedBy { it.date }
                } catch (_: Exception) { /* fall through to fetch */ }
            }
        }

        // Fetch from API
        val url = "https://quickrate-api.getonnews.workers.dev/history?days=$days&from=${from.code}&to=${to.code}"
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()

                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonObj = JSONObject(body)
                val dataArr = jsonObj.getJSONArray("data")

                val points = mutableListOf<PairRatePoint>()
                for (i in 0 until dataArr.length()) {
                    val item = dataArr.getJSONObject(i)
                    points.add(PairRatePoint(item.getString("date"), item.getDouble("rate")))
                }

                // Cache the result
                prefs.edit()
                    .putString(cacheKey, dataArr.toString())
                    .putString(cacheDateKey, today)
                    .apply()

                points.sortedBy { it.date }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun loadCachedRates() {
        val ratesJson = prefs.getString("rates_json", null) ?: return
        val updated = prefs.getLong("last_updated", 0)
        if (updated == 0L) return

        try {
            val ratesMap = json.decodeFromString<Map<String, Double>>(ratesJson)
            currentRates = ratesMap
            lastUpdated = updated
        } catch (_: Exception) {
            // Cache corrupted
        }
    }

    private fun saveCachedRates() {
        val ratesJson = json.encodeToString(
            kotlinx.serialization.serializer<Map<String, Double>>(),
            currentRates
        )
        prefs.edit()
            .putString("rates_json", ratesJson)
            .putLong("last_updated", lastUpdated ?: 0)
            .putLong("fetched_at", System.currentTimeMillis())
            .apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: ExchangeRateService? = null

        fun getInstance(context: Context): ExchangeRateService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExchangeRateService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
