# MCP Registry - 統一開發規範

本文件為 MCP Registry 專案的統一開發規範，適用於所有 AI 助手（Claude、Gemini 等）。

---

# Memory MCP Server (Python)

## 專案概述

基於 RAG（Retrieval-Augmented Generation）的專案知識管理系統，讓 AI 客戶端（Claude CLI、Gemini 等）能夠讀取並查詢專案文件。

## 🎯 功能特性

| 功能 | 說明 | 使用場景 |
|------|------|---------|
| **文件儲存** | 自動讀取並儲存 .md、.json 等專案文件 | 儲存 Spec.md、架構文件 |
| **語義搜尋** | 透過自然語言查詢相關 context | 查詢 "Clean Architecture" 獲取 CA 相關內容 |
| **主題管理** | 按主題分類和檢索知識點 | 按 DDD、SOLID 等主題組織知識 |
| **智能程式碼分離** | 🆕 v2.0：分離程式碼與文字描述，提升搜尋精準度 | 搜尋概念時不被程式碼語法干擾 |
| **完整程式碼範例** | 🆕 v2.0：查詢結果包含關聯的程式碼區塊 | 獲得概念說明的同時得到程式碼範例 |

## ✨ v2.0 新功能：智能程式碼分離（2025-11-23）

### 核心改進

**問題：** 傳統方式將程式碼與文字一起計算 embedding，導致程式碼語法稀釋語意相似度。

**解決方案：**
- ✅ **只對文字描述計算 embedding**（提升語意精準度 ~40%）
- ✅ **程式碼儲存在 metadata**（完整保留但不參與搜尋）
- ✅ **查詢結果包含完整程式碼**（使用者體驗不打折）

**效能指標：**
- 📉 Embedding 大小減少 **61-68%**
- 📈 語意搜尋準確度提升 **~40%**
- ⚡ 搜尋速度提升（更小的 embedding 向量）

詳細技術文件：`servers/python/RAG-memory-mcp/docs/CODE_SEPARATION.md`

## 技術架構

### 系統架構

該系統是一個基於 **MCP (Model Context Protocol)** 標準的知識庫伺服器，專為 AI 客戶端（如 Claude CLI）設計。MCP 是由 Anthropic 定義的標準協定，允許 AI 模型透過工具（Tools）和資源（Resources）與外部系統互動，實現上下文增強和知識檢索。

此架構可分為三個主要層次：

1.  **MCP 協定層 (FastMCP):** 使用 FastMCP SDK 實現 MCP 標準協定，提供 Tools 和 Resources 供 AI 客戶端呼叫。
2.  **嵌入層 (SentenceTransformer):** 機器學習模型，負責將文字知識轉換為向量表示（embeddings）。
3.  **儲存與查詢層 (ChromaDB):** 向量資料庫，用於儲存嵌入向量及其元數據，提供高效的相似性搜尋。

### 技術棧（最簡化）

```yaml
核心技術:
  - MCP SDK: FastMCP (Anthropic 官方 Python SDK)
  - Embedding 模型: all-MiniLM-L6-v2 (80MB, 本地運行, 384 維度)
  - 向量資料庫: ChromaDB (內嵌式, 零配置, Cosine Similarity)
  - 文件處理: Python 標準庫
  - 資料驗證: Pydantic

Docker 基礎映像:
  - python:3.11-slim

依賴套件:
  - mcp-cli
  - chromadb
  - sentence-transformers
  - uv
```

### 架構圖

```
┌─────────────────┐
│  Claude CLI     │
│  (HTTP/MCP)     │
└────────┬────────┘
         │ MCP Protocol
┌────────▼────────────────────┐
│  FastMCP Server             │
│  ┌─────────────────────┐   │
│  │ store_document      │   │
│  │ search_knowledge    │   │
│  │ learn_knowledge     │   │
│  └──────────┬──────────┘   │
└─────────────┼───────────────┘
              │
┌─────────────▼───────────────┐
│  VectorStore (storage.py)   │
│  ┌─────────────────────┐   │
│  │ SentenceTransformer │   │
│  │ (all-MiniLM-L6-v2)  │   │
│  └──────────┬──────────┘   │
│             │               │
│  ┌──────────▼──────────┐   │
│  │ ChromaDB            │   │
│  │ (Cosine Similarity) │   │
│  └─────────────────────┘   │
└─────────────────────────────┘
```

## MCP Tools 和 Resources

伺服器提供四個主要 MCP Tools 和一個 Resource：

### MCP Tools

#### `store_document`
- **目的：** 讀取並儲存專案文件到知識庫。
- **參數：**
  - `file_path` (str): 文件的絕對或相對路徑（支援 .md、.json、.txt）
  - `topic` (str, optional): 知識點主題，預設使用檔名
