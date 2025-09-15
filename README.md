# PostgreSQL MCP Server

一個通用的 PostgreSQL MCP Server，為 LLM 提供智能資料庫操作能力。此 Server 作為純工具層，不包含任何業務邏輯，所有智能決策由 LLM 根據 Context 自主完成。

## 🆕 版本 0.2.0 更新內容

### 🐛 重要 Bug 修復
- **修復 Pydantic 驗證錯誤**: 解決了 `QueryResult` 模型中 `query_id` 和 `execution_time_ms` 的型別驗證問題
- **修復欄位名稱不一致**: 統一使用 `row_count` 和 `message` 欄位，避免模型錯誤
- **修復浮點數精度問題**: `execution_time_ms` 現在正確轉換為整數，避免 Pydantic 驗證失敗

### ✨ 新增功能
- **增強 Docker HTTP 支援**: 新增 `run_sync_http()` 方法，改善與 Claude Code 的整合
- **改善安全驗證器**: 優化正則表達式模式，減少誤報並提高精確度
- **完善錯誤處理**: 更準確的錯誤訊息和結構化回應

### 🔧 改善項目
- **更穩定的批次操作**: 修復批次執行和事務操作的各種邊界情況
- **更好的 Claude Code 整合**: 使用 `stateless_http=True` 提供更好的會話管理
- **優化 Docker 部署**: 改善容器啟動和連線管理

### ✅ 驗證結果
- **通過完整測試**: 成功執行 10 筆測試資料的 MCP 寫入操作
- **連線穩定性**: 解決了 Docker 網路連線問題，確保可靠的資料庫存取
- **型別安全**: 所有 Pydantic 模型驗證通過，確保資料完整性

## 🚀 特性

- **🔒 安全性第一**: 參數化查詢、SQL 注入防護、危險操作阻擋
- **⚡ 高效能**: 異步連線池、批次操作、查詢快取
- **🔍 可觀測性**: 結構化日誌、健康檢查、效能監控
- **🛡️ 安全配置**: 只讀模式、操作白名單、查詢長度限制
- **🔧 易於部署**: Docker 支援、環境配置、一鍵部署
- **🧪 完整測試**: 單元測試、整合測試、安全測試

## 📖 文件

### 🚀 快速開始
- ⚡ [**快速開始指南**](QUICK_START.md) - **5分鐘內開始使用！**
- 🐳 [**Docker Hub 使用指南**](docs/DOCKER_HUB_GUIDE.md) - **官方映像檔部署指南**

### 📚 完整文件
- 📚 [文件中心](docs/README.md) - 完整的文件導覽和快速導引
- 🚀 [MCP Server 完整使用手冊](docs/MCP_SERVER_HANDBOOK.md) - 從入門到進階的一站式指南
- 📋 [使用者指南](docs/guides/USER_GUIDE.md) - 技術細節和 API 參考

### 💡 實用範例
- 🔌 [MCP 客戶端整合範例](docs/examples/MCP_CLIENT_EXAMPLES.md) - Python、Node.js、Claude Desktop 整合
- 🎯 [常見使用場景](docs/USE_CASES.md) - 資料遷移、分析、監控等實際應用
- ❓ [常見問題 FAQ](QA.md) - 用戶常見問題解答

