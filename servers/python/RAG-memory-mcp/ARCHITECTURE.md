# RAG Memory MCP Server - 專案架構

## 總覽

本專案採用**分層架構設計**，將 MCP Server 分為 Controller、Service、Model 三層，提供清晰的程式碼結構和良好的可維護性。

---

## 專案結構

```
servers/python/RAG-memory-mcp/
├── mcp_server.py                          # 主入口點（極簡啟動器）
├── app.py                                 # Application Factory（應用程式工廠）
│
├── controllers/                           # Controller 層（MCP Tools/Resources 入口）
│   ├── __init__.py
│   ├── knowledge_controller.py            # 知識搜尋與學習
│   ├── document_controller.py             # 文件儲存
│   ├── indexing_controller.py             # 批次索引（context chunking）
│   └── resource_controller.py             # MCP Resources
│
├── services/                              # Service 層（業務邏輯）
│   ├── __init__.py
│   ├── vector_store_service.py            # 向量儲存與檢索
│   └── context_chunking_service.py        # 智能分塊與批次索引
│
├── models/                                # Model 層（資料模型）
│   ├── __init__.py
│   └── knowledge_models.py                # Pydantic 資料模型
│
├── requirements.txt                       # Python 依賴
├── README.md                              # 專案說明
├── ARCHITECTURE.md                        # 本文件（架構說明）
├── Dockerfile                             # Docker 建置檔
├── docker-compose.yml                     # Docker Compose 配置
└── chroma_db/                             # ChromaDB 資料庫（自動建立）
```

### 檔案職責說明

| 檔案 | 職責 | 行數 |
|------|------|------|
| `mcp_server.py` | 主入口點，只負責啟動伺服器 | ~20 行 |
| `app.py` | Application Factory，負責建立和配置應用程式 | ~100 行 |
| `controllers/*` | MCP Tool/Resource 註冊 | 每個 ~50 行 |
| `services/*` | 核心業務邏輯實作 | 每個 ~200-300 行 |
| `models/*` | Pydantic 資料模型定義 | ~100 行 |

---

## 架構設計原則

### 1. 分層架構

```
┌─────────────────────────────────────────┐
│         MCP Client (Claude CLI)         │
└───────────────┬─────────────────────────┘
                │ MCP Protocol (HTTP/SSE)
┌───────────────▼─────────────────────────┐
│         Controller 層                    │
│  - 處理 MCP Tool/Resource 請求           │
│  - 參數驗證與回應格式化                   │
│  - 委派業務邏輯給 Service 層              │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│         Service 層                       │
│  - 實作核心業務邏輯                       │
│  - 向量儲存與檢索                         │
│  - 智能分塊與批次索引                      │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│         資料層                            │
│  - ChromaDB (向量資料庫)                  │
│  - SentenceTransformer (Embedding 模型)  │
└─────────────────────────────────────────┘
```

### 2. 職責分離

| 層級 | 職責 | 不應包含 |
|------|------|---------|
| **Controller** | MCP Tool 註冊、請求處理、回應格式化 | 業務邏輯、資料存取 |
| **Service** | 核心業務邏輯、演算法實作 | MCP 相關程式碼、直接資料庫操作 |
| **Model** | 資料結構定義、資料驗證 | 業務邏輯、資料存取 |

---

## 各層詳細說明

### Controller 層

**職責：** 作為 MCP Tools 和 Resources 的入口點，處理請求並委派給 Service 層。

#### knowledge_controller.py
```python
# 提供的 MCP Tools：
- search_knowledge(query, top_k, topic) -> SearchResult
- learn_knowledge(topic, content) -> str
```

#### document_controller.py
```python
# 提供的 MCP Tools：
- store_document(file_path, topic) -> str
```

#### indexing_controller.py
```python
# 提供的 MCP Tools：
- batch_index_folder(source_dir, chunk_size, chunk_overlap, file_extensions) -> IndexingStats
```

