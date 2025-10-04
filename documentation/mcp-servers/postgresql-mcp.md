# PostgreSQL MCP Server - 企業級資料庫管理工具

## 專案概述

PostgreSQL MCP Server 是一個基於 MCP (Model Context Protocol) 的智能資料庫管理工具，專為 LLM 驅動的資料庫操作設計。它提供了一套完整的工具集，讓 AI 能夠安全、高效地執行 PostgreSQL 資料庫的查詢、分析和管理任務。

## 要解決的問題

### 核心痛點

在 LLM 驅動的資料庫操作場景中，開發者面臨以下挑戰：

1. **缺乏標準化介面**：
    - LLM 無法直接操作資料庫
    - 需要人工編寫 SQL 並執行
    - 無統一的工具協定
    - 難以整合到 AI 工作流

2. **安全性風險**：
    - SQL Injection 攻擊風險
    - 敏感資料洩露風險
    - 缺乏存取控制機制
    - 危險操作無法限制

3. **操作複雜度**：
    - 資料庫連線管理困難
    - Schema 探索耗時
    - 事務處理容易出錯
    - 批次操作效能低

4. **可觀測性不足**：
    - 缺乏查詢歷史追蹤
    - 無效能監控
    - 錯誤處理不完善
    - 日誌記錄不結構化

### 解決方案

PostgreSQL MCP Server 透過 **MCP 協定標準化**，讓 LLM 能夠：
- ✅ 透過標準化的 MCP Tools 執行資料庫操作
- ✅ 使用參數化查詢防止 SQL Injection
- ✅ 智能管理連線池和事務
- ✅ 提供結構化的錯誤處理和日誌
- ✅ 支援 Schema 自動探索和查詢優化

## 系統架構

### 整體架構圖

```
┌─────────────────────────┐
│   LLM (Claude/GPT)     │  使用者介面
└───────────┬─────────────┘
            │ MCP Protocol
┌───────────▼──────────────────────────────┐
│         PostgreSQL MCP Server            │
│  ┌──────────────────────────────────┐   │
│  │ MCP Tools:                        │   │
│  │ - connection_management           │   │
│  │ - query_execution                 │   │
│  │ - schema_management               │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │ MCP Resources:                    │   │
│  │ - connections                     │   │
│  │ - healthy_connections             │   │
│  │ - connection_details              │   │
│  └──────────────────────────────────┘   │
└───────────┬──────────────────────────────┘
            │ JDBC / R2DBC
┌───────────▼──────────────┐
│   PostgreSQL Database    │
│   - 元數據存儲            │
│   - 業務資料              │
│   - 查詢執行引擎          │
└──────────────────────────┘
```

### Clean Architecture 分層

```
┌─────────────────────────────────────────────┐
│ Interface Adapters (MCP Tools)              │
│ - PostgresConnectionTool                     │
│ - PostgresQueryTool                          │
│ - PostgresSchemaTools                        │
├─────────────────────────────────────────────┤
│ Use Cases (Application Services)            │
│ - AddConnectionUseCase                       │
│ - ExecuteQueryUseCase                        │
│ - GetSchemaUseCase                           │
├─────────────────────────────────────────────┤
│ Domain Entities                              │
│ - DatabaseConnection                         │
│ - QueryRequest / QueryResult                 │
│ - TableSchema                                │
├─────────────────────────────────────────────┤
│ Framework & Infrastructure                   │
│ - Spring Boot                                │
│ - HikariCP / R2DBC Connection Pool          │
│ - PostgreSQL JDBC Driver                     │
└─────────────────────────────────────────────┘
```

### 資料流程

#### 1. 連線建立流程

```
LLM 發起連線請求
    ↓
MCP Tool: connection_management
    action: "add_connection"
    params: {host, port, database, username, password}
    ↓
AddConnectionUseCase
    ↓
並行處理：
├─→ 驗證連線參數
├─→ 建立連線池 (HikariCP)
└─→ 測試連線健康度
    ↓
儲存連線資訊到記憶體
    ↓
返回 connectionId 給 LLM
```

#### 2. 查詢執行流程

