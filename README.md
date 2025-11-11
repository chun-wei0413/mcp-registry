# MCP Registry

企業級 Model Context Protocol (MCP) Server 集合，提供資料庫管理與智能開發日誌系統。

## 🎯 專案概述

**MCP Registry** 提供三個生產級 MCP Server，讓 LLM（如 Claude）能夠安全地操作資料庫和管理開發知識：

| MCP Server | 用途 | 核心技術 |
|-----------|------|---------|
| **PostgreSQL MCP** | PostgreSQL 資料庫智能管理 | HikariCP, Spring Boot, Clean Architecture |
| **MySQL MCP** | MySQL 資料庫原生操作 | MySQL Connector/J 8.0+, Batch Optimization |
| **ContextCore MCP** | 語義搜尋驅動的開發日誌系統 | Qdrant, Ollama, SQLite |

### 為什麼需要 ContextCore？

傳統開發日誌管理的痛點：
```
❌ 載入所有日誌 → Token 消耗巨大、速度慢、超出上下文限制
✅ 語義搜尋相關日誌 → 只載入需要的內容，快速且精準
```

**ContextCore** 透過向量搜尋技術，將 LLM 從「記住所有歷史」解放出來，轉變為「智能檢索相關經驗」。

## 🏗️ 專案結構

```
mcp-registry/
├── servers/
│   ├── python/                          # Python 原型（ContextCore 概念驗證）
│   │   ├── mcp_server.py                # FastMCP 實現
│   │   ├── storage.py                   # ChromaDB + Sentence Transformers
│   │   └── requirements.txt
│   └── java/                            # 企業級實現（生產環境）
│       ├── mcp-common/                  # 共用模組
│       │   ├── model/                   # ConnectionInfo, QueryResult
│       │   ├── exception/               # McpException, QueryException
│       │   ├── util/                    # SqlValidator (SQL 注入防護)
│       │   └── mcp/                     # @McpTool, @McpResource 註解
│       ├── mcp-core/                    # Clean Architecture 核心
│       │   ├── entity/                  # 領域實體
│       │   ├── usecase/                 # Use Case 服務
│       │   ├── adapter/
│       │   │   ├── in/mcp/              # MCP Tools 和 Resources
│       │   │   └── out/repository/      # Repository 實現
│       │   └── port/                    # 介面定義
│       ├── mcp-postgresql-server/       # PostgreSQL MCP Server
│       │   ├── controller/              # PostgreSqlMcpController
│       │   ├── service/                 # Connection, Query, Schema Services
│       │   ├── tool/                    # MCP Tools
│       │   └── resource/                # MCP Resources
│       ├── mcp-mysql-server/            # MySQL MCP Server
│       │   ├── controller/              # MySqlMcpController
│       │   ├── service/                 # DatabaseConnectionService
│       │   └── tool/                    # MCP Tools
│       └── mcp-contextcore-server/      # 智能開發日誌系統
│           ├── domain/
│           │   ├── entity/              # Log, LogSearchResult
│           │   └── repository/          # LogRepository, VectorRepository
│           ├── infrastructure/
│           │   ├── sqlite/              # 完整日誌儲存
│           │   ├── qdrant/              # 向量搜尋引擎
│           │   └── ollama/              # 本地向量化服務
│           ├── usecase/                 # AddLog, SearchLogs, GetLog
│           └── controller/              # MCP Tools
├── deployment/                          # 部署配置
│   ├── docker-compose.yml               # 完整部署配置
│   ├── test-mysql-mcp/                  # MySQL MCP 測試環境
│   ├── test-postgres-mcp/               # PostgreSQL MCP 測試環境
│   └── contextcore-docker-compose.yml   # ContextCore 完整環境
├── documentation/                       # 完整技術文檔
│   ├── mcp-servers/                     # 各 MCP Server 詳細說明
│   │   ├── contextcore-mcp.md
│   │   ├── postgresql-mcp.md
│   │   └── mysql-mcp.md
│   ├── guides/                          # 使用指南
│   │   ├── GETTING_STARTED.md
│   │   └── FAQ.md
│   └── README.md                        # 文檔索引
└── scripts/                             # 啟動和測試腳本
    ├── start-contextcore.sh
    ├── start-all.sh
    └── dev.sh
```