#### resource_controller.py
```python
# 提供的 MCP Resources：
- knowledge://{topic} -> RetrievalResult
```

**設計模式：** 使用 **註冊模式**，每個 controller 提供 `register_*_tools()` 函數供 `mcp_server.py` 調用。

---

### Service 層

**職責：** 實作核心業務邏輯，獨立於 MCP 協定。

#### vector_store_service.py (VectorStoreService)

**核心功能：**
- 向量儲存與檢索
- Embedding 生成
- 智能 Chunking 策略

**主要方法：**
```python
class VectorStoreService:
    def __init__(db_path, collection_name, embedding_model)
    def add_knowledge(topic, content) -> str
    def search_knowledge(query, top_k, topic) -> List[Dict]
    def get_all_by_topic(topic) -> List[Dict]
    def add_knowledge_with_chunking(content, metadata, chunk_size, chunk_overlap) -> List[str]

    # Private methods for chunking strategies
    def _chunk_markdown(content, metadata, max_size) -> List[str]
    def _chunk_code(content, metadata, max_size) -> List[str]
    def _chunk_recursive(content, metadata, chunk_size, chunk_overlap) -> List[str]
```

**Chunking 策略：**
| 檔案類型 | 大小 | 策略 |
|---------|------|------|
| 所有類型 | < 5KB | 完整保留 |
| Markdown | > 5KB | 按 H2 標題切分 |
| 程式碼 | > 5KB | 按語義區塊 |
| 其他 | > 5KB | 遞迴智能分割 |

#### context_chunking_service.py (ContextChunkingService)

**核心功能：**
- 資料夾掃描
- 批次檔案處理
- Metadata 提取

**主要方法：**
```python
class ContextChunkingService:
    def __init__(vector_store: VectorStoreService)
    def scan_directory(source_dir, file_extensions) -> List[Path]
    def extract_metadata(file_path, source_dir) -> Dict
    def process_file(file_path, source_dir, chunk_size, chunk_overlap) -> Dict
    def index_folder(source_dir, chunk_size, chunk_overlap, file_extensions) -> Dict
```

---

### Model 層

**職責：** 定義資料結構，使用 Pydantic 進行資料驗證。

#### knowledge_models.py

**Response Models:**
```python
class KnowledgePoint(BaseModel):
    id: str
    content: str
    topic: str
    similarity: Optional[float]
    timestamp: str
    # Extended metadata
    file_path: Optional[str]
    section_title: Optional[str]
    chunk_type: Optional[str]

class SearchResult(BaseModel):
    results: List[KnowledgePoint]

class RetrievalResult(BaseModel):
    knowledge_points: List[KnowledgePoint]
```

**Request Models:**
```python
class IndexFolderRequest(BaseModel):
    source_dir: str
    chunk_size: int = 4000
    chunk_overlap: int = 200
    file_extensions: Optional[List[str]]

class IndexingStats(BaseModel):
    total_files: int
    processed_files: int
    failed_files: int
    total_chunks: int
    duration_seconds: float
    file_details: Optional[List[Dict[str, Any]]]
```

---

## MCP Tools 對應關係

| MCP Tool | Controller | Service | 說明 |
|----------|------------|---------|------|
| `search_knowledge` | knowledge_controller | VectorStoreService.search_knowledge | 語義搜尋 |
| `learn_knowledge` | knowledge_controller | VectorStoreService.add_knowledge | 新增知識點 |
| `store_document` | document_controller | VectorStoreService.add_knowledge | 儲存文件 |
| `batch_index_folder` | indexing_controller | ContextChunkingService.index_folder | 批次索引（新增） |

---

## 資料流程範例

### 範例 1：語義搜尋

