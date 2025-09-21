# Kanban 資料遷移專案 Context

## 🎯 專案目標

將 `old_kanban_data` 目錄中的 MySQL 備份資料遷移到已部署的 PostgreSQL 容器，確保舊資料能在新架構下正常運行。此系統支援 State Sourcing 和 Event Sourcing，**優先以 State Sourcing 為主進行遷移**。

## 🏗️ 技術架構

### 核心設計理念
```
MySQL MCP Server (處理舊資料)
+ LLM (智能轉換邏輯)
+ PostgreSQL MCP Server (寫入新系統)
= 智能資料遷移系統
```

### 系統組件分工
- **MySQL MCP Server**: 讀取和查詢 old_kanban_data 中的舊資料
- **PostgreSQL MCP Server**: 操作已部署的新系統資料庫
- **LLM**: 分析架構差異，制定轉換策略，執行智能遷移
- **Context**: 包含舊新系統架構差異、業務規則、轉換範例

## 📋 實現步驟

### Phase 1: 環境準備

#### 1.1 部署 MySQL Container
- 啟動 MySQL 容器用於載入舊資料
- 將 `old_kanban_data` 備份檔案匯入 MySQL
- 確保資料完整性和可訪問性

#### 1.2 實現 MySQL MCP Server
- 基於現有 PostgreSQL MCP Server 架構設計，請看 @src 底下專案， @doc底下有docker資料
- 使用 `aiomysql` 替代 `asyncpg` 作為資料庫驅動
- 提供相同的工具介面：`execute_query`, `get_table_schema`, `execute_transaction` 等
- 保持與 PostgreSQL MCP Server 一致的 API 設計

#### 1.3 連接現有 PostgreSQL
- 連接已部署的 PostgreSQL 容器
- 驗證新系統表結構完整性
- 確認目標資料庫讀寫權限

### Phase 2: 架構分析

#### 2.1 自動分析舊架構
- LLM 透過 MySQL MCP Server 讀取 `old_kanban_data` 的表結構
- 分析舊系統的資料模型和關聯關係
- 識別舊系統的業務邏輯和資料模式
- 重點分析 State Sourcing 相關的資料表

#### 2.2 對比新架構
- LLM 透過 PostgreSQL MCP Server 分析新系統表結構
- 理解新系統的設計原則和架構改進
- 識別新系統支援的 State Sourcing 和 Event Sourcing 模式
- 分析新舊系統在資料模型上的差異

#### 2.3 生成差異報告
- 自動生成詳細的架構對比報告
- 識別需要轉換的欄位、資料類型、關聯關係
- 標註可能的資料遺失風險和處理建議
- 制定 State Sourcing 優先的遷移策略

### Phase 3: 遷移執行

#### 3.1 State Sourcing 優先遷移
- 優先遷移當前狀態資料（最新的業務狀態）
- 忽略或延後處理歷史變更記錄（Event Sourcing 資料）
- 確保業務流程的連續性和資料一致性

#### 3.2 批次處理
- 分批次讀取 MySQL 資料，避免記憶體溢出
- 每批次處理 100-500 筆記錄（根據資料複雜度調整）
- 實現斷點續傳機制，支援中斷後繼續執行

#### 3.3 驗證檢查
- 每批次遷移後自動驗證資料一致性
- 比較遷移前後的記錄數量和關鍵欄位
- 執行業務邏輯驗證，確保資料關聯正確
- 生成遷移日誌和錯誤報告

## 🔧 技術實現要求

### MySQL MCP Server 規格
```python
# 核心工具
- execute_query: 執行 MySQL 查詢
- get_table_schema: 獲取 MySQL 表結構  
- execute_transaction: 事務性操作
- add_connection: 建立 MySQL 連線
- test_connection: 測試連線狀態

# 技術要求
- 使用 aiomysql 異步驅動
- 支援連線池管理
- 參數化查詢防 SQL 注入
- 完整錯誤處理和日誌
```

### 資料轉換邏輯
```python
# LLM Context 包含：
- old_kanban_data 的表結構和業務邏輯
- 新系統的架構設計和約束條件
- State Sourcing vs Event Sourcing 的差異
- 欄位映射規則和資料轉換邏輯
- 業務規則和驗證條件
```

### 遷移策略配置
```json
{
  "migration_priority": "state_sourcing_first",
  "batch_size": 200,
  "verification_enabled": true,
  "error_handling": "skip_and_log",
  "rollback_support": true
}
```

## 📊 預期成果

### 技術成果
- [x] 完整的 MySQL MCP Server 實現
- [x] 自動化的架構分析和差異報告
- [x] 智能化的資料轉換和遷移邏輯
- [x] 完整的遷移驗證和日誌系統

### 業務成果
- [x] old_kanban_data 成功遷移到新系統
- [x] State Sourcing 資料完整性保證
- [x] 新舊系統資料一致性驗證
- [x] 詳細的遷移報告和後續建議

## 🎪 預期的 LLM 對話流程

```
Human: 請執行 Kanban 資料遷移，old_kanban_data 在 MySQL，目標是 PostgreSQL

LLM: 開始執行 Kanban 資料遷移...

Phase 1: 環境準備
[建立 MySQL 連線並匯入 old_kanban_data]
[連接現有 PostgreSQL 容器]

Phase 2: 架構分析
[分析 MySQL 舊架構]
發現舊系統包含：users, projects, tasks, comments 等表
[分析 PostgreSQL 新架構] 
新系統支援 State Sourcing 和 Event Sourcing 模式
[生成差異報告]
主要差異：新系統增加了事件表、改進了關聯設計

Phase 3: 遷移執行
[以 State Sourcing 優先執行遷移]
- 遷移用戶資料：成功轉換 150 筆用戶記錄
- 遷移專案資料：成功轉換 45 個專案
- 遷移任務資料：成功轉換 1,200 個任務
[驗證資料一致性]
所有核心業務資料遷移完成，資料一致性驗證通過

遷移報告：
- 總記錄數：1,395 筆
- 成功遷移：1,390 筆 (99.6%)
- 失敗記錄：5 筆（已記錄詳細錯誤）
- 建議：Event Sourcing 歷史資料可依需求後續遷移
```

## 🚀 成功指標

- [x] **完整性**: old_kanban_data 核心業務資料 100% 遷移
- [x] **準確性**: 資料轉換邏輯正確，業務邏輯保持一致
- [x] **效率**: 遷移過程自動化，人工干預最小化
- [x] **可靠性**: 支援錯誤處理、回滾和斷點續傳
- [x] **可追蹤性**: 完整的遷移日誌和驗證報告

---

**專案核心價值**: 透過 LLM + MCP 的智能遷移方案，將複雜的資料庫遷移轉化為自動化、可靠、可追蹤的智能流程，既保證了資料完整性，又大幅降低了遷移風險和人工成本。