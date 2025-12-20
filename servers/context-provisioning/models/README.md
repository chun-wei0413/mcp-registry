# Knowledge Models - 資料模型說明

本目錄包含 Context Provisioning Server 的所有資料模型定義，使用 Pydantic 進行資料驗證和序列化。

## 📂 檔案概述

### `knowledge_models.py`
定義知識管理系統的核心資料模型，包含：
- **回應模型**：返回給客戶端的資料結構
- **請求模型**：接收客戶端請求的資料結構

---

## 📊 資料模型架構

```
Response Models (回應模型)
├── CodeBlock          # 程式碼區塊 [v2.0 新增]
├── KnowledgePoint     # 知識點 [v2.0 擴展]
├── SearchResult       # 搜尋結果
└── RetrievalResult    # 檢索結果

Request Models (請求模型)
├── IndexFolderRequest # 批次索引請求
└── IndexingStats      # 索引統計資訊
```

---

## 🔍 回應模型（Response Models）

### 1. CodeBlock（程式碼區塊）

**目的：** 儲存從 Markdown 文件中提取的程式碼區塊資訊。

**使用場景：** v2.0 智能程式碼分離功能，將程式碼與描述文字分開儲存。

```python
class CodeBlock(BaseModel):
    language: str     # 程式語言（如 'java', 'python', 'typescript'）
    code: str         # 完整的程式碼內容
    position: int     # 在原始文件中的位置索引（從 0 開始）
```

**欄位說明：**

| 欄位 | 類型 | 必填 | 說明 | 範例 |
|------|------|------|------|------|
| `language` | `str` | ✅ | 程式語言識別符 | `"java"`, `"python"`, `"bash"` |
| `code` | `str` | ✅ | 完整的程式碼內容（多行字串） | `"public class Order {...}"` |
| `position` | `int` | ✅ | 在文件中的順序位置 | `0`, `1`, `2` |

**範例：**
```python
code_block = CodeBlock(
    language="java",
    code="""public class Product {
    private ProductId id;
    public Product(ProductId id) {
        this.id = id;
    }
}""",
    position=0
)
```

**在系統中的使用：**
1. `MarkdownParser.extract_code_blocks()` 提取程式碼時建立
2. 儲存在 ChromaDB 的 metadata 中（序列化為 JSON）
3. 查詢時反序列化並附加到 `KnowledgePoint.code_blocks`

---

### 2. KnowledgePoint（知識點）

**目的：** 表示單一知識點，包含文字內容、元數據和關聯的程式碼區塊。

**使用場景：** 作為搜尋結果的基本單位，返回給 Claude CLI 使用。

```python
class KnowledgePoint(BaseModel):
    # 核心欄位
    id: str                              # 唯一識別碼（UUID）
    content: str                         # 知識點的文字內容
    topic: str                           # 主題分類
    timestamp: str                       # 建立時間（ISO 8601 格式）

    # 搜尋相關
    similarity: Optional[float] = None   # 語意相似度分數（0-1）

    # 擴展元數據（v1.0）
    file_path: Optional[str] = None      # 來源檔案路徑
    section_title: Optional[str] = None  # 所屬章節標題
    chunk_type: Optional[str] = None     # 切塊類型

    # v2.0 新增：程式碼區塊
    code_blocks: Optional[List[CodeBlock]] = None
```

**欄位詳細說明：**

#### 核心欄位

| 欄位 | 類型 | 必填 | 說明 | 範例 |
|------|------|------|------|------|
| `id` | `str` | ✅ | UUID v4 格式的唯一識別碼 | `"550e8400-e29b-41d4-a716-446655440000"` |
| `content` | `str` | ✅ | 知識點的文字內容（不含程式碼） | `"## UseCase 原則\n\nUseCase 必須遵循單一職責..."` |
| `topic` | `str` | ✅ | 主題分類標籤 | `"DDD"`, `"EventSourcing"`, `"CleanArchitecture"` |
| `timestamp` | `str` | ✅ | ISO 8601 格式的時間戳 | `"2025-11-23T10:30:00Z"` |

#### 搜尋欄位

| 欄位 | 類型 | 必填 | 說明 | 範例 |
|------|------|------|------|------|
| `similarity` | `Optional[float]` | ❌ | 語意相似度（Cosine Similarity） | `0.92`（僅在搜尋結果中存在） |

#### 元數據欄位（v1.0）

