# MySQL MCP Server - 企業級 MySQL 資料庫管理工具

## 專案概述

MySQL MCP Server 是一個基於 MCP (Model Context Protocol) 的智能 MySQL 資料庫管理工具，專為 LLM 驅動的資料庫操作設計。它提供了一套完整的工具集，讓 AI 能夠安全、高效地執行 MySQL 資料庫的查詢、分析和管理任務。

## 要解決的問題

### 核心痛點

在 LLM 驅動的 MySQL 資料庫操作場景中，開發者面臨以下挑戰：

1. **MySQL 特有功能缺失**：
    - 無標準化的 MySQL 操作介面
    - LLM 無法直接使用 MySQL 特性
    - 缺乏 MySQL 方言的 SQL 支援
    - 無法利用 MySQL 優化器特性

2. **生態整合困難**：
    - 與 PostgreSQL 操作方式不一致
    - MySQL 特有語法無法標準化
    - 儲存引擎差異難以處理
    - 字符集和編碼問題複雜

3. **效能優化挑戰**：
    - MySQL 連線池配置困難
    - InnoDB 事務處理複雜
    - 查詢快取機制不明確
    - 索引優化建議缺失

4. **資料遷移需求**：
    - 需要與 PostgreSQL 互通
    - Schema 轉換複雜
    - 資料類型對應不清晰
    - 跨資料庫遷移困難

### 解決方案

MySQL MCP Server 透過 **MCP 協定標準化**，讓 LLM 能夠：
- ✅ 透過標準化的 MCP Tools 執行 MySQL 特定操作
- ✅ 使用參數化查詢防止 SQL Injection
- ✅ 智能處理 MySQL 方言和特性
- ✅ 提供 MySQL 優化的連線池和事務管理
- ✅ 支援與其他資料庫的無縫協作

## 系統架構

### 整體架構圖

```
┌─────────────────────────┐
│   LLM (Claude/GPT)     │  使用者介面
└───────────┬─────────────┘
            │ MCP Protocol
┌───────────▼──────────────────────────────┐
│         MySQL MCP Server                 │
│  ┌──────────────────────────────────┐   │
│  │ MCP Tools:                        │   │
│  │ - mysql_connection_management     │   │
│  │ - mysql_query_execution           │   │
│  │ - mysql_schema_management         │   │
│  │ - mysql_storage_engine_tools      │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │ MCP Resources:                    │   │
│  │ - mysql_connections               │   │
│  │ - mysql_databases                 │   │
│  │ - mysql_performance_metrics       │   │
│  └──────────────────────────────────┘   │
└───────────┬──────────────────────────────┘
            │ JDBC / R2DBC
┌───────────▼──────────────┐
│   MySQL Database         │
│   - 業務資料              │
│   - InnoDB 儲存引擎      │
│   - 查詢優化器            │
└──────────────────────────┘
```

### Clean Architecture 分層

```
┌─────────────────────────────────────────────┐
│ Interface Adapters (MCP Tools)              │
│ - MySQLConnectionTool                        │
│ - MySQLQueryTool                             │
│ - MySQLSchemaTools                           │
│ - MySQLStorageEngineTool                     │
├─────────────────────────────────────────────┤
│ Use Cases (Application Services)            │
│ - AddMySQLConnectionUseCase                  │
│ - ExecuteMySQLQueryUseCase                   │
│ - GetMySQLSchemaUseCase                      │
│ - OptimizeMySQLQueryUseCase                  │
├─────────────────────────────────────────────┤
│ Domain Entities                              │
│ - MySQLConnection                            │
│ - MySQLQueryRequest / QueryResult            │
│ - MySQLTableSchema                           │
│ - StorageEngineInfo                          │
├─────────────────────────────────────────────┤
│ Framework & Infrastructure                   │
│ - Spring Boot                                │
│ - HikariCP / R2DBC MySQL Driver             │
│ - MySQL Connector/J                          │
└─────────────────────────────────────────────┘
```

### 資料流程

#### 1. MySQL 連線建立流程

