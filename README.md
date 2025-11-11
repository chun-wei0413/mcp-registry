# MCP Registry

企業級 Model Context Protocol (MCP) Server 集合，提供資料庫管理與智能知識管理系統。

## 🎯 專案概述

**MCP Registry** 提供 4 個生產級 MCP Server，讓 AI 助手（如 Claude、Gemini）能夠安全地操作資料庫和管理開發知識：

| MCP Server | 語言 | 狀態 | 用途 |
|-----------|------|------|------|
| **PostgreSQL MCP** | Java | ✅ 生產就緒 | PostgreSQL 資料庫智能管理 |
| **MySQL MCP** | Java | ✅ 生產就緒 | MySQL 資料庫操作 |
| **ContextCore MCP** | Java | ✅ 生產就緒 | 語義搜尋驅動的開發日誌系統 |
| **Memory MCP** | Python | ✅ 原型實現 | RAG 知識庫管理（專案文件語義搜尋）|

## 🏗️ 專案結構

```
mcp-registry/
├── 📄 Spec.md                    # 統一開發規範（唯一維護）
├── 📄 CLAUDE.md                  # Claude AI 規範（自動同步）
├── 📄 GEMINI.md                  # Gemini AI 規範（自動同步）
├── 📄 SPEC_SYNC.md               # 同步機制說明
│
├── 📁 servers/                   # MCP Server 實作
│   ├── 📁 java/                  # Java 企業級實現（100+ 類別）
│   │   ├── mcp-common/           # 共用模組（15 個類別）
│   │   ├── mcp-core/             # Clean Architecture 核心（29 個類別）
│   │   ├── mcp-postgresql-server/    # PostgreSQL MCP（10 個類別 + 6 個測試）
│   │   ├── mcp-mysql-server/         # MySQL MCP（4 個類別 + 7 個測試）
│   │   ├── mcp-contextcore-server/   # ContextCore MCP（26 個類別）
│   │   ├── docker-compose.yml        # Java 服務部署配置
│   │   └── pom.xml                   # Maven 主配置
│   │
│   └── 📁 python/                # Python 原型實現
│       ├── mcp_server.py         # FastMCP 伺服器（4 個工具）
│       ├── storage.py            # ChromaDB + Sentence Transformers
│       ├── docker-compose.yml    # Docker 部署
│       ├── Dockerfile            # 容器定義
│       ├── requirements.txt      # Python 依賴
│       └── README.md             # Memory MCP 說明
│
└── 📄 README.md                  # 本文件
```

## 🛠️ 技術棧

### Java 企業級實現
- **語言**: Java 17+
- **框架**: Spring Boot 3.x, Spring AI MCP SDK
- **資料庫**: HikariCP (連線池), MySQL Connector/J 8.0+, PostgreSQL JDBC
- **向量搜尋**: Qdrant Client, Ollama (本地嵌入)
- **測試**: JUnit 5, Mockito, TestContainers
- **建置**: Maven 3.8+
- **部署**: Docker, Jib Plugin

### Python 原型實現
- **語言**: Python 3.11+
- **框架**: FastMCP (Anthropic 官方 SDK)
- **向量搜尋**: ChromaDB, Sentence Transformers (all-MiniLM-L6-v2)
- **資料驗證**: Pydantic
- **部署**: Docker Compose

## 🚀 快速開始

### 方法 1: Docker Compose（推薦）

#### 啟動 Java MCP Servers（PostgreSQL + MySQL）
```bash
cd servers/java
docker-compose up -d

# 查看服務狀態
docker-compose ps
```

#### 啟動 Python Memory MCP Server
```bash
cd servers/python
docker-compose up -d

# 查看日誌
docker-compose logs -f memory-mcp
```

#### 啟動 ContextCore MCP Server
```bash
cd servers/java/mcp-contextcore-server
docker-compose up -d

# 包含: Qdrant, Ollama, ContextCore MCP
```

### 方法 2: Maven 本地開發

