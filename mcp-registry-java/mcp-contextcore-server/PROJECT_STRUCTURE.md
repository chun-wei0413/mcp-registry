# ContextCore MCP Server - 專案結構

## 📁 專案架構總覽

```
mcp-contextcore-server/
├── src/main/java/com/mcp/contextcore/
│   ├── ContextCoreMCPApplication.java          # 應用程式入口點
│   │
│   ├── 🎯 domain/                              # Domain Layer (核心業務邏輯)
│   │   ├── entity/
│   │   │   ├── Log.java                        # 日誌實體
│   │   │   └── LogSearchResult.java            # 搜尋結果實體
│   │   └── repository/
│   │       ├── LogRepository.java              # 日誌倉儲介面
│   │       ├── VectorRepository.java           # 向量倉儲介面
│   │       └── EmbeddingService.java           # Embedding 服務介面
│   │
│   ├── 🔧 infrastructure/                      # Infrastructure Layer (技術實作)
│   │   ├── sqlite/
│   │   │   └── SqliteLogRepository.java        # SQLite 實作
│   │   ├── qdrant/
│   │   │   └── QdrantVectorRepository.java     # Qdrant 實作
│   │   └── ollama/
│   │       └── OllamaEmbeddingService.java     # Ollama 實作
│   │
│   ├── 💼 usecase/                             # Use Case Layer (應用邏輯)
│   │   ├── AddLogUseCase.java                  # 新增日誌
│   │   ├── SearchLogsUseCase.java              # 搜尋日誌
│   │   ├── GetLogUseCase.java                  # 獲取日誌
│   │   ├── ListLogSummariesUseCase.java        # 列出摘要
│   │   └── GetProjectContextUseCase.java       # 獲取專案上下文
│   │
│   ├── 🌐 controller/                          # Controller Layer (API 層)
│   │   ├── ContextCoreMCPController.java       # MCP Tools Controller
│   │   └── dto/
│   │       ├── AddLogRequest.java              # 新增日誌請求
│   │       ├── SearchLogsRequest.java          # 搜尋日誌請求
│   │       ├── LogResponse.java                # 日誌回應
│   │       └── SearchResultResponse.java       # 搜尋結果回應
│   │
│   └── ⚙️  config/                             # Configuration (配置)
│       └── DatabaseConfig.java                 # 資料庫配置
│
├── src/main/resources/
│   ├── application.yml                         # Spring Boot 配置
│   └── logback-spring.xml                      # 日誌配置
│
├── src/test/java/                              # 測試目錄
│
├── pom.xml                                     # Maven 配置
└── README.md                                   # 專案說明

```

## 🏗️ Clean Architecture 分層

### 1️⃣ Domain Layer（最內層 - 核心業務邏輯）

**目的**: 定義業務實體和介面，不依賴任何外部框架

**檔案**:
- `Log.java` - 日誌實體，包含業務驗證邏輯
- `LogSearchResult.java` - 搜尋結果值物件
- `LogRepository.java` - 日誌持久化介面
- `VectorRepository.java` - 向量儲存介面
- `EmbeddingService.java` - 向量化服務介面

**特點**:
- ✅ 純 Java 物件，無框架依賴
- ✅ 包含業務規則和驗證
- ✅ 使用介面定義外部依賴

### 2️⃣ Infrastructure Layer（技術實作層）

**目的**: 實作 Domain 層定義的介面，處理技術細節

**檔案**:
- `SqliteLogRepository.java` - SQLite 資料庫操作
- `QdrantVectorRepository.java` - Qdrant 向量操作
- `OllamaEmbeddingService.java` - Ollama API 整合

**特點**:
- ✅ 依賴 Domain Layer 的介面
- ✅ 處理資料庫、HTTP、檔案等技術細節
- ✅ 可替換（例如 SQLite → PostgreSQL）

### 3️⃣ Use Case Layer（應用邏輯層）

**目的**: 協調 Domain 和 Infrastructure，實現具體業務流程

**檔案**:
- `AddLogUseCase.java` - 新增日誌流程
- `SearchLogsUseCase.java` - 搜尋日誌流程
- `GetLogUseCase.java` - 獲取日誌流程
- `ListLogSummariesUseCase.java` - 列出摘要流程
- `GetProjectContextUseCase.java` - 獲取專案上下文流程

**特點**:
- ✅ 編排多個 Repository 和 Service
- ✅ 實現完整的業務流程
- ✅ 處理事務和錯誤邏輯

### 4️⃣ Controller Layer（最外層 - API 介面）

**目的**: 提供 MCP Tools 的 HTTP API

**檔案**:
- `ContextCoreMCPController.java` - REST API 控制器
- `AddLogRequest.java` - API 請求 DTO
- `LogResponse.java` - API 回應 DTO

**特點**:
- ✅ 依賴 Use Case Layer
- ✅ 處理 HTTP 請求/回應
- ✅ DTO 轉換

## 🔄 資料流程範例

### 新增日誌流程

```
1. Controller 接收 HTTP POST 請求
   ↓
2. 轉換 AddLogRequest → Use Case 參數
   ↓
3. AddLogUseCase.execute()
   ├─→ LogRepository.save() (SQLite)
   ├─→ EmbeddingService.embed() (Ollama)
   └─→ VectorRepository.storeVector() (Qdrant)
   ↓
4. 返回 Log 實體
   ↓
5. Controller 轉換 Log → LogResponse
   ↓
6. 返回 HTTP 200 + JSON
```

### 搜尋日誌流程

```
1. Controller 接收 HTTP POST 請求
   ↓
2. SearchLogsUseCase.execute()
   ├─→ EmbeddingService.embed(query) (Ollama)
   ├─→ VectorRepository.searchSimilar() (Qdrant)
   └─→ LogRepository.findByIds() (SQLite)
   ↓
3. 返回 List<LogSearchResult>
   ↓
4. Controller 轉換為 SearchResultResponse
   ↓
5. 返回 HTTP 200 + JSON
```

## 📦 依賴關係

```
Controller Layer
    ↓ (依賴)
Use Case Layer
    ↓ (依賴)
Domain Layer (介面)
    ↑ (實作)
Infrastructure Layer
```

**依賴規則**:
- ✅ 外層可依賴內層
- ❌ 內層不可依賴外層
- ✅ Domain 只依賴 Java 標準庫

## 🛠️ 技術選型

| 層級 | 技術 | 理由 |
|------|------|------|
| **Domain** | Pure Java | 業務邏輯獨立於技術 |
| **Infrastructure** | SQLite, Qdrant, Ollama | 輕量、易部署、免費 |
| **Use Case** | Reactor (Reactive) | 非同步、高效能 |
| **Controller** | Spring WebFlux | 反應式 REST API |

## 📊 統計資訊

- **總檔案數**: 20+ Java 檔案
- **程式碼行數**: ~2,000+ 行
- **架構層級**: 4 層 (Clean Architecture)
- **設計模式**: Repository, Use Case, DTO

## 🚀 下一步

1. ✅ 核心程式碼完成
2. ⏳ 撰寫單元測試
3. ⏳ 整合測試
4. ⏳ 部署測試

## 📖 相關文檔

- [完整功能說明](../../documentation/mcp-servers/contextcore-mcp.md)
- [API 使用範例](README.md)
- [快速開始指南](../../documentation/GETTING_STARTED.md)