```
LLM 發起 MySQL 連線請求
    ↓
MCP Tool: mysql_connection_management
    action: "add_connection"
    params: {host, port: 3306, database, username, password}
    ↓
AddMySQLConnectionUseCase
    ↓
並行處理：
├─→ 驗證連線參數
├─→ 設定 MySQL 特定配置
│   (character_set, collation, autoReconnect)
├─→ 建立 HikariCP 連線池
└─→ 測試連線並獲取 MySQL 版本
    ↓
儲存連線資訊（包含 MySQL 版本和特性）
    ↓
返回 connectionId 和 MySQL 資訊給 LLM
```

#### 2. MySQL 查詢執行流程

```
LLM 發起查詢: "SELECT * FROM users WHERE age > ?"
    ↓
MCP Tool: mysql_query_execution
    action: "query"
    params: {connectionId, sql, parameters: [18]}
    ↓
ExecuteMySQLQueryUseCase
    ↓
MySQL 特定處理：
├─→ SQL 方言檢查 (LIMIT vs FETCH)
├─→ 參數化查詢綁定
├─→ 字符集編碼處理
└─→ InnoDB 事務隔離級別設定
    ↓
透過連線池取得 Connection
    ↓
執行 PreparedStatement
    ↓
結果轉換為 JSON (處理 MySQL 特殊類型)
    ↓
返回 QueryResult 給 LLM
```

#### 3. MySQL Schema 探索流程

```
LLM 詢問: "users 表使用哪種儲存引擎？"
    ↓
MCP Tool: mysql_schema_management
    action: "get_table_schema"
    params: {connectionId, tableName: "users"}
    ↓
GetMySQLSchemaUseCase
    ↓
查詢 MySQL 特定的系統表：
├─→ information_schema.TABLES (儲存引擎)
├─→ information_schema.COLUMNS (欄位資訊)
├─→ information_schema.STATISTICS (索引)
└─→ SHOW CREATE TABLE (完整 DDL)
    ↓
組織 MySQL Schema 資訊：
├─→ 欄位名稱和 MySQL 類型
├─→ 儲存引擎 (InnoDB, MyISAM)
├─→ 字符集和排序規則
├─→ 索引和外鍵
└─→ AUTO_INCREMENT 資訊
    ↓
返回結構化的 MySQLTableSchema
    ↓
LLM 理解 MySQL 表結構並生成後續操作
```

## 技術棧

### 核心組件

| 組件 | 技術選型 | 用途 | 為什麼選它 |
|------|---------|------|-----------|
| **應用框架** | Spring Boot 3.2.1 | 主要服務 | 企業級、生態成熟 |
| **MCP 協定** | Spring AI MCP | MCP 協定支援 | 官方整合、標準化 |
| **MySQL 驅動** | MySQL Connector/J 8.0+ | 同步資料庫連線 | 官方驅動、穩定可靠 |
| **連線池** | HikariCP | 連線池管理 | 業界標準、MySQL 優化 |
| **響應式支援** | R2DBC MySQL (未來) | 非同步資料庫操作 | 高並發、支援 MySQL |
| **日誌** | SLF4J + Logback | 結構化日誌 | 靈活配置、效能好 |
| **測試** | JUnit 5 + TestContainers MySQL | 單元測試和整合測試 | MySQL 容器化測試 |

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

    <!-- MySQL 驅動 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- 連線池 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </dependency>

    <!-- 測試 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <version>1.19.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### MySQL 連線配置

```java
@Configuration
public class MySQLDataSourceConfig {

    @Bean
    public HikariConfig mysqlHikariConfig() {
        HikariConfig config = new HikariConfig();

        // MySQL 特定配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // 連線池設定
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return config;
    }
}
```

## MCP Tools 設計

### 1. mysql_connection_management - MySQL 連線管理

#### add_connection - 新增 MySQL 連線

```java
@MCPTool(
    name = "mysql_connection_management",
    description = "管理 MySQL 資料庫連線"
)
public class MySQLConnectionTool {

    public MySQLConnectionResult addConnection(
        String connectionId,        // 連線唯一識別碼
        String host,                // MySQL 主機
        int port,                   // 連線埠號 (預設 3306)
        String database,            // 資料庫名稱
        String username,            // 使用者名稱
        String password,            // 密碼
        Integer maxPoolSize,        // 最大連線數 (選填，預設 10)
        String charset,             // 字符集 (選填，預設 utf8mb4)
        String collation,           // 排序規則 (選填)
        Boolean ssl,                // 是否使用 SSL (選填，預設 false)
        Boolean readonly            // 是否唯讀模式 (選填，預設 false)
    ) {
        // 實現邏輯
    }
}
```