## 🛠️ 技術棧

### 核心框架
- **Java 17**: 現代語言特性和效能優化
- **Spring Boot 3.x**: 企業級應用框架
- **Spring AI MCP SDK**: 原生 MCP 協議支援（預留整合）

### 資料庫與連接
- **HikariCP**: 高效能連線池
- **MySQL Connector/J 8.0+**: MySQL 驅動
- **PostgreSQL JDBC**: PostgreSQL 驅動
- **R2DBC**: 反應式資料庫連接（規劃中）

### ContextCore 技術棧
- **Qdrant**: 高效能向量資料庫（毫秒級搜尋）
- **Ollama**: 本地向量化服務（支援 nomic-embed-text 等模型）
- **SQLite**: 完整日誌內容儲存
- **ChromaDB**: Python 原型使用（驗證概念）

### 開發與測試
- **Maven**: 專案管理和建置工具
- **ezSpec**: BDD 風格測試框架
- **TestContainers**: 整合測試環境
- **Jib Plugin**: 優化的 Docker 映像建置

## 🚀 核心特性

### 資料庫 MCP Server 特性
- **🔒 安全性第一**: 強制參數化查詢、SQL 注入防護、危險操作阻擋
- **⚡ 高效能**: HikariCP 連線池、批次操作優化、PreparedStatement 快取
- **🔍 可觀測性**: Spring Boot Actuator 監控、結構化日誌
- **🛡️ 安全配置**: 唯讀模式、操作白名單、查詢長度限制
- **🧪 完整測試**: 單元測試、整合測試、TestContainers 支援

### ContextCore MCP 特性
- **🎯 語義搜尋**: 向量相似度搜尋，精準找到相關開發日誌
- **💾 混合儲存**: Qdrant (向量) + SQLite (完整內容)
- **🏠 完全本地**: Ollama 本地部署，資料不離開本機
- **⚡ 毫秒級響應**: Qdrant 高效能向量搜尋
- **📊 多維度過濾**: 專案、標籤、時間範圍組合搜尋
- **🔄 版本追蹤**: 完整的日誌歷史記錄

## 📋 系統需求

### 基礎需求
- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 資料庫（根據需求選擇）
- PostgreSQL 12+ (使用 PostgreSQL MCP)
- MySQL 8.0+ (使用 MySQL MCP)

### ContextCore 額外需求
- Qdrant 1.7+ (向量資料庫)
- Ollama (本地向量化服務)
- 建議至少 4GB RAM

## 🔧 快速開始

### 1. 使用 Docker Compose（推薦）

#### 啟動所有服務
```bash
# 克隆專案
git clone <repository-url>
cd mcp-registry

# 啟動完整環境（包含 PostgreSQL, MySQL, ContextCore）
cd deployment/
docker-compose up -d

# 查看服務狀態
docker-compose ps
```

#### 僅啟動 ContextCore
```bash
cd deployment/
docker-compose -f contextcore-docker-compose.yml up -d
```

### 2. 使用 Maven 建置

#### 建置所有模組
```bash
cd servers/java/
mvn clean install
```

#### 執行 PostgreSQL MCP Server
```bash
cd servers/java/mcp-postgresql-server
mvn spring-boot:run
```

#### 執行 MySQL MCP Server
```bash
cd servers/java/mcp-mysql-server
mvn spring-boot:run
```

#### 執行 ContextCore MCP Server
```bash
# 確保 Qdrant 和 Ollama 已啟動
cd servers/java/mcp-contextcore-server
mvn spring-boot:run
```

