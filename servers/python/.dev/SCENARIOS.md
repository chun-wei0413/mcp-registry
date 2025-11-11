# Memory MCP Server v3.0 使用情境

本文件概述了使用 Memory MCP Server 的核心情境。此伺服器基於 FastMCP SDK 實現，透過 MCP Tools 和 Resources 提供持續學習、文件儲存和強大的語意搜尋功能，專為 Claude CLI 等 AI 客戶端設計。

## 情境一：儲存專案文件 (Store Document)

當您需要將專案文件（如 Spec.md、ARCHITECTURE.md）存入知識庫時，可以使用此工具自動讀取並儲存文件內容。

**目標：** 快速將專案文件載入知識庫，供後續語意搜尋使用。

**MCP Tool：** `store_document`

**參數格式：**
```python
file_path: str              # 文件路徑（支援 .md, .json, .txt）
topic: Optional[str]        # 可選主題，預設使用檔名
```

**使用範例（Claude CLI）：**
```python
# 儲存架構文件
store_document(
    file_path="./documentation/ARCHITECTURE.md",
    topic="Architecture"
)

# 儲存專案規格（自動使用檔名 "CLAUDE" 作為 topic）
store_document(
    file_path="./CLAUDE.md"
)
```

**處理流程：**
1.  讀取指定路徑的文件內容。
2.  對於 JSON 檔案，格式化為可讀的 JSON 字串。
3.  為文件內容產生向量嵌入。
4.  將內容、向量以及包含 `topic` 的元數據存入 ChromaDB。
5.  返回確認訊息。

**回應範例：**
```
Document stored successfully:
- File: ARCHITECTURE.md
- Topic: Architecture
- ID: 550e8400-e29b-41d4-a716-446655440000
- Size: 15234 characters
```

---

## 情境二：手動新增知識點 (Learn Knowledge)

當您在開發過程中發現一個值得記錄的知識點時，可以透過此工具將其即時存入系統。

**目標：** 持續地、零碎地為知識庫增加內容。

**MCP Tool：** `learn_knowledge`

**參數格式：**
```python
topic: str        # 知識點主題（如 "DDD", "SOLID"）
content: str      # 知識點內容
```

**使用範例（Claude CLI）：**
```python
learn_knowledge(
    topic="DDD",
    content="一個 Aggregate 是一群相關領域物件的集合，它被視為一個單一的資料修改單元。"
)
```

**處理流程：**
1.  為 `content` 產生向量嵌入。
2.  將 `content`、向量以及包含 `topic` 的元數據存入 ChromaDB。
3.  返回確認訊息，包含該知識點的唯一 ID。

**回應範例：**
```
Knowledge learned with ID: 7c9e6679-7425-40de-944b-e07fc1f90ae7
```

---

## 情境三：進行語意搜尋 (Search Knowledge)

這是系統的核心功能。您可以提出一個自然語言問題，系統會找出語意上最相關的知識點。

**目標：** 根據問題的語意，從知識庫中找到最相關的解答或參考資料。

**MCP Tool：** `search_knowledge`

**參數格式：**
```python
query: str              # 自然語言搜尋問題（必填）
top_k: int              # 返回結果數量（預設 5）
topic: Optional[str]    # 可選主題過濾器
```

**使用範例 1：全域搜尋（Claude CLI）**
```python
search_knowledge(
    query="如何保護業務規則不被外部隨意修改？",
    top_k=3
)
```

**使用範例 2：在特定主題內搜尋**
```python
search_knowledge(
    query="關於模型邊界劃分的最佳實踐是什麼？",
    topic="DDD",
    top_k=3
)
```

**使用範例 3：查詢 Clean Architecture**
```python
search_knowledge(
    query="Clean Architecture",
    topic="Architecture",
    top_k=5
)
```

**處理流程：**
1.  為 `query` 產生向量嵌入。
2.  在 ChromaDB 中執行向量相似度搜尋（Cosine Similarity）。
3.  如果指定 `topic`，則過濾結果只包含該主題的知識點。
4.  返回前 `top_k` 個最相關的結果，按相似度排序。

