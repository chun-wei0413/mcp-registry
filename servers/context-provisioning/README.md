# Context Provisioning Server

MCP Registry 的 MCP Server 實作：把團隊慣例文件做成向量知識庫，透過 MCP 協定提供語意搜尋給 AI Coding Agent。

專案背景、運作流程圖與設計理念請見 [根目錄 README](../../README.md)，本文件聚焦在**這個 server 本身怎麼跑、怎麼改**。

## 快速開始

```bash
# 1. 安裝依賴（首次會下載 embedding 模型約 600MB）
pip install -r requirements.txt

# 2. 建立向量資料庫
python scripts/ingest_ai_docs.py

# 3. 啟動 MCP Server（SSE, port 3031）
python mcp_server.py
```

macOS / Linux 也可以用一鍵腳本（自動建 venv、裝依賴、啟動）：

```bash
bash start.sh
```

Docker Compose：

```bash
docker-compose up -d
docker-compose logs -f rag-context-provisioning
```

> Docker 映像只包含程式碼，`chroma_db/` 以 volume 掛載。請先在本機跑過 `ingest_ai_docs.py`，否則容器啟動後知識庫是空的。

## 架構

```
┌──────────────────────────────┐
│  AI Client (Claude Code ...) │
└──────────────┬───────────────┘
               │ MCP over SSE (:3031)
┌──────────────▼───────────────────────────────┐
│  mcp_server.py  →  app.py (Application Factory)│
├───────────────────────────────────────────────┤
│  controllers/            MCP 介面層            │
│    knowledge_controller  search / learn        │
│    resource_controller   knowledge://{topic}   │
├───────────────────────────────────────────────┤
│  services/                                     │
│    vector_store_service  Embedding + 檢索      │
├───────────────────────────────────────────────┤
│  models/     Pydantic 回傳契約                  │
│  utils/      Markdown 解析與程式碼分離          │
└──────────────┬────────────────────────────────┘
               │
   ┌───────────▼────────────┐   ┌──────────────────┐
   │ SentenceTransformer    │   │ ChromaDB         │
   │ embeddinggemma-300m    │   │ (cosine, 內嵌式) │
   └────────────────────────┘   └──────────────────┘
```

設定以「參數 > 環境變數 > 預設值」的順序解析，全部集中在 `create_app()`。

## MCP 介面

### Tool: `search_knowledge(query, top_k=20, topic=None)`

語意搜尋，回傳 `SearchResult`。查詢文字會自動套上 EmbeddingGemma 的 query 模板
（`task: search result | query: {query}`）再向量化。

```python
search_knowledge(
    query="Aggregate 的建構子應該怎麼設計？",
    top_k=5
)
```

`topic` 是 ChromaDB 的精確比對（`where={"topic": topic}`）。Ingest 產生的 topic 格式為
`"{category} - {section_title}"`，不是單純的目錄名，傳錯會得到空結果——一般情況不要傳。

### Tool: `learn_knowledge(topic, content)`

新增單一知識點，回傳 ID。內容會套上 document 模板（`title: {topic} | text: {content}`）。
`content` 請放**文字描述**，程式碼建議走 ingest 流程（才會做程式碼分離）。

### Resource: `knowledge://{topic}`

取出該主題所有知識點，回傳 `RetrievalResult`。同樣是精確比對，需傳完整 topic 字串。

## 資料模型

```python
class CodeBlock(BaseModel):
    language: str     # java / python / ...
    code: str         # 完整程式碼
    position: int     # 在原文件中的位置

class KnowledgePoint(BaseModel):
    id: str
    content: str                      # 文字描述（embedding 來源）
    topic: str
    timestamp: str
    similarity: float | None          # 僅搜尋時有值；cosine 相似度 [-1,1]，越大越相似
    file_path: str | None             # 來源追溯
    section_title: str | None
    chunk_type: str | None
    code_blocks: list[CodeBlock] | None
```

`SearchResult` 包 `results`，`RetrievalResult` 包 `knowledge_points`。

## 知識庫與 Ingest

原始文件放在 `data/.ai/`，目前收錄 18 篇 DDD / Clean Architecture 團隊慣例：

| 目錄 | 篇數 |
|------|------|
| `aggregate/` | 8 |
| `contract/` | 6 |
| `patterns/` | 1 |
| `quick-reference/` | 2 |
| `testing/` | 1 |

`scripts/ingest_ai_docs.py` 的處理策略：

1. **語意完整 Chunking** —— 每個 `##` 章節 = 一個 chunk，不按字數細分，確保 Why + How + ✅ + ❌ 留在同一個 chunk。
2. **智能程式碼分離** —— embedding 只算文字，程式碼以 JSON 存進 metadata，查詢時再還原成 `code_blocks`。
3. **豐富元數據** —— 分類、優先級、來源檔案、章節標題一併寫入，方便追溯與過濾。

寫入的 collection 預設為 `aggregate`。

### 換成自己團隊的文件

把 `data/.ai/` 換成自己的 Markdown（沿用 `## 章節` 的組織方式效果最好），然後：

```bash
rm -rf chroma_db/
python scripts/ingest_ai_docs.py
```

## 環境變數

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `CHROMA_DB_PATH` | `./chroma_db` | ChromaDB 持久化目錄 |
| `COLLECTION_NAME` | `aggregate` | 使用的 collection |
| `EMBEDDING_MODEL` | `google/embeddinggemma-300m` | Embedding 模型 |
| `MCP_SERVER_HOST` | `0.0.0.0` | 監聽位址 |
| `MCP_SERVER_PORT` | `3031` | 監聽埠號 |

## 開發

| 目錄 | 說明 | 細節 |
|------|------|------|
| `controllers/` | MCP Tools / Resources 註冊 | [README](./controllers/README.md) |
| `services/` | 向量儲存與檢索邏輯 | [README](./services/README.md) |
| `models/` | Pydantic 資料模型 | [README](./models/README.md) |
| `utils/` | Markdown 解析、程式碼分離 | [README](./utils/README.md) |
| `scripts/` | Ingest / 驗證 / 路徑檢查 | [README](./scripts/README.md) |
| `tests/` | 單元測試與除錯腳本 | [README](./tests/README.md) |

```bash
# 執行測試
pytest tests/

# 驗證知識庫內容
python scripts/verify_ai_docs.py

# 檢查文件路徑格式（跨平台）
python scripts/check_paths.py
```

執行裝置自動偵測 `cuda` → `mps` → `cpu`；GPU / MPS 環境下模型以 bfloat16 載入。

## 疑難排解

**模型下載失敗**

```bash
python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('google/embeddinggemma-300m')"
```

**搜尋沒有結果** —— `chroma_db/` 不存在或是空的，重跑 `python scripts/ingest_ai_docs.py`。

**Docker 容器起不來**

```bash
docker-compose logs rag-context-provisioning
docker-compose build --no-cache
```

## 未來擴展

- [ ] 純程式碼檔案解析（`.java` / `.py` / `.ts`）
- [ ] 增量更新（只處理新增或修改的文件）
- [ ] 多 collection 管理與切換
- [ ] 程式碼語意搜尋（AST / Code Embeddings）

## 授權

此專案為 MCP Registry 的一部分，遵循專案主授權協議（MIT）。
