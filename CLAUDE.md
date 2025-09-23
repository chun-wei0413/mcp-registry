# MCP Registry Java Edition - Claude Code 開發指南

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
├── 📁 mcp-registry-java/              # Java 主專案目錄
│   ├── 📁 mcp-common/                  # 共用模組
│   │   ├── src/main/java/             # 共用程式碼
│   │   │   ├── models/                # 資料模型
│   │   │   ├── validators/            # 安全驗證
│   │   │   └── exceptions/            # 例外處理
│   │   └── pom.xml                    # Maven 配置
│   ├── 📁 mcp-postgresql-server/       # PostgreSQL MCP Server
│   │   ├── src/main/java/             # PostgreSQL 服務實現
│   │   │   ├── controllers/           # MCP 工具控制器
│   │   │   ├── services/              # 業務服務層
│   │   │   └── config/                # 配置管理
│   │   └── pom.xml                    # Maven 配置
│   ├── 📁 mcp-mysql-server/            # MySQL MCP Server
│   │   ├── src/main/java/             # MySQL 服務實現
│   │   │   ├── controllers/           # MCP 工具控制器
│   │   │   ├── services/              # 業務服務層
│   │   │   └── config/                # 配置管理
│   │   └── pom.xml                    # Maven 配置
│   ├── 📁 testing-tools/               # 測試工具模組
│   │   ├── src/main/java/             # 測試工具實現
│   │   └── pom.xml                    # Maven 配置
│   └── 📄 pom.xml                     # 主 Maven 配置
├── 📁 deployment/                      # 部署配置
│   ├── docker-compose.yml             # Docker Compose
│   └── 📁 k8s/                        # Kubernetes 配置
├── 📁 documentation/                   # 文檔中心
│   ├── 📁 guides/                     # 使用指南
│   ├── 📁 project/                    # 專案資訊
│   ├── 📁 release-notes/              # 版本說明
│   └── 📁 examples/                   # 程式範例
├── 📁 scripts/                        # 管理腳本
│   ├── start-all.sh                   # 統一管理腳本
│   ├── run_mysql_mcp.py               # MySQL MCP 執行腳本
│   └── run_postgres_mcp.py            # PostgreSQL MCP 執行腳本
└── 📄 README.md                       # 主專案說明
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
2. **通用性**: 適用於任何 PostgreSQL 資料庫操作場景
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

## 相關文檔

請參考以下文檔獲取更多資訊：

- [**快速開始指南**](documentation/guides/QUICK_START.md) - 5分鐘內啟動 Java MCP Server
- [**系統架構**](documentation/ARCHITECTURE.md) - 技術架構和設計原則
- [**Java 遷移計畫**](documentation/project/JAVA_MIGRATION_PLAN.md) - 從 Python 到 Java 的遷移
- [**文檔中心**](documentation/README.md) - 完整文檔導覽

---

**開發提示**: 這是一個企業級 Java MCP Registry，你的任務是實現高品質、可擴展的資料庫 MCP Server，讓 LLM 能夠透過這些企業級工具執行複雜的資料管理和遷移任務。記住：**工具提供能力，LLM 提供智慧，Java 提供企業級穩定性**。