# ChromaDB 查詢指南

## 快速開始

### 1️⃣ 通過 Python 直接查詢

最推薦的方式 - 直接使用 VectorStoreService：

```python
from services.vector_store_service import VectorStoreService

# 初始化
vs = VectorStoreService(
    db_path="./chroma_db",
    collection_name="ai_documentation"  # ⭐ 主要文檔索引
)

# 基本搜尋
results = vs.search_knowledge(
    query="aggregate",
    top_k=5
)

# 處理結果
for result in results:
    print(f"主題: {result['topic']}")
    print(f"相似度: {result['similarity']:.4f}")
    print(f"內容: {result['content'][:200]}...")
    print(f"來源: {result.get('file_path', 'N/A')}")

    # 獲取代碼塊
    if result.get('code_blocks'):
        for code in result['code_blocks']:
            print(f"\n代碼 ({code['language']}):")
            print(code['code'])
    print("\n" + "="*80)
```

**輸出示例：**
```
主題: core-index
相似度: 0.5360
內容: ## 🎯 核心概念

### 什麼是 Aggregate？
- **一致性邊界**：保證內部狀態的一致性
- **事務邊界**：所有變更在單一事務中完成
- **聚合根**：外部只能通過聚合根訪問聚合內部
...
來源: tech-stacks/java-ca-ezddd-spring/examples/aggregate/README.md
```

---

### 2️⃣ 按優先級搜尋

只搜尋 Critical 和 High 優先級的文檔：

```python
from services.vector_store_service import VectorStoreService

vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

# 獲取所有文檔
all_docs = vs.collection.get()

# 過濾優先級
high_priority_docs = [
    (all_docs['ids'][i], all_docs['documents'][i], all_docs['metadatas'][i])
    for i in range(len(all_docs['ids']))
    if all_docs['metadatas'][i].get('priority') in ['critical', 'high']
]

print(f"高優先級文檔: {len(high_priority_docs)}")

# 在高優先級文檔中搜尋
query_embedding = vs.model.encode("aggregate").tolist()
results = vs.collection.query(
    query_embeddings=[query_embedding],
    n_results=5,
    where={"priority": {"$in": ["critical", "high"]}}
)
```

---

### 3️⃣ 按分類搜尋

只搜尋某個分類的文檔：

```python
vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

# 搜尋提示語（prompts）分類
results = vs.search_knowledge(
    query="如何寫測試?",
    top_k=5
)

# 過濾結果
prompt_results = [
    r for r in results
    if 'prompts' in r['topic'].lower()
]

for result in prompt_results:
    print(f"{result['topic']}: {result['content'][:150]}...")
```

---

### 4️⃣ 按主題搜尋

利用元數據中的 topics 欄位：

```python
vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

# 搜尋包含特定主題的文檔
results = vs.search_knowledge("testing", top_k=5)

# 過濾包含 "testing" 主題的結果
testing_results = [
    r for r in results
    if 'testing' in r.get('topic', '').lower()
]

for result in testing_results:
    print(f"主題: {result['topic']}")
    print(f"內容: {result['content'][:100]}...")
```

---

### 5️⃣ 獲取特定類別的所有文檔

```python
vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

# 獲取所有文檔
all_docs = vs.collection.get()

# 按分類分組
from collections import defaultdict
by_category = defaultdict(list)

for i, doc_id in enumerate(all_docs['ids']):
    category = all_docs['metadatas'][i].get('category', 'unknown')
    by_category[category].append({
        'id': doc_id,
        'content': all_docs['documents'][i][:100],
        'source': all_docs['metadatas'][i].get('source_file')
    })

# 顯示聚合文檔
print(f"Aggregate 相關文檔:")
for doc in by_category.get('examples', []):
    if 'aggregate' in doc['source'].lower():
        print(f"  - {doc['source']}")
```

---

## Collections 選擇

### 何時使用 `ai_documentation`

✅ **推薦用於：**
- 搜尋 .ai 目錄的文檔
- 獲取編碼標準和最佳實踐
- 查找提示語和範例
- 獲取完整的代碼片段

```python
vs = VectorStoreService(
    db_path="./chroma_db",
    collection_name="ai_documentation"  # ⭐ 1,116 chunks
)
```

### 何時使用 `mcp_knowledge_base`

✅ **推薦用於：**
- 查詢項目摘要和總結
- 獲取工作進度記錄
- 存儲個人筆記和決策
- 簡單的知識管理

```python
vs = VectorStoreService(
    db_path="./chroma_db",
    collection_name="mcp_knowledge_base"  # 2 documents（可擴展）
)
```

---

## 實用示例

### 範例 1：查詢 Aggregate 的完整信息

```python
from services.vector_store_service import VectorStoreService

vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

print("=" * 80)
print("查詢: Aggregate 實作")
print("=" * 80)

results = vs.search_knowledge("aggregate", top_k=5)

for i, result in enumerate(results, 1):
    print(f"\n{i}. 【{result['topic']}】")
    print(f"   相似度: {result['similarity']:.4f}")
    print(f"   優先級: {result.get('priority', 'N/A')}")
    print(f"   來源: {result.get('file_path', 'N/A')}")
    print(f"   內容預覽:")
    print(f"   {result['content'][:300]}...\n")

    if result.get('code_blocks'):
        print(f"   ✅ 包含 {len(result['code_blocks'])} 個代碼塊")
        for code in result['code_blocks'][:2]:  # 只顯示前 2 個
            print(f"\n   【{code['language']} 代碼】")
            print(f"   {code['code'][:200]}...\n")
```

