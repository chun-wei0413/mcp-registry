# Services - 業務邏輯服務層

本目錄包含所有核心業務邏輯服務的實現，提供向量儲存、檔案索引、嵌入生成等功能。每個服務都採用單一職責原則，便於維護和擴展。

## 📂 檔案概述

### `vector_store_service.py`
負責向量資料庫管理、語義搜尋和知識儲存操作。

### `context_chunking_service.py`
負責批量檔案索引、分塊和智能文件掃描。

---

## 🏗️ 服務架構

```
┌──────────────────────────────────────┐
│   Controllers Layer                  │
│  (MCP Tools 入口)                    │
└────────────────┬─────────────────────┘
                 │
┌────────────────▼──────────────────────────────────────┐
│   Services Layer (本目錄)             │
├─────────────────────────────────────────────────────┤
│ • VectorStoreService    (向量儲存和搜尋)   │
│ • ContextChunkingService (批量索引)      │
└────────────────┬──────────────────────────────────────┘
                 │
┌────────────────▼──────────────────────────────────────┐
│   Storage & External Layer           │
├─────────────────────────────────────────────────────┤
│ • ChromaDB          (向量資料庫)     │
│ • SentenceTransformer (文本嵌入模型)  │
│ • MarkdownParser    (文檔解析)        │
└──────────────────────────────────────────────────────┘
```

---

## 📚 VectorStoreService

處理所有向量儲存、嵌入生成和語義搜尋相關操作的核心服務。

### 主要職責

| 職責 | 說明 | 相關方法 |
|------|------|---------|
| **知識儲存** | 將文本轉換為向量並儲存到 ChromaDB | `add_knowledge()` |
| **語義搜尋** | 執行向量相似度搜尋，返回相關知識 | `search_knowledge()` |
| **檔案儲存** | 解析並儲存 Markdown/JSON/TXT 檔案 | `store_document()` |
| **主題檢索** | 按主題檢索所有知識點 | `get_all_by_topic()` |
| **嵌入生成** | 使用 SentenceTransformer 生成文本嵌入 | 內部使用 |
| **程式碼分離** | v2.0 特性：分離程式碼和文字內容 | `_extract_code_blocks()` |

### 初始化

```python
from services.vector_store_service import VectorStoreService

# 使用預設配置
vector_store = VectorStoreService(
    db_path="./chroma_db",                              # ChromaDB 儲存路徑
    collection_name="ai_documentation",                 # 集合名稱
    embedding_model="paraphrase-multilingual-MiniLM-L12-v2"  # 嵌入模型
)
```

**參數說明：**

| 參數 | 類型 | 預設值 | 說明 |
|------|------|--------|------|
| `db_path` | `str` | `"./chroma_db"` | ChromaDB 數據庫儲存目錄 |
| `collection_name` | `str` | `"mcp_knowledge_base"` | 使用的集合名稱（支援多個集合） |
| `embedding_model` | `str` | `"paraphrase-multilingual-MiniLM-L12-v2"` | 嵌入模型（支援多語言） |

### 核心方法

#### 1. `add_knowledge(topic: str, content: str) -> str`

**功能：** 添加新知識點到向量資料庫。

**參數：**
- `topic` (str)：知識點主題分類
- `content` (str)：知識點文本內容

**返回值：** UUID 字符串，作為知識點的唯一識別符

**實現流程：**
1. 生成 UUID 作為文檔 ID
2. 使用 SentenceTransformer 計算 embedding
3. 儲存到 ChromaDB 的當前集合

**使用範例：**
```python
doc_id = vector_store.add_knowledge(
    topic="DDD",
    content="Aggregate 是領域驅動設計中的核心概念..."
)
print(f"Stored with ID: {doc_id}")
```

---

#### 2. `search_knowledge(query: str, top_k: int = 20, topic: Optional[str] = None) -> List[KnowledgePoint]`

**功能：** 執行語義搜尋，返回最相關的知識點。

**參數：**
- `query` (str)：自然語言搜尋查詢
- `top_k` (int, 預設=20)：返回結果數量
- `topic` (str, 可選)：按主題篩選

**返回值：** `KnowledgePoint` 物件列表（按相似度排序）

**搜尋流程：**
1. 計算查詢的 embedding 向量
2. 在 ChromaDB 中執行 cosine similarity 搜尋
3. 反序列化 code_blocks 元數據
4. 返回 KnowledgePoint 物件（包含相似度分數）

