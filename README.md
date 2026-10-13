# Fubon FX Monitor (台北富邦銀行匯率監控)

[![Android CI Build](https://github.com/jimmy0933/money/actions/workflows/android.yml/badge.svg)](https://github.com/jimmy0933/money/actions)

Fubon FX Monitor 是一款專為台北富邦銀行匯率監控設計的原生 Android App。不需要登入銀行帳號即可自動查詢最新公開牌告匯率、多外幣即期與現鈔監控、自訂目標匯率到價通知、歷史趨勢圖表、桌面 Widget 與換匯試算。

---

## 📱 主要功能說明

1. **公開匯率監控與自動解析**：
   - 優先使用台北富邦銀行官方網站，解析 14 種官方幣別（USD、JPY、CNY、EUR、HKD、AUD、CAD、GBP、SGD、CHF、NZD、ZAR、SEK、THB）。
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

## 🔑 CI / CD 與 簽章指紋報告說明

為支援 Google Cloud OAuth / Google 登入或相關認證登記，本專案提供自動簽章指紋擷取與驗證機制：

### 1. APK 檔案校驗碼 (`.apk.sha256`) vs 簽章憑證指紋 (`apk_signature_fingerprints.txt`)
- **APK 檔案校驗碼 (`app-debug.apk.sha256`)**：為整個 APK 二進位檔案本身的雜湊值（File Checksum），隨每次編譯產生改變，用於驗證下載檔案完整性，**絕不能**做為 Google Cloud OAuth SHA-1 使用。
- **簽章憑證指紋 (`apk_signature_fingerprints.txt`)**：使用 Android SDK 的 `apksigner verify --verbose --print-certs` 專門從最終交付 APK 中提取的簽章憑證 SHA-1 與 SHA-256 指紋。

### 2. Google Cloud Android OAuth Client 設定指南
前往 Google Cloud Console -> APIs & Services -> Credentials -> Create Credentials -> OAuth client ID (Android)：
- **Package Name (應用程式 ID)**：`com.fubon.fxmonitor`（或依專案需求指定如 `com.moneytracker.app`，請務必與簽章報告中產出的 Application ID / Package 一致）。
- **SHA-1 憑證指紋**：請從 CI Build 產出的 Artifact `apk-signature-fingerprints` 或 GitHub Actions Summary 中複製 SHA-1 指紋（包含可直接複製之格式）。

### 3. GitHub Secrets 設定方式（固定 Keystore 簽署）
若要使用固定密鑰進行連續建置（確保建置產出相同的簽章 SHA-1）：
1. 建立金鑰庫檔案（如 `release.keystore`）並轉為 Base64 字串：
   ```bash
   base64 -w 0 release.keystore > keystore_base64.txt
   ```
2. 至 GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions**，新增以下 Secrets：
   - `KEYSTORE_BASE64`：金鑰庫檔案 Base64 編碼內容
   - `KEYSTORE_PASSWORD`：金鑰庫密碼
   - `KEY_ALIAS`：金鑰別名
   - `KEY_PASSWORD`：金鑰密碼
3. 若未設定 Secrets，CI 將自動採用預設 Debug 簽章簽署，並於報告中明確標示為 `臨時 Debug 簽章` 及列出待設定 Secrets。

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

## 📦 如何下載 APK 及簽章報告

1. 至本 Repository 的 **Actions** 頁面。
2. 點選最新成功的 **Android CI** Build。
3. 在頁面下方的 **Artifacts** 區塊下載：
   - `app-debug`：包含最新交付 APK 檔案。
   - `apk-signature-fingerprints`：包含簽章報告 `apk_signature_fingerprints.txt` 與 APK 檔案校驗碼 `app-debug.apk.sha256`。
4. 亦可在 Workflow 的 **Summary** 摘要頁面直接閱讀與複製 SHA-1 指紋。

---

## ⚠️ 待設定項目與聲明

1. **尚未設定之 GitHub Secrets**：如欲使用固定簽章，請至 GitHub Repository Secrets 設定 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS` 與 `KEY_PASSWORD`。
2. **Google Cloud 外部設定**：需要在 Google Cloud Console 建立對應的 Android OAuth Client ID，並填入 Package Name 與最終交付 APK 的 SHA-1 憑證指紋。
3. **重要聲明**：在完成固定 Keystore 設定並於真實 Android 裝置實測 Google 登入流程前，**不得宣稱 10 DEVELOPER_ERROR 登入問題已完全解決**。
