package com.exchangecalc.app.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.exchangecalc.app.R
import com.exchangecalc.app.ui.theme.Primary
import com.exchangecalc.app.ui.theme.SwapAccent
import com.exchangecalc.app.util.NumberFormatUtil
import com.exchangecalc.app.viewmodel.CalculatorViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    onShowChart: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            HeaderSection(
                onShowHistory = onShowHistory,
                onShowSettings = onShowSettings,
                onShowChart = onShowChart,
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
                lastUpdatedDisplay = viewModel.lastUpdatedDisplay,
                isLoading = viewModel.isLoading,
                hasRates = viewModel.hasRates,
                manualRateEnabled = viewModel.manualRateEnabled,
                fromCurrency = viewModel.fromCurrency,
                toCurrency = viewModel.toCurrency,
                manualRateText = viewModel.manualRateText,
                onToggleManualRate = { viewModel.toggleManualRate(it) },
                onManualRateChanged = { viewModel.setManualRate(it) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // AdMob Banner
            AdBannerView()

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
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit,
    onShowChart: () -> Unit,
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
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("app_title")
        )

        Spacer(modifier = Modifier.weight(1f))

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Primary)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rate_chart)) },
                    onClick = {
                        menuExpanded = false
                        onShowChart()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.history)) },
                    onClick = {
                        menuExpanded = false
                        onShowHistory()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        menuExpanded = false
                        onShowSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.refresh_rates)) },
                    onClick = {
                        menuExpanded = false
                        onRefreshRates()
                    }
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
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("from_currency_selector"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(fromCurrency.flag, fontSize = 20.sp)
            Text(fromCurrency.code, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("from_currency_code"))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        // Swap button
        IconButton(
            onClick = onSwap,
            modifier = Modifier
                .size(44.dp)
                .background(SwapAccent, CircleShape)
                .testTag("swap_button")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = Color.White)
        }

        // To currency
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f))
                .clickable { onToClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("to_currency_selector"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(toCurrency.flag, fontSize = 20.sp)
            Text(toCurrency.code, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("to_currency_code"))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InputDisplay(displayText: String, currencySymbol: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = currencySymbol,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.alignByBaseline()
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = displayText,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .testTag("input_display")
                .alignByBaseline()
        )
    }
}

@Composable
fun DisplaySection(
    result: com.exchangecalc.app.model.ConversionResult?,
    rateDisplay: String?,
    lastUpdatedDisplay: String?,
    isLoading: Boolean,
    hasRates: Boolean,
    manualRateEnabled: Boolean,
    fromCurrency: com.exchangecalc.app.model.Currency,
    toCurrency: com.exchangecalc.app.model.Currency,
    manualRateText: String,
    onToggleManualRate: (Boolean) -> Unit,
    onManualRateChanged: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.End,
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

            if (lastUpdatedDisplay != null) {
                Text(
                    text = lastUpdatedDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // Manual Rate section
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.manual_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = manualRateEnabled,
                    onCheckedChange = onToggleManualRate,
                    colors = SwitchDefaults.colors(checkedTrackColor = SwapAccent)
                )
            }

            if (manualRateEnabled) {
                RateDrumRollPicker(
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    initialRate = manualRateText,
                    onRateChanged = onManualRateChanged
                )
            }
        }
    }
}

@Composable
private fun RateDrumRollPicker(
    fromCurrency: com.exchangecalc.app.model.Currency,
    toCurrency: com.exchangecalc.app.model.Currency,
    initialRate: String,
    onRateChanged: (Double) -> Unit
) {
    var d0 by remember { mutableIntStateOf(0) }
    var d1 by remember { mutableIntStateOf(0) }
    var d2 by remember { mutableIntStateOf(0) }
    var d3 by remember { mutableIntStateOf(0) }
    var d4 by remember { mutableIntStateOf(0) }
    var d5 by remember { mutableIntStateOf(0) }

    fun parseRate() {
        val value = initialRate.toDoubleOrNull() ?: return
        val intPart = value.toInt() % 1000
        d0 = (intPart / 100) % 10
        d1 = (intPart / 10) % 10
        d2 = intPart % 10
        val decPart = value - value.toInt()
        val decDigits = Math.round(decPart * 1000).toInt()
        d3 = (decDigits / 100) % 10
        d4 = (decDigits / 10) % 10
        d5 = decDigits % 10
    }

    fun emitRate() {
        val intValue = d0 * 100.0 + d1 * 10.0 + d2
        val decValue = d3 * 0.1 + d4 * 0.01 + d5 * 0.001
        onRateChanged(intValue + decValue)
    }

    LaunchedEffect(initialRate) { parseRate() }

    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "1 ${fromCurrency.code} =",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            DigitWheel(d0) { d0 = it; emitRate() }
            DigitWheel(d1) { d1 = it; emitRate() }
            DigitWheel(d2) { d2 = it; emitRate() }
            Text(".", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 2.dp))
            DigitWheel(d3) { d3 = it; emitRate() }
            DigitWheel(d4) { d4 = it; emitRate() }
            DigitWheel(d5) { d5 = it; emitRate() }
            Text(
                " ${toCurrency.code}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DigitWheel(value: Int, onValueChange: (Int) -> Unit) {
    val items = (0..9).toList()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)

    LaunchedEffect(value) {
        if (listState.firstVisibleItemIndex != value) {
            listState.animateScrollToItem(value)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val idx = listState.firstVisibleItemIndex
            if (idx in 0..9 && idx != value) {
                onValueChange(idx)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(32.dp)
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AdBannerView() {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-4861952933639074/4526623890"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