**使用範例**：
```javascript
// LLM 建立 MySQL 連線
await mcp.callTool("mysql_connection_management", {
  action: "add_connection",
  connectionId: "app_mysql",
  host: "mysql.company.com",
  port: 3306,
  database: "ecommerce",
  username: "app_user",
  password: "***",
  maxPoolSize: 20,
  charset: "utf8mb4",
  collation: "utf8mb4_unicode_ci",
  ssl: true
});

// 回傳範例
{
  "connectionId": "app_mysql",
  "status": "connected",
  "mysqlVersion": "8.0.35",
  "defaultEngine": "InnoDB",
  "charset": "utf8mb4",
  "collation": "utf8mb4_unicode_ci",
  "features": {
    "windowFunctions": true,
    "cte": true,
    "json": true
  }
}
```

#### get_server_info - 獲取 MySQL 伺服器資訊

```java
public MySQLServerInfo getServerInfo(String connectionId) {
    // 執行 SHOW VARIABLES
    // 返回 MySQL 版本、儲存引擎、字符集等資訊
}
```

### 2. mysql_query_execution - MySQL 查詢執行

#### query - 執行 MySQL 查詢

```java
@MCPTool(
    name = "mysql_query_execution",
    description = "執行 MySQL 查詢和事務"
)
public class MySQLQueryTool {

    public MySQLQueryResult query(
        String connectionId,        // 連線 ID
        String sql,                 // MySQL SQL 語句
        List<Object> parameters,    // 參數列表
        Integer fetchSize,          // 每次抓取行數 (選填)
        Boolean streaming           // 是否流式處理 (選填)
    ) {
        // 使用 PreparedStatement 執行
        // 處理 MySQL 特殊類型 (ENUM, SET, JSON)
    }
}
```

**使用範例（MySQL 特有語法）**：
```javascript
// 1. JSON 查詢 (MySQL 5.7+)
const result = await mcp.callTool("mysql_query_execution", {
  action: "query",
  connectionId: "app_mysql",
  sql: `
    SELECT
      id,
      JSON_EXTRACT(metadata, '$.tags') as tags,
      JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.author')) as author
    FROM articles
    WHERE JSON_CONTAINS(metadata, ?, '$.tags')
  `,
  parameters: [JSON.stringify("mysql")]
});

// 2. Window Functions (MySQL 8.0+)
const ranking = await mcp.callTool("mysql_query_execution", {
  action: "query",
  connectionId: "app_mysql",
  sql: `
    SELECT
      product_id,
      sale_date,
      amount,
      ROW_NUMBER() OVER (PARTITION BY product_id ORDER BY sale_date) as sale_rank
    FROM sales
  `
});

// 3. Common Table Expressions (MySQL 8.0+)
const hierarchical = await mcp.callTool("mysql_query_execution", {
  action: "query",
  connectionId: "app_mysql",
  sql: `
    WITH RECURSIVE category_tree AS (
      SELECT id, name, parent_id, 0 as level
      FROM categories
      WHERE parent_id IS NULL
      UNION ALL
      SELECT c.id, c.name, c.parent_id, ct.level + 1
      FROM categories c
      INNER JOIN category_tree ct ON c.parent_id = ct.id
    )
    SELECT * FROM category_tree
  `
});
```

#### transaction - MySQL 事務處理

```java
public MySQLTransactionResult transaction(
    String connectionId,
    List<MySQLQueryRequest> queries,
    String isolationLevel  // 選填: READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE
) {
    // SET TRANSACTION ISOLATION LEVEL ...
    // START TRANSACTION
    // 執行查詢序列
    // COMMIT / ROLLBACK
}
```

### 3. mysql_schema_management - MySQL Schema 管理

#### get_table_schema - 獲取 MySQL 表結構

```java
@MCPTool(
    name = "mysql_schema_management",
    description = "探索和管理 MySQL Schema"
)
public class MySQLSchemaTools {

    public MySQLTableSchema getTableSchema(
        String connectionId,
        String tableName,
        String database  // 選填
    ) {
        // 查詢 information_schema
        // 執行 SHOW CREATE TABLE
        // 返回完整的 MySQL 表結構
    }
}
```

