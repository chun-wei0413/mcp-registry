# 🚀 PostgreSQL & MySQL MCP Servers - 快速開始

此指南幫助您在 5 分鐘內啟動完整的雙 MCP Server 環境（使用統一架構）。

## 📋 前置需求

- Docker & Docker Compose
- Python 3.11+ (用於本地開發)
- Git (可選)

## ⚡ 一鍵啟動

### 方法 1: 使用統一腳本 (推薦)

```bash
# 克隆專案
git clone <repository-url>
cd pg-mcp

# 一鍵啟動所有服務
./scripts/start-all.sh start

# 檢查服務狀態
./scripts/start-all.sh status

# 查看專案結構
./scripts/start-all.sh structure
```

### 方法 2: 使用 Docker Compose

```bash
# 直接啟動
docker-compose up -d

# 檢查服務
docker-compose ps
```

## 🎯 服務端點

啟動成功後，您可以存取以下服務：

| 服務 | 端點 | 說明 |
|------|------|------|
| PostgreSQL MCP Server | `http://localhost:3000` | 目標資料庫操作 |
| MySQL MCP Server | `http://localhost:3001` | 舊資料庫存取 |
| PostgreSQL 資料庫 | `localhost:5432` | 目標資料庫 |
| MySQL 資料庫 | `localhost:3306` | 舊資料庫 |

## 🏗️ 統一架構說明

新的專案架構更清晰、更有組織：

```
pg-mcp/
├── src/                           # 統一源碼目錄
│   ├── postgresql_mcp/            # PostgreSQL MCP Server
│   └── mysql_mcp/                # MySQL MCP Server
├── deployment/docker/             # Docker 配置
│   ├── postgres/                  # PostgreSQL 配置
│   └── mysql/                     # MySQL 配置
├── tests/                         # 測試目錄
│   ├── postgresql_mcp/
│   └── mysql_mcp/
└── pyproject.toml                 # 統一專案配置
```

## ✅ 驗證安裝

### 檢查服務健康狀態

```bash
# 使用腳本檢查
./scripts/start-all.sh health

# 或手動檢查
curl http://localhost:3000/health  # PostgreSQL MCP
curl http://localhost:3001/health  # MySQL MCP
```

### 檢查容器狀態

```bash
docker-compose ps
```

預期輸出：
```
NAME                   COMMAND                  STATUS          PORTS
postgresql-mcp-server  "python -m src.post…"   Up             0.0.0.0:3000->3000/tcp
mysql-mcp-server       "python -m src.mysq…"   Up             0.0.0.0:3001->3001/tcp
postgres-target-db     "docker-entrypoint.s…"   Up (healthy)   0.0.0.0:5432->5432/tcp
mysql-source-db        "docker-entrypoint.s…"   Up (healthy)   0.0.0.0:3306->3306/tcp
```

## 🎪 開始 Kanban 資料遷移

### 1. 準備舊資料

```bash
# 將您的 MySQL 備份檔案放到正確位置
cp your_old_kanban_backup.sql deployment/docker/mysql/backup_data/

# 載入到 MySQL 容器
docker exec -i mysql-source-db mysql -u migration_user -pmigration_pass old_kanban_data < deployment/docker/mysql/backup_data/your_old_kanban_backup.sql
```

### 2. 驗證資料載入

```bash
# 檢查資料是否載入成功
docker exec mysql-source-db mysql -u migration_user -pmigration_pass -e "USE old_kanban_data; SHOW TABLES;"
```

### 3. 執行智能遷移

使用 LLM (如 Claude) 連接兩個 MCP Server 並執行遷移：

```python
# 連接 PostgreSQL MCP Server (目標)
await pg_mcp.add_connection(
    connection_id="target_db",
    host="localhost",
    port=5432,
    database="target_database",
    user="postgres",
    password="postgres_pass"
)

# 連接 MySQL MCP Server (來源)
await mysql_mcp.add_connection(
    connection_id="source_db",
    host="localhost",
    port=3306,
    database="old_kanban_data",
    user="migration_user",
    password="migration_pass"
)

# 執行遷移 (參考 docs/data_migration/migration_instructions.md)
```

## 🔧 常用管理指令

### 服務管理

```bash
# 啟動服務
./scripts/start-all.sh start

# 停止服務
./scripts/start-all.sh stop

# 重新啟動
./scripts/start-all.sh restart

# 查看日誌
./scripts/start-all.sh logs

# 查看特定服務日誌
./scripts/start-all.sh logs postgresql-mcp-server
./scripts/start-all.sh logs mysql-mcp-server
```

### 清理和重建

```bash
# 重新建置服務
./scripts/start-all.sh build

# 完全清理 (注意：會刪除所有資料)
./scripts/start-all.sh clean
```

## 🛠️ 本地開發模式

### 統一架構開發

```bash
# 安裝依賴 (統一配置)
pip install -e .

# PostgreSQL MCP Server
python -m src.postgresql_mcp.server

# MySQL MCP Server
python -m src.mysql_mcp.mysql_server
```

### 開發指令

```bash
# 查看開發指令
./scripts/start-all.sh dev

# 查看專案結構
./scripts/start-all.sh structure
```

## 📊 監控和除錯

### 查看即時日誌

```bash
# 所有服務
docker-compose logs -f

# 特定服務
docker-compose logs -f postgresql-mcp-server
docker-compose logs -f mysql-mcp-server
```

### 進入容器除錯

```bash
# PostgreSQL 容器
docker exec -it postgres-target-db psql -U postgres -d target_database

# MySQL 容器
docker exec -it mysql-source-db mysql -u migration_user -pmigration_pass old_kanban_data
```

## ❗ 常見問題

### 服務啟動失敗

1. **檢查端口是否被佔用**：
   ```bash
   netstat -tlnp | grep -E ':(3000|3001|5432|3306)'
   ```

2. **檢查 Docker 資源**：
   ```bash
   docker system df
   docker system prune  # 清理未使用資源
   ```

### 連線問題

1. **等待資料庫完全啟動**：
   ```bash
   # 資料庫服務需要一些時間初始化
   docker-compose logs postgres-target-db
   docker-compose logs mysql-source-db
   ```

2. **檢查防火牆設定**：
   確保防火牆允許相關端口

### 資料遷移問題

1. **檢查資料是否正確載入**：
   ```bash
   # 檢查 MySQL 資料
   docker exec mysql-source-db mysql -u migration_user -pmigration_pass -e "USE old_kanban_data; SELECT COUNT(*) FROM users;"
   ```

2. **檢查 MCP Server 健康狀態**：
   ```bash
   ./scripts/start-all.sh health
   ```

## 🚀 統一架構優勢

- ✅ **單一專案配置**：統一的 `pyproject.toml`
- ✅ **清晰的目錄結構**：`src/postgresql_mcp/` 和 `src/mysql_mcp/`
- ✅ **統一部署**：單一 `docker-compose.yml`
- ✅ **一致的開發體驗**：相同的工具和流程
- ✅ **簡化的管理**：統一的啟動腳本

## 📖 下一步

- 參考 [Kanban 遷移指南](docs/data_migration/migration_instructions.md)
- 查看 [常見問題解答](QA.md)
- 閱讀完整的 [README](README.md)

## 🆘 需要幫助？

如果遇到問題：
1. 使用 `./scripts/start-all.sh help` 查看所有可用指令
2. 查看 [GitHub Issues](../../issues)
3. 聯繫支援團隊

---

**祝您使用愉快！** 🎉