| 欄位 | 類型 | 必填 | 說明 | 範例 |
|------|------|------|------|------|
| `file_path` | `Optional[str]` | ❌ | 來源檔案的相對路徑 | `"./docs/DDD_Aggregate.md"` |
| `section_title` | `Optional[str]` | ❌ | Markdown 章節標題 | `"Aggregate 實作原則"` |
| `chunk_type` | `Optional[str]` | ❌ | 切塊類型標記 | `"section"`, `"section_part"`, `"complete"` |

#### 程式碼欄位（v2.0）

| 欄位 | 類型 | 必填 | 說明 | 範例 |
|------|------|------|------|------|
| `code_blocks` | `Optional[List[CodeBlock]]` | ❌ | 關聯的程式碼區塊清單 | `[CodeBlock(...), CodeBlock(...)]` |

**完整範例：**
```python
knowledge_point = KnowledgePoint(
    id="abc-123-def",
    content="## Event Sourcing 建構子規則\n\n業務建構子不可直接設定狀態。\n\n[CODE_BLOCK_0]",
    topic="EventSourcing",
    timestamp="2025-11-23T10:30:00Z",
    similarity=0.92,
    file_path="./docs/EventSourcing.md",
    section_title="建構子規則",
    chunk_type="section",
    code_blocks=[
        CodeBlock(
            language="java",
            code="public Product(...) { apply(new ProductCreated(...)); }",
            position=0
        )
    ]
)
```

**資料流程：**
```
1. 文件儲存階段：
   Markdown 文件 → MarkdownParser → (text_only, code_blocks)
                                    ↓
   VectorStoreService → ChromaDB: {
       embeddings: [0.12, 0.45, ...],  # 只用 text_only 計算
       documents: "...",
       metadatas: {
           topic: "DDD",
           code_blocks: "[{...}]"       # JSON 字串
       }
   }

2. 查詢階段：
   search_knowledge(query="...") → ChromaDB 搜尋
                                    ↓
   VectorStoreService → 反序列化 metadata → KnowledgePoint(
       content="...",
       code_blocks=[CodeBlock(...)]    # 反序列化後的物件
   )
```

---

### 3. SearchResult（搜尋結果）

**目的：** 包裝多個知識點的搜尋結果。

**使用場景：** `search_knowledge` MCP Tool 的回傳格式。

```python
class SearchResult(BaseModel):
    results: List[KnowledgePoint]  # 知識點列表（按相似度排序）
```

**範例：**
```python
search_result = SearchResult(
    results=[
        KnowledgePoint(
            id="abc-123",
            content="...",
            similarity=0.92,
            code_blocks=[...]
        ),
        KnowledgePoint(
            id="def-456",
            content="...",
            similarity=0.87,
            code_blocks=[...]
        )
    ]
)
```

**使用流程：**
```python
# 在 knowledge_controller.py 中
@server.tool()
def search_knowledge(query: str, top_k: int = 50, topic: Optional[str] = None) -> SearchResult:
    search_results = vector_store.search_knowledge(query, top_k, topic)
    return SearchResult(results=search_results)  # 自動驗證資料格式
```

---

### 4. RetrievalResult（檢索結果）

**目的：** 包裝按主題檢索的所有知識點。

**使用場景：** `knowledge://{topic}` MCP Resource 的回傳格式。

```python
class RetrievalResult(BaseModel):
    knowledge_points: List[KnowledgePoint]  # 特定主題下的所有知識點
```

**範例：**
```python
retrieval_result = RetrievalResult(
    knowledge_points=[
        KnowledgePoint(id="1", topic="DDD", ...),
        KnowledgePoint(id="2", topic="DDD", ...),
        KnowledgePoint(id="3", topic="DDD", ...)
    ]
)
```

**與 SearchResult 的差異：**

| 特性 | SearchResult | RetrievalResult |
|------|--------------|-----------------|
| **用途** | 語意搜尋結果 | 主題檢索結果 |
| **排序** | 按相似度排序 | 無特定排序 |
| **`similarity`** | 有值 | 無值（None） |
| **數量** | 限制 `top_k` | 返回該主題所有資料 |

---

## 📥 請求模型（Request Models）

### 5. IndexFolderRequest（批次索引請求）

**目的：** 定義批次索引資料夾的請求參數。

**使用場景：** `batch_index_folder` MCP Tool 的輸入參數。