**使用範例**：
```javascript
const schema = await mcp.callTool("mysql_schema_management", {
  action: "get_table_schema",
  connectionId: "app_mysql",
  tableName: "orders",
  database: "ecommerce"
});

// 回傳範例 (MySQL 特有資訊)
{
  "tableName": "orders",
  "database": "ecommerce",
  "engine": "InnoDB",
  "charset": "utf8mb4",
  "collation": "utf8mb4_unicode_ci",
  "autoIncrement": 15234,
  "rowFormat": "Dynamic",
  "columns": [
    {
      "name": "id",
      "type": "bigint unsigned",
      "nullable": false,
      "default": null,
      "extra": "auto_increment",
      "isPrimaryKey": true
    },
    {
      "name": "status",
      "type": "enum('pending','processing','completed','cancelled')",
      "nullable": false,
      "default": "pending"
    },
    {
      "name": "metadata",
      "type": "json",
      "nullable": true,
      "default": null
    }
  ],
  "indexes": [
    {
      "name": "PRIMARY",
      "columns": ["id"],
      "type": "BTREE",
      "unique": true
    },
    {
      "name": "idx_user_status",
      "columns": ["user_id", "status"],
      "type": "BTREE",
      "unique": false
    }
  ],
  "foreignKeys": [...],
  "createStatement": "CREATE TABLE `orders` (...) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
}
```

#### show_index_usage - 索引使用分析

```java
public IndexUsageStats showIndexUsage(
    String connectionId,
    String tableName
) {
    // 查詢 sys.schema_index_statistics
    // 分析索引使用情況
}
```

#### optimize_table - 優化表

```java
public OptimizeResult optimizeTable(
    String connectionId,
    String tableName
) {
    // 執行 OPTIMIZE TABLE
    // 整理碎片、更新統計資訊
}
```

### 4. mysql_storage_engine_tools - 儲存引擎工具

#### get_engine_status - 獲取儲存引擎狀態

```java
@MCPTool(
    name = "mysql_storage_engine_tools",
    description = "MySQL 儲存引擎管理和優化"
)
public class MySQLStorageEngineTool {

    public InnDBStatus getEngineStatus(
        String connectionId,
        String engine  // InnoDB, MyISAM, Memory
    ) {
        // 執行 SHOW ENGINE INNODB STATUS
        // 分析事務、鎖、緩衝池狀態
    }
}
```

**使用範例**：
```javascript
// 分析 InnoDB 狀態
const status = await mcp.callTool("mysql_storage_engine_tools", {
  action: "get_engine_status",
  connectionId: "app_mysql",
  engine: "InnoDB"
});

// 回傳範例
{
  "engine": "InnoDB",
  "bufferPool": {
    "size": "8GB",
    "hitRate": 99.87,
    "dirtyPages": 1234
  },
  "transactions": {
    "active": 5,
    "maxHistory": 1000,
    "purgeQueueLength": 0
  },
  "locks": {
    "activeWaits": 0,
    "deadlocks": 0
  },
  "io": {
    "reads": 1234567,
    "writes": 234567,
    "pending": 0
  }
}
```

## MCP Resources 設計

### 1. mysql_connections - MySQL 連線列表

```java
@MCPResource(
    uri = "mysql://connections",
    name = "MySQL Connections",
    description = "所有 MySQL 資料庫連線列表"
)
public List<MySQLConnectionInfo> getConnections() {
    // 返回所有 MySQL 連線資訊
}
```

### 2. mysql_databases - 資料庫列表

```java
@MCPResource(
    uri = "mysql://databases/{connectionId}",
    name = "MySQL Databases",
    description = "特定連線下的所有資料庫"
)
public List<DatabaseInfo> getDatabases(String connectionId) {
    // SHOW DATABASES
}
```

### 3. mysql_performance_metrics - 效能指標

```java
@MCPResource(
    uri = "mysql://performance/{connectionId}",
    name = "MySQL Performance Metrics",
    description = "MySQL 效能指標和統計資訊"
)
public PerformanceMetrics getPerformanceMetrics(String connectionId) {
    // 查詢 performance_schema
    // 返回查詢統計、索引使用、鎖等待等
}
```

