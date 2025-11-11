# ContextCore MCP Server

智能開發日誌管理系統 - 基於向量搜尋的 MCP Server

## 快速開始

### 1. 啟動依賴服務

```bash
# 使用 Docker Compose 啟動 Qdrant + Ollama
docker-compose -f ../../deployment/contextcore-docker-compose.yml up -d

# 下載 Embedding 模型
docker exec contextcore-ollama ollama pull nomic-embed-text
```

### 2. 建置專案

```bash
mvn clean package
```

### 3. 啟動服務

```bash
java -jar target/mcp-contextcore-server-1.0.0-SNAPSHOT.jar
```

或使用一鍵啟動腳本：

```bash
../../scripts/start-contextcore.sh
```

### 4. 驗證服務

```bash
curl http://localhost:8082/actuator/health
```

## MCP Tools

### add_log - 新增日誌

```bash
curl -X POST http://localhost:8082/api/mcp/contextcore/logs \
  -H "Content-Type: application/json" \
  -d '{
    "title": "實現 JWT 登入功能",
    "content": "使用 Spring Security + JWT 實現用戶認證...",
    "tags": ["auth", "security"],
    "module": "authentication",
    "type": "feature"
  }'
```

### search_logs - 語義搜尋

```bash
curl -X POST http://localhost:8082/api/mcp/contextcore/logs/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "如何實現用戶認證",
    "limit": 3,
    "tags": ["auth"]
  }'
```

### get_log - 獲取日誌

```bash
curl http://localhost:8082/api/mcp/contextcore/logs/{log_id}
```

### list_log_summaries - 列出日誌摘要

```bash
curl "http://localhost:8082/api/mcp/contextcore/logs/summaries?limit=10&module=authentication"
```

### get_project_context - 獲取專案上下文

```bash
curl "http://localhost:8082/api/mcp/contextcore/context?limit=10"
```

## 環境變數

| 變數名稱 | 預設值 | 說明 |
|---------|--------|------|
| `MCP_SERVER_PORT` | 8082 | MCP Server 端口 |
| `CONTEXTCORE_SQLITE_PATH` | ./data/contextcore.db | SQLite 資料庫路徑 |
| `CONTEXTCORE_QDRANT_HOST` | localhost | Qdrant 主機 |
| `CONTEXTCORE_QDRANT_PORT` | 6334 | Qdrant gRPC 端口 |
| `CONTEXTCORE_OLLAMA_HOST` | localhost | Ollama 主機 |
| `CONTEXTCORE_OLLAMA_PORT` | 11434 | Ollama 端口 |
| `CONTEXTCORE_OLLAMA_MODEL` | nomic-embed-text | Embedding 模型 |
| `CONTEXTCORE_LOG_LEVEL` | DEBUG | 日誌級別 |

## 系統架構

```
ContextCore MCP
├── Domain Layer
│   ├── Entity (Log, LogSearchResult)
│   └── Repository Interface
├── Infrastructure Layer
│   ├── SQLite (完整日誌儲存)
│   ├── Qdrant (向量搜尋)
│   └── Ollama (文字向量化)
├── Use Case Layer
│   ├── AddLogUseCase
│   ├── SearchLogsUseCase
│   ├── GetLogUseCase
│   ├── ListLogSummariesUseCase
│   └── GetProjectContextUseCase
└── Controller Layer
    └── ContextCoreMCPController (MCP Tools)
```

## 技術棧

- **Java 17** + **Spring Boot 3.2.1**
- **SQLite** - 完整資料儲存
- **Qdrant** - 向量資料庫
- **Ollama** - Embedding 服務
- **nomic-embed-text** - Embedding 模型 (768 維)

## 資料流程

### 寫入日誌

```
開發者輸入 → MCP Server → SQLite (儲存完整內容)
                    ↓
            Ollama (文字 → 向量)
                    ↓
            Qdrant (儲存向量)
```

### 搜尋日誌

```
查詢文字 → Ollama (文字 → 向量)
              ↓
         Qdrant (向量相似搜尋)
              ↓
         SQLite (獲取完整內容)
              ↓
         返回結果 (附相似度分數)
```

## 開發資訊

- **專案結構**: Clean Architecture + DDD
- **測試框架**: JUnit 5 + ezSpec
- **日誌**: Logback (./logs/contextcore-mcp.log)
- **監控**: Spring Boot Actuator

## 相關文檔

- [完整文檔](../../documentation/mcp-servers/contextcore-mcp.md)
- [快速開始](../../documentation/GETTING_STARTED.md)
- [常見問題](../../documentation/guides/FAQ.md)
