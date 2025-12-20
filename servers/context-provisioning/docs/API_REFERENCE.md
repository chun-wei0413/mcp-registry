# MCP Server 配置指南

## 快速開始

### 啟動 MCP Server

```bash
cd servers/python/RAG-context-provisioning
./venv/bin/python mcp_server.py
```

**輸出示例：**
```
[*] Initializing services...
    - Database: ./chroma_db
    - Collection: ai_documentation
    - Embedding model: paraphrase-multilingual-MiniLM-L12-v2
[OK] Services initialized

[*] Creating MCP Server...
    - Host: 0.0.0.0
    - Port: 3031

[*] Registering controllers...
    - Knowledge tools registered
    - Document tools registered
    - Indexing tools registered
    - Resources registered
[OK] All controllers registered

[*] Listening on 0.0.0.0:3031
```

---

## 🔧 配置選項

### 默認配置（推薦）

```bash
# 使用默認配置啟動
./venv/bin/python mcp_server.py

# 配置詳情：
# - Database: ./chroma_db
# - Collection: ai_documentation  ⭐ (1,116 chunks)
# - Model: paraphrase-multilingual-MiniLM-L12-v2
# - Host: 0.0.0.0
# - Port: 3031
```

### 環境變數配置

可以通過環境變數自訂配置：

```bash
# 使用自訂 collection
COLLECTION_NAME=mcp_knowledge_base ./venv/bin/python mcp_server.py

# 使用自訂 port
MCP_SERVER_PORT=3032 ./venv/bin/python mcp_server.py

# 同時配置多個參數
COLLECTION_NAME=ai_documentation \
MCP_SERVER_PORT=3031 \
MCP_SERVER_HOST=127.0.0.1 \
./venv/bin/python mcp_server.py
```

### 環境變數說明

| 環境變數 | 默認值 | 說明 |
|---------|--------|------|
| `CHROMA_DB_PATH` | `./chroma_db` | ChromaDB 數據庫路徑 |
| `COLLECTION_NAME` | `ai_documentation` | ChromaDB collection 名稱 |
| `EMBEDDING_MODEL` | `paraphrase-multilingual-MiniLM-L12-v2` | SentenceTransformer 模型 |
| `MCP_SERVER_HOST` | `0.0.0.0` | MCP server 監聽地址 |
| `MCP_SERVER_PORT` | `3031` | MCP server 監聽埠號 |

---

## 📚 Collections 說明

### 1️⃣ ai_documentation（推薦）

**用途：** .ai 目錄的 RAG 索引庫

```
COLLECTION_NAME=ai_documentation
```

**特點：**
- ✅ 包含 **1,116 個 chunks**
- ✅ 來自 165 個 .ai 目錄 Markdown 文件
- ✅ 支援智能程式碼分離
- ✅ 豐富的元數據（category、priority、topics 等）
- ✅ 適合 RAG 搜尋

**何時使用：**
- 🔍 搜尋 .ai 目錄的文檔
- 📖 獲取編碼標準和最佳實踐
- 💡 查找提示語和範例
- 🔧 獲取完整的代碼片段

**示例：**
```python
# Controller 會自動使用此 collection
results = search_knowledge("aggregate", top_k=5)
# 返回 ai_documentation 中的最相關 chunks
```

### 2️⃣ mcp_knowledge_base

**用途：** 通用知識庫

```
COLLECTION_NAME=mcp_knowledge_base
```

**特點：**
- ✅ 包含 2 個 documents（可擴展）
- ✅ 簡單的 topic + content 結構
- ✅ 手動添加的知識點
- ✅ 適合存儲摘要和筆記

**何時使用：**
- 📝 記錄工作進度
- 💾 存儲臨時知識點
- 📋 保存項目決策
- 🎯 存儲個人筆記

---

## 🔌 MCP Server 提供的工具（Tools）

### 1. search_knowledge

**功能：** 語義搜尋知識

```
Tool Name: search_knowledge

Parameters:
  - query (string, required): 搜尋關鍵詞
  - top_k (integer, optional): 返回的最大結果數（默認：50）
  - topic (string, optional): 過濾主題

Returns:
  SearchResult object with list of KnowledgePoint
```

**使用示例：**

