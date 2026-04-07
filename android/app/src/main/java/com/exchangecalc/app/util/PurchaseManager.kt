package com.exchangecalc.app.util

import android.content.Context
import android.content.SharedPreferences

class PurchaseManager private constructor() {
    private var prefs: SharedPreferences? = null
    var isProUnlocked: Boolean = false
        private set

    companion object {
        private const val KEY_PURCHASED = "pro_purchased"
        private const val PREFS_NAME = "purchase_manager"

        @Volatile
        private var INSTANCE: PurchaseManager? = null

        fun getInstance(): PurchaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PurchaseManager().also { INSTANCE = it }
            }
        }

        fun init(context: Context) {
            val instance = getInstance()
            instance.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            instance.isProUnlocked = instance.prefs?.getBoolean(KEY_PURCHASED, false) ?: false
        }
    }

    fun unlockPro() {
        isProUnlocked = true
        prefs?.edit()?.putBoolean(KEY_PURCHASED, true)?.apply()
    }

    fun restorePurchase() {
        // TODO: Implement Google Play Billing verification
        isProUnlocked = prefs?.getBoolean(KEY_PURCHASED, false) ?: false
    }
}
