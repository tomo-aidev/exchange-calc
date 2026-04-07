package com.exchangecalc.app.util

import android.content.Context
import android.content.SharedPreferences

class ReviewManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("review_manager", Context.MODE_PRIVATE)

    private val thresholds = listOf(5, 15, 35)
    private val minimumDaysBetweenRequests = 14

    companion object {
        @Volatile
        private var INSTANCE: ReviewManager? = null

        fun getInstance(context: Context): ReviewManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReviewManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun recordSave() {
        val current = prefs.getInt("total_save_count", 0)
        val newCount = current + 1
        prefs.edit().putInt("total_save_count", newCount).apply()

        if (shouldRequestReview(newCount)) {
            requestReview()
        }
    }

    private fun shouldRequestReview(saveCount: Int): Boolean {
        if (!thresholds.contains(saveCount)) return false
        val lastTime = prefs.getLong("last_review_date", 0)
        if (lastTime > 0) {
            val daysSince = (System.currentTimeMillis() - lastTime) / (1000 * 60 * 60 * 24)
            if (daysSince < minimumDaysBetweenRequests) return false
        }
        return true
    }

    private fun requestReview() {
        prefs.edit().putLong("last_review_date", System.currentTimeMillis()).apply()
        // TODO: Add Google Play In-App Review when play-core dependency is added
    }
}
