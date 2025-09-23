# MCP Registry Java

🇯‍💻 Java 版本的 PostgreSQL 和 MySQL MCP Server 完整實現

## 🎯 專案概述

這是原 Python 版本 MCP Registry 的 Java 實現，使用現代化的 Java 技術棧，提供更強大的企業級特性和更好的效能。

### 🔧 技術棧

- **Java 17** - 現代化的 Java 版本
- **Spring Boot 3.x** - 企業級 Spring 框架
- **Spring AI MCP** - 官方 MCP Java SDK 整合
- **Project Reactor** - 反應式程式設計
- **R2DBC** - 反應式資料庫連線
- **Maven** - 依賴管理和建置工具
- **TestContainers** - 整合測試
- **Docker** - 容器化部署

## 🏗️ 專案結構

```
mcp-registry-java/
├── 📁 mcp-common/                    # 共用模組
│   ├── model/                        # 共用資料模型
│   ├── exception/                    # 例外類別
│   ├── util/                         # 工具類
│   └── config/                       # 共用配置
├── 📁 mcp-postgresql-server/         # PostgreSQL MCP Server
│   ├── controller/                   # MCP 工具控制器
│   ├── service/                      # 業務邏輯服務
│   ├── config/                       # PostgreSQL 配置
│   └── PostgreSqlMcpServerApplication.java
├── 📁 mcp-mysql-server/              # MySQL MCP Server
│   ├── controller/                   # MCP 工具控制器
│   ├── service/                      # 業務邏輯服務
│   ├── config/                       # MySQL 配置
│   └── MySqlMcpServerApplication.java
├── 📁 testing-tools/                 # 測試工具模組
│   └── 互動式測試工具和自動化測試
├── 📁 deployment/                    # 部署配置
├── 📁 documentation/                 # 文檔中心
└── pom.xml                           # 根 Maven 配置
```

## 🚀 快速開始

### 環境需求

- Java 17+
- Maven 3.6+
- Docker (可選)
- PostgreSQL 12+ (用於 PostgreSQL MCP Server)
- MySQL 8.0+ (用於 MySQL MCP Server)

### 建置專案

```bash
# 克隆專案
git clone <repository-url>
cd mcp-registry-java

# 建置所有模組
mvn clean install

# 建置特定模組
mvn clean install -pl mcp-postgresql-server
mvn clean install -pl mcp-mysql-server
```

### 執行 MCP Servers

#### PostgreSQL MCP Server

```bash
# 使用 Maven
cd mcp-postgresql-server
mvn spring-boot:run

# 或使用 JAR 檔案
java -jar mcp-postgresql-server/target/mcp-postgresql-server-1.0.0-SNAPSHOT.jar
```

#### MySQL MCP Server

```bash
# 使用 Maven
cd mcp-mysql-server
mvn spring-boot:run

# 或使用 JAR 檔案
java -jar mcp-mysql-server/target/mcp-mysql-server-1.0.0-SNAPSHOT.jar
```

### Docker 部署

```bash
# 建置 Docker 映像檔
mvn clean package jib:build

# 執行 PostgreSQL MCP Server
docker run -p 8080:8080 russellli/postgresql-mcp-server-java:1.0.0-SNAPSHOT

# 執行 MySQL MCP Server
docker run -p 8081:8081 russellli/mysql-mcp-server-java:1.0.0-SNAPSHOT
```

## 🔧 配置

### PostgreSQL MCP Server 配置

```yaml
# application.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/postgres
    username: postgres
    password: your_password

mcp:
  postgresql:
    default-pool-size: 10
    max-query-length: 50000
    query-timeout: 30s
    readonly-mode: false
    allowed-operations:
      - SELECT
      - INSERT
      - UPDATE
      - DELETE
    blocked-keywords:
      - DROP
      - TRUNCATE
```

### MySQL MCP Server 配置

```yaml
# application.yml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/mysql
    username: root
    password: your_password

mcp:
  mysql:
    default-pool-size: 10
    max-query-length: 50000
    query-timeout: 30s
```

