# Fubon FX Monitor (台北富邦銀行匯率監控)

[![Android CI Build](https://github.com/jimmy0933/exchange/actions/workflows/android.yml/badge.svg)](https://github.com/jimmy0933/exchange/actions)

Fubon FX Monitor 是一款專為台北富邦銀行匯率監控設計的原生 Android App。不需要登入銀行帳號即可自動查詢最新公開牌告匯率、多外幣即期與現鈔監控、自訂目標匯率到價通知、歷史趨勢圖表、桌面 Widget 與換匯試算。

---

## 📱 主要功能說明

1. **公開匯率監控與自動解析**：
   - 優先使用台北富邦銀行官方網站，解析 14 種官方幣別（USD、JPY、CNY、EUR、HKD、AUD、CAD、GBP、SGD、CHF、NZD、ZAR、SEK、THB）。
   - 支援解析官方牌告更新時間與資料新鮮度狀態（即時、快取、資料過期警示）。
   - 當部分幣別無現鈔報價（如 ZAR、GBP、SGD、CHF、NZD、SEK、THB）時正確標示為 N/A，不自動補零。

2. **多幣別收藏與首頁動態卡片**：
   - 預設收藏：美元 USD、日圓 JPY、人民幣 CNY、歐元 EUR。
   - 支援即期 / 現鈔切換、即時顯示漲跌金額與百分比、手動下拉更新、深色模式與離線快取顯示。

3. **匯率到價通知（核心功能）**：
   - 支援 4 種提醒條件：大於等於指定價格（≥）、小於等於指定價格（≤）、單次漲跌百分比、每日漲跌百分比（基於前一交易日基準報價）。
   - Android 原生系統通知（支援 Android 13+ POST_NOTIFICATIONS 權限安全檢查），具備防重複發送機制（離開門檻後重新進入方再次觸發）與完整觸發紀錄。

4. **背景監控與啟動復原**：
   - WorkManager 處理背景排程，設定檔更新頻率後立即更新排程。
   - `BootReceiver` (RECEIVE_BOOT_COMPLETED) 在手機重啟後依 DataStore 保存之設定自動恢復背景監控作業。

5. **歷史匯率與趨勢圖表**：
   - Canvas 自訂繪製趨勢圖，支援 今天、7天、30天、3個月、6個月 時間區間動態過濾。
   - 依實際採樣時間時間軸均勻分布，顯示區間最高價、最低價、平均價與漲跌幅度。歷史紀錄使用 Room Unique Index 防止重複寫入。

6. **Android 桌面 Widget**：
   - 小型 Widget（收藏 / 預設外幣）、中型 Widget（精選外幣看板）、大型 Widget（多外幣完整看板）。
   - 採用 AndroidX Glance 開發，共用 Room 數據快取與資料過期提醒，點擊即可開啟 App。

7. **換匯計算機**：
   - 支援雙向台幣 ⇆ 外幣試算，使用 `BigDecimal` 避免浮點數誤差，精確套用銀行買入/賣出價。

---

## 🔑 CI / CD 與 簽章指紋報告說明

為支援憑證簽章校驗，本專案提供自動簽章指紋擷取機制：

1. **APK 檔案校驗碼 (`.apk.sha256`) vs 簽章憑證指紋 (`apk_signature_fingerprints.txt`)**：
   - `.apk.sha256`：為整個 APK 二進位檔案的雜湊值，用於驗證下載檔案完整性。
   - `apk_signature_fingerprints.txt`：使用 Android SDK `apksigner verify --verbose --print-certs` 專門從最終交付 APK 中提取的簽章憑證 SHA-1 與 SHA-256 指紋。

2. **GitHub Secrets 設定方式**（若要使用固定密鑰）：
   - `KEYSTORE_BASE64`：固定 JKS / Keystore 檔案的 Base64 字串。
   - `KEYSTORE_PASSWORD`：Keystore 密碼。
   - `KEY_ALIAS`：Key 別名。
   - `KEY_PASSWORD`：Key 密碼。
   - 若未設定 Secrets，CI 將採用預設 Debug 簽章簽署，並於報告中明確標示為 `臨時 Debug 簽章`。

---

## 🛠️ 技術架構

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose, Material 3
- **Architecture**: MVVM, Repository Pattern, Clean Architecture
- **Asynchronous**: Coroutines, Flow
- **Dependency Injection**: Hilt
- **Local Storage**: Room Database, DataStore Preferences
- **Background Work**: WorkManager, BroadcastReceiver
- **Widgets**: AndroidX Glance AppWidget
- **Network / Parsing**: OkHttp, Jsoup HTML Scraper

---

## 🧪 測試與驗證結果

專案包含完整的單元測試，執行命令：
```bash
./gradlew testDebugUnitTest
```
測試涵蓋：
- 官方 HTML 解析與 14 種幣別報價擷取（HTML Fixture Tests，涵蓋正常、缺值、欄位調整與錯誤格式）
- 換匯計算機 `BigDecimal` 精確度
- 到價提醒觸發規則評估 (AlertEngine)
- 幣別代碼與 Data Mapping
