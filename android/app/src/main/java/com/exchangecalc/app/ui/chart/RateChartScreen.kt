package com.exchangecalc.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchangecalc.app.R
import com.exchangecalc.app.model.Currency
import com.exchangecalc.app.service.ExchangeRateService
import com.exchangecalc.app.service.PairRatePoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val Primary = Color(0xFF005BB2)
private val SwapAccent = Color(0xFFD95926)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateChartScreen(
    fromCurrency: Currency,
    toCurrency: Currency,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dataPoints by remember { mutableStateOf<List<PairRatePoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(fromCurrency, toCurrency) {
        isLoading = true
        val service = ExchangeRateService.getInstance(context)
        dataPoints = service.fetchPairHistory(fromCurrency, toCurrency)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rate_chart)) },
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
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Currency pair header
            CurrencyPairHeader(fromCurrency, toCurrency)

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.loading_chart),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                dataPoints.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_chart_data),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Summary section
                    SummarySection(dataPoints)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chart
                    RateChart(
                        dataPoints = dataPoints,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { selectedIndex = it }
                    )

                    // Selected point info
                    if (selectedIndex in dataPoints.indices) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SelectedPointInfo(
                            point = dataPoints[selectedIndex],
                            fromCurrency = fromCurrency,
                            toCurrency = toCurrency
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyPairHeader(fromCurrency: Currency, toCurrency: Currency) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${fromCurrency.flag} ${fromCurrency.code}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = " \u2192 ",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${toCurrency.flag} ${toCurrency.code}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Primary.copy(alpha = 0.12f)
        ) {
            Text(
                text = stringResource(R.string.days_90),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SummarySection(dataPoints: List<PairRatePoint>) {
    val lastRate = dataPoints.last().rate
    val firstRate = dataPoints.first().rate
    val highRate = dataPoints.maxOf { it.rate }
    val lowRate = dataPoints.minOf { it.rate }
    val changePercent = if (firstRate != 0.0) ((lastRate - firstRate) / firstRate) * 100.0 else 0.0
    val changeColor = if (changePercent >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(stringResource(R.string.current), String.format("%.3f", lastRate))
        StatItem(stringResource(R.string.high), String.format("%.3f", highRate))
        StatItem(stringResource(R.string.low), String.format("%.3f", lowRate))
        StatItem(
            stringResource(R.string.change),
            String.format("%+.1f%%", changePercent),
            valueColor = changeColor
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun RateChart(
    dataPoints: List<PairRatePoint>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    val rates = dataPoints.map { it.rate }
    val minRate = rates.min() * 0.998
    val maxRate = rates.max() * 1.002
    val rateRange = maxRate - minRate

    // Parse dates for x-axis labels
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val outputFormat = SimpleDateFormat("MMM d", Locale.US)
    val dateLabels = dataPoints.map { point ->
        try {
            val date = inputFormat.parse(point.date)
            date?.let { outputFormat.format(it) } ?: point.date.takeLast(5)
        } catch (_: Exception) {
            point.date.takeLast(5)
        }
    }

    val chartHeight = 220.dp
    val leftPadding = 0f
    val rightPadding = with(density) { 50.dp.toPx() }
    val topPadding = with(density) { 8.dp.toPx() }
    val bottomPadding = with(density) { 24.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .pointerInput(dataPoints) {
                detectTapGestures { offset ->
                    val chartWidth = size.width - leftPadding - rightPadding
                    val idx = ((offset.x - leftPadding) / chartWidth * (dataPoints.size - 1))
                        .toInt()
                        .coerceIn(0, dataPoints.size - 1)
                    onSelectedIndexChange(idx)
                }
            }
            .pointerInput(dataPoints) {
                detectHorizontalDragGestures { change, _ ->
                    val chartWidth = size.width - leftPadding - rightPadding
                    val idx = ((change.position.x - leftPadding) / chartWidth * (dataPoints.size - 1))
                        .toInt()
                        .coerceIn(0, dataPoints.size - 1)
                    onSelectedIndexChange(idx)
                }
            }
    ) {
        val chartWidth = size.width - leftPadding - rightPadding
        val chartDrawHeight = size.height - topPadding - bottomPadding

        fun xForIndex(i: Int): Float = leftPadding + chartWidth * i / (dataPoints.size - 1).coerceAtLeast(1)
        fun yForRate(rate: Double): Float = topPadding + chartDrawHeight * (1 - ((rate - minRate) / rateRange)).toFloat()

        // Draw x-axis labels at ~15 day intervals
        val labelInterval = (dataPoints.size / 6).coerceAtLeast(1)
        val textPaint = android.graphics.Paint().apply {
            color = textColor.hashCode()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        for (i in dataPoints.indices step labelInterval) {
            val x = xForIndex(i)
            drawContext.canvas.nativeCanvas.drawText(
                dateLabels[i],
                x,
                size.height - 2f,
                textPaint
            )
        }
        // Always draw last label
        if (dataPoints.size > 1) {
            val lastIdx = dataPoints.size - 1
            drawContext.canvas.nativeCanvas.drawText(
                dateLabels[lastIdx],
                xForIndex(lastIdx),
                size.height - 2f,
                textPaint
            )
        }

        // Draw y-axis labels on the right
        val yLabelPaint = android.graphics.Paint().apply {
            color = textColor.hashCode()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.LEFT
            isAntiAlias = true
        }
        val ySteps = 4
        for (i in 0..ySteps) {
            val rate = minRate + rateRange * i / ySteps
            val y = yForRate(rate)
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.2f", rate),
                size.width - rightPadding + with(density) { 6.dp.toPx() },
                y + with(density) { 4.dp.toPx() },
                yLabelPaint
            )
        }

        // Build line path
        val linePath = Path().apply {
            dataPoints.forEachIndexed { i, point ->
                val x = xForIndex(i)
                val y = yForRate(point.rate)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        // Area fill path
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(xForIndex(dataPoints.size - 1), topPadding + chartDrawHeight)
            lineTo(xForIndex(0), topPadding + chartDrawHeight)
            close()
        }

        // Draw area gradient
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(Primary.copy(alpha = 0.15f), Color.Transparent),
                startY = topPadding,
                endY = topPadding + chartDrawHeight
            )
        )

        // Draw line
        drawPath(
            path = linePath,
            color = Primary,
            style = Stroke(width = with(density) { 2.dp.toPx() })
        )

        // Selected point indicator
        if (selectedIndex in dataPoints.indices) {
            val sx = xForIndex(selectedIndex)
            val sy = yForRate(dataPoints[selectedIndex].rate)

            // Dashed vertical line
            drawLine(
                color = textColor.copy(alpha = 0.4f),
                start = Offset(sx, topPadding),
                end = Offset(sx, topPadding + chartDrawHeight),
                strokeWidth = with(density) { 1.dp.toPx() },
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // Point marker
            drawCircle(
                color = surfaceColor,
                radius = with(density) { 5.dp.toPx() },
                center = Offset(sx, sy)
            )
            drawCircle(
                color = SwapAccent,
                radius = with(density) { 4.dp.toPx() },
                center = Offset(sx, sy)
            )
        }
    }
}

@Composable
private fun SelectedPointInfo(
    point: PairRatePoint,
    fromCurrency: Currency,
    toCurrency: Currency
) {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val displayDate = try {
        val date = inputFormat.parse(point.date)
        date?.let { displayFormat.format(it) } ?: point.date
    } catch (_: Exception) {
        point.date
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayDate,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "1 ${fromCurrency.code} = ${String.format("%.4f", point.rate)} ${toCurrency.code}",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SwapAccent
        )
    }
}