```python
class IndexFolderRequest(BaseModel):
    source_dir: str                        # 要索引的資料夾路徑
    chunk_size: int = 4000                 # 最大切塊大小（字元數）
    chunk_overlap: int = 200               # 切塊重疊字元數
    file_extensions: Optional[List[str]] = None  # 要處理的檔案副檔名
```

**欄位說明：**

| 欄位 | 類型 | 預設值 | 說明 | 範例 |
|------|------|--------|------|------|
| `source_dir` | `str` | 必填 | 要索引的資料夾路徑（絕對或相對） | `"./documentation"`, `"/app/docs"` |
| `chunk_size` | `int` | `4000` | 每個 chunk 的最大字元數 | `4000`, `6000` |
| `chunk_overlap` | `int` | `200` | 相鄰 chunk 之間的重疊字元數 | `200`, `300` |
| `file_extensions` | `Optional[List[str]]` | `None` | 要處理的檔案副檔名列表 | `[".md", ".txt"]` |

**預設支援的檔案類型：**
```python
默認值（當 file_extensions=None 時）：
['.md', '.txt', '.java', '.py', '.js', '.ts', '.sh', '.json', '.yaml', '.yml']
```

**範例：**
```python
# 範例 1：使用預設設定
request = IndexFolderRequest(
    source_dir="./documentation"
)

# 範例 2：自訂設定
request = IndexFolderRequest(
    source_dir="/app/docs",
    chunk_size=6000,
    chunk_overlap=300,
    file_extensions=[".md", ".txt"]  # 只處理 Markdown 和純文字
)
```

---

### 6. IndexingStats（索引統計資訊）

**目的：** 回傳批次索引操作的統計資訊。

**使用場景：** `batch_index_folder` 執行完成後的回傳結果。

```python
class IndexingStats(BaseModel):
    total_files: int                            # 找到的檔案總數
    processed_files: int                        # 成功處理的檔案數
    failed_files: int                           # 處理失敗的檔案數
    total_chunks: int                           # 生成的 chunk 總數
    skipped_files: int                          # 跳過的檔案數
    duration_seconds: float                     # 總耗時（秒）
    file_details: Optional[List[Dict[str, Any]]] = None  # 詳細檔案資訊
```

**欄位說明：**

| 欄位 | 類型 | 說明 | 範例 |
|------|------|------|------|
| `total_files` | `int` | 掃描到的檔案總數 | `165` |
| `processed_files` | `int` | 成功處理並索引的檔案數 | `160` |
| `failed_files` | `int` | 處理失敗的檔案數 | `5` |
| `total_chunks` | `int` | 生成的 chunk 總數 | `339` |
| `skipped_files` | `int` | 跳過的檔案數（如已存在） | `0` |
| `duration_seconds` | `float` | 總處理時間（秒） | `45.6` |
| `file_details` | `Optional[List[Dict]]` | 每個檔案的詳細資訊（可選） | `[{file: "...", chunks: 2}]` |

**範例：**
```python
stats = IndexingStats(
    total_files=165,
    processed_files=160,
    failed_files=5,
    total_chunks=339,
    skipped_files=0,
    duration_seconds=45.6,
    file_details=[
        {"file": "ARCHITECTURE.md", "chunks": 3, "status": "success"},
        {"file": "CLAUDE.md", "chunks": 5, "status": "success"},
        {"file": "corrupted.md", "chunks": 0, "status": "failed", "error": "decode error"}
    ]
)
```

**使用範例：**
```python
# 在 MCP Tool 中使用
@server.tool()
def batch_index_folder(source_dir: str, ...) -> IndexingStats:
    start_time = time.time()

    # 處理檔案...

    stats = IndexingStats(
        total_files=len(all_files),
        processed_files=success_count,
        failed_files=fail_count,
        total_chunks=total_chunks,
        skipped_files=skip_count,
        duration_seconds=time.time() - start_time
    )

    return stats  # Pydantic 自動序列化為 JSON
```

---

## 🔄 資料流程圖

### 儲存流程
```
User Input (Markdown 文件)
    ↓
MarkdownParser.extract_code_blocks()
    ↓
(text_only, code_blocks)
    ↓
VectorStoreService._chunk_markdown()
    ↓
{
    description: str,         # 用於 embedding
    code_blocks: List[Dict]   # 序列化到 metadata
}
    ↓
ChromaDB.add()
    ↓
metadata: {
    topic: str,
    code_blocks: str (JSON)   # CodeBlock 序列化
}
```

