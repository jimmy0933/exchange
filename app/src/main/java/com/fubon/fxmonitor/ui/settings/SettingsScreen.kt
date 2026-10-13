package com.fubon.fxmonitor.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系統設定") },
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
            item {
                Text("背景監控設定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("背景更新頻率:", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.intervalMins == 15,
                                onClick = { viewModel.setInterval(15) },
                                label = { Text("15分鐘") }
                            )
                            FilterChip(
                                selected = uiState.intervalMins == 30,
                                onClick = { viewModel.setInterval(30) },
                                label = { Text("30分鐘") }
                            )
                            FilterChip(
                                selected = uiState.intervalMins == 60,
                                onClick = { viewModel.setInterval(60) },
                                label = { Text("1小時") }
                            )
                        }
                    }
                }
            }

            item {
                Text("通知與介面設定", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("通知聲音")
                            Switch(
                                checked = uiState.soundEnabled,
                                onCheckedChange = { viewModel.setSoundEnabled(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("通知震動")
                            Switch(
                                checked = uiState.vibrationEnabled,
                                onCheckedChange = { viewModel.setVibrationEnabled(it) }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("深色模式")
                            Switch(
                                checked = uiState.darkMode,
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }
                    }
                }
            }

            item {
                Text("系統資訊與維護", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val formattedTime = if (uiState.lastSuccessTime > 0) {
                            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.TAIWAN).format(Date(uiState.lastSuccessTime))
                        } else "尚未成功更新"
                        Text("最後成功更新時間: $formattedTime")
                        Text("官方資料來源: 台北富邦銀行官方網站 / 無障礙官方頁面")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { viewModel.refreshRatesNow() }) {
                                Text("手動重新整理")
                            }
                            OutlinedButton(onClick = { viewModel.clearCache() }) {
                                Text("清除歷史快取")
                            }
                        }
                    }
                }
            }

            item {
                Text("最近錯誤紀錄 (${uiState.errorLogs.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (uiState.errorLogs.isEmpty()) {
                            Text("無任何錯誤紀錄", color = Color.Gray)
                        } else {
                            uiState.errorLogs.take(5).forEach { log ->
                                val time = SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN).format(Date(log.timestamp))
                                Text("• [$time] ${log.message}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