## 安全性設計

### 1. MySQL SQL Injection 防護

```java
// ✅ 正確：參數化查詢
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * FROM users WHERE username = ? AND role = ?"
);
stmt.setString(1, username);
stmt.setString(2, role);

// MySQL 特殊處理：防止 LIKE 注入
stmt.setString(1, escapeLike(searchTerm) + "%");
```

### 2. MySQL 特有安全配置

```yaml
# application.yml
spring:
  datasource:
    hikari:
      data-source-properties:
        # 防止 SQL 注入
        allowMultiQueries: false
        # 啟用 SSL
        useSSL: true
        requireSSL: true
        # 防止本地檔案讀取攻擊
        allowLoadLocalInfile: false
        allowLoadLocalInfileInPath: ""
```

### 3. 權限最小化

```java
public class MySQLSecurityValidator {

    // 檢查危險的 MySQL 特定語法
    private static final Set<String> DANGEROUS_FUNCTIONS = Set.of(
        "LOAD_FILE",      // 讀取伺服器檔案
        "INTO OUTFILE",   // 寫入伺服器檔案
        "INTO DUMPFILE",  // 匯出資料
        "LOAD DATA"       // 載入外部資料
    );

    public void validateQuery(String sql) {
        String upper = sql.toUpperCase();
        for (String func : DANGEROUS_FUNCTIONS) {
            if (upper.contains(func)) {
                throw new SecurityException(
                    "Dangerous MySQL function not allowed: " + func
                );
            }
        }
    }
}
```

## 部署配置

### Docker Compose

```yaml
version: '3.8'

services:
  mysql-mcp:
    build: ./mcp-registry-java/mcp-mysql-server
    container_name: mysql-mcp
    ports:
      - "8082:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - MCP_SERVER_PORT=8080
      - MYSQL_DEFAULT_CHARSET=utf8mb4
      - MYSQL_DEFAULT_COLLATION=utf8mb4_unicode_ci
      - DEFAULT_POOL_SIZE=10
      - MAX_POOL_SIZE=50
      - QUERY_TIMEOUT=30s
    deploy:
      resources:
        limits:
          memory: 512M
    restart: unless-stopped

  mysql-db:
    image: mysql:8.0
    container_name: mysql-test-db
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=rootpass
      - MYSQL_DATABASE=testdb
      - MYSQL_USER=testuser
      - MYSQL_PASSWORD=testpass
    volumes:
      - mysql-data:/var/lib/mysql
    restart: unless-stopped

volumes:
  mysql-data:
```

## 使用場景示例

### 場景 1: MySQL JSON 資料分析

```javascript
// 使用者: "分析產品評論的情感分布"

// 1. 連線到包含 JSON 資料的 MySQL
await mcp.callTool("mysql_connection_management", {
  action: "add_connection",
  connectionId: "reviews_db",
  database: "ecommerce"
});

// 2. 使用 MySQL JSON 函數分析
const sentimentAnalysis = await mcp.callTool("mysql_query_execution", {
  action: "query",
  connectionId: "reviews_db",
  sql: `
    SELECT
      JSON_UNQUOTE(JSON_EXTRACT(review_data, '$.sentiment')) as sentiment,
      COUNT(*) as count,
      AVG(JSON_EXTRACT(review_data, '$.rating')) as avg_rating
    FROM product_reviews
    WHERE JSON_EXTRACT(review_data, '$.verified') = true
    GROUP BY sentiment
    ORDER BY count DESC
  `
});

// 3. LLM 分析結果並生成洞察
// "正面評論佔 65%，平均評分 4.2 分..."
```

### 場景 2: InnoDB 效能優化

```javascript
// 使用者: "為什麼訂單查詢這麼慢？"

// 1. 分析查詢執行計畫 (MySQL 格式)
const explainResult = await mcp.callTool("mysql_query_execution", {
  action: "query",
  connectionId: "orders_db",
  sql: "EXPLAIN FORMAT=JSON SELECT * FROM orders WHERE user_id = ? AND status = ?",
  parameters: [12345, "pending"]
});

// 2. 檢查索引使用情況
const indexStats = await mcp.callTool("mysql_schema_management", {
  action: "show_index_usage",
  connectionId: "orders_db",
  tableName: "orders"
});

// 3. 檢查 InnoDB 狀態
const innodbStatus = await mcp.callTool("mysql_storage_engine_tools", {
  action: "get_engine_status",
  connectionId: "orders_db",
  engine: "InnoDB"
});

// 4. LLM 提供優化建議
// "查詢未使用索引，建議創建複合索引：
//  ALTER TABLE orders ADD INDEX idx_user_status (user_id, status);"
```

