# MCP Registry Java Edition - 專案結構

本文檔說明 MCP Registry Java Edition 專案的目錄結構和檔案組織方式，基於企業級 Clean Architecture + DDD 設計模式。

## 📁 專案目錄結構

```
mcp-registry/
├── 📁 mcp-registry-java/                    # Java 主專案目錄
│   ├── 📄 pom.xml                           # Maven 主配置文件
│   │
│   ├── 📁 mcp-common/                       # 共用模組
│   │   ├── 📄 pom.xml                       # Maven 子模組配置
│   │   └── src/main/java/com/mcp/common/
│   │       ├── 📁 config/                   # 共用配置
│   │       │   └── McpCommonConfig.java
│   │       ├── 📁 exception/                # 例外處理
│   │       │   ├── ConnectionException.java
│   │       │   ├── McpException.java
│   │       │   └── QueryException.java
│   │       ├── 📁 mcp/                      # MCP 協議相關
│   │       │   ├── McpResource.java
│   │       │   ├── McpResourceResult.java
│   │       │   ├── McpTool.java
│   │       │   └── McpToolResult.java
│   │       ├── 📁 model/                    # 資料模型
│   │       │   ├── ConnectionInfo.java
│   │       │   ├── ConnectionStatus.java
│   │       │   ├── DatabaseConnection.java
│   │       │   ├── DatabaseType.java
│   │       │   ├── QueryRequest.java
│   │       │   └── QueryResult.java
│   │       └── 📁 util/                     # 工具類
│   │           └── SqlValidator.java        # SQL 安全驗證
│   │
│   ├── 📁 mcp-core/                         # Clean Architecture 核心模組
│   │   ├── 📄 pom.xml                       # Maven 子模組配置
│   │   └── src/main/java/com/mcpregistry/core/
│   │       ├── 📁 entity/                   # 領域實體 (Domain Layer)
│   │       │   ├── ConnectionId.java        # 值對象
│   │       │   ├── ConnectionInfo.java      # 連線資訊實體
│   │       │   ├── ConnectionStatus.java    # 連線狀態
│   │       │   ├── DatabaseConnection.java  # 資料庫連線聚合根
│   │       │   ├── QueryExecution.java      # 查詢執行聚合根
│   │       │   ├── QueryId.java            # 查詢 ID 值對象
│   │       │   ├── QueryStatus.java        # 查詢狀態
│   │       │   ├── QueryType.java          # 查詢類型
│   │       │   ├── ServerId.java           # 伺服器 ID
│   │       │   └── ServerType.java         # 伺服器類型
│   │       ├── 📁 usecase/                  # 應用層 (Use Case Layer)
│   │       │   ├── 📁 port/                # 端口定義
│   │       │   │   ├── 📁 common/          # 共用端口
│   │       │   │   │   └── UseCaseOutput.java
│   │       │   │   ├── 📁 in/              # 輸入端口
│   │       │   │   │   ├── 📁 connection/
│   │       │   │   │   │   ├── AddConnectionInput.java
│   │       │   │   │   │   ├── AddConnectionUseCase.java
│   │       │   │   │   │   └── TestConnectionUseCase.java
│   │       │   │   │   └── 📁 query/
│   │       │   │   │       ├── ExecuteQueryInput.java
│   │       │   │   │       └── ExecuteQueryUseCase.java
│   │       │   │   └── 📁 out/             # 輸出端口
│   │       │   │       ├── DatabaseConnectionRepository.java
│   │       │   │       ├── DatabaseQueryExecutor.java
│   │       │   │       └── QueryExecutionRepository.java
│   │       │   └── 📁 service/             # 應用服務實現
│   │       │       ├── AddConnectionService.java
│   │       │       └── ExecuteQueryService.java
│   │       └── 📁 adapter/                 # 適配器層 (Interface Adapter)
│   │           ├── 📁 in/                  # 輸入適配器
│   │           │   └── 📁 mcp/
│   │           │       ├── McpServerController.java
│   │           │       ├── 📁 resource/
│   │           │       │   └── ConnectionResource.java
│   │           │       └── 📁 tool/
│   │           │           ├── ConnectionManagementTool.java
│   │           │           └── QueryExecutionTool.java
│   │           └── 📁 out/                 # 輸出適配器
│   │               ├── 📁 query/
│   │               │   └── MockDatabaseQueryExecutor.java
│   │               └── 📁 repository/
│   │                   ├── InMemoryDatabaseConnectionRepository.java
│   │                   └── InMemoryQueryExecutionRepository.java
│   │
│   ├── 📁 mcp-postgresql-server/            # PostgreSQL MCP Server
│   │   ├── 📄 pom.xml                       # Maven 子模組配置
│   │   └── src/main/java/com/mcp/postgresql/
│   │       ├── PostgreSQLMcpServerApplication.java  # Spring Boot 應用入口
│   │       ├── 📁 config/                   # 配置
│   │       │   └── CleanArchitectureConfig.java
│   │       ├── 📁 controller/               # MCP 控制器
│   │       │   └── PostgreSqlMcpController.java
│   │       ├── 📁 resource/                 # MCP 資源
│   │       │   └── ConnectionResource.java
│   │       ├── 📁 service/                  # 業務服務
│   │       │   ├── DatabaseConnectionService.java
│   │       │   ├── DatabaseQueryService.java
│   │       │   └── DatabaseSchemaService.java
│   │       └── 📁 tool/                     # MCP 工具
│   │           ├── ConnectionManagementTool.java
│   │           ├── QueryExecutionTool.java
│   │           └── SchemaManagementTool.java
│   │
│   ├── 📁 mcp-mysql-server/                 # MySQL MCP Server
│   │   ├── 📄 pom.xml                       # Maven 子模組配置
│   │   └── src/main/java/com/mcp/mysql/
│   │       ├── MySQLMcpServerApplication.java      # Spring Boot 應用入口
│   │       ├── 📁 controller/               # MCP 控制器
│   │       │   └── MySqlMcpController.java
│   │       ├── 📁 service/                  # 業務服務
│   │       │   └── DatabaseConnectionService.java
│   │       └── 📁 tool/                     # MCP 工具
│   │           └── ConnectionManagementTool.java
│   │
│   └── 📁 testing-tools/                    # 測試工具模組
│       ├── 📄 pom.xml                       # Maven 子模組配置
│       └── src/main/java/                   # 測試工具實現
│
├── 📁 deployment/                           # 部署配置
│   ├── 📄 docker-compose.yml               # Docker Compose 配置
│   └── 📁 docker/                           # Docker 相關檔案
│       ├── 📁 postgres/                     # PostgreSQL 容器配置
│       └── 📁 mysql/                        # MySQL 容器配置
│
│
├── 📁 scripts/                              # 管理腳本
│   └── start-all.sh                         # 統一管理腳本
│
├── 📁 documentation/                        # 文檔中心
│   ├── 📄 README.md                        # 文檔導覽中心
│   ├── 📄 ARCHITECTURE.md                  # 系統架構設計
│   ├── 📄 CLEAN_ARCHITECTURE_IMPLEMENTATION.md  # Clean Architecture 實現說明
│   ├── 📄 DOCKER_HUB_GUIDE.md             # Docker Hub 使用指南
│   ├── 📄 MCP_SERVERS_USAGE.md            # MCP Servers 使用說明
│   ├── 📄 MCP_SERVER_HANDBOOK.md          # MCP Server 開發手冊
│   ├── 📄 MODULE_SPECIFICATIONS.md        # 模組規格說明
│   ├── 📄 PROJECT_SUMMARY.md              # 專案總結
│   ├── 📁 examples/                        # 程式範例
│   │   └── MCP_CLIENT_EXAMPLES.md
│   ├── 📁 guides/                          # 使用指南
│   │   ├── QA.md                          # 常見問題
│   │   ├── QUICK_START.md                 # 快速開始指南
│   │   └── USER_GUIDE.md                  # 用戶使用指南
│   ├── 📁 project/                         # 專案資訊
│   │   ├── database-summary-mcp.md        # 資料庫摘要 MCP
│   │   ├── JAVA_MIGRATION_PLAN.md         # Java 遷移計畫
│   │   └── PROJECT_STRUCTURE.md           # 專案結構說明
│   └── 📁 release-notes/                   # 版本說明
│       ├── RELEASE_NOTES_v0.2.0.md
│       ├── RELEASE_NOTES_v0.3.0.md
│       └── RELEASE_NOTES_v0.4.0.md
│
├── 📁 old_kanban_data/                     # 舊看板資料（被 gitignore）
│
├── 📄 .gitignore                           # Git 忽略規則
├── 📄 README.md                            # 專案主說明文檔
└── 📄 CLAUDE.md                            # Claude Code 開發指南
```

