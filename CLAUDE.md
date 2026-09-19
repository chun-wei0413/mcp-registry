# MCP Registry — 開發規範

本文件是**給 AI 助手（Claude、Gemini 等）在這個 repo 裡作業時使用的規範**，說明架構約束、程式碼慣例與踩雷點。

專案在幹嘛、怎麼安裝、怎麼用，一律以 [README.md](README.md) 為準，本文件不重複。

---

## 1. 專案定位（一句話）

把團隊開發慣例文件切成語意完整的知識片段、算成向量存進 ChromaDB，透過 MCP 協定讓 AI Coding Agent 在寫程式前先查到「我們團隊怎麼做」。

唯一的 server 實作在 `servers/context-provisioning/`。

---

## 2. 架構與分層職責

```
mcp_server.py          進入點，只負責 create_app() + server.run(transport="sse")
  └── app.py           Application Factory：讀設定 → 建 Service → 建 FastMCP → 註冊 Controller
        ├── controllers/   MCP 介面層：@server.tool() / @server.resource() 的註冊與型別轉換
        ├── services/      業務邏輯：embedding、檢索、ChromaDB 存取
        ├── models/        Pydantic 資料契約（MCP 回傳結構）
        └── utils/         純函式工具：Markdown 解析、程式碼分離
```

### 分層規則（不可違反）

| 規則 | 說明 |
|------|------|
| Controller **不寫業務邏輯** | 只做「呼叫 service → 包成 Pydantic model 回傳」。看到 controller 裡出現 `chromadb` 或 `model.encode` 就是放錯層。 |
| Service **不認識 MCP** | `VectorStoreService` 回傳 `dict`，不回傳 Pydantic model，也不 import `mcp`。轉型是 controller 的事。 |
| Utils **保持純函式** | `MarkdownParser` 全是 `@staticmethod`，不持有狀態、不碰 I/O。新增解析邏輯沿用這個形式。 |
| 設定**只從 `create_app()` 進來** | 優先序：函式參數 > 環境變數 > 預設值。不要在 service 或 controller 裡直接 `os.getenv()`。 |

### 新增一個 MCP Tool 的標準流程

1. `services/` 加方法，回傳 `dict` / `list[dict]`
2. `models/knowledge_models.py` 定義回傳契約（如果是新結構）
3. `controllers/xxx_controller.py` 的 `register_xxx_tools()` 裡加 `@server.tool()`
4. `app.py` 如果是新的 controller 檔案，補上 `register_` 呼叫
5. `Dockerfile` 如果是新的頂層目錄，補 `COPY`

Tool 的 docstring **會被 AI 客戶端當成工具說明讀取**，要寫清楚參數語意與範例，不能只寫一行。

---

## 3. Embedding Prompt Template（最容易踩的雷）

查詢是短問句、文件是長敘述文，兩者形式差很多。`google/embeddinggemma-300m` 靠一段固定前綴分辨「這是查詢」還是「這是文件」，據此把向量推到對的區域——這就是**非對稱檢索**。兩邊必須各套各的模板，否則檢索品質明顯下降（完整說明見 [README.md](README.md) 核心設計第 3 點）：

| 用途 | 模板 | 實作位置 |
|------|------|----------|
| 查詢 | `task: search result \| query: {query}` | `VectorStoreService._format_query_prompt()` |
| 文件 | `title: {topic} \| text: {content}` | `VectorStoreService._format_document_prompt()` |

規則：

- **任何新增的 embedding 呼叫都必須先過模板**，不要直接 `model.encode(raw_text)`。
- 存進 ChromaDB 的 `documents` 要放**原始內容**，不是套完模板的字串（模板只用於算向量）。

所有 embedding 一律走 `VectorStoreService.encode_query()` / `encode_document()` 這兩個公開方法，模板套用封裝在裡面。**不要直接呼叫 `self.model.encode()`**（含 `scripts/` 底下的腳本），否則就會重演文件與查詢落在不同語意空間的問題。

---

## 4. 知識庫文件撰寫規範（`data/.ai/`）

這些文件的格式**直接決定 chunking 品質**，不是隨便寫的 Markdown。

### `##` 是切分邊界

`MarkdownParser.chunk_with_code_awareness()` 以 `^##\s+(.+)$` 切 chunk，**一個 `##` 章節 = 一個 chunk，不再細分**。因此：

- 一個 `##` 底下要放得下**完整的一條慣例**：Why + How + ✅ + ❌ 全在裡面
- 不要為了排版好看把「設計意圖」和「程式碼範例」拆成兩個 `##`
- `#`（H1）是文件標題，`###` 以下不影響切分

### `##` 底下的五段式結構（人為慣例，parser 不強制）

````markdown
## <這條慣例的名稱>

### 🎯 設計意圖 (Why)
解決什麼問題、為什麼這樣決定

### 📋 實作規範 (How)
條列式的具體做法

### ✅ 正確範例
```java
// 可直接照抄的程式碼
```

### ❌ 常見錯誤
```java
// 反例 + 為什麼錯
```

### 🔍 檢查清單
- [ ] 可勾選的驗收項目
````

**標題請照抄上面的字串**（含 emoji 與 `(Why)` / `(How)` 後綴）。現有文件已有漂移（`### 🎯 設計意圖` 少了 `(Why)`、`### ✅ 正確做法` 而非 `正確範例`），新增文件時**以上表為準**，不要跟著漂。

### 程式碼區塊

- **一定要標語言**：`` ```java ``。`MarkdownParser.CODE_BLOCK_PATTERN` 要求 ` ```(\w+) `，**沒標語言的區塊不會被抽出來**，會直接留在文字裡污染 embedding。

### 檔案與目錄命名