```
LLM 發起查詢請求: "SELECT * FROM users WHERE age > ?"
    ↓
MCP Tool: query_execution
    action: "query"
    params: {connectionId, sql, parameters: [18]}
    ↓
ExecuteQueryUseCase
    ↓
安全驗證：
├─→ SQL Injection 檢查
├─→ 參數化查詢綁定
└─→ 權限檢查
    ↓
透過連線池取得 Connection
    ↓
執行 PreparedStatement
    ↓
結果轉換為 JSON
    ↓
返回 QueryResult 給 LLM
```

#### 3. Schema 探索流程

```
LLM 詢問: "users 表的結構是什麼？"
    ↓
MCP Tool: schema_management
    action: "get_table_schema"
    params: {connectionId, tableName: "users"}
    ↓
GetSchemaUseCase
    ↓
查詢 information_schema.columns
    ↓
組織 Schema 資訊：
├─→ 欄位名稱和類型
├─→ 約束條件 (NOT NULL, UNIQUE)
├─→ 預設值
└─→ 索引資訊
    ↓
返回結構化的 TableSchema
    ↓
LLM 理解表結構並生成後續查詢
```

## 技術棧

### 核心組件

| 組件 | 技術選型 | 用途 | 為什麼選它 |
|------|---------|------|-----------|
| **應用框架** | Spring Boot 3.2.1 | 主要服務 | 企業級、生態成熟、依賴注入 |
| **MCP 協定** | Spring AI MCP | MCP 協定支援 | 官方整合、標準化實現 |
| **資料庫驅動** | PostgreSQL JDBC | 同步資料庫連線 | 穩定、高效能 |
| **連線池** | HikariCP | 連線池管理 | 業界標準、高效能 |
| **響應式支援** | R2DBC PostgreSQL (未來) | 非同步資料庫操作 | 高並發、低資源消耗 |
| **日誌** | SLF4J + Logback | 結構化日誌 | 靈活配置、效能好 |
| **測試** | JUnit 5 + TestContainers | 單元測試和整合測試 | 完整測試覆蓋 |

### Java 核心依賴

```xml
<dependencies>
    <!-- Spring Boot 核心 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Spring AI MCP -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-mcp</artifactId>
    </dependency>

    <!-- PostgreSQL 驅動 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- 連線池 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </dependency>

    <!-- 測試 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## MCP Tools 設計

### 1. connection_management - 連線管理

#### add_connection - 新增資料庫連線

```java
@MCPTool(
    name = "postgresql_connection_management",
    description = "管理 PostgreSQL 資料庫連線"
)
public class PostgresConnectionTool {

    public ConnectionResult addConnection(
        String connectionId,        // 連線唯一識別碼
        String host,                // 資料庫主機
        int port,                   // 連線埠號 (預設 5432)
        String database,            // 資料庫名稱
        String username,            // 使用者名稱
        String password,            // 密碼
        Integer maxPoolSize,        // 最大連線數 (選填，預設 10)
        Boolean readonly            // 是否唯讀模式 (選填，預設 false)
    ) {
        // 實現邏輯
    }
}
```

**使用範例**：
```javascript
// LLM 自動調用
await mcp.callTool("postgresql_connection_management", {
  action: "add_connection",
  connectionId: "analytics_db",
  host: "localhost",
  port: 5432,
  database: "warehouse",
  username: "analyst",
  password: "***",
  maxPoolSize: 20,
  readonly: true
});
```

#### test_connection - 測試連線健康度

```java
public ConnectionHealth testConnection(String connectionId) {
    // 執行健康檢查查詢: SELECT 1
    // 返回連線狀態和延遲
}
```

#### remove_connection - 移除連線

```java
public void removeConnection(String connectionId) {
    // 關閉連線池
    // 清理資源
}
```

### 2. query_execution - 查詢執行

#### query - 執行 SELECT 查詢

```java
@MCPTool(
    name = "postgresql_query_execution",
    description = "執行 PostgreSQL 查詢和事務"
)
public class PostgresQueryTool {