**使用範例：**
```python
# 全域搜尋
results = vector_store.search_knowledge(
    query="如何實現 DDD 中的 Aggregate",
    top_k=5
)

# 按主題搜尋
results = vector_store.search_knowledge(
    query="Event Sourcing",
    topic="EventSourcing",
    top_k=3
)

# 遍歷結果
for point in results:
    print(f"相似度: {point.similarity:.2f}")
    print(f"主題: {point.topic}")
    print(f"內容: {point.content[:200]}...")
    if point.code_blocks:
        print(f"程式碼範例: {len(point.code_blocks)} 個")
```

**效能特性：**
- 搜尋延遲：< 100ms（1000 文檔內）
- 嵌入計算：~1000 tokens/sec
- 記憶體使用：模型載入 ~200MB

---

#### 3. `store_document(file_path: str, topic: Optional[str] = None) -> str`

**功能：** 讀取檔案並儲存到向量資料庫。

**參數：**
- `file_path` (str)：檔案的絕對或相對路徑
- `topic` (str, 可選)：主題分類（預設使用檔名）

**支援格式：** `.md`, `.txt`, `.json`

**返回值：** 確認訊息（字符串）

**處理流程：**
1. 檢驗檔案存在性和類型
2. 讀取檔案內容
3. 使用 MarkdownParser 提取文本和程式碼
4. 進行智能分塊（chunk）
5. 逐個 chunk 計算 embedding 並儲存

**使用範例：**
```python
# 使用檔名作為 topic
result = vector_store.store_document("./docs/ARCHITECTURE.md")

# 指定自訂 topic
result = vector_store.store_document(
    "./docs/spec.md",
    topic="Specification"
)
```

---

#### 4. `get_all_by_topic(topic: str) -> List[KnowledgePoint]`

**功能：** 檢索特定主題的所有知識點。

**參數：**
- `topic` (str)：主題名稱

**返回值：** 該主題的所有 KnowledgePoint 物件

**使用範例：**
```python
# 獲取所有 DDD 相關知識
ddd_knowledge = vector_store.get_all_by_topic("DDD")
print(f"Found {len(ddd_knowledge)} DDD knowledge points")

# 顯示主題下的所有知識
for point in ddd_knowledge:
    print(f"- {point.section_title} (ID: {point.id})")
```

---

### 內部實現細節

#### 程式碼分離 (v2.0 特性)

系統使用智能程式碼分離以提升搜尋品質：

```python
# 儲存時：
原始文本 (含程式碼)
  ↓
MarkdownParser.extract_code_blocks()
  ↓
(text_only, code_blocks)
  ↓
embedding = model.encode(text_only)  # 只對文字計算
  ↓
ChromaDB.add(
    embeddings=[embedding],
    documents=[text_only],
    metadatas={
        code_blocks: JSON.stringify(code_blocks)
    }
)
```

**效能改善：**
- 嵌入大小減少：61-68%
- 語意精準度提升：~40%
- 搜尋速度提升：更小的向量運算

---

#### ChromaDB 集合管理

支援多個集合以組織不同類型的知識：

```python
# 集合 1：AI 文檔索引
vector_store_ai = VectorStoreService(
    collection_name="ai_documentation"  # 1,116 chunks
)

# 集合 2：手動知識庫
vector_store_kb = VectorStoreService(
    collection_name="mcp_knowledge_base"  # 2+ 文檔
)
```

---

## 📋 ContextChunkingService

處理批量檔案索引和智能分塊的服務。

### 主要職責

| 職責 | 說明 | 相關方法 |
|------|------|---------|
| **目錄掃描** | 遞歸掃描資料夾尋找支援的檔案 | `scan_directory()` |
| **檔案過濾** | 按副檔名篩選檔案 | `scan_directory()` |
| **元數據提取** | 從檔案路徑提取結構化元數據 | `extract_metadata()` |
| **批量索引** | 批量處理和索引檔案 | `chunk_folder()` |
| **統計收集** | 收集索引操作的統計資訊 | `chunk_folder()` |

### 初始化

```python
from services.context_chunking_service import ContextChunkingService

chunking_service = ContextChunkingService(vector_store=vector_store)
```

### 核心方法

#### 1. `scan_directory(source_dir: str, file_extensions: Optional[Set[str]] = None) -> List[Path]`

**功能：** 遞歸掃描目錄尋找指定類型的檔案。

**參數：**
- `source_dir` (str)：要掃描的目錄路徑
- `file_extensions` (Optional[Set[str]])：要包含的檔案副檔名

