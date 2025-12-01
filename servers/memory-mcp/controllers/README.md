# Controllers - MCP 工具與資源入口

本目錄包含所有 MCP（Model Context Protocol）工具和資源的控制器實現。每個控制器負責將業務邏輯服務層暴露為 MCP 工具，供外部 AI 客戶端調用。

## 📂 檔案概述

### `knowledge_controller.py`
處理知識搜尋和學習相關的 MCP 工具。

**提供的 MCP 工具：**

#### 1. `search_knowledge` - 語義搜尋
```python
def search_knowledge(query: str, top_k: int = 20, topic: Optional[str] = None) -> SearchResult
```

**功能：** 在知識庫中執行語義搜尋，返回最相關的知識點。

**參數：**
- `query` (str, 必填)：自然語言搜尋查詢
  - 範例：`"如何實作 Aggregate？"`、`"測試編寫規範"`
- `top_k` (int, 可選, 默認=20)：返回的最大結果數
  - 範例：`5`、`10`、`20`
- `topic` (str, 可選)：按主題過濾搜尋結果
  - 範例：`"DDD"`、`"testing"`、`"spring-boot"`

**返回值：** `SearchResult` 包含：
- 知識點列表（按相似度排序）
- 每個知識點包含文本、元數據和相關代碼塊

**使用示例：**
```python
# 搜尋 Aggregate 實作
results = search_knowledge("aggregate", top_k=5)

# 搜尋並按主題過濾
results = search_knowledge("test", top_k=10, topic="prompts-subagent")
```

**實現細節：**
- 使用 `VectorStoreService.search_knowledge()` 進行語義搜尋
- 默認搜尋 `ai_documentation` collection（1,116 chunks）
- 支援可選的主題過濾

---

#### 2. `learn_knowledge` - 新增知識點
```python
def learn_knowledge(topic: str, content: str) -> str
```

**功能：** 手動添加新的知識點到知識庫。

**參數：**
- `topic` (str, 必填)：知識點的主題分類
  - 範例：`"DDD"`、`"EventSourcing"`、`"CleanArchitecture"`
- `content` (str, 必填)：知識點的文本內容
  - 可包含 Markdown 格式
  - 可包含代碼塊（會自動分離）

**返回值：** 確認訊息，包含知識點的唯一 ID

**使用示例：**
```python
# 添加新的 DDD 知識點
result = learn_knowledge(
    topic="DDD",
    content="Aggregate 是 DDD 中的核心概念，代表一個一致性邊界..."
)
# 返回：Knowledge learned with ID: 550e8400-e29b-41d4-a716-446655440000
```

**實現細節：**
- 使用 `VectorStoreService.add_knowledge()` 添加知識
- 自動生成 UUID 作為唯一識別碼
- 支持 Markdown 格式的內容
- 代碼塊會自動分離（v2.0 特性）

---

### `document_controller.py`
處理文件存儲相關的 MCP 工具。

**提供的 MCP 工具：**

#### 1. `store_document` - 存儲文件
```python
def store_document(file_path: str, topic: Optional[str] = None) -> str
```

**功能：** 讀取並儲存文件到知識庫，自動進行 chunking 和 embedding。

**參數：**
- `file_path` (str, 必填)：要儲存的文件路徑（相對或絕對）
  - 支持格式：`.md`, `.txt`, `.json`
  - 範例：`"./docs/ARCHITECTURE.md"`、`"/path/to/file.txt"`
- `topic` (str, 可選)：知識點的主題分類
  - 如果未指定，使用文件名作為 topic
  - 範例：`"Architecture"`、`"API-Reference"`

**返回值：** 確認訊息，包含文件信息

**使用示例：**
```python
# 使用文件名作為 topic
result = store_document("./docs/ARCHITECTURE.md")

# 指定自訂 topic
result = store_document("./docs/spec.md", topic="Specification")
```

**實現細節：**
- 使用 `VectorStoreService` 進行文件處理
- 自動檢測文件編碼
- 遵循 v2.0 智能代碼分離策略
- 支持大文件的自動分塊

---

### `indexing_controller.py`
處理批量索引相關的 MCP 工具。

**提供的 MCP 工具：**

#### 1. `batch_index_folder` - 批量索引文件夾
```python
def batch_index_folder(source_dir: str, chunk_size: int = 4000,
                      chunk_overlap: int = 200,
                      file_extensions: Optional[List[str]] = None) -> IndexingStats
```

**功能：** 批量掃描和索引整個文件夾中的所有支持的文件。

**參數：**
- `source_dir` (str, 必填)：要索引的文件夾路徑
  - 範例：`"./documentation"`、`"/app/docs"`
- `chunk_size` (int, 可選, 默認=4000)：每個 chunk 的最大字符數
- `chunk_overlap` (int, 可選, 默認=200)：chunk 之間的重疊字符數
- `file_extensions` (list, 可選)：要處理的文件副檔名
  - 默認：`['.md', '.txt', '.java', '.py', '.js', '.ts', '.sh', '.json', '.yaml', '.yml']`
  - 範例：`['.md', '.txt']` 只處理 Markdown 和純文本

**返回值：** `IndexingStats` 包含：
- 處理統計（文件數、chunk 數、耗時等）
- 成功/失敗詳情

**使用示例：**
```python
# 批量索引文件夾
stats = batch_index_folder("./documentation")

# 自訂設定
stats = batch_index_folder(
    source_dir="./docs",
    chunk_size=6000,
    chunk_overlap=300,
    file_extensions=[".md"]
)

# 查看統計
print(f"處理了 {stats.processed_files} 個文件")
print(f"生成了 {stats.total_chunks} 個 chunks")
```

