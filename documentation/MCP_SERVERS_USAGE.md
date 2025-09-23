# MCP Registry 使用指南

## 概述

MCP Registry 提供兩個通用的資料庫 MCP Server：
- **PostgreSQL MCP Server**: 提供 PostgreSQL 資料庫操作的 MCP 工具
- **MySQL MCP Server**: 提供 MySQL 資料庫操作的 MCP 工具

這些 Server 是**純工具層**，不包含任何業務邏輯，讓 LLM 能夠透過 MCP 協定執行智能資料遷移和操作。

## 架構特色

### Clean Architecture + DDD
- **Entity Layer**: 核心領域實體和業務規則
- **Use Case Layer**: 應用服務和 Ports
- **Interface Adapter Layer**: MCP Tools 和 Resources 適配器
- **Framework Layer**: Spring Boot 基礎設施

### 核心原則
- **零業務邏輯**: Server 只提供工具，所有智能決策由 LLM 完成
- **通用性**: 適用於任何資料庫操作場景
- **安全性**: 參數化查詢防止 SQL Injection
- **可靠性**: 完整的錯誤處理和事務支援

## PostgreSQL MCP Server

### 啟動服務
```bash
cd mcp-registry-java/mcp-postgresql-server
mvn spring-boot:run
```

### 可用工具

#### 1. postgresql_connection_management
連線管理工具，支援連線建立、測試和移除。

**新增連線**
```json
{
  "action": "add",
  "connectionId": "my_postgres_db",
  "host": "localhost",
  "port": 5432,
  "database": "myapp",
  "username": "postgres",
  "password": "password",
  "poolSize": 10,
  "readOnly": false
}
```

**測試連線**
```json
{
  "action": "test",
  "connectionId": "my_postgres_db"
}
```

#### 2. postgresql_query_execution
查詢執行工具，支援 SELECT、UPDATE、事務和批次操作。

**執行查詢**
```json
{
  "action": "query",
  "connectionId": "my_postgres_db",
  "sql": "SELECT * FROM users WHERE status = $1",
  "parameters": ["active"],
  "fetchSize": 1000
}
```

**執行更新**
```json
{
  "action": "update",
  "connectionId": "my_postgres_db",
  "sql": "UPDATE users SET last_login = NOW() WHERE id = $1",
  "parameters": [123]
}
```

**執行事務**
```json
{
  "action": "transaction",
  "connectionId": "my_postgres_db",
  "queries": [
    {
      "sql": "INSERT INTO orders (user_id, amount) VALUES ($1, $2)",
      "parameters": [123, 99.99]
    },
    {
      "sql": "UPDATE inventory SET quantity = quantity - 1 WHERE product_id = $1",
      "parameters": [456]
    }
  ]
}
```

#### 3. postgresql_schema_management
Schema 管理工具，支援表結構查詢和執行計畫分析。

**獲取表結構**
```json
{
  "action": "get_table_schema",
  "connectionId": "my_postgres_db",
  "tableName": "users",
  "schemaName": "public"
}
```

**列出所有表**
```json
{
  "action": "list_tables",
  "connectionId": "my_postgres_db",
  "schemaName": "public"
}
```

**分析執行計畫**
```json
{
  "action": "explain_query",
  "connectionId": "my_postgres_db",
  "sql": "SELECT * FROM users WHERE email = 'test@example.com'",
  "analyze": false
}
```

### 可用資源

#### connections
獲取所有連線列表
```
GET /mcp/postgresql/resources/connections
```

#### healthy_connections
獲取健康連線列表
```
GET /mcp/postgresql/resources/healthy_connections
```

#### connection_details
獲取特定連線詳情
```
GET /mcp/postgresql/resources/connection_details?connectionId=my_postgres_db
```

## MySQL MCP Server

### 啟動服務
```bash
cd mcp-registry-java/mcp-mysql-server
mvn spring-boot:run
```

### 可用工具

#### mysql_connection_management
MySQL 連線管理工具，功能與 PostgreSQL 相似，但預設埠號為 3306。

**新增連線**
```json
{
  "action": "add",
  "connectionId": "my_mysql_db",
  "host": "localhost",
  "port": 3306,
  "database": "myapp",
  "username": "root",
  "password": "password",
  "poolSize": 10
}
```

## API 端點