```python
# 從另一個 AI app 呼叫此 MCP 工具
result = mcp.call_tool("search_knowledge", {
    "query": "aggregate",
    "top_k": 5,
    "topic": None
})

# 返回結果包含：
# - id: 文檔 ID
# - content: 文檔內容
# - topic: 主題分類
# - similarity: 相似度分數（0-1）
# - code_blocks: 相關代碼塊（如果有）
# - file_path: 源文件路徑
# - priority: 優先級
```

### 2. learn_knowledge

**功能：** 添加新的知識點

```
Tool Name: learn_knowledge

Parameters:
  - topic (string, required): 主題分類
  - content (string, required): 知識點內容

Returns:
  string: "Knowledge learned with ID: {doc_id}"
```

**使用示例：**

```python
# 添加新知識點
result = mcp.call_tool("learn_knowledge", {
    "topic": "DDD",
    "content": "Aggregate 是 DDD 中的核心概念..."
})
```

### 3. store_document

**功能：** 讀取並儲存文件到知識庫

```
Tool Name: store_document

Parameters:
  - file_path (string, required): 文件路徑（支援 .md、.txt、.json）
  - topic (string, optional): 主題分類，默認使用檔名

Returns:
  string: 確認訊息
```

### 4. batch_index_folder

**功能：** 批量索引整個文件夾

```
Tool Name: batch_index_folder

Parameters:
  - source_dir (string, required): 目錄路徑
  - chunk_size (integer, optional): 分塊大小（默認：4000）
  - chunk_overlap (integer, optional): 重疊大小（默認：200）
  - file_extensions (list, optional): 文件副檔名過濾

Returns:
  IndexingStats 物件
```

---

## 📡 從另一個 AI App 使用

### Python 示例

```python
import httpx
import json

# 連接到 MCP Server
async def query_knowledge(query: str, top_k: int = 5):
    """從 MCP Server 查詢知識"""

    # MCP 使用 JSON-RPC 協議
    payload = {
        "jsonrpc": "2.0",
        "method": "tools/call",
        "params": {
            "name": "search_knowledge",
            "arguments": {
                "query": query,
                "top_k": top_k
            }
        },
        "id": 1
    }

    async with httpx.AsyncClient() as client:
        response = await client.post(
            "http://localhost:3031",
            json=payload
        )
        return response.json()

# 使用
results = await query_knowledge("aggregate", top_k=5)
for result in results['results']:
    print(f"主題: {result['topic']}")
    print(f"相似度: {result['similarity']}")
    print(f"內容: {result['content'][:200]}...")
```

### JavaScript/Node.js 示例

```javascript
async function queryKnowledge(query, topK = 5) {
    const payload = {
        jsonrpc: "2.0",
        method: "tools/call",
        params: {
            name: "search_knowledge",
            arguments: {
                query: query,
                top_k: topK
            }
        },
        id: 1
    };

    const response = await fetch("http://localhost:3031", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    });

    return await response.json();
}

// 使用
const results = await queryKnowledge("aggregate", 5);
results.results.forEach(r => {
    console.log(`主題: ${r.topic}`);
    console.log(`內容: ${r.content.substring(0, 200)}...`);
});
```

---

## ✅ 驗證配置

### 檢查 Server 狀態

```bash
# 檢查 server 是否運行
curl http://localhost:3031/health 2>/dev/null || echo "Server not running"

# 查看 server 進程
ps aux | grep "mcp_server.py" | grep -v grep
```

### 驗證 Collection

```bash
# 測試搜尋
python3 << 'EOF'
from services.vector_store_service import VectorStoreService

vs = VectorStoreService(
    db_path="./chroma_db",
    collection_name="ai_documentation"
)

# 檢查集合
print(f"Collection count: {vs.collection.count()}")

# 執行搜尋
results = vs.search_knowledge("aggregate", top_k=3)
print(f"Found {len(results)} results")

for r in results:
    print(f"  - {r['topic']}: {r['content'][:100]}...")
EOF
```

---

## 🚀 部署方案

### 本地開發

```bash
./venv/bin/python mcp_server.py
# 在本地 127.0.0.1:3031 運行
```

### Docker 部署

```bash
# 構建鏡像
docker build -t mcp-knowledge-server .

# 運行容器
docker run -d \
  -p 3031:3031 \
  -e COLLECTION_NAME=ai_documentation \
  -e MCP_SERVER_HOST=0.0.0.0 \
  -v $(pwd)/chroma_db:/app/chroma_db \
  mcp-knowledge-server
```

