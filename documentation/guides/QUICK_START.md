# 🚀 MCP Registry Java Edition - 快速開始

此指南幫助您在 5 分鐘內啟動完整的企業級 Java MCP Server 環境。

## 📋 前置需求

- **Java 17+** (必需)
- **Maven 3.8+** (必需)
- **Docker & Docker Compose** (部署用)
- **Git** (可選)

## ⚡ 一鍵啟動

### 方法 1: 檢查環境並建置 (推薦)

```bash
# 克隆專案
git clone <repository-url>
cd mcp-registry

# 檢查開發環境
./scripts/start-all.sh env-check

# 建置 Java 專案
./scripts/start-all.sh build

# 建置 Docker 映像並啟動
./scripts/start-all.sh docker-build
./scripts/start-all.sh start
```

### 方法 2: 本地開發模式

```bash
# 克隆專案
git clone <repository-url>
cd mcp-registry

# 建置專案
cd mcp-registry-java
mvn clean install

# 啟動 PostgreSQL MCP Server (終端 1)
cd mcp-postgresql-server
mvn spring-boot:run

# 啟動 MySQL MCP Server (終端 2)
cd mcp-mysql-server
mvn spring-boot:run
```

### 方法 3: 僅 Docker Compose

```bash
# 直接啟動 (如果已有映像)
cd deployment
docker-compose up -d

# 檢查服務
docker-compose ps
```

## 🎯 服務端點

啟動成功後，您可以存取以下服務：

| 服務 | 端點 | 說明 |
|------|------|------|
| PostgreSQL MCP Server | `http://localhost:8080` | Spring Boot + R2DBC PostgreSQL |
| MySQL MCP Server | `http://localhost:8081` | Spring Boot + R2DBC MySQL |
| PostgreSQL 資料庫 | `localhost:5432` | PostgreSQL 12+ |
| MySQL 資料庫 | `localhost:3306` | MySQL 8.0+ |

## 📊 健康檢查

### Spring Boot Actuator 端點

```bash
# PostgreSQL MCP Server 健康檢查
curl http://localhost:8080/actuator/health

# MySQL MCP Server 健康檢查
curl http://localhost:8081/actuator/health

# 查看應用程式資訊
curl http://localhost:8080/actuator/info
curl http://localhost:8081/actuator/info

# 查看系統指標
curl http://localhost:8080/actuator/metrics
curl http://localhost:8081/actuator/metrics
```

### 使用腳本檢查

```bash
# 檢查所有服務健康狀態
./scripts/start-all.sh health

# 檢查服務狀態
./scripts/start-all.sh status

# 查看服務日誌
./scripts/start-all.sh logs
./scripts/start-all.sh logs postgresql-mcp-server
./scripts/start-all.sh logs mysql-mcp-server
```

## 🔧 基本配置

### 應用程式配置檔案

PostgreSQL MCP Server (`mcp-postgresql-server/src/main/resources/application.yml`):

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/mydb
    username: postgres
    password: password
  application:
    name: postgresql-mcp-server

server:
  port: 8080

mcp:
  server:
    security:
      readonly-mode: false
      allowed-operations: SELECT,INSERT,UPDATE,DELETE
      blocked-keywords: DROP,TRUNCATE,ALTER
      max-query-length: 10000

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
```

### 環境變數配置

```bash
# 資料庫連線
export R2DBC_URL=r2dbc:postgresql://localhost:5432/mydb
export R2DBC_USERNAME=postgres
export R2DBC_PASSWORD=password

# 安全配置
export MCP_SECURITY_READONLY_MODE=false
export MCP_SECURITY_ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE
export MCP_SECURITY_BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
export MCP_SECURITY_MAX_QUERY_LENGTH=10000

# 伺服器配置
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=development
```

## 🧪 第一次測試

### 1. 測試連線

```bash
# 測試 PostgreSQL MCP Server
curl -X POST http://localhost:8080/api/connections \
  -H "Content-Type: application/json" \
  -d '{
    "connectionId": "test_db",
    "host": "localhost",
    "port": 5432,
    "database": "postgres",
    "username": "postgres",
    "password": "password"
  }'
```

### 2. 執行查詢

```bash
# 執行簡單查詢
curl -X POST http://localhost:8080/api/queries \
  -H "Content-Type: application/json" \
  -d '{
    "connectionId": "test_db",
    "query": "SELECT version()",
    "params": []
  }'
```

### 3. 檢查表結構

```bash
# 列出所有表
curl -X GET http://localhost:8080/api/schema/tables/test_db

# 獲取特定表結構
curl -X GET http://localhost:8080/api/schema/tables/test_db/users
```

## 🛠️ 開發工具命令

```bash
# 查看可用命令
./scripts/start-all.sh help

# 檢查專案結構
./scripts/start-all.sh structure

# 執行測試
./scripts/start-all.sh test
./scripts/start-all.sh integration-test

# 清理專案
./scripts/start-all.sh clean

# 顯示本地開發指令
./scripts/start-all.sh dev
```

## 🐳 Docker 操作

```bash
# 建置並啟動所有服務
./scripts/start-all.sh docker-build
./scripts/start-all.sh start

# 停止服務
./scripts/start-all.sh stop

# 重新啟動服務
./scripts/start-all.sh restart

# 清理 Docker 資源
./scripts/start-all.sh clean
```

## 🔍 故障排除

### Java 環境問題

```bash
# 檢查 Java 版本
java -version

# 檢查 Maven 版本
mvn --version

# 檢查環境
./scripts/start-all.sh env-check
```

### 建置問題

```bash
# 清理並重新建置
cd mcp-registry-java
mvn clean install -X

# 跳過測試建置
mvn clean install -DskipTests
```

### Docker 問題

```bash
# 檢查 Docker 狀態
docker --version
docker-compose --version

# 查看容器日誌
docker-compose logs postgresql-mcp-server
docker-compose logs mysql-mcp-server

# 重新建置映像
./scripts/start-all.sh docker-build
```

### 連線問題

```bash
# 檢查埠是否被占用
netstat -tulpn | grep :8080
netstat -tulpn | grep :8081

# 檢查資料庫連線
psql -h localhost -p 5432 -U postgres
mysql -h localhost -P 3306 -u root -p
```

## 📚 進階配置

### 安全配置 (生產環境)

```yaml
mcp:
  server:
    security:
      readonly-mode: true
      allowed-operations: SELECT
      blocked-keywords: DROP,TRUNCATE,ALTER,CREATE,GRANT,REVOKE
      max-query-length: 5000
      enable-audit-log: true

spring:
  security:
    user:
      name: admin
      password: ${MCP_ADMIN_PASSWORD}
```

### 效能調優

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 5
      max-size: 20
      max-idle-time: 30m
      validation-query: SELECT 1

management:
  metrics:
    export:
      prometheus:
        enabled: true
```

## 🎯 下一步

1. 閱讀 [完整文件](docs/ARCHITECTURE.md)
2. 查看 [API 參考](docs/API_REFERENCE.md)
3. 探索 [Java 遷移計畫](docs/JAVA_MIGRATION_PLAN.md)
4. 嘗試 [測試工具](mcp-registry-java/testing-tools/)

## 🤝 支援

- 📧 Email: a910413frank@gmail.com
- 🐛 Issues: [GitHub Issues](../../issues)
- 💬 Discussions: [GitHub Discussions](../../discussions)

---

**注意**: 這是 Java 版本的 MCP Server，相比 Python 版本提供更好的效能、企業級特性和強型別安全。