# ContextCore MCP - 智能開發日誌管理系統

## 專案概述

ContextCore MCP 是一個基於 MCP (Model Context Protocol) 的智能開發日誌管理系統，專為使用 Claude Code 的開發者設計。它透過向量搜尋技術，讓 LLM 能夠智能地檢索相關的歷史開發記錄，而不需要每次都載入所有日誌內容。

## 要解決的問題

### 核心痛點

在使用 Claude Code 進行開發時，開發者經常需要參考歷史的開發決策、實作細節或 Bug 修復記錄。傳統做法有以下問題：

1. **Context 過載**：每次對話都載入所有歷史日誌，隨著專案增長會導致：
    - Token 消耗量暴增
    - 回應速度變慢
    - 成本大幅增加
    - 超出 Context 長度限制

2. **檢索效率低**：使用傳統全文搜尋：
    - 只能匹配關鍵字，無法理解語義
    - 無法找到相關但用詞不同的內容
    - 例如搜尋「登入」找不到「authentication」相關記錄

3. **資訊組織困難**：
    - 日誌分散在不同文件
    - 缺乏結構化的標籤和分類
    - 難以追蹤特定模組或功能的演進歷史

### 解決方案

ContextCore MCP 透過**語義向量搜尋**技術，讓 LLM 能夠：
- ✅ 只檢索與當前任務相關的日誌
- ✅ 理解查詢的語義，找到概念相關的內容
- ✅ 支援多維度過濾（標籤、模組、時間、類型）
- ✅ 隨著專案規模增長仍保持高效

## 系統架構

### 整體架構圖

```
┌─────────────────┐
│   Claude Code   │ (使用者介面)
└────────┬────────┘
         │ MCP Protocol
┌────────▼─────────────────────────────┐
│         MCP Server (Java)            │
│  ┌──────────────────────────────┐   │
│  │ Tools:                        │   │
│  │ - add_log                     │   │
│  │ - search_logs                 │   │
│  │ - get_log                     │   │
│  │ - list_log_summaries          │   │
│  │ - get_project_context         │   │
│  └──────────────────────────────┘   │
└─────┬─────────────────┬──────────────┘
      │                 │
      │                 │
┌─────▼─────┐     ┌─────▼──────┐
│  SQLite   │     │  Qdrant    │
│ (元數據)   │     │  (向量DB)   │
│           │     │            │
│ - 日誌內容 │     │ - 文本向量  │
│ - 標籤     │     │ - 相似搜尋  │
│ - 時間戳   │     │ - 過濾條件  │
└───────────┘     └─────┬──────┘
                        │
                  ┌─────▼──────┐
                  │   Ollama   │
                  │ (Embedding) │
                  │            │
                  │ 文字 → 向量 │
                  └────────────┘
```

### 資料流程

#### 1. 寫入日誌流程

```
開發者輸入日誌
    ↓
MCP Server 接收
    ↓
並行處理：
├─→ SQLite: 儲存完整文本 + 元數據
│   (ID, 標題, 內容, 標籤, 模組, 類型, 時間)
│
└─→ Ollama: 文字 → 向量轉換
    "實現 JWT 登入功能" → [0.12, -0.34, 0.56, ...]
        ↓
    Qdrant: 儲存向量 + 關聯資訊
    (向量, log_id, 標籤, 模組)
```

#### 2. 搜尋日誌流程

```
Claude Code 發起查詢: "之前怎麼做身份驗證?"
    ↓
MCP Server 處理
    ↓
Ollama: 查詢 → 向量
"身份驗證" → [0.15, -0.32, 0.58, ...]
    ↓
Qdrant: 向量相似度搜尋 + 條件過濾
    找到最相似的 5 條日誌 ID
    [log_123, log_456, log_789, ...]
    ↓
SQLite: 根據 ID 獲取完整內容
    返回日誌詳細資訊
    ↓
回傳給 Claude Code
```

## 技術棧

### 核心組件

| 組件 | 技術選型 | 用途 | 為什麼選它 |
|------|---------|------|-----------|
| **MCP Server** | Java | 主要服務 | 高效能、生態成熟、易於維護 |
| **向量資料庫** | Qdrant | 向量儲存與搜尋 | 輕量、支援 Docker、有官方 Java SDK |
| **Embedding 服務** | Ollama | 文字向量化 | 本地部署、免費、支援多種模型 |
| **關聯式資料庫** | SQLite | 完整資料儲存 | 輕量、無需額外服務、檔案式儲存 |
| **容器化** | Docker Compose | 服務編排 | 一鍵部署、環境一致性 |

### Embedding 模型

使用 Ollama 提供的預訓練模型（無需自行訓練）：

