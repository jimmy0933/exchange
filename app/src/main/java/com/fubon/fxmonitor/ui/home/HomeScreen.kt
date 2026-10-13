package com.fubon.fxmonitor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fubon.fxmonitor.domain.model.RateType
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCurrencyClick: (String) -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToCalculator: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fubon FX Monitor", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToCalculator) {
                        Icon(Icons.Default.Calculate, contentDescription = "換匯計算")
                    }
                    IconButton(onClick = onNavigateToAlerts) {
                        Icon(Icons.Default.Notifications, contentDescription = "到價提醒")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "系統設定")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stale / Status Banner
            if (uiState.isStale) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "網路失敗或資料已過期，目前顯示快取資料。",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Controls Header (Spot/Cash selector + Refresh)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = uiState.rateType == RateType.SPOT,
                        onClick = { viewModel.toggleRateType(RateType.SPOT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("即期匯率")
                    }
                    SegmentedButton(
                        selected = uiState.rateType == RateType.CASH,
                        onClick = { viewModel.toggleRateType(RateType.CASH) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("現鈔匯率")
                    }
                }

                IconButton(
                    onClick = { viewModel.refreshData() },
                    enabled = !uiState.isRefreshing
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "重新整理")
                    }
                }
            }

            // Rate Cards List
            val favorites = uiState.rates.filter { it.isFavorite }
            val others = uiState.rates.filter { !it.isFavorite }

            if (uiState.rates.isEmpty() && !uiState.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("尚無匯率資料，請點擊右上方重新整理", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (favorites.isNotEmpty()) {
                        item {
                            Text(
                                text = "⭐ 我的收藏",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(favorites, key = { "fav_${it.rate.currency.name}" }) { cardState ->
                            CurrencyCard(
                                cardState = cardState,
                                selectedType = uiState.rateType,
                                onCardClick = { onCurrencyClick(cardState.rate.currency.name) },
                                onFavoriteToggle = { viewModel.toggleFavorite(cardState.rate.currency, !cardState.isFavorite) }
                            )
                        }
                    }

                    if (others.isNotEmpty()) {
                        item {
                            Text(
                                text = "所有幣別",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(others, key = { "other_${it.rate.currency.name}" }) { cardState ->
                            CurrencyCard(
                                cardState = cardState,
                                selectedType = uiState.rateType,
                                onCardClick = { onCurrencyClick(cardState.rate.currency.name) },
                                onFavoriteToggle = { viewModel.toggleFavorite(cardState.rate.currency, !cardState.isFavorite) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyCard(
    cardState: CurrencyCardUiState,
    selectedType: RateType,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val rate = cardState.rate
    val buyVal = if (selectedType == RateType.SPOT) rate.spotBuy else rate.cashBuy
    val sellVal = if (selectedType == RateType.SPOT) rate.spotSell else rate.cashSell

    val changeAmount = if (selectedType == RateType.SPOT) cardState.spotChangeAmount else cardState.cashChangeAmount
    val changePct = if (selectedType == RateType.SPOT) cardState.spotChangePct else cardState.cashChangePct

    val changeColor = when {
        changePct != null && changePct > 0 -> Color(0xFFD32F2F) // Red for up
        changePct != null && changePct < 0 -> Color(0xFF388E3C) // Green for down
        else -> Color.Gray
    }

    val changeText = if (changeAmount != null && changePct != null) {
        val sign = if (changePct >= 0) "+" else ""
        "$sign${changeAmount.setScale(4, RoundingMode.HALF_UP).toPlainString()} ($sign${BigDecimal(changePct).setScale(2, RoundingMode.HALF_UP)}%)"
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = rate.currency.flagEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${rate.currency.displayName} (${rate.currency.name})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "牌告時間: ${rate.officialUpdateTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (changeText != null) {
                        Text(
                            text = changeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = changeColor,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (cardState.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "收藏",
                            tint = if (cardState.isFavorite) Color(0xFFFFB300) else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RateBox(title = "銀行買入", value = buyVal?.toPlainString() ?: "N/A")
                RateBox(title = "銀行賣出", value = sellVal?.toPlainString() ?: "N/A", isHighlighted = true)
            }
        }
    }
}

@Composable
fun RateBox(title: String, value: String, isHighlighted: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .background(
                color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
