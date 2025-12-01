# MCP Registry

企業級 Model Context Protocol (MCP) Server 集合，提供資料庫管理與智能知識管理系統。

## 🎯 專案概述

**MCP Registry** 提供 2 個生產級 MCP Server，讓 AI 助手（如 Claude、Gemini）能夠安全地操作資料庫和管理開發知識：

| MCP Server | 功能 | 狀態 | 用途 |
|-----------|------|------|------|
| **Database MCP** | PostgreSQL & MySQL | ✅ 生產就緒 | 資料庫智能管理（查詢、Schema、事務） |
| **Memory MCP** | RAG 知識庫 | ✅ 原型實現 | 專案文件語義搜尋與知識管理 |

## 🏗️ 專案結構

```
mcp-registry/
├── 📄 Spec.md                    # 統一開發規範（唯一維護）
├── 📄 CLAUDE.md                  # Claude AI 規範（自動同步）
├── 📄 GEMINI.md                  # Gemini AI 規範（自動同步）
├── 📄 SPEC_SYNC.md               # 同步機制說明
│
├── 📁 servers/                         # MCP Server 實作
│   ├── 📁 database-mcp/                # Database MCP Server（PostgreSQL & MySQL）
│   │   ├── mcp-common/                 # 共用模組（15 個類別）
│   │   ├── mcp-core/                   # Clean Architecture 核心（29 個類別）
│   │   ├── mcp-postgresql-server/      # PostgreSQL MCP Server（10 個類別 + 6 個測試）
│   │   ├── mcp-mysql-server/           # MySQL MCP Server（4 個類別 + 7 個測試）
│   │   ├── testing-tools/              # 測試工具模組
│   │   ├── docker-compose.yml          # 部署配置
│   │   ├── pom.xml                     # Maven 主配置
│   │   └── README.md                   # Database MCP 說明
│   │
│   └── 📁 memory-mcp/                  # Memory MCP Server（RAG 知識庫）
│       ├── mcp_server.py               # FastMCP 伺服器（4 個工具）
│       ├── storage.py                  # ChromaDB + Sentence Transformers
│       ├── docker-compose.yml          # Docker 部署
│       ├── Dockerfile                  # 容器定義
│       ├── requirements.txt            # Python 依賴
│       └── README.md                   # Memory MCP 說明
│
└── 📄 README.md                  # 本文件
```

## 🛠️ 技術棧

### Database MCP Server（Java）
- **語言**: Java 17+
- **框架**: Spring Boot 3.x, Spring AI MCP SDK
- **資料庫**: HikariCP (連線池), MySQL Connector/J 8.0+, PostgreSQL JDBC
- **測試**: JUnit 5, Mockito, TestContainers
- **建置**: Maven 3.8+
- **部署**: Docker, Jib Plugin

### Memory MCP Server（Python RAG）
- **語言**: Python 3.11+
- **框架**: FastMCP (Anthropic 官方 SDK)
- **向量搜尋**: ChromaDB, Sentence Transformers (all-MiniLM-L6-v2)
- **資料驗證**: Pydantic
- **部署**: Docker Compose

## 🚀 快速開始

### 方法 1: Docker Compose（推薦）

#### 啟動 Database MCP Server（PostgreSQL + MySQL）
```bash
cd servers/database-mcp
docker-compose up -d

# 查看服務狀態
docker-compose ps
```

#### 啟動 Memory MCP Server
```bash
cd servers/memory-mcp
docker-compose up -d

# 查看日誌
docker-compose logs -f memory-mcp
```

### 方法 2: Database MCP Server 本地開發

```bash
# 建置所有模組
cd servers/database-mcp
mvn clean install

# 執行 PostgreSQL MCP Server
cd mcp-postgresql-server && mvn spring-boot:run

# 執行 MySQL MCP Server
cd mcp-mysql-server && mvn spring-boot:run
```

### 方法 3: Memory MCP Server 本地開發

