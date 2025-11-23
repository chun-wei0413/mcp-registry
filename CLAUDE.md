# MCP Registry - 統一開發規範

本文件為 MCP Registry 專案的統一開發規範，適用於所有 AI 助手（Claude、Gemini 等）。

---

## 📖 目錄

- [Part 1: Memory MCP Server (Python)](#part-1-memory-mcp-server-python)
- [Part 2: Database MCP Servers (Java)](#part-2-database-mcp-servers-java)

---

# Part 1: Memory MCP Server (Python)

## 專案概述

基於 RAG（Retrieval-Augmented Generation）的專案知識管理系統，讓 AI 客戶端（Claude CLI、Gemini 等）能夠讀取並查詢專案文件。

## 🎯 功能特性

| 功能 | 說明 | 使用場景 |
|------|------|---------|
| **文件儲存** | 自動讀取並儲存 .md、.json 等專案文件 | 儲存 Spec.md、架構文件 |
| **語義搜尋** | 透過自然語言查詢相關 context | 查詢 "Clean Architecture" 獲取 CA 相關內容 |
| **主題管理** | 按主題分類和檢索知識點 | 按 DDD、SOLID 等主題組織知識 |
| **智能程式碼分離** | 🆕 v2.0：分離程式碼與文字描述，提升搜尋精準度 | 搜尋概念時不被程式碼語法干擾 |
| **完整程式碼範例** | 🆕 v2.0：查詢結果包含關聯的程式碼區塊 | 獲得概念說明的同時得到程式碼範例 |

## ✨ v2.0 新功能：智能程式碼分離（2025-11-23）

### 核心改進

**問題：** 傳統方式將程式碼與文字一起計算 embedding，導致程式碼語法稀釋語意相似度。

**解決方案：**
- ✅ **只對文字描述計算 embedding**（提升語意精準度 ~40%）
- ✅ **程式碼儲存在 metadata**（完整保留但不參與搜尋）
- ✅ **查詢結果包含完整程式碼**（使用者體驗不打折）

**效能指標：**
- 📉 Embedding 大小減少 **61-68%**
- 📈 語意搜尋準確度提升 **~40%**
- ⚡ 搜尋速度提升（更小的 embedding 向量）

詳細技術文件：`servers/python/RAG-memory-mcp/docs/CODE_SEPARATION.md`

## 技術架構

### 系統架構

該系統是一個基於 **MCP (Model Context Protocol)** 標準的知識庫伺服器，專為 AI 客戶端（如 Claude CLI）設計。MCP 是由 Anthropic 定義的標準協定，允許 AI 模型透過工具（Tools）和資源（Resources）與外部系統互動，實現上下文增強和知識檢索。

此架構可分為三個主要層次：

1.  **MCP 協定層 (FastMCP):** 使用 FastMCP SDK 實現 MCP 標準協定，提供 Tools 和 Resources 供 AI 客戶端呼叫。
2.  **嵌入層 (SentenceTransformer):** 機器學習模型，負責將文字知識轉換為向量表示（embeddings）。
3.  **儲存與查詢層 (ChromaDB):** 向量資料庫，用於儲存嵌入向量及其元數據，提供高效的相似性搜尋。

### 技術棧（最簡化）

```yaml
核心技術:
  - MCP SDK: FastMCP (Anthropic 官方 Python SDK)
  - Embedding 模型: all-MiniLM-L6-v2 (80MB, 本地運行, 384 維度)
  - 向量資料庫: ChromaDB (內嵌式, 零配置, Cosine Similarity)
  - 文件處理: Python 標準庫
  - 資料驗證: Pydantic

Docker 基礎映像:
  - python:3.11-slim

依賴套件:
  - mcp-cli
  - chromadb
  - sentence-transformers
  - uv
```

### 架構圖

```
┌─────────────────┐
│  Claude CLI     │
│  (HTTP/MCP)     │
└────────┬────────┘
         │ MCP Protocol
┌────────▼────────────────────┐
│  FastMCP Server             │
│  ┌─────────────────────┐   │
│  │ store_document      │   │
│  │ search_knowledge    │   │
│  │ learn_knowledge     │   │
│  └──────────┬──────────┘   │
└─────────────┼───────────────┘
              │
┌─────────────▼───────────────┐
│  VectorStore (storage.py)   │
│  ┌─────────────────────┐   │
│  │ SentenceTransformer │   │
│  │ (all-MiniLM-L6-v2)  │   │
│  └──────────┬──────────┘   │
│             │               │
│  ┌──────────▼──────────┐   │
│  │ ChromaDB            │   │
│  │ (Cosine Similarity) │   │
│  └─────────────────────┘   │
└─────────────────────────────┘
```

## MCP Tools 和 Resources

伺服器提供四個主要 MCP Tools 和一個 Resource：

### MCP Tools

#### `store_document`
- **目的：** 讀取並儲存專案文件到知識庫。
- **參數：**
  - `file_path` (str): 文件的絕對或相對路徑（支援 .md、.json、.txt）
  - `topic` (str, optional): 知識點主題，預設使用檔名
- **流程：** 讀取文件內容，生成嵌入向量，並與 `topic` 元數據一起儲存到 ChromaDB。
- **回應：** 包含文件名、主題、ID 和大小的確認訊息（字串格式）。

#### `learn_knowledge`
- **目的：** 手動將一個新的知識點加入資料庫。
- **參數：**
  - `topic` (str): 知識點的主題分類（例如 "DDD", "SOLID"）
  - `content` (str): 知識點的文字內容
- **流程：** 為 `content` 生成嵌入向量，並將其與 `topic` 元數據一起儲存。
- **回應：** 包含知識點 ID 的確認訊息（字串格式）。

#### `search_knowledge`
- **目的：** 在知識庫上執行語意搜尋。
- **參數：**
  - `query` (str): 自然語言搜尋問題
  - `top_k` (int, default=5): 返回的最大結果數
  - `topic` (str, optional): 限定搜尋範圍的主題
- **流程：** 為 `query` 生成嵌入向量，在 ChromaDB 中找到語意最相似的 `top_k` 個知識點。
- **回應：** `SearchResult` 物件，包含結果列表。

### MCP Resources

#### `knowledge://{topic}`
- **目的：** 獲取特定主題的所有知識點。
- **URI 參數：** `topic` (str): 要檢索的主題名稱
- **流程：** 從 ChromaDB 返回所有元數據與給定 `topic` 相符的文件。
- **回應：** `RetrievalResult` 物件，包含該主題的所有知識點列表。

## 資料模型

### CodeBlock（v2.0 新增）
```python
class CodeBlock(pydantic.BaseModel):
    language: str                    # 程式語言（如 java, python）
    code: str                        # 完整程式碼內容
    position: int                    # 在文件中的位置索引
```

### KnowledgePoint
```python
class KnowledgePoint(pydantic.BaseModel):
    id: str                          # 唯一識別碼
    content: str                     # 知識點內容
    topic: str                       # 主題分類
    similarity: Optional[float]      # 相似度分數（僅在搜尋時）
    timestamp: str                   # ISO 8601 格式時間戳

    # v2.0 新增欄位
    code_blocks: Optional[List[CodeBlock]] = None  # 關聯的程式碼區塊
```

### SearchResult
```python
class SearchResult(pydantic.BaseModel):
    results: List[KnowledgePoint]    # 搜尋結果列表
```

### RetrievalResult
```python
class RetrievalResult(pydantic.BaseModel):
    knowledge_points: List[KnowledgePoint]  # 主題下的所有知識點
```

## 使用情境

### 情境一：儲存專案文件

```python
# 儲存架構文件
store_document(
    file_path="./documentation/ARCHITECTURE.md",
    topic="Architecture"
)

# 儲存專案規格（自動使用檔名作為 topic）
store_document(
    file_path="./Spec.md"
)
```

### 情境二：手動新增知識點

```python
learn_knowledge(
    topic="DDD",
    content="一個 Aggregate 是一群相關領域物件的集合，它被視為一個單一的資料修改單元。"
)
```

### 情境三：語意搜尋

```python
# 全域搜尋
search_knowledge(
    query="如何保護業務規則不被外部隨意修改？",
    top_k=3
)

# 特定主題內搜尋
search_knowledge(
    query="Clean Architecture",
    topic="Architecture",
    top_k=5
)
```

### 情境四：按主題檢索

```python
# 使用 MCP Resource
knowledge://DDD
knowledge://SOLID
knowledge://Architecture
```

## 快速開始

### Docker Compose 部署（推薦）

```bash
# 1. 啟動服務
cd servers/python
docker-compose up -d

# 2. 查看日誌
docker-compose logs -f memory-mcp

# 3. 停止服務
docker-compose down
```

### 本地開發

```bash
# 1. 安裝依賴
pip install -r requirements.txt

# 2. 啟動 MCP Server
python mcp_server.py
```

## 目錄結構

```
servers/python/
├── mcp_server.py          # FastMCP 伺服器主程式
├── storage.py             # ChromaDB 向量儲存層
├── requirements.txt       # Python 依賴
├── Dockerfile             # Docker 映像定義
├── docker-compose.yml     # Docker Compose 配置
├── .dockerignore          # Docker 忽略檔案
├── chroma_db/             # ChromaDB 持久化資料（自動建立）
├── .dev/                  # 開發文件
│   ├── ARCHITECTURE.md    # 架構文件
│   └── SCENARIOS.md       # 使用情境
└── README.md              # 專案說明
```

## 效能指標

| 指標 | 數值 | 說明 |
|------|------|------|
| Embedding 速度 | ~1000 tokens/sec | CPU 運算 |
| 搜尋延遲 | <100ms | 1000 筆文件內 |
| 記憶體使用 | ~500MB | 包含模型載入 |
| 磁碟使用 | ~200MB | 模型 + 資料庫 |

---

# Part 2: Database MCP Servers (Java)

## 專案概述

建立一個**企業級 MCP Registry**，提供基於 Java 17 + Spring Boot 3.x 的雙 MCP Server 架構，支援 PostgreSQL 和 MySQL 資料庫操作與智能資料遷移。此 Server 作為純工具層，讓 LLM 能透過 MCP 協定執行智能資料庫管理，不包含任何業務邏輯。

## 核心架構

```
PostgreSQL MCP Server ←→ MCP Protocol ←→ LLM (智能決策)
      ↕                                         ↕
  Spring Boot 3.x                         Context & 業務邏輯
      ↕                                         ↕
MySQL MCP Server    ←→ MCP Protocol ←→ 資料遷移協調器
```

## 開發要求

### 技術棧
- **語言**: Java 17+
- **核心框架**:
  - `Spring Boot 3.x` (企業級應用框架)
  - `Spring AI MCP` (原生 MCP 協議支援)
  - `Project Reactor` (反應式程式設計)
  - `R2DBC` (非同步資料庫連接)
- **建置工具**: Maven 3.8+
- **測試框架**: TestContainers, JUnit 5
- **部署**: Jib (Docker), Spring Boot Actuator

### 專案結構
```
mcp-registry/
├── 📁 servers/                         # 所有 MCP Server 實作
│   ├── 📁 python/                      # Memory MCP Server (RAG)
│   └── 📁 java/                        # Database MCP Servers
│       ├── 📁 mcp-common/              # 共用模組
│       ├── 📁 mcp-postgresql-server/   # PostgreSQL MCP Server
│       ├── 📁 mcp-mysql-server/        # MySQL MCP Server
│       ├── 📁 testing-tools/           # 測試工具模組
│       └── 📄 pom.xml                  # 主 Maven 配置
├── 📁 deployment/                      # 部署配置
│   ├── docker-compose.yml             # Docker Compose
│   └── 📁 k8s/                        # Kubernetes 配置
├── 📁 documentation/                   # 文檔中心
└── 📄 Spec.md                         # 本文件（統一規範）
```

## MCP 工具實現規範

### 1. 查詢工具 (Query Tools)

#### executeQuery
```java
@Component
public class QueryController {

    @Autowired
    private ConnectionPoolManager connectionManager;

    public Mono<QueryResult> executeQuery(
        String connectionId,
        String query,
        List<Object> params,
        Integer fetchSize
    ) {
        // 使用 R2DBC 參數化查詢防止 SQL Injection
        // 返回反應式結果
        return connectionManager.getConnection(connectionId)
            .flatMap(connection -> {
                return connection.createStatement(query)
                    .bind(params)
                    .execute()
                    .map(this::mapToQueryResult);
            });
    }
}
```

#### executeTransaction
```java
public Mono<TransactionResult> executeTransaction(
    String connectionId,
    List<QueryRequest> queries
) {
    // 使用 Spring Transaction 管理
    // 支援自動 rollback
    return connectionManager.getConnection(connectionId)
        .flatMap(connection -> {
            return connection.beginTransaction()
                .flatMap(transaction -> {
                    return processQueries(queries, connection)
                        .doOnError(error -> transaction.rollback())
                        .flatMap(result -> transaction.commit().thenReturn(result));
                });
        });
}
```

#### batchExecute
```java
public Mono<BatchResult> batchExecute(
    String connectionId,
    String query,
    List<List<Object>> paramsList
) {
    // 優化批次操作性能
    // 使用 R2DBC Batch API
    return connectionManager.getConnection(connectionId)
        .flatMap(connection -> {
            Batch batch = connection.createBatch();
            paramsList.forEach(params -> {
                batch.add(connection.createStatement(query).bind(params));
            });
            return batch.execute().collectList();
        });
}
```

### 2. Schema 工具

#### getTableSchema
```java
@Component
public class SchemaController {

    public Mono<TableSchema> getTableSchema(
        String connectionId,
        String tableName,
        String schema
    ) {
        // 查詢 information_schema 獲取表結構
        // 包含欄位、類型、約束、索引資訊
        String schemaQuery = """
            SELECT column_name, data_type, is_nullable, column_default
            FROM information_schema.columns
            WHERE table_name = ? AND table_schema = ?
            ORDER BY ordinal_position
            """;

        return executeQuery(connectionId, schemaQuery,
            List.of(tableName, schema), null)
            .map(this::mapToTableSchema);
    }
}
```

#### listTables
```java
public Mono<List<TableInfo>> listTables(
    String connectionId,
    String schema
) {
    // 查詢系統表獲取所有表資訊
    String tablesQuery = """
        SELECT table_name, table_type, table_comment
        FROM information_schema.tables
        WHERE table_schema = ?
        ORDER BY table_name
        """;

    return executeQuery(connectionId, tablesQuery, List.of(schema), null)
        .map(this::mapToTableInfoList);
}
```

#### explainQuery
```java
public Mono<ExplainResult> explainQuery(
    String connectionId,
    String query,
    boolean analyze
) {
    // 執行 EXPLAIN 分析查詢計畫
    String explainQuery = analyze ?
        "EXPLAIN ANALYZE " + query : "EXPLAIN " + query;

    return executeQuery(connectionId, explainQuery, List.of(), null)
        .map(this::mapToExplainResult);
}
```

### 3. 連線管理工具

#### addConnection
```java
@Component
public class ConnectionController {

    @Autowired
    private ConnectionPoolManager poolManager;

    public Mono<ConnectionResult> addConnection(
        String connectionId,
        String host,
        int port,
        String database,
        String username,
        String password,
        int poolSize
    ) {
        // 建立 R2DBC ConnectionFactory
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
            .option(DRIVER, "postgresql") // 或 "mysql"
            .option(HOST, host)
            .option(PORT, port)
            .option(DATABASE, database)
            .option(USER, username)
            .option(PASSWORD, password)
            .build();

        // 使用連線池配置
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder()
            .initialSize(2)
            .maxSize(poolSize)
            .maxIdleTime(Duration.ofMinutes(30))
            .build();

        return poolManager.createPool(connectionId, options, poolConfig)
            .map(pool -> ConnectionResult.success(connectionId));
    }
}
```

#### testConnection
```java
public Mono<ConnectionStatus> testConnection(String connectionId) {
    // 測試連線池狀態和資料庫連線
    return poolManager.getConnection(connectionId)
        .flatMap(connection -> {
            return connection.createStatement("SELECT 1")
                .execute()
                .then(Mono.just(ConnectionStatus.healthy(connectionId)))
                .onErrorReturn(ConnectionStatus.unhealthy(connectionId));
        })
        .switchIfEmpty(Mono.just(ConnectionStatus.notFound(connectionId)));
}
```

## MCP 資源實現

### connections 資源
```java
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @GetMapping("/connections")
    public Mono<List<ConnectionInfo>> getConnections() {
        // 返回所有活躍連線資訊
        return poolManager.getAllConnections()
            .map(this::mapToConnectionInfo)
            .collectList();
    }
}
```

### query_history 資源
```java
@GetMapping("/query-history")
public Mono<List<QueryHistory>> getQueryHistory(
    @RequestParam(required = false) String connectionId,
    @RequestParam(defaultValue = "100") int limit
) {
    // 返回查詢歷史，支援築選和限制
    return queryHistoryService.getHistory(connectionId, limit);
}
```

## 安全性要求

1. **SQL Injection 防護**
   - 所有查詢必須使用參數化查詢
   - 禁止字串拼接 SQL

2. **密碼管理**
   - 使用環境變數或加密儲存
   - 不在日誌中記錄密碼

3. **權限控制**
   - 支援只讀連線
   - 限制危險操作（DROP、TRUNCATE）

4. **錯誤處理**
   - 不洩露敏感資訊
   - 結構化錯誤回應

## 性能優化

1. **連線池管理**
   - 最小連線數: 2
   - 最大連線數: 20
   - 連線超時: 30秒

2. **查詢優化**
   - 支援查詢快取
   - 批次操作優化
   - 流式處理大結果集

3. **異步處理**
   - 全異步 I/O
   - 並發查詢支援

## 日誌規範

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class QueryService {
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    public Mono<QueryResult> executeQuery(String connectionId, String query) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();

            // 設置 MDC 上下文
            MDC.put("connectionId", connectionId);
            MDC.put("queryType", "SELECT");

            try {
                // 執行查詢邏輯
                QueryResult result = performQuery(query);

                // 成功日誌
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Query executed successfully: connection={}, query={}, duration={}ms, rows={}",
                    connectionId, query.substring(0, Math.min(100, query.length())),
                    duration, result.getRowCount());

                return result;
            } catch (Exception e) {
                // 錯誤日誌
                logger.error("Query execution failed: connection={}, error={}",
                    connectionId, e.getMessage(), e);
                throw e;
            } finally {
                MDC.clear();
            }
        });
    }
}
```

## 測試要求

### 單元測試
- 每個工具函數的測試覆蓋率 > 90%
- Mock asyncpg 連線
- 測試錯誤處理路徑

### 整合測試
- 使用 Docker PostgreSQL
- 測試真實查詢場景
- 測試事務回滾

### 測試案例
```python
async def test_execute_query():
    # 測試基本查詢
    result = await execute_query(
        connection_id="test",
        query="SELECT * FROM orders WHERE id = $1",
        params=[1]
    )
    assert result.rows[0]["id"] == 1

