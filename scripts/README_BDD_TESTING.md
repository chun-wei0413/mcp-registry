# MCP Registry BDD 測試指南

## 概述

此目錄包含用於執行和報告 MCP Registry Java 專案 BDD (Behavior-Driven Development) 測試的工具和腳本。

## 檔案說明

### 執行腳本
- **`run-bdd-tests.sh`** - Linux/macOS 的 BDD 測試執行腳本
- **`run-bdd-tests.bat`** - Windows 的 BDD 測試執行腳本

### 報告產生器
- **`generate-bdd-report.py`** - Python 腳本，用於分析測試結果並產生詳細的 HTML 和 JSON 報告
- **`requirements.txt`** - Python 依賴套件清單

## 快速開始

### 1. 環境需求

#### 基本需求
- Java 17 或以上版本
- Maven 3.8 或以上版本
- Python 3.7 或以上版本 (用於報告產生器)

#### 可選需求
- Docker (用於 TestContainers 整合測試)

### 2. 安裝 Python 依賴

```bash
# 在 scripts 目錄下執行
pip install -r requirements.txt
```

### 3. 執行 BDD 測試

#### Linux/macOS
```bash
# 進入專案根目錄
cd /path/to/mcp-registry

# 執行所有 BDD 測試
./scripts/run-bdd-tests.sh

# 清理模式執行
./scripts/run-bdd-tests.sh --clean

# 跳過 Docker 相關測試
./scripts/run-bdd-tests.sh --no-docker

# 查看說明
./scripts/run-bdd-tests.sh --help
```

#### Windows
```cmd
# 進入專案根目錄
cd C:\path\to\mcp-registry

# 執行所有 BDD 測試
scripts\run-bdd-tests.bat
```

### 4. 產生測試報告

```bash
# 產生 HTML 和 JSON 報告
python scripts/generate-bdd-report.py test-reports/latest

# 只產生 HTML 報告
python scripts/generate-bdd-report.py test-reports/latest --format html

# 只產生 JSON 報告
python scripts/generate-bdd-report.py test-reports/latest --format json
```

## BDD 測試結構

### 測試檔案命名規範
- 所有 BDD 測試檔案以 `*BDDTest.java` 結尾
- 使用 `@DisplayName` 註解提供中文描述
- 使用 `@Nested` 類別組織相關測試場景

### BDD 場景格式
每個測試方法遵循 Given-When-Then 結構：

```java
@Test
@DisplayName("作為資料分析師，我希望能執行 SELECT 查詢來檢視客戶資料，以便進行數據分析")
void should_execute_select_query_successfully() {
    // Given: 設定測試前置條件

    // When: 執行被測試的操作

    // Then: 驗證結果
}
```

### 測試模組

#### mcp-core
- 跨資料庫整合測試
- 效能測試
- 資料遷移測試

#### mcp-postgresql-server
- 連線管理測試
- 查詢執行測試
- Schema 管理測試

#### mcp-mysql-server
- 連線管理測試
- 查詢執行測試
- Schema 管理測試

## 測試報告

### 報告位置
測試執行後，報告會儲存在 `test-reports/` 目錄下：

```
test-reports/
├── latest/                    # 指向最新測試結果的符號連結
├── bdd-tests-20240115_143052/ # 時間戳記的測試結果目錄
│   ├── mcp-core/             # 各模組的 XML 測試報告
│   ├── mcp-postgresql-server/
│   ├── mcp-mysql-server/
│   ├── test-summary.txt      # 測試摘要
│   ├── bdd-test-report.html  # HTML 格式詳細報告
│   └── bdd-test-report.json  # JSON 格式報告
```

### HTML 報告功能
- 整體測試統計摘要
- 各模組詳細測試結果
- 成功率視覺化
- 失敗測試的錯誤詳情
- 響應式設計，支援行動裝置

### JSON 報告用途
- 可用於 CI/CD 管道集成
- 便於程式化處理測試結果
- 支援進一步的數據分析

## 進階使用

### 自訂測試篩選
可以透過修改 Maven 命令來篩選特定的測試：

```bash
# 只執行特定類別的測試
mvn test -Dtest="*ConnectionManagement*BDDTest"

# 只執行特定方法的測試
mvn test -Dtest="QueryExecutionToolBDDTest#should_execute_select_query_successfully"
```

### CI/CD 整合
腳本返回適當的退出碼，便於 CI/CD 系統判斷測試結果：
- `0`: 所有測試通過
- `1`: 有測試失敗

### 效能監控
測試報告包含執行時間統計，可用於：
- 監控測試套件效能
- 識別慢速測試
- 最佳化測試執行時間

## 故障排除

### 常見問題

#### 1. Java 版本不符
```
錯誤: Java 版本過舊
解決: 升級至 Java 17 或以上版本
```

#### 2. Maven 無法找到
```
錯誤: mvn 命令不存在
解決: 安裝 Maven 並確保在 PATH 中
```

#### 3. Docker 相關錯誤
```
錯誤: TestContainers 無法啟動容器
解決:
- 確保 Docker 服務正在執行
- 檢查 Docker 權限
- 使用 --no-docker 參數跳過相關測試
```

#### 4. Python 依賴問題
```
錯誤: ModuleNotFoundError: No module named 'jinja2'
解決: pip install -r requirements.txt
```

### 日誌檔案
所有輸出都會記錄在各模組的 `*-output.log` 檔案中，可用於詳細的故障分析。

## 最佳實踐

### 1. 測試編寫
- 使用清晰的使用者故事描述
- 每個測試應該獨立且可重複執行
- 適當使用 Mock 以提高測試穩定性

### 2. 測試執行
- 定期執行完整測試套件
- 在重要變更前執行相關測試
- 監控測試執行時間趨勢

### 3. 報告分析
- 重視成功率趨勢
- 關注新增的失敗測試
- 分析效能回歸

## 擴展指南

### 新增測試模組
1. 在對應模組目錄建立 `*BDDTest.java` 檔案
2. 更新腳本中的模組清單
3. 確保遵循既有的 BDD 測試模式

### 自訂報告格式
可以修改 `generate-bdd-report.py` 中的範本來自訂報告樣式和內容。

---

## 聯絡資訊
如有問題或建議，請聯絡專案維護團隊。