# MCP Server Docker Hub 使用指南

## 概述

本專案已成功將 PostgreSQL 和 MySQL MCP Server 發布到 Docker Hub，讓用戶可以輕鬆使用預建的 Docker 映像。

## 可用映像

### 1. PostgreSQL MCP Server
```bash
docker pull russellli/postgresql-mcp-server:latest
```
- **映像大小**: ~403MB
- **端口**: 3001 (預設)
- **功能**: 提供完整的 PostgreSQL 資料庫操作 MCP 工具

### 2. MySQL MCP Server
```bash
docker pull russellli/mysql-mcp-server:latest
```
- **映像大小**: ~401MB
- **端口**: 3002 (預設)
- **功能**: 提供完整的 MySQL 資料庫操作 MCP 工具

## 快速開始

### 單獨運行 PostgreSQL MCP Server

```bash
# 拉取映像
docker pull russellli/postgresql-mcp-server:latest

# 運行容器
docker run -d \
  --name postgres-mcp \
  -p 3001:3001 \
  -e MCP_SERVER_PORT=3001 \
  -e MCP_LOG_LEVEL=INFO \
  russellli/postgresql-mcp-server:latest
```

### 單獨運行 MySQL MCP Server

```bash
# 拉取映像
docker pull russellli/mysql-mcp-server:latest

# 運行容器
docker run -d \
  --name mysql-mcp \
  -p 3002:3002 \
  -e MCP_SERVER_PORT=3002 \
  -e MCP_LOG_LEVEL=INFO \
  russellli/mysql-mcp-server:latest
```

## 使用測試環境

專案提供了完整的測試環境，包含資料庫和管理工具：

### PostgreSQL 測試環境

```bash
cd test-postgres-mcp
docker-compose -f docker-compose.test.yml up -d
```

**服務包含**：
- PostgreSQL MCP Server (端口: 3001)
- PostgreSQL 資料庫 (端口: 5432)
- pgAdmin 管理界面 (端口: 8080)

**預設連線資訊**：
- 資料庫: `testdb`
- 用戶: `testuser`
- 密碼: `testpass`
- pgAdmin: admin@test.com / admin

### MySQL 測試環境

```bash
cd test-mysql-mcp
docker-compose -f docker-compose.test.yml up -d
```

**服務包含**：
- MySQL MCP Server (端口: 3002)
- MySQL 資料庫 (端口: 3306)
- phpMyAdmin 管理界面 (端口: 8081)

**預設連線資訊**：
- 資料庫: `testdb`
- 用戶: `testuser`
- 密碼: `testpass`
- Root 密碼: `rootpass`

## 使用情境

### 情境 1: 資料遷移系統

**適用場景**: 企業需要將舊系統資料遷移到新系統

```yaml
version: '3.8'
services:
  # 源資料庫 (MySQL)
  source-mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: old_system_pass
      MYSQL_DATABASE: legacy_data
    ports:
      - "3306:3306"

  # 目標資料庫 (PostgreSQL)
  target-postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: new_system
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: new_system_pass
    ports:
      - "5432:5432"

  # MySQL MCP Server (讀取源資料)
  mysql-mcp:
    image: russellli/mysql-mcp-server:latest
    ports:
      - "3001:3001"
    depends_on:
      - source-mysql

  # PostgreSQL MCP Server (寫入目標資料)
  postgres-mcp:
    image: russellli/postgresql-mcp-server:latest
    ports:
      - "3002:3002"
    depends_on:
      - target-postgres
```

**遷移流程**：
1. LLM 透過 MySQL MCP Server 分析源資料結構
2. 生成資料轉換規則
3. 透過 PostgreSQL MCP Server 建立目標表結構
4. 執行批次資料遷移
5. 驗證資料完整性

### 情境 2: 多資料庫報表系統

**適用場景**: 需要從多個不同類型資料庫生成統一報表

```yaml
version: '3.8'
services:
  # 業務資料庫 (PostgreSQL)
  business-db:
    image: postgres:15
    environment:
      POSTGRES_DB: business
      POSTGRES_USER: business_user
    ports:
      - "5432:5432"

  # 日誌資料庫 (MySQL)
  log-db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: logs
      MYSQL_USER: log_user
    ports:
      - "3306:3306"

  # PostgreSQL MCP (業務資料)
  postgres-mcp:
    image: russellli/postgresql-mcp-server:latest
    ports:
      - "3001:3001"

  # MySQL MCP (日誌資料)
  mysql-mcp:
    image: russellli/mysql-mcp-server:latest
    ports:
      - "3002:3002"
```

**報表生成流程**：
1. LLM 同時連接兩個 MCP Server
2. 從業務資料庫獲取交易數據
3. 從日誌資料庫獲取操作記錄
4. 智能關聯分析，生成綜合報表
5. 自動化排程執行

### 情境 3: 開發環境資料同步

**適用場景**: 開發團隊需要保持測試資料一致性