### 生產環境配置

```bash
# 使用 systemd 管理
cat > /etc/systemd/system/mcp-server.service << 'EOF'
[Unit]
Description=MCP Knowledge Base Server
After=network.target

[Service]
Type=simple
User=mcpserver
WorkingDirectory=/opt/mcp-server
Environment="COLLECTION_NAME=ai_documentation"
Environment="MCP_SERVER_HOST=0.0.0.0"
Environment="MCP_SERVER_PORT=3031"
ExecStart=/opt/mcp-server/venv/bin/python mcp_server.py
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 啟動服務
sudo systemctl start mcp-server
sudo systemctl enable mcp-server
```

---

## 🔍 故障排除

### 問題 1：Server 啟動失敗

```
ModuleNotFoundError: No module named 'mcp'
```

**解決方案：**
```bash
# 確保使用虛擬環境
./venv/bin/python mcp_server.py

# 或重新安裝依賴
pip install -r requirements.txt
```

### 問題 2：Collection 未找到

```
ValueError: Collection ai_documentation not found
```

**解決方案：**
```bash
# 檢查集合是否存在
python3 -c "
import chromadb
client = chromadb.PersistentClient(path='./chroma_db')
print('Collections:', [c.name for c in client.list_collections()])
"

# 如果不存在，運行 ingest 腳本
python3 scripts/ingest_ai_docs_v2.py
```

### 問題 3：查詢無結果

```
[no results]
```

**解決方案：**
```bash
# 驗證集合有數據
python3 -c "
from services.vector_store_service import VectorStoreService
vs = VectorStoreService(db_path='./chroma_db', collection_name='ai_documentation')
print(f'Collection count: {vs.collection.count()}')
"

# 嘗試更通用的查詢
results = vs.search_knowledge('code', top_k=5)
```

---

## 📋 常見工作流

### 工作流 1：搜尋並返回結果

```
AI App
  ↓
調用 search_knowledge("aggregate", top_k=5)
  ↓
MCP Server
  ↓
查詢 ai_documentation collection
  ↓
返回 SearchResult (5 個相關 chunks)
  ↓
AI App 展示結果給用戶
```

### 工作流 2：添加新知識

```
AI App
  ↓
調用 learn_knowledge("DDD", "Aggregate 是...")
  ↓
MCP Server
  ↓
添加到 mcp_knowledge_base collection
  ↓
返回 document ID
  ↓
AI App 確認成功
```

### 工作流 3：索引新文件

```
AI App
  ↓
調用 store_document("./path/to/file.md", "Architecture")
  ↓
MCP Server
  ↓
讀取文件 → chunking → embedding
  ↓
添加到 ai_documentation collection
  ↓
返回確認訊息
  ↓
AI App 更新索引完成
```

---

## 💾 數據持久化

### ChromaDB 位置

```
servers/python/RAG-context-provisioning/chroma_db/
├── chroma.sqlite3          # 主數據庫
└── 089237fa-.../           # Collection 數據
```

### 備份

```bash
# 備份 ChromaDB
cp -r chroma_db chroma_db.backup

# 恢復備份
cp -r chroma_db.backup chroma_db
```

### 數據遷移

```bash
# 複製到另一個機器
scp -r chroma_db user@remote:/path/to/destination/
```

---

## 📊 性能指標

| 操作 | 延遲 | 說明 |
|------|------|------|
| 單次搜尋 | <100ms | ai_documentation (1,116 chunks) |
| Server 啟動 | 2-3s | 包含模型載入 |
| Embedding 計算 | ~100ms/query | 並行執行 |
| 全集合掃描 | ~500ms | 1,116 個文檔 |

**優化建議：**
- ✅ 複用 HTTP 連接
- ✅ 使用 `top_k` 限制結果
- ✅ 如果可能，過濾 `topic`
- ✅ 批量查詢時使用連接池

---

## 📞 支援資源

- **查詢指南：** `QUERY_GUIDE.md`
- **Ingest 指南：** `INGEST_GUIDE.md`
- **API 文檔：** `API_REFERENCE.md`（如有）
- **架構說明：** `docs/CODE_SEPARATION.md`

---

**最後更新：** 2025-11-24
**默認 Collection：** `ai_documentation`（1,116 chunks）
**推薦配置：** 使用默認值（無需額外配置）