## 🗂️ 目錄說明

### `/mcp-registry-java/` - Java 主專案目錄
基於 Maven 多模組架構，包含所有 Java 實現的核心程式碼：

#### **`mcp-common/`** - 共用模組
- **配置管理**: Spring Boot 共用配置
- **例外處理**: 統一例外處理機制
- **MCP 協議**: MCP 工具和資源基礎類別
- **資料模型**: 共用的值物件和實體
- **工具類別**: SQL 安全驗證器等工具

#### **`mcp-core/`** - Clean Architecture 核心模組
- **領域層 (entity/)**: 核心業務實體和值物件，包含聚合根設計
- **應用層 (usecase/)**: Use Case 定義和應用服務實現
- **介面適配器層 (adapter/)**: MCP 協議適配器和基礎設施適配器
- **端口定義 (port/)**: 輸入端口和輸出端口抽象

#### **`mcp-postgresql-server/`** - PostgreSQL MCP Server
- **Spring Boot 應用**: 完整的 PostgreSQL 資料庫 MCP Server
- **MCP 工具**: 連線管理、查詢執行、Schema 管理工具
- **Clean Architecture 整合**: 使用 mcp-core 模組的領域邏輯
- **業務服務**: 資料庫操作相關服務層

#### **`mcp-mysql-server/`** - MySQL MCP Server
- **Spring Boot 應用**: MySQL 資料庫 MCP Server 實現
- **基礎連線管理**: 基本的資料庫連線功能
- **可擴展設計**: 預留擴展其他 MySQL 特定功能