### PostgreSQL Server
- 服務資訊: `GET /mcp/postgresql/info`
- 健康檢查: `GET /mcp/postgresql/health`
- 工具列表: `GET /mcp/postgresql/tools`
- 執行工具: `POST /mcp/postgresql/tools/{toolName}`
- 資源列表: `GET /mcp/postgresql/resources`
- 獲取資源: `GET /mcp/postgresql/resources/{resourceType}`

### MySQL Server
- 服務資訊: `GET /mcp/mysql/info`
- 健康檢查: `GET /mcp/mysql/health`
- 工具列表: `GET /mcp/mysql/tools`
- 執行工具: `POST /mcp/mysql/tools/{toolName}`

## 使用範例

### LLM 智能資料遷移流程

1. **建立連線**
```javascript
// 建立來源資料庫連線
await mcpClient.callTool("postgresql_connection_management", {
  action: "add",
  connectionId: "source_db",
  host: "old-server.example.com",
  database: "legacy_app"
});

// 建立目標資料庫連線
await mcpClient.callTool("mysql_connection_management", {
  action: "add",
  connectionId: "target_db",
  host: "new-server.example.com",
  database: "new_app"
});
```

2. **分析來源 Schema**
```javascript
// 獲取表列表
const tables = await mcpClient.callTool("postgresql_schema_management", {
  action: "list_tables",
  connectionId: "source_db"
});

// 獲取詳細表結構
const userSchema = await mcpClient.callTool("postgresql_schema_management", {
  action: "get_table_schema",
  connectionId: "source_db",
  tableName: "users"
});
```

3. **執行資料遷移**
```javascript
// 查詢來源資料
const sourceData = await mcpClient.callTool("postgresql_query_execution", {
  action: "query",
  connectionId: "source_db",
  sql: "SELECT * FROM users WHERE created_at > $1",
  parameters: ["2024-01-01"]
});

// 批次插入目標資料庫
await mcpClient.callTool("mysql_query_execution", {
  action: "batch",
  connectionId: "target_db",
  sql: "INSERT INTO users (name, email, status) VALUES (?, ?, ?)",
  parameters: sourceData.rows.map(row => [row.name, row.email, row.status])
});
```

## 安全考量

### SQL Injection 防護
- 所有查詢都使用參數化查詢
- 禁止字串拼接 SQL

### 連線安全
- 支援唯讀模式
- 連線池限制
- 密碼加密儲存（實際實現中）

### 權限控制
- 可配置限制危險操作
- 支援角色基礎存取控制（未來版本）

## 監控與維護

### 健康檢查
```bash
curl http://localhost:8080/mcp/postgresql/health
curl http://localhost:8080/mcp/mysql/health
```

### 連線狀態監控
```bash
curl http://localhost:8080/mcp/postgresql/resources/healthy_connections
```

### 日誌監控
Server 提供結構化日誌，包含：
- 查詢執行時間
- 錯誤詳情
- 連線狀態變更
- 效能指標

## 開發與擴展

### 添加新工具
1. 實現 `McpTool` 介面
2. 定義參數 Schema
3. 在 Controller 中註冊工具

### 添加新資源
1. 實現 `McpResource` 介面
2. 定義資源內容格式
3. 在 Controller 中註冊資源

### 自定義資料庫驅動
1. 實現 DataSource
2. 配置連線池
3. 處理資料庫特定 SQL 語法

## 故障排除

### 常見問題

**連線失敗**
- 檢查資料庫服務是否啟動
- 驗證連線參數
- 確認網路連通性

**查詢超時**
- 調整 timeout 參數
- 優化 SQL 查詢
- 檢查資料庫效能

**記憶體不足**
- 調整 fetchSize
- 使用分頁查詢
- 監控連線池大小

### 日誌位置
- Spring Boot 日誌: `logs/application.log`
- 查詢日誌: `logs/query.log`
- 錯誤日誌: `logs/error.log`

## 未來規劃

- [ ] 完整的 R2DBC 響應式支援
- [ ] Redis 快取整合
- [ ] 查詢效能監控
- [ ] 自動故障轉移
- [ ] 多租戶支援
- [ ] GraphQL 查詢支援

---

**重要提醒**: 這些 MCP Server 是純工具層，所有業務邏輯和智能決策應由 LLM 根據上下文完成。Server 提供能力，LLM 提供智慧。