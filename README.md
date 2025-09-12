# PostgreSQL MCP Server

一個通用的 PostgreSQL MCP Server，作為純工具層，讓 LLM 能透過 MCP 協定執行智能資料操作。

## 特色

- 🔧 **純工具層**: 不包含業務邏輯，所有智能決策由 LLM 完成
- 🚀 **異步處理**: 全異步 I/O，支援高並發查詢
- 🔒 **安全性**: SQL 注入防護、密碼加密、權限控制
- 📊 **完整功能**: 查詢、事務、批次操作、Schema 檢查
- 🎯 **高性能**: 連線池管理、查詢優化
- 📝 **可觀測性**: 結構化日誌、查詢歷史

## 安裝

```bash
# 從 PyPI 安裝（未來）
pip install postgresql-mcp-server

# 或從原始碼安裝
git clone https://github.com/chun-wei0413/pg-mcp.git
cd pg-mcp
pip install -e .
```

## 快速開始

### 1. 啟動 MCP Server

```bash
postgresql-mcp-server
```

### 2. 基本使用流程

```python
# 1. 建立連線
await add_connection(
    connection_id="my_db",
    host="localhost",
    port=5432,
    database="myapp",
    user="postgres",
    password="password"
)

# 2. 查看表結構
schema = await get_table_schema("my_db", "users")

# 3. 執行查詢
result = await execute_query(
    "my_db",
    "SELECT * FROM users WHERE created_at > $1",
    ["2024-01-01"]
)

# 4. 執行事務
await execute_transaction("my_db", [
    {
        "query": "INSERT INTO users (name, email) VALUES ($1, $2)",
        "params": ["John Doe", "john@example.com"]
    },
    {
        "query": "UPDATE user_stats SET total_users = total_users + 1",
        "params": []
    }
])
```

## MCP 工具

### 連線管理

- `add_connection`: 建立資料庫連線
- `test_connection`: 測試連線狀態

### 查詢執行

- `execute_query`: 執行 SELECT 查詢
- `execute_transaction`: 事務中執行多個查詢
- `batch_execute`: 批次執行相同查詢

### Schema 檢查

- `get_table_schema`: 獲取表結構詳情
- `list_tables`: 列出所有表
- `explain_query`: 分析查詢執行計畫

## MCP 資源

- `connections`: 所有活躍連線資訊
- `query_history`: 查詢歷史記錄

## 配置

### 環境變數

```bash
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=INFO
DEFAULT_POOL_SIZE=10
QUERY_TIMEOUT=30
POSTGRES_MCP_ENCRYPTION_KEY=your-secret-key
```

### 連線池配置

- 最小連線數: 2
- 最大連線數: 20
- 連線超時: 30秒

## 安全性

- ✅ 參數化查詢防止 SQL 注入
- ✅ 密碼加密儲存
- ✅ 連線池管理
- ✅ 查詢超時控制
- ✅ 錯誤資訊過濾

## 開發

### 安裝開發依賴

```bash
pip install -e ".[dev]"
```

### 執行測試

```bash
pytest
```

### 程式碼格式化

```bash
black src tests
ruff check src tests
```

### 型別檢查

```bash
mypy src
```

## Docker 支援

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY . .
RUN pip install -e .
CMD ["postgresql-mcp-server"]
```

## 授權

MIT License

## 貢獻

歡迎提交 Issue 和 Pull Request！

## 相關資源

- [MCP 官方文件](https://modelcontextprotocol.io/)
- [asyncpg 文件](https://magicstack.github.io/asyncpg/)
- [PostgreSQL 文件](https://www.postgresql.org/docs/)