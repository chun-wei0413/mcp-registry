# MCP Registry - Java Edition

企業級 Model Context Protocol (MCP) Server 的 Java 實現，專為現代資料庫操作和智能資料遷移設計。

## 🎯 專案概述

此專案提供基於 Java 17 + Spring Boot 3.x 的企業級 MCP Server，支援：

- **PostgreSQL MCP Server**: 針對現代 PostgreSQL 資料庫的完整操作和管理
- **MySQL MCP Server**: 專為 MySQL 資料庫設計的企業級操作工具
- **智能資料遷移**: LLM 驅動的跨資料庫遷移和同步方案
- **反應式程式設計**: 基於 Project Reactor 的高效能非同步操作

## 🏗️ 專案結構

```
mcp-registry-java/
├── 📁 mcp-common/                    # 共用模組
│   ├── src/main/java/               # 共用程式碼
│   │   ├── models/                  # 資料模型 (ConnectionInfo, QueryResult)
│   │   ├── validators/              # 安全驗證 (SqlValidator)
│   │   └── exceptions/              # 例外處理
│   └── pom.xml                      # Maven 配置
├── 📁 mcp-postgresql-server/         # PostgreSQL MCP Server
│   ├── src/main/java/               # PostgreSQL 服務實現
│   │   ├── controllers/             # MCP 工具控制器
│   │   ├── services/                # 業務服務層
│   │   └── config/                  # 配置管理
│   └── pom.xml                      # Maven 配置
├── 📁 mcp-mysql-server/              # MySQL MCP Server
│   ├── src/main/java/               # MySQL 服務實現
│   │   ├── controllers/             # MCP 工具控制器
│   │   ├── services/                # 業務服務層
│   │   └── config/                  # 配置管理
│   └── pom.xml                      # Maven 配置
├── 📁 testing-tools/                 # 測試工具模組
│   ├── src/main/java/               # 測試工具實現
│   └── pom.xml                      # Maven 配置
├── 📁 deployment/                    # 部署相關檔案
│   ├── docker-compose.yml           # Docker Compose 配置
│   └── 📁 k8s/                      # Kubernetes 部署檔案
├── 📁 docs/                         # 技術文檔目錄
│   ├── ARCHITECTURE.md              # 架構設計文檔
│   ├── JAVA_MIGRATION_PLAN.md       # Java 遷移計畫
│   └── API_REFERENCE.md             # API 參考文檔
├── 📄 pom.xml                       # 主 Maven 配置
├── 📄 QUICK_START.md                # 快速開始指南
└── 📄 README.md                     # 主專案說明 (本檔案)
```

## 🛠️ 技術棧

### 核心技術
- **Java 17**: 現代語言特性和效能優化
- **Spring Boot 3.x**: 企業級應用框架
- **Spring AI MCP**: 原生 MCP 協議支援
- **Project Reactor**: 反應式程式設計

### 資料庫與連接
- **R2DBC**: 非同步資料庫連接
- **Connection Pooling**: R2DBC 連線池管理
- **PostgreSQL**: 支援 PostgreSQL 12+
- **MySQL**: 支援 MySQL 8.0+

### 開發與部署
- **Maven**: 專案管理和建置工具
- **TestContainers**: 整合測試環境
- **Jib Plugin**: 優化的 Docker 映像建置
- **Spring Boot Actuator**: 監控和健康檢查

## 🚀 特性

- **🔒 安全性第一**: 參數化查詢、SQL 注入防護、危險操作阻擋
- **⚡ 高效能**: 反應式程式設計、非同步連線池、批次操作
- **🔍 可觀測性**: Spring Boot Actuator 監控、結構化日誌、健康檢查
- **🛡️ 安全配置**: 只讀模式、操作白名單、查詢長度限制
- **🔧 易於部署**: Docker 支援、Kubernetes 配置、一鍵部署
- **🧪 完整測試**: 單元測試、整合測試、TestContainers 支援

## 📋 系統需求

- Java 17+
- Maven 3.8+
- PostgreSQL 12+ 或 MySQL 8.0+
- Docker & Docker Compose (可選)

## 🔧 快速開始

### 使用 Maven 建置

```bash
# 克隆專案
git clone <repository-url>
cd mcp-registry

# 建置所有模組
mvn clean install

# 執行 PostgreSQL MCP Server
cd mcp-postgresql-server
mvn spring-boot:run

# 執行 MySQL MCP Server (另一個終端)
cd mcp-mysql-server
mvn spring-boot:run
```

### 使用 Docker Compose

```bash
# 部署完整環境
cd deployment/
docker-compose up -d

# 查看服務狀態
docker-compose ps
```

### 使用 Jib 建置 Docker 映像

```bash
# 建置 PostgreSQL MCP Server Docker 映像
cd mcp-postgresql-server
mvn jib:dockerBuild

# 建置 MySQL MCP Server Docker 映像
cd mcp-mysql-server
mvn jib:dockerBuild
```

## ⚙️ 配置

### 應用程式配置 (application.yml)