    public QueryResult query(
        String connectionId,        // 連線 ID
        String sql,                 // SQL 查詢語句
        List<Object> parameters,    // 參數列表
        Integer fetchSize,          // 每次抓取行數 (選填)
        Integer timeout             // 查詢超時 (秒，選填)
    ) {
        // 使用 PreparedStatement 執行參數化查詢
    }
}
```

**使用範例**：
```javascript
// LLM 查詢資料
const result = await mcp.callTool("postgresql_query_execution", {
  action: "query",
  connectionId: "analytics_db",
  sql: "SELECT product_id, SUM(amount) as total FROM sales WHERE sale_date > ? GROUP BY product_id",
  parameters: ["2024-01-01"],
  fetchSize: 1000
});
```

#### execute - 執行 DML 操作

```java
public ExecuteResult execute(
    String connectionId,
    String sql,
    List<Object> parameters
) {
    // 執行 INSERT, UPDATE, DELETE
    // 返回影響的行數
}
```

#### transaction - 執行事務

```java
public TransactionResult transaction(
    String connectionId,
    List<QueryRequest> queries  // 多個 SQL 操作
) {
    // BEGIN TRANSACTION
    // 依序執行所有查詢
    // 任一失敗則 ROLLBACK
    // 全部成功則 COMMIT
}
```

#### batch - 批次操作

```java
public BatchResult batch(
    String connectionId,
    String sql,
    List<List<Object>> parametersList  // 多組參數
) {
    // 使用 JDBC Batch API 優化效能
}
```

### 3. schema_management - Schema 管理

#### get_table_schema - 獲取表結構

```java
@MCPTool(
    name = "postgresql_schema_management",
    description = "探索和管理 PostgreSQL Schema"
)
public class PostgresSchemaTools {

    public TableSchema getTableSchema(
        String connectionId,
        String tableName,
        String schema  // 選填，預設 public
    ) {
        // 查詢 information_schema.columns
        // 返回完整的表結構資訊
    }
}
```

**使用範例**：
```javascript
// LLM 探索 Schema
const schema = await mcp.callTool("postgresql_schema_management", {
  action: "get_table_schema",
  connectionId: "analytics_db",
  tableName: "sales",
  schema: "public"
});