**預設支援類型：**
```python
{'.md', '.txt', '.java', '.py', '.js', '.ts', '.sh', '.json', '.yaml', '.yml'}
```

**返回值：** 符合條件的檔案路徑列表

**使用範例：**
```python
# 掃描所有支援的檔案
files = chunking_service.scan_directory("./documentation")

# 只掃描 Markdown 檔案
files = chunking_service.scan_directory(
    "./docs",
    file_extensions={'.md', '.txt'}
)

print(f"Found {len(files)} files")
```

---

#### 2. `extract_metadata(file_path: Path, source_dir: Path) -> Dict[str, Any]`

**功能：** 從檔案路徑提取結構化元數據。

**參數：**
- `file_path` (Path)：檔案的完整路徑
- `source_dir` (Path)：根目錄路徑

**返回值：** 包含以下欄位的字典：
```python
{
    "file_path": str,          # 完整檔案路徑
    "relative_path": str,      # 相對於根目錄的路徑
    "file_name": str,          # 檔案名稱
    "category": str,           # 目錄分類
    "topic": str,              # 主題（來自檔名）
    "file_size": int           # 檔案大小（位元組）
}
```

**使用範例：**
```python
from pathlib import Path

metadata = chunking_service.extract_metadata(
    file_path=Path("./docs/architecture/ARCHITECTURE.md"),
    source_dir=Path("./docs")
)
# 結果：
# {
#     "file_path": "/full/path/to/ARCHITECTURE.md",
#     "relative_path": "architecture/ARCHITECTURE.md",
#     "file_name": "ARCHITECTURE.md",
#     "category": "architecture",
#     "topic": "ARCHITECTURE",
#     "file_size": 15234
# }
```

---

#### 3. `chunk_folder(source_dir: str, chunk_size: int = 4000, chunk_overlap: int = 200, file_extensions: Optional[List[str]] = None) -> IndexingStats`

**功能：** 批量索引資料夾中的所有檔案。

**參數：**
- `source_dir` (str)：要索引的資料夾路徑
- `chunk_size` (int, 預設=4000)：每個 chunk 的最大字元數
- `chunk_overlap` (int, 預設=200)：相鄰 chunk 之間的重疊字元數
- `file_extensions` (Optional[List[str]])：要處理的副檔名

**返回值：** `IndexingStats` 物件，包含：
```python
{
    "total_files": int,              # 掃描到的檔案總數
    "processed_files": int,          # 成功處理的檔案數
    "failed_files": int,             # 失敗的檔案數
    "total_chunks": int,             # 生成的 chunk 總數
    "duration_seconds": float,       # 總耗時
    "file_details": List[Dict]       # 每個檔案的詳細資訊
}
```

**索引流程：**
1. 掃描目錄尋找符合條件的檔案
2. 依次處理每個檔案
3. 進行智能分塊（混合策略）
4. 計算 embedding 並儲存
5. 收集統計資訊並返回

**使用範例：**
```python
# 基本用法
stats = chunking_service.chunk_folder("./documentation")
print(f"Processed {stats.processed_files}/{stats.total_files} files")
print(f"Generated {stats.total_chunks} chunks in {stats.duration_seconds:.2f}s")

# 自訂配置
stats = chunking_service.chunk_folder(
    source_dir="./docs",
    chunk_size=6000,
    chunk_overlap=300,
    file_extensions=[".md", ".txt"]
)

# 查看詳細資訊
for file_info in stats.file_details:
    if file_info["status"] == "success":
        print(f"✓ {file_info['file']}: {file_info['chunks']} chunks")
    else:
        print(f"✗ {file_info['file']}: {file_info.get('error', 'Unknown error')}")
```

---

## 🔄 資料流程

### 知識儲存流程

```
User 提供文本或檔案
    ↓
Controllers (知識入口)
    ↓
VectorStoreService.add_knowledge()
    或 .store_document()
    ↓
MarkdownParser.extract_code_blocks() [v2.0]
    ↓
(text_only, code_blocks)
    ↓
SentenceTransformer.encode(text_only)
    ↓
ChromaDB.add(
    embeddings=[向量],
    documents=[文本],
    metadatas={topic, code_blocks, ...}
)
    ↓
返回 Document ID 給 User
```

### 語義搜尋流程