```bash
# 建置所有 Java 模組
cd servers/java
mvn clean install

# 執行特定 MCP Server
cd mcp-postgresql-server && mvn spring-boot:run
cd mcp-mysql-server && mvn spring-boot:run
cd mcp-contextcore-server && mvn spring-boot:run
```

### 方法 3: Python 本地開發

```bash
cd servers/python
pip install -r requirements.txt
python mcp_server.py
```

## 🔧 核心功能

### 1. PostgreSQL & MySQL MCP Tools

#### 連線管理
- `add_connection` - 建立資料庫連線
- `test_connection` - 測試連線狀態
- `remove_connection` - 移除連線

#### 查詢執行
- `execute_query` - 執行 SELECT 查詢（參數化，防 SQL Injection）
- `execute_update` - 執行 INSERT/UPDATE/DELETE
- `execute_transaction` - 事務執行
- `batch_execute` - 批次操作

#### Schema 管理
- `get_table_schema` - 獲取表結構
- `list_tables` - 列出所有表
- `explain_query` - 查詢執行計畫

### 2. ContextCore MCP Tools（智能開發日誌）

**為什麼需要 ContextCore？**
```
❌ 傳統方式: 載入所有日誌 → Token 消耗巨大、速度慢、超出上下文限制
✅ ContextCore: 語義搜尋相關日誌 → 只載入需要的內容，快速且精準
```

#### 日誌管理
- `add_log` - 新增開發日誌（自動向量化）
- `get_log` - 根據 ID 獲取完整日誌
- `list_log_summaries` - 列出所有日誌摘要

#### 智能搜尋
- `search_logs` - 語義搜尋相關日誌（基於向量相似度）
- `get_project_context` - 獲取專案關鍵上下文

**範例**:
```json
{
  "query": "如何處理使用者登入",
  "project": "my-app",
  "top_k": 5
}
// 返回: 最相關的 5 筆日誌（JWT實現、OAuth2、Session管理...）
```

### 3. Memory MCP Tools（Python RAG 系統）

#### 文件管理
- `store_document` - 讀取並儲存專案文件（.md, .json, .txt）
- `learn_knowledge` - 手動新增知識點

#### 語義搜尋
- `search_knowledge` - 在知識庫上執行語義搜尋
- `retrieve_all_by_topic` - 按主題檢索所有知識點

**範例**:
```python
# 儲存專案規格
store_document(file_path="./Spec.md", topic="ProjectSpec")

# 查詢 Clean Architecture
search_knowledge(query="Clean Architecture", top_k=5)
# 返回: CA 相關的所有文件內容
```

## 📊 技術亮點

### ✅ 企業級可靠性
- **Clean Architecture** 設計（29 個核心類別）
- **100+ Java 類別**，完整測試覆蓋（16 個測試文件）
- **Spring Boot 3.x** 生態系統支援

### ✅ 安全性第一
- **強制參數化查詢**，防止 SQL Injection
- **SQL 驗證器**，阻擋危險操作（DROP, TRUNCATE）
- **敏感資訊脫敏**
- **本地部署**（Ollama），資料不離開本機

### ✅ 高效能
- **HikariCP** 高效能連線池
- **Qdrant** 向量資料庫（毫秒級搜尋）
- **批次操作優化**
- **ChromaDB** 內嵌式向量資料庫（零配置）

### ✅ 智能開發日誌管理
- **向量語義搜尋** vs 全量載入（解決 Context 過載）
- **毫秒級響應**
- **多維度過濾**（專案、標籤、時間）
- **混合儲存**（Qdrant + SQLite）

## 🧪 測試

### 執行所有測試
```bash
cd servers/java
mvn test
```

### 執行特定模組測試
```bash
cd servers/java/mcp-postgresql-server
mvn test
```

### 整合測試（使用 TestContainers）
```bash
mvn integration-test
```

## 🔐 安全配置

### PostgreSQL/MySQL MCP Server

```yaml
# application.yml
mcp:
  security:
    readonly-mode: true              # 生產環境建議啟用
    allowed-operations: SELECT       # 限制允許的操作
    blocked-keywords: DROP,TRUNCATE  # 阻擋危險操作
```

