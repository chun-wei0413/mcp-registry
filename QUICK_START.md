# PostgreSQL MCP Server - 快速開始指南 ⚡

> 在 5 分鐘內開始使用 PostgreSQL MCP Server！

## 🎯 三種部署方式

### 🚀 方式 1: 一鍵 Docker Hub 部署 (推薦)

**最快速的開始方式 - 30 秒內完成！**

```bash
# 1. 拉取並運行 (一條命令搞定)
docker run -d \
  --name pg-mcp-server \
  -p 3000:3000 \
  -e READONLY_MODE=false \
  russellli/postgresql-mcp-server:latest

# 2. 驗證服務運行
curl http://localhost:3000/health

# ✅ 看到 {"status": "healthy"} 就成功了！
```

### 🔧 方式 2: 完整環境部署 (包含 PostgreSQL)

**適合開發和測試 - 2 分鐘完成！**

1. **建立配置檔案** `docker-compose.yml`:
   ```yaml
   version: '3.8'
   services:
     mcp-server:
       image: russellli/postgresql-mcp-server:latest
       ports:
         - "3000:3000"
       environment:
         - DB_HOST=postgres
         - DB_USER=postgres
         - DB_PASSWORD=mypassword
         - DB_DATABASE=testdb
       depends_on:
         - postgres

     postgres:
       image: postgres:15
       environment:
         - POSTGRES_DB=testdb
         - POSTGRES_USER=postgres
         - POSTGRES_PASSWORD=mypassword
       ports:
         - "5432:5432"
   ```

2. **啟動服務**:
   ```bash
   docker-compose up -d
   docker-compose ps  # 檢查狀態
   ```

### 📁 方式 3: 從原始碼部署

**適合開發貢獻者 - 5 分鐘完成！**

```bash
# 1. 克隆專案
git clone https://github.com/your-repo/postgresql-mcp-server
cd postgresql-mcp-server

# 2. 快速部署
cp .env.example .env
docker-compose -f deployment/docker/docker-compose.yml up -d

# 3. 驗證
./scripts/deploy.sh status
```

## 🧪 快速驗證

### 檢查服務健康

```bash
# 健康檢查
curl http://localhost:3000/health

# 預期回應:
# {"status": "healthy", "timestamp": "...", "uptime_seconds": 123}
```

### 查看服務指標

```bash
curl http://localhost:3000/metrics
```

## 🔌 第一次使用 MCP 工具

### 前置需求

確保你有 MCP 客戶端環境。如果沒有，可以使用 Python 快速測試：

```bash
pip install mcp anthropic-mcp-client
```

### 連線到資料庫

```python
# 使用 MCP 工具連線 PostgreSQL
await add_connection(
    connection_id="my_db",
    host="localhost",     # 或你的資料庫主機
    port=5432,
    database="testdb",
    user="postgres",
    password="mypassword"
)
```

### 執行第一個查詢

```python
# 查看所有表
tables = await list_tables("my_db")
print("可用的表:", tables)

# 執行簡單查詢
result = await execute_query(
    "my_db",
    "SELECT current_database(), current_user, version()"
)
print("查詢結果:", result)
```

### 建立測試資料

```python
# 建立測試表並插入資料
await execute_transaction("my_db", [
    {
        "query": "CREATE TABLE IF NOT EXISTS users (id SERIAL, name TEXT, email TEXT)",
        "params": []
    },
    {
        "query": "INSERT INTO users (name, email) VALUES ($1, $2), ($3, $4)",
        "params": ["Alice", "alice@test.com", "Bob", "bob@test.com"]
    }
])

# 查詢測試資料
users = await execute_query("my_db", "SELECT * FROM users")
print("使用者列表:", users)
```

## 🛡️ 安全配置選項

### 只讀模式 (生產環境推薦)

