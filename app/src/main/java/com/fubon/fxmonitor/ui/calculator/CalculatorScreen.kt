package com.fubon.fxmonitor.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.ConversionDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("換匯試算計算機") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Currency Selector
            Text("選擇外幣:", fontWeight = FontWeight.Bold)
            ScrollableTabRow(selectedTabIndex = CurrencyCode.entries.indexOf(uiState.selectedCurrency)) {
                CurrencyCode.entries.forEach { code ->
                    Tab(
                        selected = uiState.selectedCurrency == code,
                        onClick = { viewModel.setCurrency(code) },
                        text = { Text("${code.flagEmoji} ${code.name}") }
                    )
                }
            }

            // Options: Spot / Cash & Direction Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = uiState.selectedRateType == RateType.CASH,
                        onClick = { viewModel.setRateType(RateType.CASH) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("現鈔")
                    }
                    SegmentedButton(
                        selected = uiState.selectedRateType == RateType.SPOT,
                        onClick = { viewModel.setRateType(RateType.SPOT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("即期")
                    }
                }

                ElevatedButton(
                    onClick = {
                        val newDir = if (uiState.direction == ConversionDirection.TWD_TO_FOREIGN) {
                            ConversionDirection.FOREIGN_TO_TWD
                        } else {
                            ConversionDirection.TWD_TO_FOREIGN
                        }
                        viewModel.setDirection(newDir)
                    }
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (uiState.direction == ConversionDirection.TWD_TO_FOREIGN) "台幣 ➔ 外幣" else "外幣 ➔ 台幣"
                    )
                }
            }

            // Input Amount
            OutlinedTextField(
                value = uiState.inputAmountString,
                onValueChange = { viewModel.setInputAmount(it) },
                label = {
                    Text(
                        if (uiState.direction == ConversionDirection.TWD_TO_FOREIGN) "輸入台幣金額 (TWD)"
                        else "輸入外幣金額 (${uiState.selectedCurrency.name})"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Result Display
            uiState.conversionResult?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("試算預估結果", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "${res.outputAmount.toPlainString()} ${if (uiState.direction == ConversionDirection.TWD_TO_FOREIGN) res.currencyCode else "TWD"}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Text("套用匯率: ${res.rateUsed} (${if (res.rateType == RateType.SPOT) "即期" else "現鈔"}${if (res.bankSide == com.fubon.fxmonitor.domain.model.BankSide.SELL) "賣出" else "買入"})")
                        Text("牌告時間: ${res.officialUpdateTime}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = res.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } ?: run {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("請輸入有效金額進行試算", color = Color.Gray)
                    }
                }
            }
        }
    }
}
