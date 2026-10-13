package com.fubon.fxmonitor.ui.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fubon.fxmonitor.data.local.ExchangeAlertEntity
import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.TriggerCondition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(
    viewModel: AlertViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("匯率到價提醒管理") },
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
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { viewModel.openAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "新增提醒")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("提醒條件列表 (${uiState.alerts.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("觸發紀錄 (${uiState.historyLogs.size})") }
                )
            }

            if (selectedTab == 0) {
                if (uiState.alerts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("尚未建立任何提醒條件，點擊右下角 + 新增", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.alerts, key = { it.id }) { alert ->
                            AlertCard(
                                alert = alert,
                                onToggleEnabled = { enabled -> viewModel.toggleAlertEnabled(alert, enabled) },
                                onDelete = { viewModel.deleteAlert(alert.id) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.historyLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("無觸發歷史紀錄", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.historyLogs, key = { it.id }) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "${log.currencyCode} 到價通知",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "觸發匯率: ${log.triggeredRate}")
                                    Text(text = "條件: ${log.conditionDescription}")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showAddDialog) {
            AddAlertDialog(
                uiState = uiState,
                onDismiss = { viewModel.closeAddDialog() },
                onConfirm = { viewModel.saveAlert() },
                onCurrencyChange = { viewModel.updateSelectedCurrency(it) },
                onRateTypeChange = { viewModel.updateSelectedRateType(it) },
                onBankSideChange = { viewModel.updateSelectedBankSide(it) },
                onConditionChange = { viewModel.updateSelectedCondition(it) },
                onValueChange = { viewModel.updateTargetValue(it) }
            )
        }
    }
}

@Composable
fun AlertCard(
    alert: ExchangeAlertEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val currency = CurrencyCode.fromString(alert.currencyCode) ?: CurrencyCode.USD
    val rateTypeLabel = if (alert.rateType == "SPOT") "即期" else "現鈔"
    val bankSideLabel = if (alert.bankSide == "BUY") "銀行買入" else "銀行賣出"

    val conditionLabel = when (TriggerCondition.fromString(alert.triggerCondition)) {
        TriggerCondition.GREATER_EQUAL -> "≥ ${alert.targetValue}"
        TriggerCondition.LESS_EQUAL -> "≤ ${alert.targetValue}"
        TriggerCondition.SINGLE_CHANGE_PCT -> "單次變動 ≥ ${alert.targetValue}%"
        TriggerCondition.DAILY_CHANGE_PCT -> "每日變動 ≥ ${alert.targetValue}%"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${currency.displayName} (${currency.name}) $rateTypeLabel$bankSideLabel",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "目標條件: $conditionLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AddAlertDialog(
    uiState: AlertUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCurrencyChange: (CurrencyCode) -> Unit,
    onRateTypeChange: (RateType) -> Unit,
    onBankSideChange: (BankSide) -> Unit,
    onConditionChange: (TriggerCondition) -> Unit,
    onValueChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增匯率到價提醒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Currency dropdown/choice
                Text("選擇幣別:", fontWeight = FontWeight.Bold)
                ScrollableTabRow(selectedTabIndex = CurrencyCode.entries.indexOf(uiState.selectedCurrency)) {
                    CurrencyCode.entries.forEach { code ->
                        Tab(
                            selected = uiState.selectedCurrency == code,
                            onClick = { onCurrencyChange(code) },
                            text = { Text(code.name) }
                        )
                    }
                }

                // Rate Type & Bank Side
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilterChip(
                        selected = uiState.selectedRateType == RateType.SPOT,
                        onClick = { onRateTypeChange(RateType.SPOT) },
                        label = { Text("即期") }
                    )
                    FilterChip(
                        selected = uiState.selectedRateType == RateType.CASH,
                        onClick = { onRateTypeChange(RateType.CASH) },
                        label = { Text("現鈔") }
                    )
                    FilterChip(
                        selected = uiState.selectedBankSide == BankSide.SELL,
                        onClick = { onBankSideChange(BankSide.SELL) },
                        label = { Text("賣出") }
                    )
                    FilterChip(
                        selected = uiState.selectedBankSide == BankSide.BUY,
                        onClick = { onBankSideChange(BankSide.BUY) },
                        label = { Text("買入") }
                    )
                }

                OutlinedTextField(
                    value = uiState.targetValueInput,
                    onValueChange = onValueChange,
                    label = { Text("目標價格或百分比") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