```
User 提供搜尋查詢
    ↓
Controllers (搜尋入口)
    ↓
VectorStoreService.search_knowledge()
    ↓
SentenceTransformer.encode(query)
    ↓
ChromaDB.query(
    query_embeddings=[向量],
    n_results=top_k,
    where={topic: ...}  # 可選篩選
)
    ↓
反序列化 code_blocks 元數據
    ↓
List[KnowledgePoint] (按相似度排序)
    ↓
Controllers 包裝為 SearchResult
    ↓
返回給 Claude CLI
```

### 批量索引流程

```
User 指定資料夾
    ↓
ContextChunkingService.chunk_folder()
    ↓
scan_directory()
    ↓
[file1, file2, file3, ...]
    ↓
For each file:
  ├─ extract_metadata()
  ├─ read file content
  ├─ intelligent chunking
  ├─ VectorStoreService.store_document()
  │   ├─ MarkdownParser
  │   ├─ SentenceTransformer.encode()
  │   └─ ChromaDB.add()
  └─ track statistics
    ↓
IndexingStats {
    processed_files,
    total_chunks,
    duration_seconds,
    file_details
}
    ↓
返回統計資訊給 User
```

---

## 🎯 設計原則

### 1. 單一職責
- **VectorStoreService**：只負責向量儲存和搜尋
- **ContextChunkingService**：只負責檔案掃描和索引協調
- 其他職責委派給專門的工具（MarkdownParser、SentenceTransformer）

### 2. 依賴注入
```python
# 良好實踐：依賴作為參數傳入
chunking_service = ContextChunkingService(vector_store=vector_store)

# 而不是內部建立
# chunking_service = ContextChunkingService()
# chunking_service.vector_store = VectorStoreService()  # 不好
```

### 3. 可測試性
- 所有方法都是純函數（無副作用）
- 支援 mock VectorStoreService 進行單元測試
- 支援測試資料庫路徑

### 4. 可擴展性
- 支援自訂嵌入模型
- 支援多個 ChromaDB 集合
- 支援自訂檔案副檔名過濾

---

## 💡 常見使用場景

### 場景 1：建立新的知識庫

```python
# 初始化
vs = VectorStoreService(
    db_path="./project_kb",
    collection_name="my_project"
)

# 添加單個知識點
doc_id = vs.add_knowledge(
    topic="Architecture",
    content="系統採用微服務架構..."
)

# 或儲存整個檔案
vs.store_document("./docs/ARCHITECTURE.md")
```

### 場景 2：批量索引現有文檔

```python
chunking = ContextChunkingService(vs)

# 索引整個文檔目錄
stats = chunking.chunk_folder(
    source_dir="./project_documentation",
    chunk_size=4000,
    chunk_overlap=200
)

print(f"Successfully indexed {stats.processed_files} files")
print(f"Generated {stats.total_chunks} searchable chunks")
```

### 場景 3：實現智能搜尋

```python
# 全域搜尋
results = vs.search_knowledge(
    query="如何配置微服務通信？",
    top_k=5
)

# 打印結果
for point in results:
    print(f"[{point.similarity:.2f}] {point.section_title}")
    print(f"    {point.content[:100]}...")
    if point.code_blocks:
        for cb in point.code_blocks:
            print(f"    Code: {cb.language}")
```

### 場景 4：按主題檢索

```python
# 獲取所有架構相關文檔
architecture_docs = vs.get_all_by_topic("Architecture")

for doc in architecture_docs:
    print(f"- {doc.file_path}")
    print(f"  Section: {doc.section_title}")
```

---

## 🔧 效能最佳實踐

1. **批量操作**
   - 使用 `chunk_folder()` 而非多次 `add_knowledge()`
   - 減少 API 呼叫次數

2. **Top-K 限制**
   - 搜尋時限制 `top_k <= 20`
   - 減少傳輸數據量和計算時間

3. **集合隔離**
   - 為不同類型知識使用不同集合
   - 提升搜尋效率

4. **模型選擇**
   - 預設模型支援 50+ 語言
   - 對於特殊場景可考慮更專門的模型

---

## 📚 相關文件

- [models/README.md](../models/README.md) - 資料模型定義
- [controllers/README.md](../controllers/README.md) - MCP 工具層
- [CODE_SEPARATION.md](../docs/CODE_SEPARATION.md) - v2.0 技術細節
- [MCP_SERVER_CONFIG.md](../MCP_SERVER_CONFIG.md) - 伺服器配置

---

**最後更新：** 2025-11-24
**版本：** v2.0
**維護者：** RAG Memory MCP Team