| 模型 | 大小 | 向量維度 | 適用場景 |
|------|------|---------|---------|
| **nomic-embed-text** (推薦) | 274 MB | 768 | 最佳平衡，中英文支援良好 |
| all-minilm | 45 MB | 384 | 資源受限環境 |
| mxbai-embed-large | 670 MB | 1024 | 追求極致品質 |

### Java 框架選擇

```java
// 核心依賴
- Qdrant Java Client: io.qdrant:client
- SQLite JDBC: org.xerial:sqlite-jdbc
- HTTP Client: Java 11+ HttpClient (內建)
- JSON 處理: com.google.code.gson:gson
- MCP SDK: (待整合官方 SDK)
```

## MCP Tools 設計

### 1. add_log - 新增開發日誌

```java
@MCPTool(name = "add_log", description = "新增開發日誌到記憶庫")
public class AddLogTool {
    /**
     * @param title 日誌標題（必填）
     * @param content 日誌內容（必填）
     * @param tags 標籤列表（選填）例如: ["auth", "backend"]
     * @param module 所屬模組（選填）例如: "authentication"
     * @param type 日誌類型（選填）: feature | bug | decision | note
     * @return 日誌 ID
     */
}
```

**使用範例**：
```
Claude Code 自動調用：
add_log(
  title="實現 JWT 登入功能",
  content="使用 jjwt 庫實現 JWT token 生成和驗證...",
  tags=["auth", "security", "backend"],
  module="authentication",
  type="feature"
)
```

### 2. search_logs - 語義搜尋日誌

```java
@MCPTool(name = "search_logs", description = "使用語義搜尋查找相關日誌")
public class SearchLogsTool {
    /**
     * @param query 搜尋查詢（必填）
     * @param limit 返回數量（選填，預設 5）
     * @param tags 過濾標籤（選填）
     * @param module 過濾模組（選填）
     * @param type 過濾類型（選填）
     * @param dateFrom 開始日期（選填）ISO 8601 格式
     * @param dateTo 結束日期（選填）ISO 8601 格式
     * @return 相關日誌列表（按相似度排序）
     */
}
```

**使用範例**：
```
開發者問："我之前怎麼處理用戶認證的？"

Claude Code 自動調用：
search_logs(
  query="用戶認證處理方式",
  limit=3,
  tags=["auth"]
)

返回：
1. "實現 JWT 登入功能" (相似度: 0.92)
2. "OAuth2 整合" (相似度: 0.87)
3. "Session 管理機制" (相似度: 0.81)
```

### 3. get_log - 獲取特定日誌詳情

```java
@MCPTool(name = "get_log", description = "根據 ID 獲取完整日誌內容")
public class GetLogTool {
    /**
     * @param id 日誌 ID（必填）
     * @return 完整日誌內容
     */
}
```

### 4. list_log_summaries - 列出日誌摘要

```java
@MCPTool(name = "list_log_summaries", description = "列出日誌摘要（僅標題和元數據）")
public class ListLogSummariesTool {
    /**
     * @param limit 返回數量（選填）
     * @param tags 過濾標籤（選填）
     * @param module 過濾模組（選填）
     * @return 日誌摘要列表
     */
}
```

### 5. get_project_context - 獲取專案上下文

```java
@MCPTool(name = "get_project_context", description = "獲取專案關鍵決策和近期重要日誌")
public class GetProjectContextTool {
    /**
     * @param modules 關注的模組列表（選填）
     * @param limit 返回數量（選填，預設 10）
     * @return 重要日誌列表（決策類型優先）
     */
}
```

## 資料庫設計

### SQLite Schema

```sql
CREATE TABLE logs (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    tags TEXT,              -- JSON array: ["tag1", "tag2"]
    module TEXT,
    type TEXT,              -- feature | bug | decision | note
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_logs_timestamp ON logs(timestamp);
CREATE INDEX idx_logs_module ON logs(module);
CREATE INDEX idx_logs_type ON logs(type);
CREATE INDEX idx_logs_tags ON logs(tags);
```

### Qdrant Collection 設定

```java
// 向量維度: 768 (nomic-embed-text)
// 距離計算: Cosine Similarity
// Payload 結構:
{
  "log_id": "uuid",
  "tags": ["tag1", "tag2"],
  "module": "authentication",
  "type": "feature",
  "timestamp": "2025-10-04T10:30:00Z"
}
```

## 部署配置

### Docker Compose