### 3. Python 原型（僅供學習）

```bash
cd servers/python/

# 安裝依賴
pip install -r requirements.txt

# 啟動 Python 版本 ContextCore
python mcp_server.py
```

## ⚙️ 配置

### PostgreSQL MCP Server 配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: password
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

mcp:
  security:
    readonly-mode: false
    allowed-operations: SELECT,INSERT,UPDATE,DELETE
    blocked-keywords: DROP,TRUNCATE,ALTER
```

### ContextCore MCP Server 配置

```yaml
# application.yml
contextcore:
  storage:
    sqlite:
      path: ./data/logs.db
    qdrant:
      host: localhost
      port: 6333
      collection: dev_logs
  embedding:
    ollama:
      base-url: http://localhost:11434
      model: nomic-embed-text
      embedding-dim: 768
```

### 環境變數

```bash
# PostgreSQL MCP
export POSTGRES_URL=jdbc:postgresql://localhost:5432/mydb
export POSTGRES_USER=user
export POSTGRES_PASSWORD=password

# MySQL MCP
export MYSQL_URL=jdbc:mysql://localhost:3306/mydb
export MYSQL_USER=user
export MYSQL_PASSWORD=password

# ContextCore
export QDRANT_HOST=localhost
export QDRANT_PORT=6333
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=nomic-embed-text
```

## 🛠️ MCP Tools

### PostgreSQL & MySQL MCP Tools

#### 連線管理
- `add_connection` - 建立資料庫連線
- `test_connection` - 測試連線狀態
- `list_connections` - 列出所有連線
- `remove_connection` - 移除連線

#### 查詢執行
- `execute_query` - 執行 SELECT 查詢
- `execute_update` - 執行 INSERT/UPDATE/DELETE
- `execute_transaction` - 事務執行
- `batch_execute` - 批次操作

#### Schema 管理
- `get_table_schema` - 獲取表結構
- `list_tables` - 列出所有表
- `list_columns` - 列出表的所有欄位
- `explain_query` - 查詢執行計畫

### ContextCore MCP Tools

#### 日誌管理
- `add_log` - 新增開發日誌
  ```json
  {
    "project": "my-app",
    "content": "實現了 JWT 登入功能，使用 Spring Security...",
    "tags": ["authentication", "jwt", "security"],
    "created_at": "2024-01-15T10:30:00Z"
  }
  ```

- `get_log` - 根據 ID 獲取完整日誌
- `list_log_summaries` - 列出所有日誌摘要

#### 智能搜尋
- `search_logs` - 語義搜尋相關日誌
  ```json
  {
    "query": "如何處理使用者登入",
    "project": "my-app",
    "tags": ["authentication"],
    "top_k": 5
  }
  ```
  返回與查詢最相關的日誌（基於向量相似度）

- `get_project_context` - 獲取專案關鍵上下文
  ```json
  {
    "project": "my-app",
    "max_logs": 10
  }
  ```
  返回專案最重要的決策和實現記錄

## 🔍 使用範例

### PostgreSQL MCP: 建立連線並查詢

```java
// 1. 建立連線
ConnectionRequest connReq = ConnectionRequest.builder()
    .connectionId("main_db")
    .host("localhost")
    .port(5432)
    .database("myapp")
    .username("myuser")
    .password("mypassword")
    .build();

connectionController.addConnection(connReq);

// 2. 執行查詢
QueryRequest queryReq = QueryRequest.builder()
    .connectionId("main_db")
    .query("SELECT * FROM users WHERE created_at > ?")
    .params(List.of("2024-01-01"))
    .build();

QueryResult result = queryController.executeQuery(queryReq);
```

### ContextCore MCP: 新增日誌並搜尋

```java
// 1. 新增開發日誌
AddLogRequest addReq = new AddLogRequest(
    "my-app",
    "實現了 Redis 快取機制，顯著提升查詢效能...",
    List.of("cache", "redis", "performance"),
    LocalDateTime.now()
);