## 🛠️ MCP 工具

### 連線管理

- `add_connection` - 建立資料庫連線
- `test_connection` - 測試連線狀態
- `list_connections` - 列出所有連線
- `remove_connection` - 移除連線
- `health_check` - 健康檢查

### 查詢執行

- `execute_query` - 執行 SQL 查詢
- `execute_transaction` - 事務執行
- `execute_batch` - 批次操作
- `explain_query` - 查詢執行計畫分析

### Schema 管理

- `get_table_schema` - 取得表結構
- `list_tables` - 列出所有表
- `list_schemas` - 列出所有 Schema
- `get_database_stats` - 取得資料庫統計

## 📊 監控

每個 MCP Server 都提供完整的監控端點：

```bash
# 健康檢查
curl http://localhost:8080/actuator/health

# 應用資訊
curl http://localhost:8080/actuator/info

# Prometheus 指標
curl http://localhost:8080/actuator/prometheus
```

## 🧪 測試

### 執行測試

```bash
# 執行所有測試
mvn test

# 執行特定模組測試
mvn test -pl mcp-postgresql-server
mvn test -pl mcp-common

# 執行整合測試（需要 Docker）
mvn verify -Pintegration-tests
```

### 測試工具

```bash
# 啟動互動式測試工具
cd testing-tools
mvn spring-boot:run
```

## 🎯 核心優勢

### 相比 Python 版本的優勢

1. **效能提升**
   - JVM 優化的查詢執行
   - R2DBC 反應式資料庫存取
   - HikariCP 高效能連線池

2. **企業級特性**
   - Spring Security 整合支援
   - Spring Boot Actuator 完整監控
   - Spring Cloud 微服務生態系統

3. **開發體驗**
   - 強型別系統
   - 完整的 IDE 支援
   - 豐富的 Spring 生態系統

4. **部署優勢**
   - Jib 零 Dockerfile 容器化
   - Kubernetes 原生支援
   - 更小的記憶體佔用

## 🔄 與 Python 版本的功能對照

| 功能 | Python 版本 | Java 版本 | 狀態 |
|------|-------------|-----------|------|
| PostgreSQL 連線管理 | ✅ | ✅ | 完成 |
| MySQL 連線管理 | ✅ | ✅ | 完成 |
| SQL 查詢執行 | ✅ | ✅ | 完成 |
| 事務管理 | ✅ | ✅ | 完成 |
| 批次操作 | ✅ | ✅ | 完成 |
| Schema 檢查 | ✅ | ✅ | 完成 |
| 安全驗證 | ✅ | ✅ | 完成 |
| 健康檢查 | ✅ | ✅ | 完成 |
| 測試工具 | ✅ | ✅ | 完成 |
| Docker 部署 | ✅ | ✅ | 完成 |

## 📚 文檔

- [Java 遷移計畫](../JAVA_MIGRATION_PLAN.md) - 詳細的遷移策略和技術決策
- [API 文檔](../documentation/api-documentation.md) - MCP 工具 API 參考
- [Spring Boot 整合指南](../documentation/spring-boot-integration.md) - Spring Boot 特性使用指南
- [部署指南](../documentation/deployment-guide.md) - 生產環境部署說明

## 🤝 貢獻

1. Fork 專案
2. 建立功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交變更 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 開啟 Pull Request

## 📄 授權

此專案使用 MIT 授權 - 詳見 [LICENSE](LICENSE) 檔案

## 🆚 Python vs Java 版本選擇指南

### 選擇 Python 版本當：
- 快速原型開發
- 簡單的資料遷移任務
- 團隊熟悉 Python 生態系統

### 選擇 Java 版本當：
- 企業級生產環境
- 需要高效能和可擴展性
- 要整合 Spring 生態系統
- 需要強型別和 IDE 支援

---

🚀 **Java 版本提供了更強大的企業級特性和更好的效能，是生產環境的理想選擇！**