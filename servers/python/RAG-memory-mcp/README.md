# Memory MCP Server

基於 RAG（Retrieval-Augmented Generation）的專案知識管理系統，讓 Claude CLI 能夠讀取並查詢專案文件。

## 🎯 功能特性

| 功能 | 說明 | 使用場景 |
|------|------|---------|
| **文件儲存** | 自動讀取並儲存 .md、.json 等專案文件 | 儲存 Spec.md、架構文件 |
| **語義搜尋** | 透過自然語言查詢相關 context | 查詢 "Clean Architecture" 獲取 CA 相關內容 |
| **主題管理** | 按主題分類和檢索知識點 | 按 DDD、SOLID 等主題組織知識 |

## 📦 技術棧（最簡化）

```yaml
核心技術:
  - MCP SDK: FastMCP
  - Embedding 模型: all-MiniLM-L6-v2 (80MB, 本地運行)
  - 向量資料庫: ChromaDB (內嵌式, 零配置)
  - 文件處理: Python 標準庫

Docker 基礎映像:
  - python:3.11-slim

依賴套件: (requirements.txt)
  - mcp-cli
  - chromadb
  - sentence-transformers
  - uv
```

## 🚀 快速開始

### 方式 1: Docker Compose 部署（推薦）

```bash
# 1. 啟動服務
cd servers/python
docker-compose up -d

# 2. 查看日誌
docker-compose logs -f memory-mcp

# 3. 停止服務
docker-compose down
```

### 方式 2: 本地開發

```bash
# 1. 安裝依賴
pip install -r requirements.txt

# 2. 啟動 MCP Server
python mcp_server.py
```

## 🔧 MCP 工具 API

### 1. `store_document` - 儲存文件

```python
# Claude CLI 使用範例
store_document(
    file_path="./documentation/ARCHITECTURE.md",
    topic="Architecture"  # 可選，預設使用檔名
)
```

**回傳**:
```
Document stored successfully:
- File: ARCHITECTURE.md
- Topic: Architecture
- ID: 550e8400-e29b-41d4-a716-446655440000
- Size: 15234 characters
```

### 2. `search_knowledge` - 語義搜尋

```python
# 查詢 Clean Architecture 相關內容
search_knowledge(
    query="Clean Architecture principles",
    top_k=5,
    topic="Architecture"  # 可選，限定主題
)
```

**回傳**:
```json
{
  "results": [
    {
      "id": "550e8400-...",
      "content": "Clean Architecture 的核心原則是...",
      "topic": "Architecture",
      "similarity": 0.92,
      "timestamp": "2024-01-15T10:30:00Z"
    }
  ]
}
```

### 3. `learn_knowledge` - 手動新增知識

```python
# 手動新增知識點
learn_knowledge(
    topic="DDD",
    content="Aggregate Root 是 Domain-Driven Design 中的核心概念..."
)
```

### 4. `retrieve_all_by_topic` - 按主題檢索

```python
# 取得所有 DDD 相關知識
retrieve_all_by_topic(topic="DDD")
```

## 📂 目錄結構

```
servers/python/
├── mcp_server.py          # FastMCP 伺服器主程式
├── storage.py             # ChromaDB 向量儲存層
├── requirements.txt       # Python 依賴
├── Dockerfile             # Docker 映像定義
├── docker-compose.yml     # Docker Compose 配置
├── .dockerignore          # Docker 忽略檔案
├── chroma_db/             # ChromaDB 持久化資料（自動建立）
└── README.md              # 本文件
```

## 🔐 配置說明

### 環境變數

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `MCP_SERVER_NAME` | Memory MCP Server | MCP 伺服器名稱 |
| `CHROMA_DB_PATH` | ./chroma_db | ChromaDB 資料目錄 |
| `PYTHONUNBUFFERED` | 1 | Python 輸出不緩衝 |

### Volume 掛載

```yaml
volumes:
  - ./chroma_db:/app/chroma_db           # 持久化向量資料庫
  - ../../documentation:/app/documents:ro # 唯讀掛載專案文件
```

## 🧪 使用範例

### 範例 1: 儲存並查詢專案規格

```python
# Step 1: 儲存專案規格文件
store_document(
    file_path="./documentation/ARCHITECTURE.md",
    topic="Architecture"
)

store_document(
    file_path="./CLAUDE.md",
    topic="ProjectRules"
)

# Step 2: 查詢 Clean Architecture 相關內容
results = search_knowledge(
    query="What are the principles of Clean Architecture?",
    top_k=3
)

# Claude 會自動取得相關 context 並回答問題
```

### 範例 2: 建立 DDD 知識庫

```python
# 手動新增 DDD 知識點
learn_knowledge(
    topic="DDD",
    content="Aggregate Root 負責維護聚合內的一致性邊界..."
)

learn_knowledge(
    topic="DDD",
    content="Bounded Context 定義了模型的適用範圍..."
)

# 查詢所有 DDD 知識
ddd_knowledge = retrieve_all_by_topic(topic="DDD")
```

## 🏗️ 架構說明

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

## 🔍 技術細節

### Embedding 模型選擇

選用 **all-MiniLM-L6-v2** 的原因：
- ✅ 輕量（80MB）
- ✅ 本地運行（無需 API key）
- ✅ 速度快（384 維度）
- ✅ 準確度足夠（Semantic Search 排名前列）

### 向量資料庫選擇

選用 **ChromaDB** 的原因：
- ✅ 零配置（內嵌式）
- ✅ 持久化儲存
- ✅ 支援 Cosine Similarity
- ✅ 無需額外容器

## 📊 效能指標

| 指標 | 數值 | 說明 |
|------|------|------|
| Embedding 速度 | ~1000 tokens/sec | CPU 運算 |
| 搜尋延遲 | <100ms | 1000 筆文件內 |
| 記憶體使用 | ~500MB | 包含模型載入 |
| 磁碟使用 | ~200MB | 模型 + 資料庫 |

## 🐛 疑難排解

### 問題 1: 模型下載失敗

```bash
# 手動下載模型
python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')"
```

### 問題 2: ChromaDB 初始化錯誤

```bash
# 清除資料庫重新初始化
rm -rf chroma_db/
docker-compose restart
```

### 問題 3: Docker 容器無法啟動

```bash
# 查看詳細日誌
docker-compose logs memory-mcp

# 重建映像
docker-compose build --no-cache
```

## 🚧 未來擴展

- [ ] 支援 Chunking 策略（大文件分段）
- [ ] 支援更多文件格式（PDF、DOCX）
- [ ] 新增 Prompt 優化功能
- [ ] 整合 Claude CLI 配置檔

## 📝 授權

此專案為 MCP Registry 的一部分，遵循專案主授權協議。