**回應格式：**
返回一個 `SearchResult` 物件，包含最相關結果的列表。

```python
SearchResult(
    results=[
        KnowledgePoint(
            id="7c9e6679-7425-40de-944b-e07fc1f90ae7",
            content="一個 Aggregate 是一群相關領域物件的集合...",
            topic="DDD",
            similarity=0.89,  # 相似度分數（0-1，越高越相似）
            timestamp="2025-11-12T10:30:00.123456Z"
        ),
        # ... 更多結果
    ]
)
```

---

## 情境四：提取特定主題的所有知識 (Retrieve by Topic)

如果您需要某個主題的完整知識列表（例如，在開始一項新任務前，複習所有 `SOLID` 原則），可以使用此功能。

**目標：** 不進行語意比對，直接獲取某個 `topic` 下的所有知識點。

**MCP Resource：** `knowledge://{topic}`

**URI 格式：**
```
knowledge://SOLID
knowledge://DDD
knowledge://Architecture
```

**使用範例（Claude CLI）：**
```python
# Claude CLI 會自動處理 MCP Resource 請求
# 例如：當你詢問 "Show me all SOLID principles" 時
# Claude 會呼叫 knowledge://SOLID resource
```

**處理流程：**
1.  從 URI 中提取 `topic` 參數。
2.  從 ChromaDB 查詢所有元數據中 `topic` 欄位符合的知識點。
3.  返回該主題的所有知識點（不進行相似度計算）。

**回應格式：**
返回一個 `RetrievalResult` 物件，包含該主題的所有知識點。

```python
RetrievalResult(
    knowledge_points=[
        KnowledgePoint(
            id="a1b2c3d4-...",
            content="單一職責原則 (SRP) 指出一個類別或模組應該只有一個改變的理由。",
            topic="SOLID",
            similarity=None,  # Resource 不計算相似度
            timestamp="2025-10-15T10:00:00Z"
        ),
        KnowledgePoint(
            id="e5f6g7h8-...",
            content="里氏替換原則 (LSP) 要求子類別必須能夠替換其父類別...",
            topic="SOLID",
            similarity=None,
            timestamp="2025-10-20T14:30:00Z"
        ),
        # ... 該主題的所有知識點
    ]
)
```

---

## 情境整合：完整工作流程

### 範例：建立並查詢 Clean Architecture 知識庫

```python
# Step 1: 儲存架構文件
store_document(
    file_path="./documentation/ARCHITECTURE.md",
    topic="Architecture"
)

# Step 2: 手動新增補充知識
learn_knowledge(
    topic="CleanArchitecture",
    content="Clean Architecture 的核心原則：依賴方向必須由外向內，內層不依賴外層。"
)

learn_knowledge(
    topic="CleanArchitecture",
    content="Entity 層包含企業級業務規則，是系統最核心的部分。"
)

# Step 3: 語意搜尋相關內容
results = search_knowledge(
    query="Clean Architecture 的依賴規則是什麼？",
    topic="CleanArchitecture",
    top_k=3
)

# Step 4: 獲取主題下的所有知識
all_ca_knowledge = retrieve_all_by_topic(topic="CleanArchitecture")
```

### 範例：DDD 知識庫建立

```python
# 新增多個 DDD 知識點
learn_knowledge(
    topic="DDD",
    content="Aggregate Root 是聚合的入口點，外部只能透過它修改聚合內的物件。"
)

learn_knowledge(
    topic="DDD",
    content="Bounded Context 定義了模型的適用範圍和邊界。"
)

learn_knowledge(
    topic="DDD",
    content="Domain Event 用於表示領域中發生的重要事件。"
)

# 查詢 DDD 相關問題
search_knowledge(
    query="如何保護聚合內的一致性？",
    topic="DDD",
    top_k=2
)
```
