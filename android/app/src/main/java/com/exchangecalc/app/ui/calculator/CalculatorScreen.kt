package com.exchangecalc.app.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchangecalc.app.R
import com.exchangecalc.app.ui.theme.Primary
import com.exchangecalc.app.ui.theme.SwapAccent
import com.exchangecalc.app.util.NumberFormatUtil
import com.exchangecalc.app.viewmodel.CalculatorViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            HeaderSection(
                remainingCount = viewModel.remainingCount,
                isProUnlocked = viewModel.isProUnlocked,
                onShowHistory = onShowHistory,
                onShowSettings = onShowSettings,
                onRefreshRates = { viewModel.forceRefreshRates() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Currency Pair Selector
            CurrencyPairSelector(
                fromCurrency = viewModel.fromCurrency,
                toCurrency = viewModel.toCurrency,
                onFromClick = { viewModel.openFromCurrencyPicker() },
                onToClick = { viewModel.openToCurrencyPicker() },
                onSwap = { viewModel.swapCurrencies() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Input Display
            InputDisplay(
                displayText = viewModel.displayText,
                currencySymbol = viewModel.fromCurrency.symbol
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Conversion Result Card
            DisplaySection(
                result = viewModel.conversionResult,
                rateDisplay = viewModel.rateDisplay,
                lastUpdated = viewModel.lastUpdated,
                isLoading = viewModel.isLoading,
                hasRates = viewModel.hasRates
            )

            Spacer(modifier = Modifier.weight(1f))

            // Keypad
            NumericKeypad(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.46f),
                onDigit = { viewModel.appendDigit(it) },
                onDoubleZero = { viewModel.appendDoubleZero() },
                onDecimalPoint = { viewModel.appendDecimalPoint() },
                onBackspace = { viewModel.deleteLastCharacter() },
                onClear = { viewModel.clear() },
                onSave = { viewModel.saveToHistory() },
                decimalEnabled = viewModel.fromCurrency.decimalPlaces > 0
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Saved feedback
    if (viewModel.showSavedFeedback) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            viewModel.dismissSavedFeedback()
        }
    }

    // Currency pickers
    if (viewModel.showFromCurrencyPicker) {
        CurrencySelectorSheet(
            title = stringResource(R.string.from_currency),
            excludeCurrency = viewModel.toCurrency,
            onSelect = { viewModel.selectFromCurrency(it) },
            onDismiss = { viewModel.dismissFromCurrencyPicker() }
        )
    }

    if (viewModel.showToCurrencyPicker) {
        CurrencySelectorSheet(
            title = stringResource(R.string.to_currency),
            excludeCurrency = viewModel.fromCurrency,
            onSelect = { viewModel.selectToCurrency(it) },
            onDismiss = { viewModel.dismissToCurrencyPicker() }
        )
    }
}

@Composable
private fun HeaderSection(
    remainingCount: Int,
    isProUnlocked: Boolean,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    onRefreshRates: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        if (!isProUnlocked) {
            Text(
                text = "$remainingCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(
                        if (remainingCount > 3) Primary else Color.Red,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Primary)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.history)) },
                    onClick = { menuExpanded = false; onShowHistory() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = { menuExpanded = false; onShowSettings() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.refresh_rates)) },
                    onClick = { menuExpanded = false; onRefreshRates() }
                )
            }
        }
    }
}

@Composable
private fun CurrencyPairSelector(
    fromCurrency: com.exchangecalc.app.model.Currency,
    toCurrency: com.exchangecalc.app.model.Currency,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    onSwap: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // From currency
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f))
                .clickable { onFromClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(fromCurrency.flag, fontSize = 20.sp)
            Text(fromCurrency.code, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        // Swap button
        IconButton(
            onClick = onSwap,
            modifier = Modifier
                .size(44.dp)
                .background(SwapAccent, CircleShape)
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = Color.White)
        }

        // To currency
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f))
                .clickable { onToClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(toCurrency.flag, fontSize = 20.sp)
            Text(toCurrency.code, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InputDisplay(displayText: String, currencySymbol: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = currencySymbol,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = displayText,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
fun DisplaySection(
    result: com.exchangecalc.app.model.ConversionResult?,
    rateDisplay: String?,
    lastUpdated: Long?,
    isLoading: Boolean,
    hasRates: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (result != null) {
                Text(
                    text = NumberFormatUtil.formatCurrency(result.convertedAmount, result.toCurrency),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    maxLines = 1
                )
            } else {
                Text(
                    text = if (isLoading) stringResource(R.string.loading_rates)
                           else if (!hasRates) stringResource(R.string.no_rates)
                           else "-",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            if (rateDisplay != null) {
                Text(
                    text = rateDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (lastUpdated != null) {
                val dateStr = remember(lastUpdated) {
                    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(lastUpdated))
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