### ContextCore MCP Server

```yaml
# application.yml
contextcore:
  storage:
    sqlite:
      path: ./data/logs.db
    qdrant:
      host: localhost
      port: 6333
  embedding:
    ollama:
      base-url: http://localhost:11434
      model: nomic-embed-text
```

## 📚 詳細文檔

- 📖 **統一規範**: [Spec.md](Spec.md) - 完整開發規範（Python + Java）
- 🔄 **同步機制**: [SPEC_SYNC.md](SPEC_SYNC.md) - Spec 自動同步說明
- 🐍 **Memory MCP**: [servers/python/README.md](servers/python/README.md) - Python RAG 系統完整說明
- ☕ **ContextCore MCP**: [servers/java/mcp-contextcore-server/README.md](servers/java/mcp-contextcore-server/README.md) - Java 智能日誌系統

## 🎯 使用場景

### 場景 1: 資料庫管理（PostgreSQL/MySQL MCP）
```java
// AI 助手透過 MCP 執行安全的資料庫查詢
execute_query(
    connectionId: "main_db",
    query: "SELECT * FROM users WHERE created_at > ?",
    params: ["2024-01-01"]
)
```

### 場景 2: 開發日誌搜尋（ContextCore MCP）
```java
// AI 助手智能搜尋相關開發經驗
search_logs(
    query: "如何實現 JWT 登入",
    project: "my-app",
    top_k: 5
)
// 返回: 最相關的 5 筆歷史實現記錄
```

### 場景 3: 專案知識管理（Memory MCP）
```python
# AI 助手儲存專案規格到 RAG 系統
store_document(file_path="./Spec.md")

# AI 助手查詢專案規範
search_knowledge(query="Clean Architecture 原則", top_k=3)
```

## 🔄 架構設計

### Clean Architecture + DDD（Java）
```
┌─────────────────────────────────┐
│      MCP Tools (Adapter In)     │ ← AI 助手呼叫
├─────────────────────────────────┤
│      Use Case Layer             │ ← 業務邏輯
├─────────────────────────────────┤
│      Domain Layer (Entity)      │ ← 領域模型
├─────────────────────────────────┤
│    Repository (Adapter Out)     │ ← 資料存取
└─────────────────────────────────┘
```

### ContextCore 向量搜尋流程
```
使用者查詢: "如何實現登入功能"
    ↓
Ollama: 文字 → 向量 [0.12, -0.34, 0.56, ...]
    ↓
Qdrant: 向量相似度搜尋 (Cosine Similarity)
    ↓
返回 Top-5 最相關日誌 ID + 相似度分數
    ↓
SQLite: 根據 ID 批次查詢完整內容
    ↓
排序結果: [
  {"id": "123", "content": "JWT 登入實現...", "similarity": 0.92},
  {"id": "456", "content": "OAuth2 整合...", "similarity": 0.87},
  ...
]
```

## 📦 專案統計

| 指標 | 數值 |
|------|------|
| **Java 類別** | 100+ |
| **Python 模組** | 2 |
| **測試文件** | 16 |
| **Maven 模組** | 6 |
| **MCP Servers** | 4 |
| **Docker Compose 配置** | 3 |
| **文檔文件** | 6+ |

## 🤝 貢獻

歡迎貢獻！請遵循以下步驟：

1. Fork 此專案
2. 建立特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交變更 (`git commit -m '[Feature Addition] Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 💬 支援與聯繫

- 📧 Email: a910413frank@gmail.com
- 📖 完整規範: [Spec.md](Spec.md)

## 📄 授權

此專案使用 MIT 授權 - 詳見 [LICENSE](LICENSE) 檔案

---

**注意**: 這是一個純工具層的 MCP Server 集合，設計用於與 LLM 配合使用。請確保在生產環境中正確配置安全設定，特別是資料庫連線和向量搜尋服務。

## 🌟 如果這個專案對您有幫助，請給我們一個 ⭐！