```bash
# 開發者 A 的環境
docker run -d --name dev-postgres-mcp \
  -p 3001:3001 \
  -e DATABASE_URL=postgresql://dev-db:5432/dev_db_a \
  russellli/postgresql-mcp-server:latest

# 開發者 B 的環境
docker run -d --name dev-postgres-mcp \
  -p 3001:3001 \
  -e DATABASE_URL=postgresql://dev-db:5432/dev_db_b \
  russellli/postgresql-mcp-server:latest

# 資料同步腳本
# LLM 透過 MCP 自動同步測試資料結構和內容
```

### 情境 4: 資料庫健康監控

**適用場景**: 生產環境資料庫監控和自動優化

```yaml
version: '3.8'
services:
  production-db:
    image: postgres:15
    # 生產資料庫配置

  monitoring-mcp:
    image: russellli/postgresql-mcp-server:latest
    ports:
      - "3001:3001"
    environment:
      - READONLY_MODE=true  # 只讀模式
      - ENABLE_MONITORING=true
    volumes:
      - ./monitoring-logs:/app/logs
```

**監控功能**：
- 自動分析慢查詢
- 索引使用率檢查
- 資料庫容量趨勢分析
- 異常查詢模式偵測
- 自動化效能建議

## 環境變數配置

### 通用配置

| 變數名 | 預設值 | 描述 |
|--------|--------|------|
| `MCP_SERVER_PORT` | 3001/3002 | MCP Server 監聽端口 |
| `MCP_LOG_LEVEL` | INFO | 日誌級別 (DEBUG, INFO, WARNING, ERROR) |
| `DEFAULT_POOL_SIZE` | 10 | 資料庫連線池大小 |
| `QUERY_TIMEOUT` | 30 | 查詢超時時間(秒) |

### PostgreSQL 專用

| 變數名 | 描述 |
|--------|------|
| `POSTGRES_HOST` | PostgreSQL 主機 |
| `POSTGRES_PORT` | PostgreSQL 端口 |
| `POSTGRES_DB` | 資料庫名稱 |
| `POSTGRES_USER` | 用戶名 |
| `POSTGRES_PASSWORD` | 密碼 |

### MySQL 專用

| 變數名 | 描述 |
|--------|------|
| `MYSQL_HOST` | MySQL 主機 |
| `MYSQL_PORT` | MySQL 端口 |
| `MYSQL_DATABASE` | 資料庫名稱 |
| `MYSQL_USER` | 用戶名 |
| `MYSQL_PASSWORD` | 密碼 |

## 安全性建議

### 1. 網路安全
```yaml
networks:
  secure-network:
    driver: bridge
    internal: true  # 內部網路，不暴露外部
```

### 2. 密碼管理
```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt

services:
  postgres-mcp:
    secrets:
      - db_password
    environment:
      - POSTGRES_PASSWORD_FILE=/run/secrets/db_password
```

### 3. 只讀模式
```yaml
environment:
  - READONLY_MODE=true
  - ALLOWED_OPERATIONS=SELECT,EXPLAIN
```

## 故障排除

### 常見問題

**1. 容器啟動失敗**
```bash
# 檢查日誌
docker logs <container_name>

# 檢查端口衝突
netstat -tulpn | grep <port>
```

**2. 資料庫連線失敗**
```bash
# 測試網路連通性
docker exec <mcp_container> ping <db_host>

# 檢查資料庫是否就緒
docker exec <db_container> pg_isready  # PostgreSQL
docker exec <db_container> mysqladmin ping  # MySQL
```

**3. MCP Server 無回應**
```bash
# 檢查健康狀態
curl http://localhost:3001/health

# 重啟容器
docker restart <container_name>
```

### 偵錯模式

```bash
docker run -it --rm \
  -e MCP_LOG_LEVEL=DEBUG \
  -p 3001:3001 \
  russellli/postgresql-mcp-server:latest
```

## 更新和維護

### 更新映像
```bash
# 拉取最新版本
docker pull russellli/postgresql-mcp-server:latest
docker pull russellli/mysql-mcp-server:latest

# 重新建立容器
docker-compose down
docker-compose up -d
```

### 資料備份
```bash
# PostgreSQL 備份
docker exec <postgres_container> pg_dump -U <user> <db> > backup.sql

# MySQL 備份
docker exec <mysql_container> mysqldump -u <user> -p <db> > backup.sql
```

## 技術支援

- **GitHub Issues**: [mcp-registry/issues](https://github.com/your-repo/mcp-registry/issues)
- **Docker Hub**: [russellli/postgresql-mcp-server](https://hub.docker.com/r/russellli/postgresql-mcp-server)
- **Documentation**: 詳見專案 README.md

## 更新日誌

### v0.3.0 (2025-09-22)
- ✅ 初始 Docker Hub 發布
- ✅ PostgreSQL MCP Server 支援
- ✅ MySQL MCP Server 支援
- ✅ 完整測試環境
- ✅ 詳細使用文檔