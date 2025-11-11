# Memory MCP Server v3.0 - 架構文件

本文件提供 Memory MCP Server 系統架構、組件和所使用工具的技術概覽。

## 1. 系統架構

該系統是一個基於 **MCP (Model Context Protocol)** 標準的知識庫伺服器，專為 AI 客戶端（如 Claude CLI）設計。MCP 是由 Anthropic 定義的標準協定，允許 AI 模型透過工具（Tools）和資源（Resources）與外部系統互動，實現上下文增強和知識檢索。

此架構可分為三個主要層次：

1.  **MCP 協定層 (FastMCP):** 使用 FastMCP SDK 實現 MCP 標準協定，提供 Tools 和 Resources 供 AI 客戶端呼叫。這是 AI 客戶端的唯一入口點。
2.  **嵌入層 (SentenceTransformer):** 一個機器學習模型，負責將基於文字的知識和查詢轉換為數值向量表示（embeddings）。
3.  **儲存與查詢層 (ChromaDB):** 一個專門的向量資料庫，用於儲存嵌入向量及其相關的元數據。它為高效的相似性搜尋和基於元數據的過濾提供了核心功能。

## 2. 核心技術與 MCP 實作工具

- **MCP 協定實作:**
  - **FastMCP:** Anthropic 官方的 Python MCP SDK，用於實現 Model Context Protocol 標準。它提供了簡潔的 decorator 語法來定義 Tools 和 Resources，並處理與 AI 客戶端的通訊。
  - **Pydantic:** 用於資料驗證和結構化輸出，確保 MCP 工具回應格式的正確性。

- **向量資料庫:**
  - **ChromaDB:** 一個開源的嵌入資料庫。在開發/生產中作為持久化的磁碟資料庫（`./chroma_db`），儲存所有知識點向量及其元數據。使用 Cosine Similarity 進行向量相似度計算。

- **嵌入模型:**
  - **SentenceTransformer (`all-MiniLM-L6-v2`):** 一個用於生成句子和文字嵌入的 Python 函式庫。此模型（384 維度，80MB）用於將儲存的知識和傳入的搜尋查詢轉換為向量，以進行語意比較。模型在首次啟動時自動下載。

- **測試框架:**
  - **Pytest:** 用於編寫和執行測試的主要框架。
  - **MCP Test Client:** 用於測試 MCP Tools 和 Resources 的功能性。

- **容器化:**
  - **Docker & Docker Compose:** 該應用程式已完全容器化。`Dockerfile` 定義了映像檔，而 `docker-compose.yml` 檔案則用於編排服務，支援持久化儲存和文件掛載，使其易於建置、執行和部署。

## 3. MCP Tools 和 Resources

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
  - **回應：** `SearchResult` 物件，包含結果列表（每個結果包含 id、content、topic、similarity、timestamp）。

### MCP Resources

#### `knowledge://{topic}`
  - **目的：** 獲取特定主題的所有知識點。
  - **URI 參數：** `topic` (str): 要檢索的主題名稱
  - **流程：** 從 ChromaDB 返回所有元數據與給定 `topic` 相符的文件。
  - **回應：** `RetrievalResult` 物件，包含該主題的所有知識點列表。

## 4. 資料模型

### KnowledgePoint
```python
class KnowledgePoint(pydantic.BaseModel):
    id: str                          # 唯一識別碼
    content: str                     # 知識點內容
    topic: str                       # 主題分類
    similarity: Optional[float]      # 相似度分數（僅在搜尋時）
    timestamp: str                   # ISO 8601 格式時間戳
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