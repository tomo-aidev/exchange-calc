package com.exchangecalc.app.util

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageLimiter private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("usage_limiter", Context.MODE_PRIVATE)

    companion object {
        private const val DAILY_LIMIT = 10
        private const val KEY_COUNT = "dailyUsageCount"
        private const val KEY_DATE = "dailyUsageDate"

        @Volatile
        private var INSTANCE: UsageLimiter? = null

        fun getInstance(context: Context): UsageLimiter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsageLimiter(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val isLimitReached: Boolean
        get() {
            if (PurchaseManager.getInstance().isProUnlocked) return false
            loadTodayUsage()
            return todayUsageCount >= DAILY_LIMIT
        }

    val remainingCount: Int
        get() {
            if (PurchaseManager.getInstance().isProUnlocked) return 999
            loadTodayUsage()
            return maxOf(0, DAILY_LIMIT - todayUsageCount)
        }

    private var todayUsageCount: Int = 0

    init {
        loadTodayUsage()
    }

    fun recordUsage(): Boolean {
        if (PurchaseManager.getInstance().isProUnlocked) return true
        loadTodayUsage()
        if (todayUsageCount >= DAILY_LIMIT) return false
        todayUsageCount++
        saveTodayUsage()
        return true
    }

    fun canUse(): Boolean {
        if (PurchaseManager.getInstance().isProUnlocked) return true
        loadTodayUsage()
        return todayUsageCount < DAILY_LIMIT
    }

    private fun loadTodayUsage() {
        val savedDate = prefs.getString(KEY_DATE, "") ?: ""
        val today = todayString()
        if (savedDate == today) {
            todayUsageCount = prefs.getInt(KEY_COUNT, 0)
        } else {
            todayUsageCount = 0
            prefs.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_COUNT, 0)
                .apply()
        }
    }

    private fun saveTodayUsage() {
        prefs.edit()
            .putString(KEY_DATE, todayString())
            .putInt(KEY_COUNT, todayUsageCount)
            .apply()
    }

    private fun todayString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }
}