**實現細節：**
- 使用 `ContextChunkingService` 進行智能分塊
- 遞歸掃描文件夾
- 支持過濾特定文件類型
- 自動錯誤處理和日誌記錄

---

### `resource_controller.py`
處理 MCP 資源（Resources）的註冊。

**提供的 MCP 資源：**

#### 1. `knowledge://{topic}` - 按主題檢索知識點
**功能：** 獲取特定主題下的所有知識點。

**URI 參數：**
- `topic` (str)：主題名稱
  - 範例：`knowledge://DDD`、`knowledge://testing`

**返回值：** `RetrievalResult` 包含該主題下的所有知識點

**使用示例：**
```
GET /knowledge/DDD
→ 返回所有 "DDD" 主題的知識點

GET /knowledge/testing
→ 返回所有 "testing" 主題的知識點
```

**實現細節：**
- 使用 `VectorStoreService.get_all_by_topic()` 檢索
- 返回完整的知識點（包括代碼塊）
- 按建立時間排序

---

## 🏗️ 架構設計

```
MCP Client
    ↓
MCP Server (FastMCP)
    ↓
┌───────────────────────────────────────┐
│  Controllers Layer                    │
├───────────────────────────────────────┤
│ • knowledge_controller.py             │
│ • document_controller.py              │
│ • indexing_controller.py              │
│ • resource_controller.py              │
└───────────────────────────────────────┘
    ↓
┌───────────────────────────────────────┐
│  Services Layer                       │
├───────────────────────────────────────┤
│ • VectorStoreService                  │
│ • ContextChunkingService              │
└───────────────────────────────────────┘
    ↓
┌───────────────────────────────────────┐
│  Models & Storage                     │
├───────────────────────────────────────┤
│ • ChromaDB (Vector Database)          │
│ • SentenceTransformer (Embeddings)    │
└───────────────────────────────────────┘
```

---

## 📊 資料流程

### 搜尋流程
```
MCP Client
  ↓ search_knowledge(query, top_k)
Knowledge Controller
  ↓ vector_store.search_knowledge()
Vector Store Service
  ↓ model.encode(query)
SentenceTransformer
  ↓ embedding vector
ChromaDB.query()
  ↓ similar results
Knowledge Controller
  ↓ SearchResult(results=[KnowledgePoint...])
MCP Client
```

### 索引流程
```
MCP Client
  ↓ batch_index_folder(source_dir)
Indexing Controller
  ↓ context_chunking_service.chunk_folder()
Context Chunking Service
  ↓ split & embed files
Vector Store Service
  ↓ add to ChromaDB
ChromaDB.add()
  ↓ IndexingStats
Indexing Controller
  ↓ return statistics
MCP Client
```

---

## 🔌 工具註冊流程

所有工具都在 `app.py` 的 `create_app()` 函數中自動註冊：

```python
# app.py
from controllers.knowledge_controller import register_knowledge_tools
from controllers.document_controller import register_document_tools
from controllers.indexing_controller import register_indexing_tools
from controllers.resource_controller import register_resources

# 在 create_app() 中
register_knowledge_tools(server, vector_store)
register_document_tools(server, vector_store)
register_indexing_tools(server, context_chunking)
register_resources(server, vector_store)
```

---

## 💡 開發指南

### 新增工具的步驟

1. **在相應的 controller 檔案中添加工具函數**
   ```python
   @server.tool()
   def new_tool(param1: str, param2: int) -> ReturnType:
       """工具說明"""
       # 實現邏輯
       return result
   ```

2. **使用正確的返回類型**
   - 必須使用 `models/knowledge_models.py` 中定義的資料模型
   - 或返回基本類型 (str, int, dict 等)

3. **添加完整的文檔**
   - docstring 說明工具功能
   - 參數說明和類型
   - 返回值說明
   - 使用示例

4. **遵循命名規範**
   - 工具名稱使用蛇形命名法 (snake_case)
   - 控制器名稱使用 `*_controller.py` 格式

### 測試工具

```python
from services.vector_store_service import VectorStoreService

vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

# 測試搜尋
results = vs.search_knowledge("test query", top_k=5)
print(f"Found {len(results)} results")
```

---

## 🎯 最佳實踐

1. **保持 Controller 輕量級**
   - 不在 controller 中進行複雜業務邏輯
   - 將邏輯委派給 Service 層

2. **使用正確的資料模型**
   - 始終使用 `models/knowledge_models.py` 中的模型
   - Pydantic 會自動驗證資料

3. **提供清晰的文檔**
   - 每個工具都需要完整的文檔字符串
   - 說明參數、返回值和使用示例

4. **錯誤處理**
   - 捕捉和記錄異常
   - 返回有意義的錯誤訊息給客戶端

5. **性能考慮**
   - 使用分頁減少返回數據量
   - 限制 `top_k` 的大小（推薦 20 以內）

---

## 🔗 相關文件

- [models/README.md](../models/README.md) - 資料模型說明
- [services/README.md](../services/README.md) - 服務層說明
- [MCP_SERVER_CONFIG.md](../MCP_SERVER_CONFIG.md) - Server 配置指南
- [QUERY_GUIDE.md](../QUERY_GUIDE.md) - 查詢使用指南

---

**最後更新：** 2025-11-24
**版本：** v2.0
**維護者：** RAG Memory MCP Team