- **流程：** 讀取文件內容，生成嵌入向量，並與 `topic` 元數據一起儲存到 ChromaDB。
- **回應：** 包含文件名、主題、ID 和大小的確認訊息（字串格式）。

#### `learn_knowledge`
- **目的：** 手動將一個新的知識點加入資料庫。
- **參數：**
  - `topic` (str): 知識點的主題分類（例如 "DDD", "SOLID"）
  - `content` (str): 知識點的文字內容
- **流程：** 為 `content` 生成嵌入向量，並將其與 `topic` 元數據一起儲存。
- **回應：** 包含知識點 ID 的確認訊息（字串格式）。

#### `search_knowledge`
- **目的：** 在知識庫上執行語意搜尋。
- **參數：**
  - `query` (str): 自然語言搜尋問題
  - `top_k` (int, default=5): 返回的最大結果數
  - `topic` (str, optional): 限定搜尋範圍的主題
- **流程：** 為 `query` 生成嵌入向量，在 ChromaDB 中找到語意最相似的 `top_k` 個知識點。
- **回應：** `SearchResult` 物件，包含結果列表。

### MCP Resources

#### `knowledge://{topic}`
- **目的：** 獲取特定主題的所有知識點。
- **URI 參數：** `topic` (str): 要檢索的主題名稱
- **流程：** 從 ChromaDB 返回所有元數據與給定 `topic` 相符的文件。
- **回應：** `RetrievalResult` 物件，包含該主題的所有知識點列表。

## 資料模型

### CodeBlock（v2.0 新增）
```python
class CodeBlock(pydantic.BaseModel):
    language: str                    # 程式語言（如 java, python）
    code: str                        # 完整程式碼內容
    position: int                    # 在文件中的位置索引
```

### KnowledgePoint
```python
class KnowledgePoint(pydantic.BaseModel):
    id: str                          # 唯一識別碼
    content: str                     # 知識點內容
    topic: str                       # 主題分類
    similarity: Optional[float]      # 相似度分數（僅在搜尋時）
    timestamp: str                   # ISO 8601 格式時間戳

    # v2.0 新增欄位
    code_blocks: Optional[List[CodeBlock]] = None  # 關聯的程式碼區塊
```

### SearchResult
```python
class SearchResult(pydantic.BaseModel):
    results: List[KnowledgePoint]    # 搜尋結果列表
```

### RetrievalResult
```python
class RetrievalResult(pydantic.BaseModel):
    knowledge_points: List[KnowledgePoint]  # 主題下的所有知識點
```

## 使用情境

### 情境一：儲存專案文件

```python
# 儲存架構文件
store_document(
    file_path="./documentation/ARCHITECTURE.md",
    topic="Architecture"
)

# 儲存專案規格（自動使用檔名作為 topic）
store_document(
    file_path="./Spec.md"
)
```

### 情境二：手動新增知識點

```python
learn_knowledge(
    topic="DDD",
    content="一個 Aggregate 是一群相關領域物件的集合，它被視為一個單一的資料修改單元。"
)
```

### 情境三：語意搜尋

```python
# 全域搜尋
search_knowledge(
    query="如何保護業務規則不被外部隨意修改？",
    top_k=3
)

# 特定主題內搜尋
search_knowledge(
    query="Clean Architecture",
    topic="Architecture",
    top_k=5
)
```

### 情境四：按主題檢索

```python
# 使用 MCP Resource
knowledge://DDD
knowledge://SOLID
knowledge://Architecture
```

## 快速開始

### Docker Compose 部署（推薦）

```bash
# 1. 啟動服務
cd servers/python
docker-compose up -d

# 2. 查看日誌
docker-compose logs -f memory-mcp

# 3. 停止服務
docker-compose down
```

### 本地開發

```bash
# 1. 安裝依賴
pip install -r requirements.txt

# 2. 啟動 MCP Server
python mcp_server.py
```

## 目錄結構

```
servers/python/
├── mcp_server.py          # FastMCP 伺服器主程式
├── storage.py             # ChromaDB 向量儲存層
├── requirements.txt       # Python 依賴
├── Dockerfile             # Docker 映像定義
├── docker-compose.yml     # Docker Compose 配置
├── .dockerignore          # Docker 忽略檔案
├── chroma_db/             # ChromaDB 持久化資料（自動建立）
├── .dev/                  # 開發文件
│   ├── ARCHITECTURE.md    # 架構文件
│   └── SCENARIOS.md       # 使用情境
└── README.md              # 專案說明
```

## 效能指標

| 指標 | 數值 | 說明 |
|------|------|------|
| Embedding 速度 | ~1000 tokens/sec | CPU 運算 |
| 搜尋延遲 | <100ms | 1000 筆文件內 |
| 記憶體使用 | ~500MB | 包含模型載入 |
| 磁碟使用 | ~200MB | 模型 + 資料庫 |