### 🛡️ 安全與部署
- 🛡️ [安全配置](docs/MCP_SERVER_HANDBOOK.md#安全與最佳實務) - 安全最佳實務
- 🚀 [部署指南](docs/MCP_SERVER_HANDBOOK.md#部署與維運) - 生產部署說明

## 📋 系統需求

- Python 3.11+
- PostgreSQL 12+
- Docker & Docker Compose (可選)

## 🔧 快速開始

### 使用 Docker Hub 官方映像檔 (推薦)

```bash
# 直接從 Docker Hub 拉取並運行
docker run -d \
  --name postgresql-mcp-server \
  -p 3000:3000 \
  -e MCP_SERVER_PORT=3000 \
  -e MCP_LOG_LEVEL=INFO \
  -e DEFAULT_POOL_SIZE=10 \
  -e QUERY_TIMEOUT=30 \
  -e READONLY_MODE=false \
  -e ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN \
  -e BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER \
  -e MAX_QUERY_LENGTH=10000 \
  russellli/postgresql-mcp-server:latest

# 查看運行狀態
docker ps
docker logs postgresql-mcp-server
```

#### 使用 docker-compose

```yaml
version: '3.8'
services:
  mcp-server:
    image: russellli/postgresql-mcp-server:latest
    ports:
      - "3000:3000"
    environment:
      - MCP_SERVER_PORT=3000
      - MCP_LOG_LEVEL=INFO
      - DEFAULT_POOL_SIZE=10
      - QUERY_TIMEOUT=30
      - READONLY_MODE=false
      - ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
      - BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
      - MAX_QUERY_LENGTH=10000
    restart: unless-stopped
```

### 從原始碼建置

```bash
# 克隆專案
git clone <repository-url>
cd postgresql-mcp-server

# 複製並編輯配置檔案
cp .env.example .env

# 啟動所有服務 (使用新的檔案路徑)
docker-compose -f deployment/docker/docker-compose.yml up -d

# 查看服務狀態
./scripts/deploy.sh status
```

### 本地開發

```bash
# 安裝依賴
pip install -e .[dev,test]

# 複製配置檔案
cp .env.example .env
# 編輯 .env 設定資料庫連線

# 啟動開發環境
./scripts/dev.sh start

# 運行測試
python run_tests.py all
```

## ⚙️ 配置

### 環境變數

```bash
# 伺服器配置
MCP_SERVER_PORT=3000
MCP_LOG_LEVEL=INFO
DEFAULT_POOL_SIZE=10
QUERY_TIMEOUT=30

# 安全配置
READONLY_MODE=false
ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
MAX_QUERY_LENGTH=10000

# 資料庫連線
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=your_database
DB_USER=your_username
DB_PASSWORD=your_password
```

### 安全模式

#### 只讀模式
```bash
READONLY_MODE=true
ALLOWED_OPERATIONS=SELECT,WITH,EXPLAIN
```

#### 生產安全配置
```bash
BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER,CREATE,GRANT,REVOKE
MAX_QUERY_LENGTH=5000
ENABLE_QUERY_LOGGING=true
```

## 🛠️ MCP 工具

### 連線管理
- `add_connection` - 建立資料庫連線
- `test_connection` - 測試連線狀態

### 查詢執行
- `execute_query` - 執行 SELECT 查詢
- `execute_transaction` - 事務執行
- `batch_execute` - 批次操作

### Schema 檢查
- `get_table_schema` - 獲取表結構
- `list_tables` - 列出所有表
- `explain_query` - 查詢執行計畫

### 監控工具
- `health_check` - 健康檢查
- `get_metrics` - 伺服器指標

## 🔍 使用範例

### 建立連線
```python
await add_connection(
    connection_id="main_db",
    host="localhost",
    port=5432,
    database="myapp",
    user="myuser",
    password="mypassword"
)
```

### 執行查詢
```python
result = await execute_query(
    connection_id="main_db",
    query="SELECT * FROM users WHERE created_at > $1",
    params=["2024-01-01"]
)
```

### 事務操作
```python
await execute_transaction(
    connection_id="main_db",
    queries=[
        {"query": "INSERT INTO orders (user_id, total) VALUES ($1, $2)", "params": [1, 100.50]},
        {"query": "UPDATE inventory SET stock = stock - $1 WHERE id = $2", "params": [1, 123]}
    ]
)
```

## 🧪 測試

```bash
# 運行所有測試
python run_tests.py all

# 運行特定測試
python run_tests.py unit
python run_tests.py integration

# 生成覆蓋率報告
python run_tests.py coverage

# 代碼檢查
python run_tests.py lint

# 修復格式
python run_tests.py fix
```

## 🐳 Docker 部署

### 生產部署
```bash
./scripts/deploy.sh deploy
```

### 開發環境
```bash
./scripts/dev.sh start
```

### 查看日誌
```bash
./scripts/deploy.sh logs mcp-server
./scripts/dev.sh logs postgres-dev
```

## 📊 監控

### 健康檢查
```bash
curl http://localhost:3000/health
```

### 指標查詢
```bash
curl http://localhost:3000/metrics
```

### 服務狀態
```bash
./scripts/deploy.sh status
```

## 🛡️ 安全最佳實務

1. **永遠使用參數化查詢**
2. **定期更新依賴**
3. **啟用查詢日誌記錄**
4. **使用最小權限原則**
5. **定期審核安全配置**

## 🔄 版本歷史

- **v0.1.0** - 初始版本
  - 基本 MCP 工具實現
  - 安全驗證機制
  - Docker 部署支援
  - 完整測試套件

## 📄 授權

此專案使用 MIT 授權 - 詳見 [LICENSE](LICENSE) 檔案

## 🤝 支援

- 📧 Email: a910413frank@gmail.com
- 🐛 Issues: [GitHub Issues](../../issues)
- 💬 Discussions: [GitHub Discussions](../../discussions)


---

**注意**: 這是一個純工具層的 MCP Server，設計用於與 LLM 配合進行智能資料遷移和資料庫操作。請確保在生產環境中正確配置安全設定。