```bash
cd servers/memory-mcp
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

### 2. Memory MCP Tools（Python RAG 系統）

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

### ✅ 企業級可靠性（Database MCP）
- **Clean Architecture** 設計（29 個核心類別）
- **80+ Java 類別**，完整測試覆蓋（16 個測試文件）
- **Spring Boot 3.x** 生態系統支援

### ✅ 安全性第一（Database MCP）
- **強制參數化查詢**，防止 SQL Injection
- **SQL 驗證器**，阻擋危險操作（DROP, TRUNCATE）
- **敏感資訊脫敏**
- **連線池管理**，安全的資料庫存取

### ✅ 高效能（Database MCP）
- **HikariCP** 高效能連線池
- **批次操作優化**
- **支援異步操作**

### ✅ RAG 知識管理系統（Memory MCP）
- **向量語義搜尋** 提升搜尋精準度
- **ChromaDB** 內嵌式向量資料庫（零配置）
- **Sentence Transformers** 本地嵌入模型
- **智能程式碼分離** v2.0（提升精準度 ~40%）

## 🧪 測試

### 執行所有測試
```bash
cd servers/database-mcp
mvn test
```

### 執行特定模組測試
```bash
cd servers/database-mcp/mcp-postgresql-server
mvn test
```

### 整合測試（使用 TestContainers）
```bash
mvn integration-test
```

## 🔐 安全配置

### Database MCP Server

```yaml
# application.yml
mcp:
  security:
    readonly-mode: true              # 生產環境建議啟用
    allowed-operations: SELECT       # 限制允許的操作
    blocked-keywords: DROP,TRUNCATE  # 阻擋危險操作
```

### Memory MCP Server

```yaml
# config.yaml
mcp:
  memory:
    database:
      type: chromadb                 # 內嵌式向量資料庫
      path: ./data/chroma
    embedding:
      model: all-MiniLM-L6-v2       # 本地嵌入模型
      device: cpu                    # 或 cuda
```

## 📚 詳細文檔

- 📖 **統一規範**: [Spec.md](Spec.md) - 完整開發規範（Python + Java）
- 🔄 **同步機制**: [SPEC_SYNC.md](SPEC_SYNC.md) - Spec 自動同步說明
- 🐍 **Memory MCP**: [servers/memory-mcp/README.md](servers/memory-mcp/README.md) - Python RAG 系統完整說明
- ☕ **Database MCP**: [servers/database-mcp/README.md](servers/database-mcp/README.md) - Java 資料庫管理系統

## 🎯 使用場景

### 場景 1: 資料庫管理（Database MCP）
```java
// AI 助手透過 MCP 執行安全的資料庫查詢
execute_query(
    connectionId: "main_db",
    query: "SELECT * FROM users WHERE created_at > ?",
    params: ["2024-01-01"]
)
```

### 場景 2: 專案知識管理（Memory MCP）
```python
# AI 助手儲存專案規格到 RAG 系統
store_document(file_path="./Spec.md")

# AI 助手查詢專案規範
search_knowledge(query="Clean Architecture 原則", top_k=3)
```

## 🔄 架構設計

### Clean Architecture + DDD（Database MCP）
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

### Memory MCP 向量搜尋流程
```
使用者查詢: "如何實現登入功能"
    ↓
SentenceTransformer: 文字 → 向量 [0.12, -0.34, 0.56, ...]
    ↓
ChromaDB: 向量相似度搜尋 (Cosine Similarity)
    ↓
返回 Top-K 最相關文件 + 相似度分數
    ↓
排序結果: [
  {"content": "JWT 登入實現...", "similarity": 0.92},
  {"content": "OAuth2 整合...", "similarity": 0.87},
  ...
]
```

## 📦 專案統計

| 指標 | 數值 |
|------|------|
| **Database MCP Java 類別** | 80+ |
| **Memory MCP Python 模組** | 4 |
| **測試文件** | 16 |
| **Maven 模組** | 5 |
| **MCP Servers** | 2 |
| **Docker Compose 配置** | 2 |
| **文檔文件** | 10+ |

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

**注意**: 這是一個純工具層的 MCP Server 集合，設計用於與 LLM 配合使用。請確保在生產環境中正確配置安全設定，特別是資料庫連線和知識庫存取權限。

## 🌟 如果這個專案對您有幫助，請給我們一個 ⭐！
