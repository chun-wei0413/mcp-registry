# PostgreSQL MCP Server 使用指南

本文件提供完整的 PostgreSQL MCP Server 使用流程和詳細說明，讓您能夠快速上手並充分利用此工具。

## 📚 目錄

1. [系統架構](#系統架構)
2. [安裝和配置](#安裝和配置)
3. [MCP 工具詳細說明](#mcp-工具詳細說明)
4. [安全配置](#安全配置)
5. [實際使用場景](#實際使用場景)
6. [故障排除](#故障排除)
7. [性能優化](#性能優化)
8. [API 參考](#api-參考)

## 系統架構

PostgreSQL MCP Server 採用三層架構設計：

```
┌─────────────────┐
│      LLM        │  智能決策層
│   (Claude等)    │
└─────────┬───────┘
          │ MCP Protocol
┌─────────▼───────┐
│  MCP Server     │  工具提供層
│  (Pure Tools)   │
└─────────┬───────┘
          │ AsyncPG
┌─────────▼───────┐
│   PostgreSQL    │  資料存儲層
│   Database      │
└─────────────────┘
```

### 核心元件

- **MCP Server**: 純工具層，提供資料庫操作能力
- **Connection Manager**: 連線池管理
- **Security Validator**: 查詢安全驗證
- **Query Tools**: 查詢執行引擎
- **Schema Tools**: 資料庫結構檢查
- **Health Monitor**: 系統健康監控

## 安裝和配置

### 1. 環境準備

```bash
# 確保系統需求
python --version  # >= 3.11
psql --version    # PostgreSQL >= 12

# 檢查 Docker (可選)
docker --version
docker-compose --version
```

### 2. 專案安裝

#### 方法一：使用 Docker (推薦)

```bash
# 1. 克隆專案
git clone <repository-url>
cd postgresql-mcp-server

# 2. 複製環境配置
cp .env.example .env

# 3. 編輯配置檔案
nano .env
```

環境配置範例：
```bash
# 基本配置
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=INFO
DEFAULT_POOL_SIZE=10
QUERY_TIMEOUT=30
MAX_CONNECTIONS=50

# 安全配置
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
MAX_QUERY_LENGTH=10000
ENABLE_QUERY_LOGGING=true

# 資料庫配置 (如果需要預設連線)
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=your_database
DB_USER=your_username
DB_PASSWORD=your_password
DB_POOL_SIZE=10

# 監控配置
ENABLE_METRICS=true
HEALTH_CHECK_INTERVAL=30
```

```bash
# 4. 啟動所有服務
./scripts/deploy.sh

# 5. 驗證服務狀態
./scripts/deploy.sh status
```

#### 方法二：本地開發

```bash
# 1. 建立虛擬環境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 2. 安裝依賴
pip install -e .[dev,test]

# 3. 設定環境變數
export $(cat .env | xargs)

# 4. 啟動 MCP Server
python -m src.server

# 或使用開發腳本
./scripts/dev.sh start
```

### 3. 驗證安裝

```bash
# 檢查服務健康狀態
curl http://localhost:3000/health

# 檢查 MCP 工具註冊
curl http://localhost:3000/mcp/tools

# 運行測試套件
python run_tests.py all
```

## MCP 工具詳細說明

### 連線管理工具

#### add_connection
建立新的資料庫連線並設定連線池。

**參數：**
- `connection_id` (str): 唯一的連線識別符
- `host` (str): 資料庫主機地址
- `port` (int): 連接埠號 (預設 5432)
- `database` (str): 資料庫名稱
- `user` (str): 使用者名稱
- `password` (str): 密碼
- `pool_size` (int, 可選): 連線池大小 (預設 10)

**使用範例：**
```python
result = await add_connection(
    connection_id="analytics_db",
    host="analytics.company.com",
    port=5432,
    database="analytics",
    user="readonly_user",
    password="secure_password",
    pool_size=15
)
```

**返回格式：**
```json
{
  "success": true,
  "connection_id": "analytics_db",
  "message": "Connection established successfully",
  "pool_stats": {
    "size": 15,
    "idle_connections": 15,
    "active_connections": 0
  }
}
```

#### test_connection
測試指定連線的健康狀態。

**參數：**
- `connection_id` (str): 連線識別符

**使用範例：**
```python
result = await test_connection("analytics_db")
```

### 查詢執行工具

#### execute_query
執行 SELECT 查詢，支援參數化查詢和結果分頁。

**參數：**
- `connection_id` (str): 連線識別符
- `query` (str): SQL 查詢語句
- `params` (List[Any], 可選): 查詢參數
- `fetch_size` (int, 可選): 限制返回行數

**使用範例：**
```python
# 基本查詢
result = await execute_query(
    connection_id="analytics_db",
    query="SELECT * FROM orders WHERE created_at > $1",
    params=["2024-01-01"]
)

# 分頁查詢
result = await execute_query(
    connection_id="analytics_db",
    query="SELECT * FROM users ORDER BY id",
    fetch_size=100
)
```

**返回格式：**
```json
{
  "success": true,
  "rows": [
    {"id": 1, "name": "Alice", "created_at": "2024-01-15"},
    {"id": 2, "name": "Bob", "created_at": "2024-01-16"}
  ],
  "row_count": 2,
  "columns": ["id", "name", "created_at"],
  "duration_ms": 45,
  "query_plan": null
}
```

#### execute_transaction
在事務中執行多個查詢，支援自動回滾。

**參數：**
- `connection_id` (str): 連線識別符
- `queries` (List[Dict]): 查詢列表，每個包含 `query` 和 `params`

**使用範例：**
```python
result = await execute_transaction(
    connection_id="main_db",
    queries=[
        {
            "query": "INSERT INTO orders (user_id, total) VALUES ($1, $2)",
            "params": [123, 299.99]
        },
        {
            "query": "UPDATE inventory SET stock = stock - $1 WHERE product_id = $2",
            "params": [1, 456]
        },
        {
            "query": "INSERT INTO order_items (order_id, product_id, quantity) VALUES (currval('orders_id_seq'), $1, $2)",
            "params": [456, 1]
        }
    ]
)
```

#### batch_execute
批次執行相同查詢，不同參數組合。

**參數：**
- `connection_id` (str): 連線識別符
- `query` (str): SQL 查詢語句
- `params_list` (List[List[Any]]): 參數列表

**使用範例：**
```python
result = await batch_execute(
    connection_id="main_db",
    query="INSERT INTO logs (level, message, created_at) VALUES ($1, $2, $3)",
    params_list=[
        ["INFO", "User login", "2024-01-15 10:00:00"],
        ["ERROR", "Database connection failed", "2024-01-15 10:05:00"],
        ["INFO", "User logout", "2024-01-15 11:00:00"]
    ]
)
```

### Schema 檢查工具

#### get_table_schema
獲取表的完整結構資訊。

**參數：**
- `connection_id` (str): 連線識別符
- `table_name` (str): 表名稱
- `schema` (str, 可選): Schema 名稱 (預設 "public")

**使用範例：**
```python
result = await get_table_schema(
    connection_id="main_db",
    table_name="orders",
    schema="public"
)
```

**返回格式：**
```json
{
  "success": true,
  "table_name": "orders",
  "schema": "public",
  "columns": [
    {
      "name": "id",
      "data_type": "integer",
      "is_nullable": false,
      "default_value": "nextval('orders_id_seq')",
      "ordinal_position": 1
    }
  ],
  "primary_keys": ["id"],
  "indexes": [...],
  "foreign_keys": [...]
}
```

#### list_tables
列出指定 Schema 中的所有表。

**使用範例：**
```python
result = await list_tables(
    connection_id="main_db",
    schema="public"
)
```

#### explain_query
分析查詢執行計畫。

**參數：**
- `connection_id` (str): 連線識別符
- `query` (str): 要分析的 SQL 查詢
- `analyze` (bool, 可選): 是否執行 ANALYZE (預設 false)

**使用範例：**
```python
result = await explain_query(
    connection_id="main_db",
    query="SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id",
    analyze=true
)
```

### 監控工具

#### health_check
檢查系統整體健康狀態。

**使用範例：**
```python
result = await health_check()
```

**返回格式：**
```json
{
  "is_healthy": true,
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00Z",
  "uptime_seconds": 3600,
  "checks": {
    "connections": {
      "is_healthy": true,
      "total_connections": 3,
      "healthy_connections": 3
    },
    "memory": {
      "is_healthy": true,
      "memory_percent": 45.2
    }
  }
}
```

#### get_metrics
獲取伺服器性能指標。

**使用範例：**
```python
result = await get_metrics()
```

## 安全配置

### 安全層級配置

#### 只讀模式
適用於唯讀分析和報告場景：

```bash
READONLY_MODE=true
ALLOWED_OPERATIONS=SELECT,WITH,EXPLAIN
BLOCKED_KEYWORDS=INSERT,UPDATE,DELETE,DROP,TRUNCATE,ALTER,CREATE
```

#### 標準模式
適用於應用程式開發：

```bash
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
MAX_QUERY_LENGTH=10000
```

#### 生產安全模式
適用於生產環境：

```bash
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER,CREATE,GRANT,REVOKE,COPY
MAX_QUERY_LENGTH=5000
ENABLE_QUERY_LOGGING=true
```

### 安全最佳實務

1. **最小權限原則**
   ```sql
   CREATE USER mcp_user WITH PASSWORD 'secure_password';
   GRANT CONNECT ON DATABASE myapp TO mcp_user;
   GRANT USAGE ON SCHEMA public TO mcp_user;
   GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO mcp_user;
   ```

2. **網路安全**
   ```bash
   # 僅允許特定 IP
   DB_HOST=10.0.0.5  # 內部網路

   # 使用 SSL
   DB_SSL_MODE=require
   ```

3. **查詢監控**
   ```bash
   ENABLE_QUERY_LOGGING=true
   LOG_SLOW_QUERIES=true
   SLOW_QUERY_THRESHOLD=1000  # ms
   ```

## 實際使用場景

### 場景一：資料分析和報告

```python
# 1. 建立分析資料庫連線
await add_connection(
    connection_id="analytics",
    host="analytics.company.com",
    database="warehouse",
    user="analyst",
    password="***",
    pool_size=5
)

# 2. 查看表結構
tables = await list_tables("analytics", "public")
schema = await get_table_schema("analytics", "sales_summary")

# 3. 執行分析查詢
monthly_sales = await execute_query(
    "analytics",
    """
    SELECT
        DATE_TRUNC('month', order_date) as month,
        SUM(total_amount) as total_sales,
        COUNT(*) as order_count,
        AVG(total_amount) as avg_order_value
    FROM orders
    WHERE order_date >= $1
    GROUP BY month
    ORDER BY month
    """,
    ["2024-01-01"]
)
```

### 場景二：資料遷移

```python
# 1. 建立來源和目標連線
await add_connection("source_db", "old.server.com", 5432, "legacy", "user", "pass")
await add_connection("target_db", "new.server.com", 5432, "modern", "user", "pass")

# 2. 分析來源資料結構
source_tables = await list_tables("source_db")
for table in source_tables:
    schema = await get_table_schema("source_db", table["table_name"])
    # LLM 分析並決定遷移策略

# 3. 批次遷移資料
source_data = await execute_query(
    "source_db",
    "SELECT * FROM customers ORDER BY id",
    fetch_size=1000
)

# 轉換資料格式 (由 LLM 決定轉換邏輯)
transformed_data = transform_customers(source_data["rows"])

# 批次插入目標資料庫
await batch_execute(
    "target_db",
    "INSERT INTO customers_v2 (id, name, email, phone, created_at) VALUES ($1, $2, $3, $4, $5)",
    transformed_data
)
```

### 場景三：資料庫健康檢查

```python
# 1. 檢查整體健康狀態
health = await health_check()
if not health["is_healthy"]:
    print(f"系統不健康: {health['status']}")

# 2. 獲取詳細指標
metrics = await get_metrics()
print(f"查詢總數: {metrics['total_queries']}")
print(f"成功率: {metrics['successful_queries'] / metrics['total_queries'] * 100:.1f}%")
print(f"平均查詢時間: {metrics['avg_query_time_ms']:.1f}ms")

# 3. 檢查慢查詢
slow_queries = [q for q in query_history if q["duration_ms"] > 1000]
for query in slow_queries:
    plan = await explain_query(query["connection_id"], query["query"], analyze=True)
    # LLM 分析查詢計畫並建議優化
```

## 故障排除

### 常見問題

#### 1. 連線失敗
**錯誤：** `Connection not found: connection_id`

**解決方案：**
```python
# 檢查連線狀態
result = await test_connection("connection_id")
if not result["is_healthy"]:
    # 重新建立連線
    await add_connection(...)
```

#### 2. 查詢被阻擋
**錯誤：** `Query blocked: contains dangerous keyword 'DROP'`

**解決方案：**
```bash
# 檢查安全配置
echo $BLOCKED_KEYWORDS
echo $ALLOWED_OPERATIONS

# 調整配置或修改查詢
```

#### 3. 記憶體不足
**錯誤：** `Memory usage critical: 95.2%`

**解決方案：**
```bash
# 檢查系統資源
curl http://localhost:3000/health

# 調整連線池大小
DEFAULT_POOL_SIZE=5

# 使用分頁查詢
result = await execute_query(..., fetch_size=100)
```

#### 4. 查詢超時
**錯誤：** `Query timeout after 30 seconds`

**解決方案：**
```bash
# 增加超時時間
QUERY_TIMEOUT=60

# 分析查詢效能
result = await explain_query(connection_id, slow_query, analyze=True)
```

### 日誌分析

```bash
# 查看應用程式日誌
./scripts/deploy.sh logs mcp-server

# 查看資料庫日誌
./scripts/deploy.sh logs postgres

# 搜尋錯誤日誌
docker logs mcp-server 2>&1 | grep ERROR

# 搜尋慢查詢
docker logs mcp-server 2>&1 | grep "slow_query"
```

## 性能優化

### 連線池優化

```bash
# 根據負載調整
DEFAULT_POOL_SIZE=20        # 高負載環境
MAX_CONNECTIONS=50          # 最大併發連線
CONNECTION_TIMEOUT=30       # 連線超時
IDLE_CONNECTION_TIMEOUT=300 # 閒置連線超時
```

### 查詢優化

1. **使用 EXPLAIN 分析**
   ```python
   plan = await explain_query(
       "main_db",
       "SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id",
       analyze=True
   )
   ```

2. **批次操作**
   ```python
   # 避免逐條插入
   await batch_execute("main_db", insert_query, all_data)
   ```

3. **分頁查詢**
   ```python
   # 大結果集分頁處理
   result = await execute_query("main_db", query, fetch_size=1000)
   ```

### 監控和告警

```bash
# 設定監控閾值
MEMORY_WARNING_THRESHOLD=80
MEMORY_CRITICAL_THRESHOLD=95
CPU_WARNING_THRESHOLD=80
QUERY_TIME_WARNING_THRESHOLD=1000
```

## API 參考

### 工具返回格式

#### 成功回應
```json
{
  "success": true,
  "data": { ... },
  "metadata": {
    "duration_ms": 45,
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

#### 錯誤回應
```json
{
  "success": false,
  "error": "詳細錯誤訊息",
  "error_code": "CONNECTION_NOT_FOUND",
  "details": {
    "connection_id": "invalid_conn",
    "available_connections": ["main_db", "analytics_db"]
  }
}
```

### HTTP 端點

```bash
# 健康檢查
GET /health

# 指標查詢
GET /metrics

# MCP 工具列表
GET /mcp/tools

# 連線狀態
GET /connections

# 查詢歷史
GET /query_history?connection_id=main_db&limit=100
```

### 環境變數參考

| 變數名稱 | 類型 | 預設值 | 說明 |
|---------|------|---------|------|
| MCP_SERVER_PORT | int | 3000 | MCP Server 連接埠 |
| MCP_LOG_LEVEL | str | INFO | 日誌等級 |
| DEFAULT_POOL_SIZE | int | 10 | 預設連線池大小 |
| QUERY_TIMEOUT | int | 30 | 查詢超時 (秒) |
| MAX_CONNECTIONS | int | 20 | 最大連線數 |
| READONLY_MODE | bool | false | 只讀模式 |
| ALLOWED_OPERATIONS | str | SELECT,INSERT,UPDATE,DELETE | 允許的操作 |
| BLOCKED_KEYWORDS | str | DROP,TRUNCATE,ALTER | 阻擋的關鍵字 |
| MAX_QUERY_LENGTH | int | 10000 | 查詢長度限制 |
| ENABLE_QUERY_LOGGING | bool | true | 啟用查詢日誌 |
| MEMORY_WARNING_THRESHOLD | int | 80 | 記憶體警告閾值 (%) |
| CPU_WARNING_THRESHOLD | int | 80 | CPU 警告閾值 (%) |

---

**需要幫助？**
- 📧 Email: a910413frank@gmail.com
- 🐛 Issues: [GitHub Issues](../../issues)
- 💬 討論: [GitHub Discussions](../../discussions)