#### **`testing-tools/`** - 測試工具模組
- **測試工具**: 單元測試和整合測試工具
- **Mock 物件**: 測試用的模擬實現

### `/deployment/` - 部署配置目錄
包含 Docker 容器化和部署相關檔案：

- **Docker Compose**: 統一的容器編排配置
- **容器配置**: PostgreSQL 和 MySQL 資料庫容器設定
- **測試環境**: 獨立的測試容器配置

### `/scripts/` - 管理腳本目錄
包含專案管理和建置腳本：

- **統一管理腳本**: Maven 建置、Docker 部署、結構查看等功能

### `/documentation/` - 文檔中心
包含完整的專案文檔系統：

- **架構設計**: Clean Architecture + DDD 實現說明
- **使用指南**: 快速開始和用戶指南
- **專案資訊**: 專案結構、遷移計畫、版本說明
- **開發手冊**: MCP Server 開發和 API 參考

## 🎯 檔案命名規範

### Java 檔案
- **類別**: `PascalCase` (例: `DatabaseConnectionService`)
- **介面**: `PascalCase` (例: `DatabaseQueryExecutor`)
- **套件**: `lowercase` (例: `com.mcp.postgresql`)
- **常數**: `UPPER_SNAKE_CASE` (例: `DEFAULT_POOL_SIZE`)

### 文檔檔案
- **README**: `README.md`
- **指南類**: `*_GUIDE.md` (例: `QUICK_START.md`)
- **說明類**: `*_NOTES.md` (例: `RELEASE_NOTES_*.md`)
- **實現類**: `*_IMPLEMENTATION.md` (例: `CLEAN_ARCHITECTURE_IMPLEMENTATION.md`)

### 配置檔案
- **Maven**: `pom.xml`
- **Spring Boot**: `application.yml`, `application.properties`
- **Docker**: `docker-compose.yml`, `Dockerfile`

## 🏗️ 架構特色

### Clean Architecture + DDD 設計模式
- **依賴反轉原則**: 高層次模組不依賴低層次模組
- **領域驅動設計**: 以業務領域為核心的設計模式
- **端口與適配器**: 六角形架構實現
- **聚合根**: 資料一致性邊界管理

### 企業級 Java 特性
- **Spring Boot 3.x**: 現代化 Spring 框架
- **Maven 多模組**: 模組化專案管理
- **反應式程式設計**: Project Reactor 非同步處理
- **企業級安全性**: 參數化查詢、連線池管理

### MCP 協議整合
- **純工具層設計**: 不包含業務邏輯，專注工具提供
- **標準 MCP 工具**: 連線管理、查詢執行、Schema 管理
- **MCP 資源**: 連線狀態和詳細資訊提供

## 🚀 如何使用新結構

### Maven 建置
```bash
# 建置所有模組
cd mcp-registry-java/
mvn clean compile

# 執行測試
mvn test

# 打包應用
mvn package
```

### 啟動 MCP Server
```bash
# PostgreSQL MCP Server
cd mcp-registry-java/mcp-postgresql-server/
mvn spring-boot:run

# MySQL MCP Server
cd mcp-registry-java/mcp-mysql-server/
mvn spring-boot:run
```

### Docker 部署
```bash
# 使用 Docker Compose
cd deployment/
docker-compose up -d

# 使用 Jib 建置 Docker 映像
cd mcp-registry-java/
mvn compile jib:dockerBuild
```

### 統一管理腳本
```bash
# 使用統一管理腳本
./scripts/start-all.sh build    # Maven 建置
./scripts/start-all.sh test     # 執行測試
./scripts/start-all.sh docker   # Docker 建置
./scripts/start-all.sh structure # 查看專案結構
```

## 📚 相關文檔

- [**快速開始指南**](../guides/QUICK_START.md) - 5分鐘內啟動 Java MCP Server
- [**架構設計文檔**](../ARCHITECTURE.md) - 系統架構和設計原則
- [**Clean Architecture 實現**](../CLEAN_ARCHITECTURE_IMPLEMENTATION.md) - DDD + CA 實現說明
- [**Java 遷移計畫**](JAVA_MIGRATION_PLAN.md) - 從 Python 到 Java 的遷移說明
- [**Docker Hub 指南**](../DOCKER_HUB_GUIDE.md) - 容器部署和使用指南
- [**文檔中心**](../README.md) - 完整文檔導覽

---

這個 Java Edition 專案結構採用現代化企業級設計模式，提供高品質、可擴展的 MCP Server 實現。每個模組都有明確的職責範圍，遵循 Clean Architecture + DDD 設計原則，讓開發者能快速理解和擴展功能。