- 目錄 = 主題領域：`aggregate/`、`contract/`、`testing/`
- 檔名 = `NN-kebab-case.md`（`01-core-concepts.md`），數字決定閱讀順序

---

## 5. topic 的實際值（查詢時最容易搞錯）

`topic` 在兩條路徑產生的值**格式不同**，而過濾是 ChromaDB 的**精確比對**（`where={"topic": topic}`），不是前綴或模糊比對。

| 來源 | topic 值 | 範例 |
|------|----------|------|
| `scripts/ingest_ai_docs.py` | `f"{category} - {section_title}"` | `"aggregate - Aggregate 定義與核心概念"` |
| `learn_knowledge(topic, ...)` | 呼叫端傳什麼就是什麼 | `"DDD"` |

後果：

- `search_knowledge(query=..., topic="aggregate")` 對 ingest 進來的資料**會查不到任何東西**，因為沒有一筆的 topic 剛好等於 `"aggregate"`。
- 同理 `knowledge://aggregate` 也是空的，要用完整字串。
- **一般情況請不要傳 `topic`**，直接靠語意搜尋。要過濾時先確認實際值（`python scripts/verify_ai_docs.py`）。

`category` 由 `_get_file_category()` 決定：先比對 `CATEGORY_PRIORITY` 裡的 `pattern`，**都沒中就 fallback 成第一層目錄名**。目前 `data/.ai/` 底下的 `aggregate/`、`contract/`、`patterns/`、`quick-reference/`、`testing/` 全都走 fallback，所以 priority 一律是 `low`。新增主題目錄如果要有正確的 priority，要同步在 `CATEGORY_PRIORITY` 註冊。

---

## 6. 資料流與「改了什麼要重跑什麼」

```
data/.ai/*.md
    │  scripts/ingest_ai_docs.py（切 chunk + 分離程式碼 + 算 embedding）
    ▼
chroma_db/            ← 本機資料，已 gitignore，不進版控
    │  VectorStoreService
    ▼
MCP Tools / Resources ← AI 客戶端透過 SSE :3031 呼叫
```

| 改了什麼 | 要做什麼 |
|----------|----------|
| `data/.ai/` 的文件 | `rm -rf chroma_db/ && python scripts/ingest_ai_docs.py` |
| `utils/markdown_parser.py`、`scripts/ingest_ai_docs.py` | 同上（切分邏輯變了，舊資料失效） |
| `services/`、`controllers/`、`models/` | 重啟 server 即可，**不用**重建資料庫 |
| 換 `EMBEDDING_MODEL` | 必須重建資料庫（向量維度／語意空間不同） |

`chroma_db/` 是衍生資料。**任何情況都不要把它 commit 進版控**，也不要在 code review 時要求它存在。

---

## 7. 已知問題（動到相關程式碼時請一併確認）

### 尚未處理

1. **`topic` 精確比對造成的可用性問題**
   如第 5 節所述，ingest 產生的 topic 是 `"{category} - {section_title}"`，而過濾是精確比對，導致「用分類名過濾」這個最直覺的用法必定落空。已在 tool docstring 與 README 標註，但**根本解法尚未決定**——可能是改成前綴比對、或另存一個 `category` 欄位專供過濾。屬於產品行為變更，動手前先確認。

2. **`CATEGORY_PRIORITY` 對不上目前的 `data/.ai/` 結構**
   裡面的 pattern（`prompts/`、`coding-standards/`、`guides/`…）是為另一套目錄結構寫的，現有文件全部 fallback 成目錄名 + `low` priority。目前 priority 沒有被檢索邏輯使用，所以不影響結果，但新增主題目錄時要留意。

3. **README 的效能數字未經驗證**
   「精準度提升 ~40%」「embedding 減少 61-68%」沿用自早期文件，沒有可重現的量測依據。對外引用前請自行量測，或改為定性描述。

### 已修正（保留紀錄，避免改壞）

- **Ingest 沒套 document prompt template** — `_add_chunk_to_store()` 曾直接呼叫 `model.encode()`，現已改走 `encode_document()`。詳見第 3 節。
- **`similarity` 存的是 distance** — `search_knowledge()` 曾把 ChromaDB 的 `distances` 直接當成 similarity 回傳。現由 `_distance_to_similarity()` 轉換成 `1 - distance`，值域 `[-1, 1]`、越大越相似。**修改檢索邏輯時不要把這層轉換拿掉。**

## 8. 測試與驗證

```bash
cd servers/context-provisioning

pytest tests/                      # 單元測試（目前主要覆蓋 markdown_parser）
python tests/debug_chunking.py     # 看實際切出來的 chunk 長什麼樣
python scripts/verify_ai_docs.py   # 驗證已入庫的資料與 topic 實際值
python scripts/check_paths.py      # 檢查路徑格式（跨平台相容）
```

改動 chunking 或解析邏輯時，**先跑 `debug_chunking.py` 肉眼確認切分結果**，再跑 ingest。

跨平台注意：路徑一律用 `pathlib.Path`，不要手寫 `/` 或 `\` 拼字串；寫進 metadata 的路徑要正規化成 `/`。

---

## 9. 文件維護

- README 分工：[根 README](README.md) 講「為什麼與是什麼」，[server README](servers/context-provisioning/README.md) 講「怎麼跑與怎麼改」，本文件講「開發約束」。三者不要互相複製。
- README 用到的圖片等素材一律放 `docs/assets/`。
- 改了 MCP Tool 的簽章、環境變數、或第 5～7 節描述的行為，**同一個 commit 要一起更新對應文件**。

---

## 10. Commit 規範

沿用全域規範（`[Feature Addition]` / `[Bug Fixing]` / `[Optimization]` / `[Refactoring]` / `[Documentation]` / `[Deployment]`），一個 commit 只做一件事，訊息用美式英文，不要把 AI 寫成 co-author。
