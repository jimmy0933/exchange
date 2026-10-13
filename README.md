# Fubon FX Monitor (台北富邦銀行匯率監控)

[![Android CI Build](https://github.com/jimmy0933/exchange/actions/workflows/android.yml/badge.svg)](https://github.com/jimmy0933/exchange/actions)

Fubon FX Monitor 是一款專為台北富邦銀行匯率監控設計的原生 Android App。不需要登入銀行帳號即可自動查詢最新公開牌告匯率、多外幣即期與現鈔監控、自訂目標匯率到價通知、歷史趨勢圖表、桌面 Widget 與換匯試算。

---

## 📱 主要功能說明

1. **公開匯率監控與自動解析**：
   - 優先使用台北富邦銀行官方網站，解析 14 種官方幣別（USD、JPY、CNY、EUR、HKD、AUD、CAD、GBP、SGD、CHF、NZD、SEK、THB）。
   - 支援解析官方牌告更新時間與資料新鮮度狀態（即時、快取、資料過期警示）。
   - 當部分幣別無現鈔報價（如 ZAR、GBP、SGD、CHF、NZD、SEK、THB）時正確標示為 N/A，不自動補零。

2. **多幣別收藏與首頁動態卡片**：
   - 預設收藏：美元 USD、日圓 JPY、人民幣 CNY、歐元 EUR。
   - 支援即期 / 現鈔切換、手動下拉更新、深色模式與離線快取顯示。

3. **匯率到價通知（核心功能）**：
   - 支援 4 種提醒條件：大於等於指定價格、小於等於指定價格、單次漲跌百分比、每日漲跌百分比。
   - Android 原生系統通知，具備防重複發送機制（離開門檻後重新進入方再次觸發）與完整觸發紀錄。

4. **背景監控與啟動復原**：
   - WorkManager 處理背景排程（預設 15 分鐘輪詢與退避重試政策）。
   - `BootReceiver` (RECEIVE_BOOT_COMPLETED) 在手機重啟後自動恢復背景監控作業。

5. **歷史匯率與趨勢圖表**：
   - Canvas 自訂繪製趨勢圖，支援 今天、7天、30天、3個月、6個月 時間區間。
   - 顯示區間最高價、最低價、平均價與漲跌幅度。歷史紀錄保存於 Room Database。

6. **Android 桌面 Widget**：
   - 小型 Widget（單一指定幣別）、中型 Widget（精選 3 種外幣）、大型 Widget（多外幣看板）。
   - 採用 AndroidX Glance 開發，共用 Room 數據快取與點擊開 App。

7. **換匯計算機**：
   - 支援雙向台幣 ⇆ 外幣試算，使用 `BigDecimal` 避免浮點數誤差，精確套用銀行買入/賣出價。

---

## 🔑 CI / CD 說明

本專案採用 GitHub Actions 進行持續整合與自動化建置，不需要設定任何 GitHub Secrets 或外部 OAuth 認證。

1. **自動化流程**：
   - 自動執行單元測試。
   - 使用 Android 預設 Debug Keystore 自動編譯並產生 APK。
2. **未來擴充性**：
   - 專案架構保留未來擴充正式 Release 簽章的可能性。

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
- 官方 HTML 解析與 14 種幣別報價擷取（含 N/A 處理）
- 換匯計算機 `BigDecimal` 精確度
- 到價提醒觸發規則評估
- 幣別代碼與 Data Mapping

---

## 📦 如何下載 APK

1. 至本 Repository 的 **Actions** 頁面。
2. 點選最新成功的 **Android CI** Build。
3. 在頁面下方的 **Artifacts** 區塊下載 `app-debug` Artifact 即可取得最新的 APK 檔案。