### 場景 3: MySQL 到 PostgreSQL 資料遷移準備

```javascript
// 使用者: "準備將 MySQL 資料遷移到 PostgreSQL"

// 1. 分析 MySQL Schema
const mysqlSchema = await mcp.callTool("mysql_schema_management", {
  action: "get_table_schema",
  connectionId: "mysql_source",
  tableName: "users"
});

// 2. LLM 分析類型對應關係
// MySQL ENUM -> PostgreSQL CHECK constraint 或 custom type
// MySQL JSON -> PostgreSQL JSONB
// MySQL AUTO_INCREMENT -> PostgreSQL SERIAL or IDENTITY

// 3. 生成 PostgreSQL 兼容的 DDL
const pgDDL = `
  CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) CHECK (status IN ('active', 'inactive', 'suspended')),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
`;

// 4. 準備資料轉換邏輯
// (將在 Migration Coordinator MCP 中執行)
```

## 優勢特點

### 1. MySQL 原生支援
- ✅ 完整的 MySQL 方言支援
- ✅ InnoDB 事務優化
- ✅ JSON 函數和 CTE (8.0+)
- ✅ 儲存引擎特性利用

### 2. 企業級連線池
- ✅ HikariCP MySQL 優化配置
- ✅ PreparedStatement 快取
- ✅ Batch Rewrite 優化
- ✅ 連線健康檢查

### 3. 安全性加強
- ✅ 防止 MySQL 特有攻擊 (LOAD_FILE)
- ✅ SSL/TLS 連線支援
- ✅ 敏感函數限制
- ✅ 字符集注入防護

### 4. 效能監控
- ✅ Performance Schema 整合
- ✅ InnoDB 狀態監控
- ✅ 慢查詢日誌分析
- ✅ 索引使用統計

## 未來擴展方向

### Phase 1: 功能增強
- [ ] R2DBC MySQL 響應式實現
- [ ] MySQL Replication 支援
- [ ] 分區表管理工具
- [ ] 全文檢索 (FULLTEXT) 支援

### Phase 2: 高可用
- [ ] MySQL Cluster 整合
- [ ] ProxySQL 負載平衡
- [ ] 自動故障轉移
- [ ] 讀寫分離支援

### Phase 3: 資料遷移
- [ ] MySQL → PostgreSQL 自動轉換
- [ ] Schema 差異分析
- [ ] 資料類型對應優化
- [ ] 增量同步支援

## 快速開始

```bash
# 1. 克隆專案
git clone https://github.com/your-org/mcp-registry.git
cd mcp-registry/mcp-registry-java/mcp-mysql-server

# 2. 建置專案
mvn clean package

# 3. 啟動服務
java -jar target/mcp-mysql-server-0.5.0.jar

# 4. 啟動測試 MySQL (Docker)
docker run -d \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=testdb \
  mysql:8.0

# 5. 在 LLM 中使用
# 配置 MySQL MCP Server 連線後即可使用
```

## MySQL vs PostgreSQL 差異對照

| 特性 | MySQL | PostgreSQL | MCP Server 處理 |
|------|-------|------------|----------------|
| **預設埠號** | 3306 | 5432 | 自動識別 |
| **自增欄位** | AUTO_INCREMENT | SERIAL/IDENTITY | Schema 轉換 |
| **JSON 類型** | JSON | JSONB | 函數對應 |
| **字符集** | utf8mb4 | UTF8 | 編碼轉換 |
| **LIMIT 語法** | LIMIT 10 | FETCH FIRST 10 | 方言處理 |
| **儲存引擎** | InnoDB, MyISAM | N/A | 特性檢測 |

---

**專案理念**: MySQL MCP Server 提供 MySQL 原生工具能力，讓 LLM 充分發揮 MySQL 特性，並為跨資料庫遷移提供基礎。
