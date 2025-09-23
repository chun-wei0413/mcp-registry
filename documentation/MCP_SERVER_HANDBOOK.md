# PostgreSQL MCP Server 完整使用手冊

## 📋 目錄

1. [快速開始](#快速開始)
2. [專案結構](#專案結構)
3. [安裝與配置](#安裝與配置)
4. [MCP 工具使用](#mcp-工具使用)
5. [實際應用場景](#實際應用場景)
6. [安全與最佳實務](#安全與最佳實務)
7. [部署與維運](#部署與維運)
8. [故障排除](#故障排除)
9. [擴展與客製化](#擴展與客製化)

---

## 快速開始

### 🚀 一分鐘啟動

```bash
# 1. 克隆專案
git clone <repository-url>
cd postgresql-mcp-server

# 2. 使用 Docker 快速啟動
cp .env.example .env
chmod +x scripts/deploy.sh
./scripts/deploy.sh

# 3. 驗證服務
curl http://localhost:3000/health
```

### 📦 服務組件

- **MCP Server**: 核心 MCP 服務 (Port: 3000)
- **PostgreSQL**: 測試資料庫 (Port: 5432)
- **監控**: 健康檢查和指標收集

---

## 專案結構

```
postgresql-mcp-server/
├── src/                    # 核心程式碼
│   ├── tools/             # MCP 工具實現
│   ├── db/                # 資料庫層
│   ├── models/            # 資料模型
│   ├── server.py          # MCP Server 主程式
│   ├── security.py        # 安全驗證
│   ├── monitoring.py      # 監控模組
│   └── config.py          # 配置管理
├── deployment/            # 部署相關
│   ├── docker/           # Docker 配置
│   │   ├── Dockerfile
│   │   ├── docker-compose.yml
│   │   └── .dockerignore
│   └── db/               # 資料庫初始化
│       ├── init-db.sql
│       └── init-db-dev.sql
├── documentation/         # 文檔中心
│   ├── guides/           # 使用指南
│   │   └── USER_GUIDE.md
│   └── MCP_SERVER_HANDBOOK.md
├── tests/                 # 測試套件
│   ├── unit/             # 單元測試
│   └── integration/      # 整合測試
├── scripts/               # 部署腳本
│   ├── deploy.sh         # 生產部署
│   └── dev.sh            # 開發環境
└── run_tests.py          # 測試執行器
```

---

## 安裝與配置

### 🐳 Docker 部署 (推薦)

#### 生產環境

```bash
# 1. 準備配置
cp .env.example .env
nano .env  # 編輯配置

# 2. 部署
./scripts/deploy.sh deploy

# 3. 檢查狀態
./scripts/deploy.sh status
```

#### 開發環境

```bash
# 1. 啟動開發環境
./scripts/dev.sh start

# 2. 查看日誌
./scripts/dev.sh logs mcp-server

# 3. 重啟服務
./scripts/dev.sh restart
```

### 🐍 本地開發

```bash
# 1. 建立虛擬環境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 2. 安裝依賴
pip install -e .[dev,test]

# 3. 配置環境變數
export $(cat .env | xargs)

# 4. 啟動 MCP Server
python -m src.server
```

### ⚙️ 環境配置

#### 基本配置 (.env)

```bash
# === MCP Server 配置 ===
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=INFO
DEFAULT_POOL_SIZE=10
QUERY_TIMEOUT=30
MAX_CONNECTIONS=20

# === 安全配置 ===
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
MAX_QUERY_LENGTH=10000
ENABLE_QUERY_LOGGING=true

# === 預設資料庫連線 (可選) ===
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=postgres
DB_USER=postgres
DB_PASSWORD=postgres
DB_POOL_SIZE=10

# === 監控配置 ===
ENABLE_METRICS=true
HEALTH_CHECK_INTERVAL=30
MEMORY_WARNING_THRESHOLD=80
CPU_WARNING_THRESHOLD=80
```

#### 安全配置範例

**只讀模式 (分析/報告)**
```bash
READONLY_MODE=true
ALLOWED_OPERATIONS=SELECT,WITH,EXPLAIN
BLOCKED_KEYWORDS=INSERT,UPDATE,DELETE,DROP,TRUNCATE,ALTER,CREATE
```

**開發模式**
```bash
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
MAX_QUERY_LENGTH=10000
```

**生產模式**
```bash
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER,CREATE,GRANT,REVOKE,COPY
MAX_QUERY_LENGTH=5000
ENABLE_QUERY_LOGGING=true
```

---

## MCP 工具使用

### 🔗 連線管理

#### add_connection - 建立連線

**用途**: 建立新的資料庫連線並設定連線池

**參數**:
- `connection_id` (str): 連線唯一識別符
- `host` (str): 資料庫主機位址
- `port` (int): 連接埠 (預設: 5432)
- `database` (str): 資料庫名稱
- `user` (str): 使用者名稱
- `password` (str): 密碼
- `pool_size` (int): 連線池大小 (預設: 10)

**範例**:
```python
result = await add_connection(
    connection_id="analytics_db",
    host="analytics.company.com",
    port=5432,
    database="warehouse",
    user="analyst",
    password="secure_password",
    pool_size=15
)
```

**回應格式**:
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

#### test_connection - 測試連線

**用途**: 驗證連線健康狀態

```python
result = await test_connection("analytics_db")
```

### 📊 查詢執行

#### execute_query - 執行查詢

**用途**: 執行 SELECT 查詢 (僅限讀取操作)

**參數**:
- `connection_id` (str): 連線識別符
- `query` (str): SQL 查詢語句
- `params` (List[Any], 可選): 參數化查詢參數
- `fetch_size` (int, 可選): 結果集大小限制

**範例**:
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

#### execute_transaction - 事務執行

**用途**: 在事務中執行多個查詢，支援自動回滾

**參數**:
- `connection_id` (str): 連線識別符
- `queries` (List[Dict]): 查詢列表

**範例**:
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
        }
    ]
)
```

#### batch_execute - 批次執行

**用途**: 批次執行相同查詢，不同參數

**範例**:
```python
result = await batch_execute(
    connection_id="main_db",
    query="INSERT INTO logs (level, message, timestamp) VALUES ($1, $2, $3)",
    params_list=[
        ["INFO", "User login", "2024-01-15 10:00:00"],
        ["ERROR", "Connection failed", "2024-01-15 10:05:00"],
        ["INFO", "User logout", "2024-01-15 11:00:00"]
    ]
)
```

### 🗂️ Schema 檢查

#### get_table_schema - 獲取表結構

```python
result = await get_table_schema(
    connection_id="main_db",
    table_name="orders",
    schema="public"
)
```

**回應包含**:
- 欄位定義 (名稱、類型、約束)
- 主鍵資訊
- 索引列表
- 外鍵關係

#### list_tables - 列出所有表

```python
tables = await list_tables(
    connection_id="main_db",
    schema="public"
)
```

#### explain_query - 查詢計畫分析

```python
plan = await explain_query(
    connection_id="main_db",
    query="SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id",
    analyze=True
)
```

### 🔍 監控工具

#### health_check - 健康檢查

```python
status = await health_check()
```

**檢查項目**:
- 連線狀態
- 記憶體使用率
- CPU 負載
- 磁碟空間
- 系統運行時間

#### get_metrics - 性能指標

```python
metrics = await get_metrics()
```

**指標內容**:
- 查詢統計 (成功/失敗率)
- 平均回應時間
- 連線池狀態
- 系統資源使用

---

## 實際應用場景

### 📈 場景 1: 商業智能分析

**背景**: 需要從多個資料庫提取資料進行分析

**步驟**:

1. **建立多個連線**
```python
# 銷售資料庫
await add_connection(
    connection_id="sales_db",
    host="sales.company.com",
    database="sales",
    user="analyst",
    password="***"
)

# 客戶資料庫
await add_connection(
    connection_id="crm_db",
    host="crm.company.com",
    database="customer",
    user="analyst",
    password="***"
)
```

2. **探索資料結構**
```python
# 檢視銷售表結構
sales_tables = await list_tables("sales_db")
orders_schema = await get_table_schema("sales_db", "orders")

# 檢視客戶表結構
customer_schema = await get_table_schema("crm_db", "customers")
```

3. **執行分析查詢**
```python
# 月度銷售分析
monthly_sales = await execute_query(
    "sales_db",
    """
    SELECT
        DATE_TRUNC('month', order_date) as month,
        SUM(total_amount) as revenue,
        COUNT(*) as order_count,
        AVG(total_amount) as avg_order_value
    FROM orders
    WHERE order_date >= $1
    GROUP BY month
    ORDER BY month
    """,
    ["2024-01-01"]
)

# 客戶分析
customer_analysis = await execute_query(
    "crm_db",
    """
    SELECT
        customer_segment,
        COUNT(*) as customer_count,
        AVG(lifetime_value) as avg_ltv
    FROM customers
    WHERE created_at >= $1
    GROUP BY customer_segment
    """,
    ["2024-01-01"]
)
```

### 🔄 場景 2: 資料庫遷移

**背景**: 從舊系統遷移到新架構

**步驟**:

1. **建立來源和目標連線**
```python
await add_connection("legacy_db", "old-server", 5432, "legacy_app", "user", "pass")
await add_connection("modern_db", "new-server", 5432, "modern_app", "user", "pass")
```

2. **分析來源資料**
```python
# 獲取所有表
legacy_tables = await list_tables("legacy_db")

# 分析每個表的結構
for table in legacy_tables:
    schema = await get_table_schema("legacy_db", table["table_name"])
    # LLM 分析結構，決定遷移策略
```

3. **執行資料遷移**
```python
# 分批提取來源資料
source_data = await execute_query(
    "legacy_db",
    "SELECT * FROM customers ORDER BY id LIMIT 1000 OFFSET $1",
    [batch_offset]
)

# 轉換資料格式 (由 LLM 決定轉換邏輯)
transformed_data = transform_customer_data(source_data["rows"])

# 批次插入目標資料庫
await batch_execute(
    "modern_db",
    "INSERT INTO customers_v2 (id, name, email, phone, created_at) VALUES ($1, $2, $3, $4, $5)",
    transformed_data
)
```

4. **驗證資料完整性**
```python
# 比對記錄數量
legacy_count = await execute_query("legacy_db", "SELECT COUNT(*) FROM customers")
modern_count = await execute_query("modern_db", "SELECT COUNT(*) FROM customers_v2")

# 資料品質檢查
quality_check = await execute_query(
    "modern_db",
    "SELECT COUNT(*) as invalid_emails FROM customers_v2 WHERE email !~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'"
)
```

### 🔧 場景 3: 系統維運監控

**背景**: 監控多個資料庫系統的健康狀態

**監控腳本**:
```python
async def monitor_databases():
    # 檢查整體系統健康
    health = await health_check()
    if not health["is_healthy"]:
        send_alert(f"MCP Server unhealthy: {health['status']}")

    # 檢查各個資料庫連線
    connections = ["prod_db", "analytics_db", "backup_db"]
    for conn_id in connections:
        conn_status = await test_connection(conn_id)
        if not conn_status["is_healthy"]:
            send_alert(f"Database {conn_id} connection failed")

    # 獲取性能指標
    metrics = await get_metrics()

    # 檢查慢查詢
    if metrics["avg_query_time_ms"] > 1000:
        send_alert(f"High average query time: {metrics['avg_query_time_ms']}ms")

    # 檢查失敗率
    failure_rate = metrics["failed_queries"] / metrics["total_queries"] * 100
    if failure_rate > 5:
        send_alert(f"High query failure rate: {failure_rate:.1f}%")
```

### 🚨 場景 4: 故障排除與性能優化

**慢查詢分析**:
```python
# 找出慢查詢
slow_queries = [q for q in query_history if q["duration_ms"] > 1000]

for query in slow_queries:
    # 分析查詢計畫
    plan = await explain_query(
        query["connection_id"],
        query["query"],
        analyze=True
    )

    # LLM 分析計畫並提供優化建議
    optimization_suggestions = analyze_query_plan(plan)

    print(f"Slow Query: {query['query'][:100]}...")
    print(f"Duration: {query['duration_ms']}ms")
    print(f"Suggestions: {optimization_suggestions}")
```

**索引建議**:
```python
# 檢查表統計資訊
table_stats = await execute_query(
    "main_db",
    """
    SELECT
        schemaname,
        tablename,
        n_tup_ins + n_tup_upd + n_tup_del as total_operations,
        seq_scan,
        seq_tup_read,
        idx_scan,
        idx_tup_fetch
    FROM pg_stat_user_tables
    WHERE seq_scan > idx_scan * 10
    ORDER BY seq_scan DESC
    """
)

# 針對高順序掃描的表建議建立索引
for table in table_stats["rows"]:
    if table["seq_scan"] > 1000:
        print(f"Table {table['tablename']} has {table['seq_scan']} sequential scans")
        # LLM 分析並建議適當的索引
```

---

## 安全與最佳實務

### 🛡️ 安全配置

#### 1. 權限控制

**資料庫使用者權限**:
```sql
-- 建立 MCP 專用使用者
CREATE USER mcp_user WITH PASSWORD 'strong_password';

-- 只讀權限 (分析場景)
GRANT CONNECT ON DATABASE analytics TO mcp_user;
GRANT USAGE ON SCHEMA public TO mcp_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO mcp_user;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO mcp_user;

-- 有限寫入權限 (應用場景)
GRANT INSERT, UPDATE, DELETE ON specific_tables TO mcp_user;

-- 禁止危險操作
REVOKE CREATE ON SCHEMA public FROM mcp_user;
REVOKE DROP ON ALL TABLES IN SCHEMA public FROM mcp_user;
```

**網路安全**:
```bash
# 限制來源 IP
DB_HOST=10.0.1.100  # 內部網路

# 啟用 SSL
DB_SSL_MODE=require
DB_SSL_CERT=/path/to/client.crt
DB_SSL_KEY=/path/to/client.key
```

#### 2. 查詢安全

**參數化查詢** (必須):
```python
# ✅ 正確: 參數化查詢
result = await execute_query(
    "main_db",
    "SELECT * FROM users WHERE email = $1",
    ["user@example.com"]
)

# ❌ 錯誤: 字串拼接 (SQL Injection 風險)
# query = f"SELECT * FROM users WHERE email = '{email}'"
```

**查詢白名單**:
```bash
# 限制允許的操作
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE

# 阻擋危險關鍵字
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER,CREATE,GRANT,REVOKE,COPY
```

### 📋 最佳實務清單

#### 開發階段
- [ ] 使用參數化查詢
- [ ] 設定適當的查詢超時
- [ ] 實施查詢長度限制
- [ ] 啟用查詢日誌記錄
- [ ] 建立完整的錯誤處理

#### 部署階段
- [ ] 使用強密碼和 SSL 連線
- [ ] 設定防火牆規則
- [ ] 建立監控和告警
- [ ] 定期備份配置
- [ ] 設定日誌輪轉

#### 維運階段
- [ ] 定期檢查安全日誌
- [ ] 監控查詢性能
- [ ] 更新依賴套件
- [ ] 執行滲透測試
- [ ] 審核使用者權限

---

## 部署與維運

### 🚀 生產部署

#### Docker Compose 部署

**準備環境**:
```bash
# 1. 建立生產配置
cp .env.example .env.prod
nano .env.prod

# 2. 設定生產參數
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=WARNING
READONLY_MODE=false
ENABLE_QUERY_LOGGING=true
DB_HOST=prod-postgres.company.com
DB_PASSWORD=<strong-password>
```

**部署命令**:
```bash
# 使用生產配置部署
docker-compose -f deployment/docker/docker-compose.yml --env-file .env.prod up -d

# 檢查服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f mcp-server
```

#### Kubernetes 部署

**ConfigMap**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mcp-server-config
data:
  .env: |
    MCP_SERVER_PORT=3000
    MCP_LOG_LEVEL=INFO
    DEFAULT_POOL_SIZE=10
    READONLY_MODE=false
```

**Deployment**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mcp-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mcp-server
  template:
    metadata:
      labels:
        app: mcp-server
    spec:
      containers:
      - name: mcp-server
        image: postgresql-mcp-server:latest
        ports:
        - containerPort: 3000
        envFrom:
        - configMapRef:
            name: mcp-server-config
        resources:
          limits:
            memory: "512Mi"
            cpu: "500m"
          requests:
            memory: "256Mi"
            cpu: "250m"
```

### 📊 監控與告警

#### 健康檢查端點
```bash
# Kubernetes 健康檢查
livenessProbe:
  httpGet:
    path: /health
    port: 3000
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /health
    port: 3000
  initialDelaySeconds: 5
  periodSeconds: 5
```

#### 指標收集
```bash
# Prometheus 指標
curl http://localhost:3000/metrics

# 主要指標
- mcp_server_queries_total
- mcp_server_query_duration_seconds
- mcp_server_connections_active
- mcp_server_memory_usage_bytes
```

#### 告警規則 (Prometheus)
```yaml
groups:
- name: mcp-server
  rules:
  - alert: MCPServerDown
    expr: up{job="mcp-server"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "MCP Server is down"

  - alert: HighQueryLatency
    expr: avg(mcp_server_query_duration_seconds) > 1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High query latency detected"

  - alert: HighMemoryUsage
    expr: mcp_server_memory_usage_bytes / (1024*1024*1024) > 1
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "High memory usage"
```

### 🔄 更新與維護

#### 滾動更新
```bash
# 1. 建置新版本
docker build -t postgresql-mcp-server:v1.1.0 .

# 2. 標記版本
docker tag postgresql-mcp-server:v1.1.0 postgresql-mcp-server:latest

# 3. 滾動更新
kubectl set image deployment/mcp-server mcp-server=postgresql-mcp-server:v1.1.0

# 4. 檢查更新狀態
kubectl rollout status deployment/mcp-server
```

#### 備份策略
```bash
# 配置備份
tar -czf mcp-server-config-$(date +%Y%m%d).tar.gz .env* deployment/

# 資料庫備份 (如果有持久化資料)
kubectl exec -it postgres-pod -- pg_dump -U postgres > backup-$(date +%Y%m%d).sql
```

---

## 故障排除

### 🚨 常見問題

#### 1. 連線問題

**問題**: `Connection not found: connection_id`

**診斷步驟**:
```bash
# 檢查服務狀態
curl http://localhost:3000/health

# 查看連線狀態
curl http://localhost:3000/connections

# 檢查日誌
docker logs mcp-server | grep ERROR
```

**解決方案**:
```python
# 測試連線
result = await test_connection("problematic_connection")

# 重新建立連線
if not result["is_healthy"]:
    await add_connection(
        connection_id="problematic_connection",
        host="...",
        # ... 其他參數
    )
```

#### 2. 查詢被阻擋

**問題**: `Query blocked: contains dangerous keyword 'DROP'`

**解決方案**:
```bash
# 檢查安全配置
echo $BLOCKED_KEYWORDS
echo $ALLOWED_OPERATIONS

# 調整配置 (謹慎!)
BLOCKED_KEYWORDS=TRUNCATE,ALTER  # 移除 DROP

# 或修改查詢避免危險關鍵字
```

#### 3. 記憶體不足

**問題**: `Memory usage critical: 95.2%`

**診斷**:
```bash
# 檢查系統資源
curl http://localhost:3000/metrics | grep memory

# 檢查連線池配置
echo $DEFAULT_POOL_SIZE
```

**解決方案**:
```bash
# 減少連線池大小
DEFAULT_POOL_SIZE=5

# 使用分頁查詢
result = await execute_query(..., fetch_size=100)

# 增加系統記憶體或限制容器記憶體
```

#### 4. 查詢超時

**問題**: `Query timeout after 30 seconds`

**診斷**:
```python
# 分析查詢計畫
plan = await explain_query(
    connection_id="main_db",
    query="SELECT * FROM large_table WHERE condition",
    analyze=True
)
```

**解決方案**:
```bash
# 增加超時時間
QUERY_TIMEOUT=60

# 優化查詢
# - 新增索引
# - 改善 WHERE 條件
# - 使用分頁
```

### 🔍 日誌分析

#### 應用程式日誌
```bash
# 查看所有日誌
docker logs mcp-server

# 過濾錯誤日誌
docker logs mcp-server 2>&1 | grep ERROR

# 查看慢查詢
docker logs mcp-server 2>&1 | grep "slow_query"

# 即時監控
docker logs -f mcp-server
```

#### 結構化日誌查詢
```bash
# 使用 jq 解析 JSON 日誌
docker logs mcp-server 2>&1 | jq 'select(.level == "ERROR")'

# 查詢特定連線的日誌
docker logs mcp-server 2>&1 | jq 'select(.connection_id == "main_db")'

# 統計查詢類型
docker logs mcp-server 2>&1 | jq -r '.query_type' | sort | uniq -c
```

### 📊 性能調優

#### 連線池調優
```bash
# 監控連線池狀態
curl http://localhost:3000/metrics | grep pool

# 調整參數
DEFAULT_POOL_SIZE=15      # 根據併發需求
MAX_CONNECTIONS=50        # 總連線限制
CONNECTION_TIMEOUT=30     # 連線超時
```

#### 查詢優化
```python
# 使用 EXPLAIN 分析慢查詢
async def analyze_slow_queries():
    slow_queries = get_slow_queries_from_logs()

    for query in slow_queries:
        plan = await explain_query(
            query["connection_id"],
            query["sql"],
            analyze=True
        )

        # 檢查是否需要索引
        if "Seq Scan" in str(plan):
            print(f"Consider adding index for: {query['sql']}")

        # 檢查是否有笛卡爾積
        if "Nested Loop" in str(plan) and "rows=" in str(plan):
            print(f"Potential cartesian product: {query['sql']}")
```

---

## 擴展與客製化

### 🔧 新增自定義 MCP 工具

#### 建立新工具

**1. 在 `src/tools/` 建立新模組**:
```python
# src/tools/custom_analytics.py

from typing import Dict, Any, List, Optional
import asyncio

class AnalyticsTool:
    def __init__(self, connection_manager, security_validator):
        self.connection_manager = connection_manager
        self.security_validator = security_validator

    async def calculate_revenue_metrics(
        self,
        connection_id: str,
        start_date: str,
        end_date: str,
        group_by: str = "month"
    ) -> Dict[str, Any]:
        """計算收入指標"""

        # 驗證輸入
        if group_by not in ["day", "week", "month", "quarter"]:
            return {"success": False, "error": "Invalid group_by parameter"}

        # 建構查詢
        query = f"""
        SELECT
            DATE_TRUNC('{group_by}', order_date) as period,
            SUM(total_amount) as total_revenue,
            COUNT(*) as order_count,
            AVG(total_amount) as avg_order_value,
            COUNT(DISTINCT customer_id) as unique_customers
        FROM orders
        WHERE order_date BETWEEN $1 AND $2
            AND status = 'completed'
        GROUP BY period
        ORDER BY period
        """

        try:
            pool = self.connection_manager.get_pool(connection_id)
            if not pool:
                return {"success": False, "error": "Connection not found"}

            async with pool.acquire() as conn:
                rows = await conn.fetch(query, start_date, end_date)

                return {
                    "success": True,
                    "metrics": [dict(row) for row in rows],
                    "period_type": group_by,
                    "date_range": {"start": start_date, "end": end_date}
                }

        except Exception as e:
            return {"success": False, "error": str(e)}
```

**2. 在 server.py 中註冊工具**:
```python
# src/server.py

from .tools.custom_analytics import AnalyticsTool

class PostgreSQLMCPServer:
    def __init__(self, config_file: Optional[str] = None):
        # ... 現有初始化代碼

        # 初始化自定義工具
        self.analytics_tool = AnalyticsTool(
            self.connection_manager,
            self.security_validator
        )

    def setup_mcp_tools(self):
        # ... 現有工具註冊

        # 註冊自定義工具
        @self.app.tool()
        async def calculate_revenue_metrics(
            connection_id: str,
            start_date: str,
            end_date: str,
            group_by: str = "month"
        ):
            """計算指定期間的收入指標"""
            return await self.analytics_tool.calculate_revenue_metrics(
                connection_id, start_date, end_date, group_by
            )
```

#### 新增安全驗證規則

```python
# src/security.py

class SecurityValidator:
    def _check_custom_patterns(self, query: str) -> List[str]:
        """自定義安全檢查"""
        risks = []

        # 檢查敏感表格存取
        sensitive_tables = ["users", "payments", "personal_data"]
        for table in sensitive_tables:
            if table.lower() in query.lower():
                risks.append(f"Access to sensitive table: {table}")

        # 檢查大量資料查詢
        if "SELECT *" in query.upper() and "LIMIT" not in query.upper():
            risks.append("Potential large dataset query without LIMIT")

        return risks
```

### 📊 自定義監控指標

```python
# src/monitoring.py

class CustomMetricsCollector:
    def __init__(self):
        self.business_metrics = {}

    def record_business_event(self, event_type: str, value: float = 1.0):
        """記錄業務事件"""
        if event_type not in self.business_metrics:
            self.business_metrics[event_type] = []

        self.business_metrics[event_type].append({
            "timestamp": datetime.utcnow(),
            "value": value
        })

    async def get_business_metrics(self) -> Dict[str, Any]:
        """獲取業務指標"""
        metrics = {}

        for event_type, events in self.business_metrics.items():
            recent_events = [
                e for e in events
                if (datetime.utcnow() - e["timestamp"]).seconds < 3600
            ]

            metrics[event_type] = {
                "count": len(recent_events),
                "sum": sum(e["value"] for e in recent_events),
                "avg": sum(e["value"] for e in recent_events) / len(recent_events) if recent_events else 0
            }

        return metrics
```

### 🎯 配置模板

#### 不同環境的配置範本

**開發環境** (`.env.dev`):
```bash
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=DEBUG
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
BLOCKED_KEYWORDS=DROP,TRUNCATE
MAX_QUERY_LENGTH=50000
ENABLE_QUERY_LOGGING=true
```

**測試環境** (`.env.test`):
```bash
MCP_SERVER_PORT=3001
MCP_LOG_LEVEL=INFO
READONLY_MODE=true
ALLOWED_OPERATIONS=SELECT,WITH,EXPLAIN
BLOCKED_KEYWORDS=INSERT,UPDATE,DELETE,DROP,TRUNCATE,ALTER
MAX_QUERY_LENGTH=10000
```

**生產環境** (`.env.prod`):
```bash
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=WARNING
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER,CREATE,GRANT,REVOKE,COPY
MAX_QUERY_LENGTH=5000
ENABLE_QUERY_LOGGING=true
MEMORY_WARNING_THRESHOLD=85
CPU_WARNING_THRESHOLD=85
```

---

## 📞 支援與社群

### 🆘 取得幫助

- **📧 Email**: a910413frank@gmail.com
- **🐛 Bug 回報**: [GitHub Issues](../../issues)
- **💬 討論**: [GitHub Discussions](../../discussions)
- **📖 文件**: [完整文件](../../docs)

### 🤝 貢獻指南

歡迎貢獻代碼！請先閱讀：
1. Fork 專案
2. 建立功能分支
3. 撰寫測試
4. 提交 Pull Request

### 📄 授權資訊

本專案使用 MIT 授權 - 詳見 [LICENSE](../../LICENSE) 檔案

---

**🎯 記住**: PostgreSQL MCP Server 是純工具層，提供強大的資料庫操作能力，讓 LLM 能夠執行複雜的資料任務。工具提供能力，LLM 提供智慧！