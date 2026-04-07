package com.exchangecalc.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exchangecalc.app.R
import com.exchangecalc.app.util.AppSettings
import com.exchangecalc.app.util.PurchaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = AppSettings.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default Currencies
            Text(stringResource(R.string.default_currencies), style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.from_currency))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${settings.defaultFromCurrency.flag} ${settings.defaultFromCurrency.code}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.to_currency))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${settings.defaultToCurrency.flag} ${settings.defaultToCurrency.code}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Appearance
            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.dark_mode))
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.darkModeEnabled,
                        onCheckedChange = { settings.updateDarkMode(it) }
                    )
                }
            }

            // Purchase
            Text(stringResource(R.string.purchase), style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (PurchaseManager.getInstance().isProUnlocked) {
                        Text(stringResource(R.string.pro_unlocked))
                    } else {
                        TextButton(onClick = { PurchaseManager.getInstance().restorePurchase() }) {
                            Text(stringResource(R.string.restore_purchase))
                        }
                    }
                }
            }

            // Version
            Text(stringResource(R.string.about), style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.version))
                    Spacer(modifier = Modifier.weight(1f))
                    Text("1.0.0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