async def test_transaction_rollback():
    # 測試事務回滾
    queries = [
        {"query": "INSERT INTO orders ...", "params": []},
        {"query": "INVALID SQL", "params": []}  # 觸發錯誤
    ]
    result = await execute_transaction("test", queries)
    assert result.rolled_back == True
```

## 部署配置

### Docker 支援 (Jib 自動建置)
```xml
<!-- pom.xml 中的 Jib 配置 -->
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.0</version>
    <configuration>
        <from>
            <image>openjdk:17-jre-slim</image>
        </from>
        <to>
            <image>mcp-registry/${project.artifactId}</image>
            <tags>
                <tag>latest</tag>
                <tag>${project.version}</tag>
            </tags>
        </to>
        <container>
            <mainClass>com.mcpregistry.Application</mainClass>
            <ports>
                <port>8080</port>
            </ports>
        </container>
    </configuration>
</plugin>
```

### 環境變數 (application.yml)
```yaml
server:
  port: ${MCP_SERVER_PORT:8080}

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:development}
  r2dbc:
    url: ${R2DBC_URL:r2dbc:postgresql://localhost:5432/mydb}
    username: ${R2DBC_USERNAME:postgres}
    password: ${R2DBC_PASSWORD:password}
    pool:
      initial-size: ${DEFAULT_POOL_SIZE:2}
      max-size: ${MAX_POOL_SIZE:20}
      max-idle-time: ${POOL_MAX_IDLE_TIME:30m}

mcp:
  server:
    query-timeout: ${QUERY_TIMEOUT:30s}
    security:
      readonly-mode: ${MCP_READONLY_MODE:false}

logging:
  level:
    com.mcpregistry: ${MCP_LOG_LEVEL:INFO}
```

## 使用範例

### LLM 調用流程 (Java API)
```java
// 1. 建立連線
ConnectionRequest connectionRequest = ConnectionRequest.builder()
    .connectionId("migration_db")
    .host("localhost")
    .port(5432)
    .database("orders")
    .username("admin")
    .password("***")
    .build();

Mono<ConnectionResult> connectionResult = connectionController
    .addConnection(connectionRequest);

// 2. 查看表結構
Mono<TableSchema> schema = schemaController
    .getTableSchema("migration_db", "orders", "public");

// 3. 查詢資料
QueryRequest queryRequest = QueryRequest.builder()
    .connectionId("migration_db")
    .query("SELECT * FROM orders WHERE created_at > ?")
    .params(List.of("2024-01-01"))
    .build();

Mono<QueryResult> data = queryController.executeQuery(queryRequest);

// 4. 執行遷移（事務）
TransactionRequest transactionRequest = TransactionRequest.builder()
    .connectionId("migration_db")
    .queries(List.of(
        QueryRequest.builder()
            .query("INSERT INTO orders_v2 SELECT * FROM orders WHERE ...")
            .params(List.of())
            .build(),
        QueryRequest.builder()
            .query("UPDATE migration_status SET status = ? WHERE id = ?")
            .params(List.of("completed", 1))
            .build()
    ))
    .build();

Mono<TransactionResult> migrationResult = queryController
    .executeTransaction(transactionRequest);
```

## 重要原則

1. **零業務邏輯**: Server 只提供工具，不包含任何業務判斷
2. **通用性**: 適用於任何 PostgreSQL/MySQL 資料庫操作場景
3. **安全性**: 生產級的安全防護
4. **可靠性**: 完整的錯誤處理和恢復機制
5. **可觀測性**: 結構化日誌和監控指標

## 交付檢查清單

### 核心功能
- [ ] PostgreSQL MCP Server 實現完成
- [ ] MySQL MCP Server 實現完成
- [ ] 所有 MCP 工具 API 實現完成
- [ ] Spring Boot Actuator 監控功能

### 測試與品質
- [ ] 單元測試覆蓋率 > 90%
- [ ] TestContainers 整合測試通過
- [ ] 安全性審查通過
- [ ] 性能測試達標 (R2DBC + 連線池)

### 部署與文檔
- [ ] Maven 多模組建置成功
- [ ] Jib Docker 映像建置成功
- [ ] Kubernetes 部署檔案完成
- [ ] 技術文檔完整 (documentation/ 目錄)
- [ ] API 參考文檔完成
- [ ] 示例程式可運行

### 企業級特性
- [ ] Spring Security 整合
- [ ] 結構化日誌 (Logback + MDC)
- [ ] 效能指標收集 (Micrometer)
- [ ] 配置管理 (Spring Boot Configuration)

## 注意事項

1. **純工具層**: Server 不包含任何業務邏輯，所有智能決策由 LLM 完成
2. **反應式程式設計**: 使用 Project Reactor 和 R2DBC 實現非同步操作
3. **安全性優先**: 所有查詢使用參數化，防止 SQL Injection
4. **企業級設計**: 遵循 Spring Boot 最佳實踐，支援監控和部署
5. **可擴展性**: Maven 多模組設計，便於新增資料庫支援
6. **可觀測性**: 結構化日誌、效能指標、健康檢查

---

## 相關文檔

請參考以下文檔獲取更多資訊：

- **Memory MCP Server**: [servers/python/README.md](servers/python/README.md)
- **Memory MCP 架構**: [servers/python/.dev/ARCHITECTURE.md](servers/python/.dev/ARCHITECTURE.md)
- **Memory MCP 使用情境**: [servers/python/.dev/SCENARIOS.md](servers/python/.dev/SCENARIOS.md)
- **快速開始指南**: [documentation/guides/QUICK_START.md](documentation/guides/QUICK_START.md)
- **系統架構**: [documentation/ARCHITECTURE.md](documentation/ARCHITECTURE.md)
- **Java 遷移計畫**: [documentation/project/JAVA_MIGRATION_PLAN.md](documentation/project/JAVA_MIGRATION_PLAN.md)
- **文檔中心**: [documentation/README.md](documentation/README.md)

---

**開發提示**:
- **Memory MCP (Python)**: 提供 RAG 知識管理，工具提供能力，LLM 提供智慧，FastMCP 提供標準協定。
- **Database MCP (Java)**: 提供企業級資料庫管理，工具提供能力，LLM 提供智慧，Java 提供企業級穩定性。