**輸出：**
```
================================================================================
查詢: Aggregate 實作
================================================================================

1. 【core-index】
   相似度: 0.5360
   優先級: critical
   來源: tech-stacks/java-ca-ezddd-spring/examples/aggregate/README.md
   內容預覽:
   ## 🎯 核心概念

   ### 什麼是 Aggregate？
   - **一致性邊界**：保證內部狀態的一致性...

   ✅ 包含 5 個代碼塊
```

---

### 範例 2：搜尋測試相關的文檔

```python
from services.vector_store_service import VectorStoreService

vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

# 搜尋測試相關內容
results = vs.search_knowledge("如何編寫單元測試", top_k=10)

# 按優先級排序
high_priority = sorted(
    [r for r in results if r.get('priority') == 'critical'],
    key=lambda x: x['similarity'],
    reverse=True
)

print("🎯 High Priority Testing Results:\n")
for result in high_priority[:3]:
    print(f"✅ {result['topic']}")
    print(f"   {result['content'][:150]}...\n")
```

---

### 範例 3：獲取所有代碼標準

```python
from services.vector_store_service import VectorStoreService

vs = VectorStoreService(db_path="./chroma_db", collection_name="ai_documentation")

# 搜尋編碼標準
results = vs.search_knowledge("編碼規範", top_k=20)

# 過濾 coding-standards 分類
standards = [
    r for r in results
    if 'coding-standards' in r['topic'].lower()
]

print(f"找到 {len(standards)} 個編碼標準文檔\n")

for result in standards[:5]:
    print(f"📋 {result['topic']}")
    section = result.get('section_title', 'N/A')
    print(f"   Section: {section}")
    print(f"   Preview: {result['content'][:100]}...\n")
```

---

## ChromaDB 直接 API 查詢

如果你想完全掌控查詢邏輯：

```python
import chromadb
from sentence_transformers import SentenceTransformer

# 連接到 ChromaDB
client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_collection(name="ai_documentation")

# 加載模型
model = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")

# 查詢
query = "aggregate"
query_embedding = model.encode(query).tolist()

results = collection.query(
    query_embeddings=[query_embedding],
    n_results=5,
    include=["documents", "metadatas", "distances"]
)

# 處理結果
for i, doc_id in enumerate(results['ids'][0]):
    doc = results['documents'][0][i]
    meta = results['metadatas'][0][i]
    distance = results['distances'][0][i]

    print(f"{i+1}. Distance: {distance:.4f}")
    print(f"   Category: {meta['category']}")
    print(f"   Content: {doc[:100]}...")
    print()
```

---

## 常見查詢模式

### 查詢模式 1：完全相似搜尋

```python
vs.search_knowledge("aggregate", top_k=5)
```

### 查詢模式 2：帶條件的搜尋

```python
# 直接查詢 + 結果過濾
results = vs.search_knowledge("aggregate", top_k=10)
filtered = [r for r in results if r['priority'] == 'critical']
```

### 查詢模式 3：多條件過濾

```python
results = vs.search_knowledge("測試", top_k=20)
filtered = [
    r for r in results
    if r.get('priority') in ['critical', 'high']
    and 'junit' in r['topic'].lower()
]
```

### 查詢模式 4：分類檢索

```python
# 直接在低級 API 上用 where 過濾
results = vs.collection.query(
    query_embeddings=[query_embedding],
    n_results=5,
    where={"category": "prompts-subagent"}
)
```

---

## 性能貼士

| 操作 | 時間 | 說明 |
|------|------|------|
| 單次搜尋 | <100ms | 快速響應 |
| 獲取全部文檔 | ~500ms | 1,116 個文檔 |
| 過濾分類 | <50ms | 內存操作 |
| 模型載入 | 2-3s | 首次初始化 |

**優化建議：**
1. ✅ 複用 VectorStoreService 實例（不要每次都初始化）
2. ✅ 使用 `where` 過濾減少結果集
3. ✅ 批量查詢時使用列表推導式
4. ✅ 限制 `top_k` 的大小（通常 5-10 已足夠）

---

## 故障排除

### 問題 1：沒有搜尋結果

```python
# ✅ 檢查集合是否有數據
print(vs.collection.count())  # 應該是 1116

# ✅ 試試更通用的查詢
results = vs.search_knowledge("code", top_k=5)

# ✅ 檢查查詢語言
results = vs.search_knowledge("測試", top_k=5)  # 支援中文
```

### 問題 2：模型加載失敗

```python
# ✅ 確保模型已下載
import sentence_transformers
model = sentence_transformers.SentenceTransformer(
    "paraphrase-multilingual-MiniLM-L12-v2"
)  # 會自動下載 (~80MB)
```

### 問題 3：相似度分數很低

```python
# ✅ 低分數（>0.5）仍表示相關
# ✅ 嘗試調整 top_k 看更多結果
results = vs.search_knowledge(query, top_k=10)

# ✅ 查看距離而非相似度
for r in results:
    print(f"Distance: {r['similarity']}")  # <0.5 是好結果
```

---

## 推薦流程

```
開始
  ↓
選擇 Collection
  ├─ ai_documentation（推薦） → 項目文檔搜尋
  └─ mcp_knowledge_base → 知識庫查詢
  ↓
初始化 VectorStoreService
  ↓
執行搜尋
  vs.search_knowledge(query, top_k=5)
  ↓
處理結果
  ├─ 顯示內容
  ├─ 獲取代碼塊
  └─ 過濾/排序
  ↓
完成
```

---

**最後更新：** 2025-11-24
**推薦 Collection：** `ai_documentation`（1,116 chunks）
**默認模型：** paraphrase-multilingual-MiniLM-L12-v2