```bash
docker run -d \
  --name pg-mcp-readonly \
  -p 3000:3000 \
  -e READONLY_MODE=true \
  -e ALLOWED_OPERATIONS=SELECT,WITH,EXPLAIN \
  russellli/postgresql-mcp-server:latest
```

### 開發模式 (允許寫入操作)

```bash
docker run -d \
  --name pg-mcp-dev \
  -p 3000:3000 \
  -e READONLY_MODE=false \
  -e ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN \
  -e BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER \
  russellli/postgresql-mcp-server:latest
```

## 🌟 常見使用場景

### 1. 資料遷移場景

```python
# 從 table1 遷移到 table2
source_data = await execute_query(
    "my_db",
    "SELECT * FROM old_table WHERE created_at > $1",
    ["2024-01-01"]
)

# 批次插入到新表
await batch_execute(
    "my_db",
    "INSERT INTO new_table (col1, col2) VALUES ($1, $2)",
    [[row["col1"], row["col2"]] for row in source_data["rows"]]
)
```

### 2. 資料分析場景

```python
# 分析表結構
schema = await get_table_schema("my_db", "sales")
print("表結構:", schema)

# 分析查詢效能
explain = await explain_query("my_db", "SELECT * FROM sales WHERE amount > 1000")
print("執行計畫:", explain)
```

### 3. 資料庫管理場景

```python
# 檢查系統狀態
health = await health_check()
metrics = await get_metrics()

print(f"系統健康: {health['status']}")
print(f"活躍連線: {metrics['connections']['active']}")
```

## 🔧 自訂配置

### 環境變數配置

建立 `.env` 檔案：

```bash
# 服務設定
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=INFO

# 效能設定
DEFAULT_POOL_SIZE=20
QUERY_TIMEOUT=60

# 安全設定
READONLY_MODE=false
MAX_QUERY_LENGTH=10000
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE

# 預設資料庫 (可選)
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=myapp
DB_USER=myuser
DB_PASSWORD=mypassword
```

使用配置檔案運行：

```bash
docker run -d --env-file .env -p 3000:3000 russellli/postgresql-mcp-server:latest
```

## 🚨 故障排除

### 無法啟動服務？

```bash
# 檢查埠號是否被佔用
netstat -an | grep 3000

# 查看詳細錯誤日誌
docker logs pg-mcp-server

# 嘗試不同埠號
docker run -d -p 3001:3000 russellli/postgresql-mcp-server:latest
```

### 連線資料庫失敗？

```bash
# 檢查資料庫是否可連線
docker exec -it postgres-container psql -U postgres -d testdb

# 檢查網路連通性 (如果使用 Docker Compose)
docker-compose exec mcp-server ping postgres
```

### 權限錯誤？

```bash
# 確認資料庫使用者權限
docker exec -it postgres-container psql -U postgres -c "\du"

# 檢查 MCP Server 安全設定
curl http://localhost:3000/health | jq '.security'
```

## 📚 進階學習

恭喜！你已經成功啟動 PostgreSQL MCP Server。接下來可以：

1. **📖 閱讀完整文件**: [MCP Server 使用手冊](docs/MCP_SERVER_HANDBOOK.md)
2. **🔧 學習進階配置**: [使用者技術指南](docs/guides/USER_GUIDE.md)
3. **🐳 Docker 部署**: [Docker Hub 部署指南](docs/DOCKER_HUB_GUIDE.md)
4. **❓ 查看 FAQ**: [常見問題](QA.md)

## 📞 需要協助？

- 📧 **Email**: a910413frank@gmail.com
- 🐛 **問題回報**: [GitHub Issues](https://github.com/your-repo/postgresql-mcp-server/issues)
- 💬 **討論交流**: [GitHub Discussions](https://github.com/your-repo/postgresql-mcp-server/discussions)

---

> **💡 提示**: 第一次使用建議先在測試環境驗證功能，確認無誤後再部署到生產環境。
>
> **🎯 目標**: 讓你在 5 分鐘內體驗到 PostgreSQL MCP Server 的強大功能！