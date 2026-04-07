package com.exchangecalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exchangecalc.app.service.ExchangeRateService
import com.exchangecalc.app.ui.calculator.CalculatorScreen
import com.exchangecalc.app.ui.history.HistoryScreen
import com.exchangecalc.app.ui.paywall.PaywallSheet
import com.exchangecalc.app.ui.settings.SettingsScreen
import com.exchangecalc.app.ui.splash.SplashScreen
import com.exchangecalc.app.ui.theme.ExchangeCalcTheme
import com.exchangecalc.app.util.AppSettings
import com.exchangecalc.app.util.PurchaseManager
import com.exchangecalc.app.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        PurchaseManager.init(this)
        ExchangeRateService.getInstance(this)
        enableEdgeToEdge()
        setContent {
            ExchangeCalcTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: CalculatorViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf("calculator") }
    val histories by viewModel.histories.collectAsStateWithLifecycle()

    when (currentScreen) {
        "calculator" -> {
            CalculatorScreen(
                viewModel = viewModel,
                onShowHistory = { currentScreen = "history" },
                onShowSettings = { currentScreen = "settings" }
            )

            if (viewModel.showPaywall) {
                PaywallSheet(
                    onDismiss = { viewModel.dismissPaywall() },
                    onPurchased = { viewModel.onPurchased() }
                )
            }
        }
        "history" -> {
            HistoryScreen(
                histories = histories,
                onBack = { currentScreen = "calculator" },
                onRestore = { history ->
                    viewModel.restoreFromHistory(history)
                    currentScreen = "calculator"
                },
                onDelete = { viewModel.deleteHistory(it) },
                onDeleteAll = { viewModel.deleteAllHistory() }
            )
        }
        "settings" -> {
            SettingsScreen(onBack = { currentScreen = "calculator" })
        }
    }
}