```yaml
version: '3.8'

services:
  # 向量資料庫
  qdrant:
    image: qdrant/qdrant:latest
    container_name: memory-qdrant
    ports:
      - "6333:6333"
    volumes:
      - ./data/qdrant:/qdrant/storage
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M
    restart: unless-stopped

  # Embedding 服務
  ollama:
    image: ollama/ollama:latest
    container_name: memory-ollama
    ports:
      - "11434:11434"
    volumes:
      - ./data/ollama:/root/.ollama
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
    restart: unless-stopped

  # Java MCP Server
  mcp-server:
    build: ./mcp-server
    container_name: memory-mcp
    ports:
      - "8080:8080"
    volumes:
      - ./data/sqlite:/app/data
    environment:
      - QDRANT_HOST=qdrant
      - QDRANT_PORT=6333
      - OLLAMA_HOST=ollama
      - OLLAMA_PORT=11434
      - DATABASE_PATH=/app/data/memory.db
    depends_on:
      - qdrant
      - ollama
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M
    restart: unless-stopped

volumes:
  qdrant_data:
  ollama_data:
  sqlite_data:
```

### 資源需求

| 配置等級 | RAM 需求 | 適用場景 |
|---------|---------|---------|
| **最小配置** | 1 GB | 測試環境、小型專案 (<1K 日誌) |
| **標準配置** | 2 GB | 一般開發專案 (1K-10K 日誌) |
| **推薦配置** | 4 GB | 大型專案 (10K-100K 日誌) |

### 儲存空間

```
空資料庫: ~10 MB
每 1,000 條日誌: ~2-3 MB
每 10,000 條日誌: ~25-30 MB
每 100,000 條日誌: ~250-300 MB
```

## 使用場景示例

### 場景 1: 開發新功能時查找歷史實作

```
開發者: "幫我實現一個用戶註冊功能"

Claude Code 內部流程:
1. search_logs(query="用戶註冊 註冊功能", tags=["auth"], limit=3)
2. 找到相關日誌:
   - "實現 JWT 登入功能"
   - "用戶資料驗證邏輯"
3. 參考歷史實作，生成新代碼
4. add_log(title="實現用戶註冊功能", content="...", tags=["auth", "user"])
```

### 場景 2: 追蹤 Bug 修復歷史

```
開發者: "之前有修過類似的資料庫連接錯誤嗎？"

Claude Code 內部流程:
1. search_logs(query="資料庫連接錯誤", type="bug", limit=5)
2. 返回相關 Bug 修復記錄
3. 提供修復建議
```

### 場景 3: 獲取專案決策上下文

```
開發者: "這個專案的 API 設計原則是什麼？"

Claude Code 內部流程:
1. search_logs(query="API 設計原則", type="decision")
2. get_project_context(modules=["api"], limit=5)
3. 總結關鍵決策和設計理念
```

## 優勢特點

### 1. 智能檢索
- ✅ 語義理解：「登入」能找到「authentication」相關內容
- ✅ 多維度過濾：標籤、模組、時間、類型組合查詢
- ✅ 相似度排序：最相關的結果優先顯示

### 2. 高效能
- ✅ 毫秒級搜尋：即使 10 萬條日誌也能快速檢索
- ✅ 增量索引：新增日誌即時可搜
- ✅ 輕量部署：2-4 GB RAM 即可運行

### 3. 隱私安全
- ✅ 完全本地部署：數據不出本機
- ✅ 無需外部 API：不依賴雲端服務
- ✅ 離線可用：無網路環境也能使用

### 4. 可擴展性
- ✅ 模組化設計：易於添加新功能
- ✅ 支援多專案：可為不同專案創建獨立資料庫
- ✅ 靈活配置：可調整模型、向量維度等參數

## 未來擴展方向

### Phase 1: 核心功能（當前）
- [x] 基本日誌增刪查改
- [x] 語義向量搜尋
- [x] 標籤和模組分類
- [x] Docker 部署

### Phase 2: 增強功能
- [ ] 日誌版本控制
- [ ] 日誌關聯關係（引用其他日誌）
- [ ] 自動標籤建議
- [ ] 統計分析面板

### Phase 3: 進階功能
- [ ] 多專案管理
- [ ] 團隊協作支援
- [ ] 匯入匯出功能
- [ ] Web UI 管理介面

### Phase 4: 智能化
- [ ] 自動摘要生成
- [ ] 趨勢分析
- [ ] 知識圖譜構建
- [ ] 智能推薦系統

## 技術亮點

1. **向量搜尋技術**：使用最新的語義嵌入技術，理解查詢意圖
2. **混合儲存架構**：SQLite 負責結構化數據，Qdrant 負責向量檢索
3. **預訓練模型**：無需自行訓練，開箱即用
4. **容器化部署**：一鍵啟動，環境隔離
5. **MCP 協議整合**：與 Claude Code 無縫對接

## 快速開始

```bash
# 1. 克隆專案
git clone <repository-url>
cd memory-mcp

# 2. 啟動服務
docker-compose up -d

# 3. 初始化 Embedding 模型
docker exec memory-ollama ollama pull nomic-embed-text

# 4. 驗證服務
curl http://localhost:8080/health

# 5. 開始使用
# 在 Claude Code 中配置 MCP Server 後即可使用
```

