package com.fubon.fxmonitor.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fubon.fxmonitor.data.local.RateHistoryEntity
import com.fubon.fxmonitor.domain.model.RateType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onAddAlertClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${uiState.currencyCode.displayName} (${uiState.currencyCode.name}) 走勢與歷史") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onAddAlertClick(uiState.currencyCode.name) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "設定提醒")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "目前牌價 (${if (uiState.selectedRateType == RateType.SPOT) "即期賣出" else "現鈔賣出"})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    val currentVal = if (uiState.selectedRateType == RateType.SPOT) {
                        uiState.latestRate?.spotSell?.toPlainString() ?: "N/A"
                    } else {
                        uiState.latestRate?.cashSell?.toPlainString() ?: "N/A"
                    }
                    Text(
                        text = currentVal,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = uiState.selectedRateType == RateType.SPOT,
                        onClick = { viewModel.setRateType(RateType.SPOT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("即期")
                    }
                    SegmentedButton(
                        selected = uiState.selectedRateType == RateType.CASH,
                        onClick = { viewModel.setRateType(RateType.CASH) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("現鈔")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time range selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TimeRange.entries) { range ->
                    FilterChip(
                        selected = uiState.selectedTimeRange == range,
                        onClick = { viewModel.setTimeRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trend Chart Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    if (uiState.filteredHistory.size < 2) {
                        Text(
                            text = "所選時間區間內無足夠歷史資料",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    } else {
                        RateTrendChart(
                            history = uiState.filteredHistory,
                            rateType = uiState.selectedRateType,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(title = "區間最高", value = uiState.highPrice, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(title = "區間最低", value = uiState.lowPrice, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(title = "區間平均", value = uiState.avgPrice, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RateTrendChart(
    history: List<RateHistoryEntity>,
    rateType: RateType,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val chronological = history.sortedBy { it.timestamp }
        val validItems = chronological.mapNotNull { hist ->
            val str = if (rateType == RateType.SPOT) hist.spotSell else hist.cashSell
            val valFloat = str?.toFloatOrNull()
            if (valFloat != null) hist to valFloat else null
        }

        if (validItems.size < 2) return@Canvas

        val values = validItems.map { it.second }
        val minVal = values.minOrNull() ?: 0f
        val maxVal = values.maxOrNull() ?: 1f
        val rangeVal = if (maxVal == minVal) 1f else maxVal - minVal

        val minTime = validItems.first().first.timestamp
        val maxTime = validItems.last().first.timestamp
        val timeSpan = if (maxTime == minTime) 1L else maxTime - minTime

        val width = size.width
        val height = size.height

        val path = Path()
        validItems.forEachIndexed { index, (entity, value) ->
            val x = ((entity.timestamp - minTime).toFloat() / timeSpan.toFloat()) * width
            val y = height - ((value - minVal) / rangeVal) * height

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