// 回傳範例
{
  "tableName": "sales",
  "schema": "public",
  "columns": [
    {
      "name": "id",
      "type": "bigint",
      "nullable": false,
      "default": "nextval('sales_id_seq')",
      "isPrimaryKey": true
    },
    {
      "name": "product_id",
      "type": "integer",
      "nullable": false,
      "foreignKey": {
        "table": "products",
        "column": "id"
      }
    }
  ],
  "indexes": [...],
  "constraints": [...]
}
```

#### list_tables - 列出所有表

```java
public List<TableInfo> listTables(
    String connectionId,
    String schema  // 選填
) {
    // 查詢 information_schema.tables
}
```

#### explain_query - 查詢執行計畫

```java
public ExplainResult explainQuery(
    String connectionId,
    String sql,
    List<Object> parameters,
    boolean analyze  // 是否執行 EXPLAIN ANALYZE
) {
    // 執行 EXPLAIN (ANALYZE, BUFFERS) ...
}
```

## MCP Resources 設計

### 1. connections - 連線列表

```java
@MCPResource(
    uri = "postgresql://connections",
    name = "PostgreSQL Connections",
    description = "所有 PostgreSQL 資料庫連線列表"
)
public List<ConnectionInfo> getConnections() {
    // 返回所有已建立的連線資訊
}
```

**回傳格式**：
```json
[
  {
    "connectionId": "analytics_db",
    "host": "localhost",
    "port": 5432,
    "database": "warehouse",
    "username": "analyst",
    "readonly": true,
    "status": "healthy",
    "activeConnections": 5,
    "maxPoolSize": 20,
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

### 2. healthy_connections - 健康連線

```java
@MCPResource(
    uri = "postgresql://healthy_connections",
    name = "Healthy Connections",
    description = "所有健康狀態的連線"
)
public List<ConnectionInfo> getHealthyConnections() {
    // 篩選出 status = "healthy" 的連線
}
```

### 3. connection_details - 連線詳情

```java
@MCPResource(
    uri = "postgresql://connection/{connectionId}",
    name = "Connection Details",
    description = "特定連線的詳細資訊"
)
public ConnectionDetails getConnectionDetails(String connectionId) {
    // 返回連線的詳細統計資訊
}
```

## 安全性設計

### 1. SQL Injection 防護

```java
// ✅ 正確：參數化查詢
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * FROM users WHERE id = ?"
);
stmt.setInt(1, userId);

// ❌ 錯誤：字串拼接
String sql = "SELECT * FROM users WHERE id = " + userId;  // 危險！
```

### 2. 敏感資訊保護

```java
@Configuration
public class SecurityConfig {

    // 密碼加密儲存
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 日誌脫敏
    @Bean
    public LogbackEncoder logEncoder() {
        // 自動遮罩密碼、Token 等敏感欄位
    }
}
```

### 3. 權限控制

```java
public class ReadonlyValidator {

    public void validateQuery(String sql, boolean readonly) {
        if (readonly && isDMLQuery(sql)) {
            throw new SecurityException(
                "Readonly connection cannot execute DML operations"
            );
        }
    }

    private boolean isDMLQuery(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("INSERT")
            || upper.startsWith("UPDATE")
            || upper.startsWith("DELETE")
            || upper.startsWith("DROP")
            || upper.startsWith("TRUNCATE");
    }
}
```

## 部署配置

### Docker Compose

```yaml
version: '3.8'

services:
  postgresql-mcp:
    build: ./mcp-registry-java/mcp-postgresql-server
    container_name: postgresql-mcp
    ports:
      - "8081:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - MCP_SERVER_PORT=8080
      - DEFAULT_POOL_SIZE=10
      - MAX_POOL_SIZE=50
      - QUERY_TIMEOUT=30s
      - MCP_LOG_LEVEL=INFO
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M
    restart: unless-stopped
```

### 環境變數配置

```yaml
# application.yml
server:
  port: ${MCP_SERVER_PORT:8080}

spring:
  datasource:
    hikari:
      maximum-pool-size: ${MAX_POOL_SIZE:20}
      minimum-idle: ${MIN_POOL_SIZE:2}
      connection-timeout: ${CONNECTION_TIMEOUT:30000}
      idle-timeout: ${IDLE_TIMEOUT:600000}

mcp:
  server:
    query-timeout: ${QUERY_TIMEOUT:30s}
    security:
      readonly-mode: ${MCP_READONLY_MODE:false}
      sql-injection-check: ${SQL_INJECTION_CHECK:true}

logging:
  level:
    com.mcp.postgresql: ${MCP_LOG_LEVEL:INFO}
```

## 使用場景示例

### 場景 1: 資料分析任務

```javascript
// 使用者: "分析過去三個月的銷售趨勢"

// LLM 自動執行流程:

// 1. 建立分析資料庫連線
await mcp.callTool("postgresql_connection_management", {
  action: "add_connection",
  connectionId: "sales_analytics",
  host: "analytics.db.company.com",
  database: "sales_warehouse",
  readonly: true
});

// 2. 探索 Schema
const schema = await mcp.callTool("postgresql_schema_management", {
  action: "get_table_schema",
  connectionId: "sales_analytics",
  tableName: "daily_sales"
});

// 3. 執行分析查詢
const result = await mcp.callTool("postgresql_query_execution", {
  action: "query",
  connectionId: "sales_analytics",
  sql: `
    SELECT
      DATE_TRUNC('week', sale_date) as week,
      SUM(amount) as total_sales,
      COUNT(*) as transaction_count
    FROM daily_sales
    WHERE sale_date >= CURRENT_DATE - INTERVAL '3 months'
    GROUP BY week
    ORDER BY week
  `,
  parameters: []
});

// 4. LLM 分析結果並生成報告
// "根據資料顯示，過去三個月銷售呈現上升趨勢..."
```

### 場景 2: 資料清理與轉換

```javascript
// 使用者: "將 legacy_users 表的資料遷移到 new_users，並清理無效記錄"

// LLM 執行流程:

// 1. 在事務中執行資料遷移
const result = await mcp.callTool("postgresql_query_execution", {
  action: "transaction",
  connectionId: "migration_db",
  queries: [
    {
      sql: `
        INSERT INTO new_users (id, email, created_at)
        SELECT id, LOWER(TRIM(email)), created_at
        FROM legacy_users
        WHERE email IS NOT NULL
          AND email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$'
      `,
      parameters: []
    },
    {
      sql: `
        UPDATE migration_log
        SET status = ?, completed_at = NOW()
        WHERE task_id = ?
      `,
      parameters: ["completed", "USER_MIGRATION_001"]
    }
  ]
});
```

### 場景 3: 效能優化建議

```javascript
// 使用者: "為什麼這個查詢這麼慢？"

// LLM 分析流程:

// 1. 執行 EXPLAIN ANALYZE
const plan = await mcp.callTool("postgresql_schema_management", {
  action: "explain_query",
  connectionId: "app_db",
  sql: "SELECT * FROM orders WHERE user_id = ? AND status = ?",
  parameters: [12345, "pending"],
  analyze: true
});

// 2. 分析執行計畫
// plan.executionTime: 2500ms
// plan.planningTime: 15ms
// plan.nodes: [{ type: "Seq Scan", cost: 10000.00, ... }]

// 3. LLM 提供優化建議
// "查詢使用了全表掃描 (Seq Scan)，建議在 user_id 和 status 欄位上建立複合索引：
//  CREATE INDEX idx_orders_user_status ON orders(user_id, status);"
```

## 優勢特點

### 1. 標準化整合
- ✅ MCP 協定：與所有支援 MCP 的 LLM 無縫整合
- ✅ 統一介面：跨資料庫的一致操作體驗
- ✅ 工具化設計：LLM 可自主組合使用工具

### 2. 企業級安全
- ✅ SQL Injection 防護：強制參數化查詢
- ✅ 唯讀模式：防止誤操作
- ✅ 審計日誌：完整的操作記錄
- ✅ 敏感資訊脫敏：密碼、Token 自動遮罩

### 3. 高效能
- ✅ 連線池管理：HikariCP 業界領先效能
- ✅ 批次優化：大量資料操作加速
- ✅ 查詢超時：防止長查詢阻塞
- ✅ 響應式支援：未來整合 R2DBC

### 4. 可觀測性
- ✅ 結構化日誌：JSON 格式，易於分析
- ✅ 效能指標：Micrometer 整合
- ✅ 健康檢查：Spring Boot Actuator
- ✅ 查詢歷史：Resource 提供完整記錄

## 未來擴展方向

### Phase 1: 功能增強
- [ ] R2DBC 響應式實現
- [ ] 查詢結果快取 (Redis)
- [ ] 流式查詢支援 (大結果集)
- [ ] 多租戶資料隔離

### Phase 2: 智能化
- [ ] 查詢優化建議 (基於執行計畫)
- [ ] 自動索引建議
- [ ] 異常查詢檢測
- [ ] Schema 變更追蹤

### Phase 3: 企業功能
- [ ] RBAC 權限控制
- [ ] 資料加密存儲
- [ ] 合規性審計
- [ ] 災難恢復支援

### Phase 4: 生態整合
- [ ] Grafana 儀表板
- [ ] Prometheus 監控
- [ ] OpenTelemetry 追蹤
- [ ] Kafka 事件串流

## 快速開始

```bash
# 1. 克隆專案
git clone https://github.com/your-org/mcp-registry.git
cd mcp-registry/mcp-registry-java/mcp-postgresql-server

# 2. 建置專案
mvn clean package

# 3. 啟動服務
java -jar target/mcp-postgresql-server-0.5.0.jar

# 4. 驗證服務
curl http://localhost:8080/actuator/health

# 5. 在 LLM 中使用
# 配置 MCP Server 連線後即可使用
```

## 技術亮點總結

1. **Clean Architecture**：清晰的分層架構，易於維護和擴展
2. **MCP 協定整合**：標準化的 AI 工具介面
3. **企業級安全**：SQL Injection 防護、權限控制、審計日誌
4. **高效能設計**：連線池、批次優化、查詢超時
5. **完整可觀測性**：結構化日誌、效能指標、健康檢查
6. **Spring Boot 生態**：豐富的整合選項和社群支援

---

**專案理念**: PostgreSQL MCP Server 提供工具能力，LLM 提供智慧決策，兩者結合實現智能資料庫管理。