logController.addLog(addReq);

// 2. 語義搜尋相關日誌
SearchLogsRequest searchReq = new SearchLogsRequest(
    "如何提升資料庫查詢效能",
    "my-app",
    List.of("performance"),
    5  // 返回前 5 個最相關的日誌
);

List<LogSearchResult> results = logController.searchLogs(searchReq);
// 返回: ["Redis 快取實現" (0.92), "資料庫索引優化" (0.87), ...]
```

## 🧪 測試

### 單元測試
```bash
# 執行所有單元測試
cd servers/java/
mvn test

# 執行特定模組測試
cd servers/java/mcp-contextcore-server
mvn test
```

### 整合測試（使用 TestContainers）
```bash
# 執行整合測試（會自動啟動 Docker 容器）
mvn integration-test

# 執行 BDD 測試
mvn test -Dtest="*BDDTest"
```

### 手動測試工具

#### PostgreSQL MCP
```bash
cd deployment/test-postgres-mcp/
docker-compose -f docker-compose.test.yml up -d
```

#### MySQL MCP
```bash
cd deployment/test-mysql-mcp/
docker-compose -f docker-compose.test.yml up -d
```

#### ContextCore MCP
```bash
cd servers/java/mcp-contextcore-server/
./test-mcp-tools.sh
```

## 🐳 Docker 部署

### 完整部署（所有服務）

```bash
cd deployment/
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f postgresql-mcp-server
docker-compose logs -f mysql-mcp-server
docker-compose logs -f contextcore-mcp-server
```

### 使用 Jib 建置映像

```bash
# PostgreSQL MCP Server
cd servers/java/mcp-postgresql-server
mvn jib:dockerBuild

# MySQL MCP Server
cd servers/java/mcp-mysql-server
mvn jib:dockerBuild

# ContextCore MCP Server
cd servers/java/mcp-contextcore-server
mvn jib:dockerBuild
```

## 📊 監控

### 健康檢查
```bash
# PostgreSQL MCP
curl http://localhost:8080/actuator/health

# MySQL MCP
curl http://localhost:8081/actuator/health

# ContextCore MCP
curl http://localhost:8082/actuator/health
```

### 指標查詢
```bash
curl http://localhost:8080/actuator/metrics
```

### 日誌查看
```bash
# Docker 環境
docker-compose logs -f [service-name]

# 本地環境
tail -f servers/java/[server-name]/logs/application.log
```

## 🛡️ 安全最佳實務

### 資料庫 MCP Server
1. **永遠使用參數化查詢**
   ```java
   // ✅ 正確
   query("SELECT * FROM users WHERE id = ?", List.of(userId))

   // ❌ 錯誤（SQL Injection 風險）
   query("SELECT * FROM users WHERE id = " + userId)
   ```

2. **啟用唯讀模式**（生產環境）
   ```yaml
   mcp:
     security:
       readonly-mode: true
   ```

3. **限制允許的操作**
   ```yaml
   mcp:
     security:
       allowed-operations: SELECT
       blocked-keywords: DROP,TRUNCATE,DELETE,ALTER
   ```

### ContextCore MCP Server
1. **本地部署 Ollama**（不傳送資料到雲端）
2. **定期備份 SQLite 和 Qdrant 資料**
3. **設定適當的檔案權限**

## 🔄 架構設計

### Clean Architecture + DDD

```
┌─────────────────────────────────────────┐
│          MCP Controller Layer           │
│    (@McpTool, @McpResource)            │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│          Use Case Layer                 │
│    (AddLogUseCase, SearchLogsUseCase)  │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│          Domain Layer                   │
│    (Log, LogSearchResult)              │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│       Infrastructure Layer              │
│  (SqliteRepo, QdrantRepo, OllamaService)│
└─────────────────────────────────────────┘
```

### ContextCore 向量搜尋流程

```
使用者查詢: "如何實現登入功能"
    ↓
