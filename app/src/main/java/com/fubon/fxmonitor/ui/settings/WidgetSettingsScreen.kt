package com.fubon.fxmonitor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetSettingsScreen(
    viewModel: WidgetSettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Widget 設定已儲存並更新桌面看板！")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("桌面 Widget 設定") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Widget Selection Section
            item {
                Text("選擇編輯的 Widget", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("選擇 Widget Instance:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.activeWidgetIds.forEachIndexed { index, id ->
                                FilterChip(
                                    selected = uiState.selectedWidgetId == id,
                                    onClick = { viewModel.selectWidget(id) },
                                    label = { Text("Widget #${index + 1}") }
                                )
                            }
                        }
                    }
                }
            }

            // Rate Type Toggle
            item {
                Text("匯率類型設定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("模式選擇:", fontWeight = FontWeight.Medium)
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = uiState.rateType == RateType.SPOT,
                                onClick = { viewModel.setRateType(RateType.SPOT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("即期匯率")
                            }
                            SegmentedButton(
                                selected = uiState.rateType == RateType.CASH,
                                onClick = { viewModel.setRateType(RateType.CASH) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("現鈔匯率")
                            }
                        }
                    }
                }
            }

            // Selected Currencies & Reordering
            item {
                Text("已選擇顯示幣別及排序 (可調整順序)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            itemsIndexed(uiState.selectedCurrencies) { index, code ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${code.flagEmoji} ${code.displayName} (${code.name})", fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.moveCurrencyUp(index) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "上移")
                            }
                            IconButton(
                                onClick = { viewModel.moveCurrencyDown(index) },
                                enabled = index < uiState.selectedCurrencies.size - 1
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "下移")
                            }
                            IconButton(
                                onClick = { viewModel.toggleCurrency(code) },
                                enabled = uiState.selectedCurrencies.size > 1
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "移除", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Available Currencies Selection
            item {
                Text("新增 / 移除 幣別", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CurrencyCode.entries.forEach { code ->
                                val isSelected = uiState.selectedCurrencies.contains(code)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleCurrency(code) },
                                    label = { Text("${code.flagEmoji} ${code.name}") }
                                )
                            }
                        }
                    }
                }
            }

            // Live Board Preview
            item {
                Text("Widget 預覽效果", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                WidgetPreviewCard(
                    rateType = uiState.rateType,
                    selectedCurrencies = uiState.selectedCurrencies,
                    rates = uiState.availableRates
                )
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveConfig() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("儲存設定並更新 Widget", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WidgetPreviewCard(
    rateType: RateType,
    selectedCurrencies: List<CurrencyCode>,
    rates: List<ExchangeRate>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17212F)) // Dark mode board theme
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "台北富邦｜${if (rateType == RateType.SPOT) "即期匯率" else "現鈔匯率"}",
                    color = Color(0xFFF8FAFC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "即時看板",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(8.dp))

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("幣別", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.2f))
                Text("銀行買入", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("銀行賣出", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(6.dp))

            selectedCurrencies.take(6).forEach { code ->
                val rate = rates.find { it.currency == code }
                val buyVal = if (rateType == RateType.SPOT) rate?.spotBuy else rate?.cashBuy
                val sellVal = if (rateType == RateType.SPOT) rate?.spotSell else rate?.cashSell

                val buyStr = buyVal?.toPlainString() ?: "未提供"
                val sellStr = sellVal?.toPlainString() ?: "未提供"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${code.flagEmoji} ${code.name}",
                        color = Color(0xFFF8FAFC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = buyStr,
                        color = Color(0xFFF8FAFC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = sellStr,
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "最後更新：14:30",
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}