### 查詢流程
```
User Query (自然語言)
    ↓
VectorStoreService.search_knowledge()
    ↓
ChromaDB.query() → results
    ↓
反序列化 metadata["code_blocks"]
    ↓
List[Dict] → List[CodeBlock]
    ↓
KnowledgePoint(
    content="...",
    code_blocks=[CodeBlock(...)]
)
    ↓
SearchResult(results=[KnowledgePoint(...)])
    ↓
Return to Claude CLI
```

---

## 📋 使用範例

### 範例 1：建立知識點（含程式碼）

```python
from models.knowledge_models import KnowledgePoint, CodeBlock

# 建立程式碼區塊
code = CodeBlock(
    language="java",
    code="""public class Order {
    public void addItem(OrderItem item) {
        this.items.add(item);
        apply(new ItemAdded(this.id, item));
    }
}""",
    position=0
)

# 建立知識點
knowledge = KnowledgePoint(
    id="550e8400-e29b-41d4-a716-446655440000",
    content="## Aggregate 實作原則\n\n必須透過聚合根修改內部狀態。\n\n[CODE_BLOCK_0]",
    topic="DDD",
    timestamp="2025-11-23T10:30:00Z",
    similarity=0.92,
    code_blocks=[code]
)

# Pydantic 自動驗證資料格式
print(knowledge.model_dump_json(indent=2))
```

### 範例 2：批次索引

```python
from models.knowledge_models import IndexFolderRequest, IndexingStats

# 建立請求
request = IndexFolderRequest(
    source_dir="./documentation",
    chunk_size=4000,
    chunk_overlap=200,
    file_extensions=[".md", ".txt"]
)

# 執行索引（假設）
stats = batch_index_folder(**request.model_dump())

# 查看結果
print(f"處理了 {stats.processed_files}/{stats.total_files} 個檔案")
print(f"生成了 {stats.total_chunks} 個 chunks")
print(f"耗時：{stats.duration_seconds:.2f} 秒")
```

### 範例 3：搜尋結果處理

```python
from models.knowledge_models import SearchResult

# 假設從 MCP Tool 取得搜尋結果
results: SearchResult = search_knowledge(query="Aggregate 實作原則", top_k=3)

# 遍歷結果
for point in results.results:
    print(f"相似度：{point.similarity:.2f}")
    print(f"主題：{point.topic}")
    print(f"內容：{point.content[:100]}...")

    # 顯示程式碼範例
    if point.code_blocks:
        print(f"包含 {len(point.code_blocks)} 個程式碼範例：")
        for code in point.code_blocks:
            print(f"  - {code.language}: {len(code.code)} 字元")
```

---

## 🎯 設計原則

### 1. 型別安全
- 使用 Pydantic 進行運行時型別驗證
- 所有欄位都有明確的型別定義
- 使用 `Optional` 標記可選欄位

### 2. 向下相容
- v2.0 新增欄位都是 `Optional`
- 舊資料可以正常讀取（`code_blocks` 為 `None`）
- 新資料自動支援新功能

### 3. 文件化
- 每個模型都有 docstring
- 欄位使用 `Field(..., description="...")` 說明用途
- 提供使用範例

### 4. 可序列化
- 所有模型都可序列化為 JSON
- 支援 `model_dump()` 和 `model_dump_json()`
- 適合 MCP Protocol 傳輸

---

## 🔧 維護指南

### 新增欄位
1. 使用 `Optional` 標記新欄位（保持向下相容）
2. 提供 `Field(description="...")` 說明
3. 更新本 README 的欄位說明表格
4. 新增使用範例

### 新增模型
1. 決定是回應模型或請求模型
2. 繼承 `BaseModel`
3. 新增 docstring
4. 更新本 README 的架構圖
5. 提供完整範例

### 版本管理
- 主要變更：更新 CHANGELOG.md
- 欄位新增：標註版本號（如 `# v2.0 新增`）
- 向下相容性：確保舊資料可讀取

---

## 📚 相關文件

- [CODE_SEPARATION.md](../docs/CODE_SEPARATION.md) - 智能程式碼分離技術文件
- [CHANGELOG.md](../CHANGELOG.md) - 版本變更記錄
- [README.md](../README.md) - 專案概述

---

**最後更新：** 2025-11-23
**版本：** v2.0.0
**維護者：** MCP Registry Team
