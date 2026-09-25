package com.exchangecalc.app.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exchangecalc.app.R
import com.exchangecalc.app.util.AppSettings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = AppSettings.getInstance()
    val context = LocalContext.current
    var showLanguagePicker by remember { mutableStateOf(false) }

    val languages = listOf(
        "" to "System Default",
        "en" to "English",
        "ja" to "日本語",
        "ko" to "한국어",
        "zh" to "中文(简体)",
        "zh-Hant" to "中文(繁體)",
        "th" to "ไทย",
        "vi" to "Tiếng Việt",
        "ru" to "Русский",
        "id" to "Bahasa Indonesia",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español"
    )

    var selectedLocale by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
                if (locales.isEmpty) {
                    ""
                } else {
                    val locale = locales[0]
                    if (locale != null && locale.script == "Hant") {
                        "${locale.language}-Hant"
                    } else {
                        locale?.language ?: ""
                    }
                }
            } else {
                val locale = Locale.getDefault()
                if (locale.script == "Hant") "${locale.language}-Hant" else locale.language
            }
        )
    }

    fun currentLanguageDisplay(): String {
        return languages.find { it.first == selectedLocale }?.second ?: languages[0].second
    }

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

            // Language
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguagePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.language))
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        currentLanguageDisplay(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLocale = code
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val localeManager = context.getSystemService(LocaleManager::class.java)
                                        localeManager.applicationLocales = if (code.isEmpty()) {
                                            LocaleList.getEmptyLocaleList()
                                        } else {
                                            LocaleList.forLanguageTags(code)
                                        }
                                    }
                                    showLanguagePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLocale == code,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (code.isEmpty()) stringResource(R.string.system_default) else name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
