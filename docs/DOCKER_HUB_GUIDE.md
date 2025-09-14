# PostgreSQL MCP Server - Docker Hub 使用指南 🐳

## 📦 官方映像檔

PostgreSQL MCP Server 已發布到 Docker Hub，提供兩個官方映像檔：

- **主要映像檔**: [`russellli/postgresql-mcp-server`](https://hub.docker.com/r/russellli/postgresql-mcp-server)

### 🏷️ 可用標籤

- `latest` - 最新穩定版本
- `v0.1.0` - 特定版本標籤

## 🚀 快速部署

### 方式 1: 單容器部署 (僅 MCP Server)

適用於已有 PostgreSQL 資料庫的環境。

```bash
# 拉取並運行最新版本
docker pull russellli/postgresql-mcp-server:latest

# 運行容器
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
```

### 方式 2: Docker Compose 部署 (包含 PostgreSQL)

適用於完整的開發或測試環境。

建立 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  mcp-server:
    image: russellli/postgresql-mcp-server:latest
    container_name: postgresql-mcp-server
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
      # 預設連線設定 (可選)
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_DATABASE=postgres
      - DB_USER=postgres
      - DB_PASSWORD=password
    depends_on:
      - postgres
    restart: unless-stopped
    networks:
      - mcp-network

  postgres:
    image: postgres:15
    container_name: postgres-db
    environment:
      - POSTGRES_DB=postgres
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      # 可選：載入初始化 SQL
      # - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    restart: unless-stopped
    networks:
      - mcp-network

volumes:
  postgres_data:

networks:
  mcp-network:
    driver: bridge
```

啟動服務：

```bash
# 啟動所有服務
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f mcp-server
```

## 🔧 環境變數配置

### 基本配置

| 變數名 | 預設值 | 說明 |
|--------|--------|------|
| `MCP_SERVER_PORT` | `3000` | MCP Server 監聽埠 |
| `MCP_LOG_LEVEL` | `INFO` | 日誌等級 (DEBUG/INFO/WARNING/ERROR) |
| `DEFAULT_POOL_SIZE` | `10` | 預設連線池大小 |
| `QUERY_TIMEOUT` | `30` | 查詢超時時間 (秒) |

### 安全配置

| 變數名 | 預設值 | 說明 |
|--------|--------|------|
| `READONLY_MODE` | `false` | 是否啟用只讀模式 |
| `ALLOWED_OPERATIONS` | `SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN` | 允許的 SQL 操作 |
| `BLOCKED_KEYWORDS` | `DROP,TRUNCATE,ALTER` | 阻擋的 SQL 關鍵字 |
| `MAX_QUERY_LENGTH` | `10000` | 最大查詢長度 |

### 預設連線配置 (可選)

| 變數名 | 說明 |
|--------|------|
| `DB_HOST` | PostgreSQL 主機地址 |
| `DB_PORT` | PostgreSQL 埠號 |
| `DB_DATABASE` | 資料庫名稱 |
| `DB_USER` | 使用者名稱 |
| `DB_PASSWORD` | 密碼 |

## 🛡️ 安全最佳實務

### 1. 生產環境配置

```yaml
# 推薦的生產環境設定
environment:
  - READONLY_MODE=true  # 生產環境建議只讀
  - ALLOWED_OPERATIONS=SELECT,WITH,EXPLAIN
  - BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER,CREATE,GRANT,REVOKE,DELETE,INSERT,UPDATE
  - MAX_QUERY_LENGTH=5000
  - MCP_LOG_LEVEL=WARNING
```

### 2. 開發環境配置

```yaml
# 開發環境設定
environment:
  - READONLY_MODE=false
  - ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN
  - BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
  - MAX_QUERY_LENGTH=10000
  - MCP_LOG_LEVEL=DEBUG
```

### 3. 網路安全

```yaml
# 限制網路存取
services:
  mcp-server:
    ports:
      - "127.0.0.1:3000:3000"  # 只允許本地存取
    networks:
      - mcp-internal  # 使用內部網路

networks:
  mcp-internal:
    driver: bridge
    internal: true  # 內部網路，無外網存取
```

## 📊 監控與維運

### 健康檢查

```bash
# 檢查服務健康狀態
curl http://localhost:3000/health

# 預期回應
{
  "status": "healthy",
  "timestamp": "2024-01-01T12:00:00Z",
  "uptime_seconds": 3600,
  "connections": {
    "active": 2,
    "total": 5
  }
}
```

### 效能監控

```bash
# 查看效能指標
curl http://localhost:3000/metrics

# 查看容器資源使用
docker stats postgresql-mcp-server
```

### 日誌管理

```bash
# 查看即時日誌
docker logs -f postgresql-mcp-server

# 查看最近 100 行日誌
docker logs --tail 100 postgresql-mcp-server

# 使用 Docker Compose
docker-compose logs -f mcp-server
```

## 🔄 更新與維護

### 更新到最新版本

```bash
# 拉取最新映像檔
docker pull russellli/postgresql-mcp-server:latest

# 停止現有容器
docker stop postgresql-mcp-server
docker rm postgresql-mcp-server

# 重新啟動新版本
docker run -d [相同的運行參數] russellli/postgresql-mcp-server:latest
```

### 使用 Docker Compose 更新

```bash
# 拉取最新映像檔
docker-compose pull

# 重建並啟動服務
docker-compose up -d --force-recreate
```

### 資料備份

```bash
# 備份 PostgreSQL 資料
docker exec postgres-db pg_dump -U postgres postgres > backup.sql

# 使用 volume 備份
docker run --rm -v postgres_data:/source -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz -C /source .
```

## 🚨 故障排除

### 常見問題

1. **容器無法啟動**
   ```bash
   # 檢查日誌
   docker logs postgresql-mcp-server

   # 檢查埠號衝突
   netstat -an | grep 3000
   ```

2. **無法連線到 PostgreSQL**
   ```bash
   # 檢查 PostgreSQL 服務
   docker exec -it postgres-db psql -U postgres -d postgres -c "SELECT version();"

   # 檢查網路連通性
   docker exec -it postgresql-mcp-server ping postgres
   ```

3. **效能問題**
   ```bash
   # 增加連線池大小
   docker run -e DEFAULT_POOL_SIZE=20 [其他參數]

   # 調整查詢超時
   docker run -e QUERY_TIMEOUT=60 [其他參數]
   ```

### 除錯模式

```bash
# 以除錯模式運行
docker run -d \
  -e MCP_LOG_LEVEL=DEBUG \
  -e QUERY_LOGGING=true \
  [其他參數] \
  russellli/postgresql-mcp-server:latest
```

## 📞 取得協助

- 📧 **Email**: a910413frank@gmail.com

## 🔗 相關連結

- [Docker Hub - russellli/postgresql-mcp-server](https://hub.docker.com/r/russellli/postgresql-mcp-server)

---

> **提示**: 首次使用建議先在開發環境測試，確認配置正確後再部署到生產環境。