```
Claude CLI
    ↓ search_knowledge("如何實作 Aggregate?")
knowledge_controller.search_knowledge()
    ↓ 委派給
VectorStoreService.search_knowledge()
    ↓ 生成 embedding
SentenceTransformer.encode()
    ↓ 向量相似度搜尋
ChromaDB.query()
    ↓ 回傳結果
SearchResult (Pydantic Model)
    ↓ 返回給
Claude CLI
```

### 範例 2：批次索引

```
Claude CLI
    ↓ batch_index_folder(source_dir=".ai")
indexing_controller.batch_index_folder()
    ↓ 委派給
ContextChunkingService.index_folder()
    ↓ 掃描目錄
scan_directory() -> List[Path]
    ↓ 處理每個檔案
process_file()
    ↓ 提取 metadata
extract_metadata()
    ↓ 智能分塊並儲存
VectorStoreService.add_knowledge_with_chunking()
    ↓ 回傳統計
IndexingStats (Pydantic Model)
    ↓ 返回給
Claude CLI
```

---

## 技術棧

### 核心框架
- **FastMCP**: MCP 協定實作（Anthropic 官方 SDK）
- **Pydantic**: 資料驗證與模型定義
- **Python 3.11+**: 程式語言

### 資料層
- **ChromaDB**: 向量資料庫（內嵌式）
- **SentenceTransformer**: Embedding 模型
  - 預設：`paraphrase-multilingual-MiniLM-L12-v2`（支援中英文）
  - 替代：`all-MiniLM-L6-v2`（純英文，更快）

### 傳輸協定
- **HTTP/SSE**: Server-Sent Events（FastMCP 內建）
- **Port**: 3031

---

## 優點

### 1. 可維護性
- 清晰的分層結構
- 單一職責原則
- 易於定位和修復問題

### 2. 可測試性
- Service 層可獨立測試（不依賴 MCP）
- Controller 層可 mock Service 進行測試
- Model 層自動驗證資料格式

### 3. 可擴展性
- 新增 MCP Tool：只需新增 Controller
- 新增業務邏輯：只需擴展 Service
- 新增資料模型：只需擴展 Model

### 4. 重用性
- Service 層可被其他介面重用（CLI、Web API 等）
- Chunking 邏輯獨立，可用於其他場景

---

## 與舊架構的對比

### 舊架構（單檔案）
```python
# mcp_server.py (舊)
# 包含所有內容：
- FastMCP 初始化
- VectorStore 類別
- 所有 @server.tool() 定義
- 所有業務邏輯
# 問題：單檔案過大、職責不清、難以測試
```

### 新架構（分層）
```
mcp_server.py           # 只負責啟動和註冊
controllers/            # MCP 入口點
services/               # 業務邏輯
models/                 # 資料模型
# 優勢：職責分離、易於維護、可測試
```

---

## 開發指南

### 新增 MCP Tool

1. **在 Controller 層新增函數**
   ```python
   # controllers/my_controller.py
   def register_my_tools(server, my_service):
       @server.tool()
       def my_new_tool(param1: str) -> str:
           return my_service.do_something(param1)
   ```

2. **在 Service 層實作邏輯**
   ```python
   # services/my_service.py
   class MyService:
       def do_something(self, param1: str) -> str:
           # 實作業務邏輯
           pass
   ```

3. **在 mcp_server.py 註冊**
   ```python
   # mcp_server.py
   from controllers.my_controller import register_my_tools
   from services.my_service import MyService

   my_service = MyService()
   register_my_tools(server, my_service)
   ```

### 新增 Pydantic Model

```python
# models/knowledge_models.py
class MyNewModel(BaseModel):
    field1: str
    field2: int = Field(..., description="Description")
```

---

## 相關文件

- [README.md](./README.md) - 專案總覽與快速開始
- [INDEXING_GUIDE.md](./INDEXING_GUIDE.md) - 批次索引使用指南
- [Spec.md](../../../Spec.md) - 專案規格說明

---

**版本：** 2.0 (重構後)
**最後更新：** 2025-01-19