1. Ollama: 文字 → 向量 [0.12, -0.34, 0.56, ...]
    ↓
2. Qdrant: 向量相似度搜尋 (Cosine Similarity)
    ↓
3. 返回 Top-K 最相關的日誌 ID + 相似度分數
    ↓
4. SQLite: 根據 ID 批次查詢完整內容
    ↓
5. 返回排序結果: [
     {"id": "123", "content": "JWT 登入實現...", "similarity": 0.92},
     {"id": "456", "content": "OAuth2 整合...", "similarity": 0.87},
     ...
   ]
```

## 📚 完整文檔

詳細文檔請參閱 [文檔中心](documentation/README.md)。

### 快速連結
- [快速開始指南](documentation/guides/GETTING_STARTED.md)
- [ContextCore MCP 使用指南](documentation/mcp-servers/contextcore-mcp.md)
- [PostgreSQL MCP 使用指南](documentation/mcp-servers/postgresql-mcp.md)
- [MySQL MCP 使用指南](documentation/mcp-servers/mysql-mcp.md)
- [常見問題](documentation/guides/FAQ.md)

## 🎯 專案特色

### 為什麼選擇 MCP Registry？

#### 1. 企業級可靠性
- ✅ Clean Architecture 設計，易於維護和擴展
- ✅ 完整的單元測試和整合測試
- ✅ Spring Boot 生態系統支援

#### 2. 安全性第一
- ✅ 強制參數化查詢，防止 SQL Injection
- ✅ 敏感資訊自動脫敏
- ✅ 本地部署，資料不離開本機

#### 3. 智能開發日誌管理
- ✅ 解決 Context 過載問題（向量搜尋 vs 全量載入）
- ✅ 毫秒級語義搜尋
- ✅ 多維度過濾（專案、標籤、時間）

#### 4. 高效能
- ✅ HikariCP 高效能連線池
- ✅ Qdrant 向量資料庫（毫秒級）
- ✅ 批次操作優化

#### 5. 完全開源
- ✅ MIT 授權
- ✅ 歡迎貢獻
- ✅ 持續維護

## 🚧 未來規劃

### Phase 1: 效能優化（Q1 2025）
- [ ] R2DBC 反應式實現
- [ ] 查詢結果快取（Redis）
- [ ] 流式查詢支援

### Phase 2: 智能化增強（Q2 2025）
- [ ] 查詢優化建議（基於執行計畫）
- [ ] 自動索引建議
- [ ] 異常查詢檢測

### Phase 3: 資料遷移工具（Q3 2025）
- [ ] MySQL ↔ PostgreSQL 自動轉換
- [ ] Schema 差異分析
- [ ] 增量同步支援

### Phase 4: ContextCore 進階功能（Q4 2025）
- [ ] 日誌版本控制
- [ ] 自動摘要生成（LLM）
- [ ] 知識圖譜構建
- [ ] 多專案關聯分析

## 📄 授權

此專案使用 MIT 授權 - 詳見 [LICENSE](LICENSE) 檔案

## 🤝 貢獻

歡迎貢獻！請參閱 [CONTRIBUTING.md](CONTRIBUTING.md) 了解詳情。

### 如何貢獻
1. Fork 此專案
2. 建立您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的變更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 💬 支援與聯繫

- 📧 Email: a910413frank@gmail.com
- 🐛 Issues: [GitHub Issues](../../issues)
- 💬 Discussions: [GitHub Discussions](../../discussions)
- 📖 Documentation: [文檔中心](documentation/README.md)

## 🌟 Star History

如果這個專案對您有幫助，請給我們一個 ⭐！

---

**注意**: 這是一個純工具層的 MCP Server 集合，設計用於與 LLM 配合使用。請確保在生產環境中正確配置安全設定，特別是資料庫連線和向量搜尋服務。