```yaml
# PostgreSQL MCP Server
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/mydb
    username: user
    password: password
  application:
    name: postgresql-mcp-server

mcp:
  server:
    port: 8080
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

### 環境變數

```bash
# 伺服器配置
MCP_SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production

# 資料庫連線
R2DBC_URL=r2dbc:postgresql://localhost:5432/mydb
R2DBC_USERNAME=user
R2DBC_PASSWORD=password

# 安全配置
MCP_SECURITY_READONLY_MODE=false
MCP_SECURITY_ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE
MCP_SECURITY_BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
MCP_SECURITY_MAX_QUERY_LENGTH=10000
```

## 🛠️ MCP 工具

### 連線管理
- `addConnection` - 建立資料庫連線
- `testConnection` - 測試連線狀態
- `listConnections` - 列出所有連線

### 查詢執行
- `executeQuery` - 執行 SELECT 查詢
- `executeTransaction` - 事務執行
- `batchExecute` - 批次操作

### Schema 檢查
- `getTableSchema` - 獲取表結構
- `listTables` - 列出所有表
- `explainQuery` - 查詢執行計畫

### 監控工具
- `healthCheck` - 健康檢查
- `getMetrics` - 伺服器指標

## 🔍 使用範例

### 建立連線
```java
@Autowired
private ConnectionController connectionController;

ConnectionRequest request = ConnectionRequest.builder()
    .connectionId("main_db")
    .host("localhost")
    .port(5432)
    .database("myapp")
    .username("myuser")
    .password("mypassword")
    .build();

Mono<ConnectionResult> result = connectionController.addConnection(request);
```

### 執行查詢
```java
@Autowired
private QueryController queryController;

QueryRequest request = QueryRequest.builder()
    .connectionId("main_db")
    .query("SELECT * FROM users WHERE created_at > ?")
    .params(List.of("2024-01-01"))
    .build();

Mono<QueryResult> result = queryController.executeQuery(request);
```

### 事務操作
```java
TransactionRequest request = TransactionRequest.builder()
    .connectionId("main_db")
    .queries(List.of(
        QueryRequest.builder()
            .query("INSERT INTO orders (user_id, total) VALUES (?, ?)")
            .params(List.of(1, 100.50))
            .build(),
        QueryRequest.builder()
            .query("UPDATE inventory SET stock = stock - ? WHERE id = ?")
            .params(List.of(1, 123))
            .build()
    ))
    .build();

Mono<TransactionResult> result = queryController.executeTransaction(request);
```

## 🧪 測試

### 單元測試
```bash
# 執行所有單元測試
mvn test

# 執行特定模組測試
cd mcp-postgresql-server
mvn test
```

### 整合測試 (使用 TestContainers)
```bash
# 執行整合測試
mvn integration-test

# 執行特定的整合測試
mvn test -Dtest=PostgreSqlIntegrationTest
```

## 🐳 Docker 部署

### Docker Compose 部署
```bash
cd deployment/
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs postgresql-mcp-server
docker-compose logs mysql-mcp-server
```

### Kubernetes 部署
```bash
cd deployment/k8s/
kubectl apply -f .

# 查看 Pod 狀態
kubectl get pods

# 查看服務
kubectl get services
```

## 📊 監控

### 健康檢查
```bash
curl http://localhost:8080/actuator/health
```

### 指標查詢
```bash
curl http://localhost:8080/actuator/metrics
```

### 應用程式資訊
```bash
curl http://localhost:8080/actuator/info
```

## 🛡️ 安全最佳實務

1. **永遠使用參數化查詢**
2. **啟用 Spring Security (生產環境)**
3. **定期更新依賴**
4. **使用最小權限原則**
5. **啟用審計日誌**

## 🔄 從 Python 版本遷移

此 Java 版本相比於 Python 版本提供：

- **更好的效能**: JVM 效能優化和反應式程式設計
- **企業級特性**: Spring Boot 生態系統支援
- **強型別安全**: 編譯時型別檢查
- **更好的工具**: Maven 生態系統和 IDE 支援
- **更好的可維護性**: 企業級架構和設計模式

詳見 [Java 遷移計畫](documentation/project/JAVA_MIGRATION_PLAN.md)。

## 📚 文檔

完整文檔請參閱 [文檔中心](documentation/README.md)。

### 快速連結
- [快速開始指南](documentation/guides/QUICK_START.md)
- [系統架構](documentation/ARCHITECTURE.md)
- [使用案例](documentation/USE_CASES.md)
- [常見問題](documentation/guides/QA.md)

## 📄 授權

此專案使用 MIT 授權 - 詳見 [LICENSE](LICENSE) 檔案

## 🤝 支援

- 📧 Email: a910413frank@gmail.com
- 🐛 Issues: [GitHub Issues](../../issues)
- 💬 Discussions: [GitHub Discussions](../../discussions)

---

**注意**: 這是一個純工具層的 MCP Server，設計用於與 LLM 配合進行智能資料遷移和資料庫操作。請確保在生產環境中正確配置安全設定。