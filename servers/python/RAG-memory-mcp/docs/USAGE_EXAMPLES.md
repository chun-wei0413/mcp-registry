# Memory MCP Server 使用範例

本文件提供 Memory MCP Server v2.0 的實際使用範例，展示智能程式碼分離功能的應用。

## 目錄
- [基本使用](#基本使用)
- [智能程式碼分離範例](#智能程式碼分離範例)
- [進階查詢](#進階查詢)
- [整合到工作流程](#整合到工作流程)

---

## 基本使用

### 1. 儲存技術文件

```python
# 儲存包含程式碼範例的技術文件
store_document(
    file_path="./docs/DDD_Aggregate.md",
    topic="DDD"
)
```

**文件範例 (DDD_Aggregate.md):**
```markdown
# Aggregate 實作指南

## 什麼是 Aggregate？

Aggregate 是一組相關領域物件的集合，作為資料變更的單位。

```java
public class Order {
    private OrderId id;
    private List<OrderItem> items;

    public void addItem(OrderItem item) {
        this.items.add(item);
        apply(new ItemAdded(this.id, item));
    }
}
```

## 關鍵原則

1. 透過聚合根修改
2. 維護不變條件
3. 使用領域事件

```java
// 正確：透過聚合根修改
order.addItem(newItem);

// 錯誤：直接修改內部狀態
order.getItems().add(newItem);  // 不要這樣做！
```
```

**內部處理：**
```
文字部分（用於 embedding）：
"# Aggregate 實作指南\n\n## 什麼是 Aggregate？\n\nAggregate 是...\n\n[CODE_BLOCK_0]\n\n## 關鍵原則\n\n1. 透過聚合根修改\n2. 維護不變條件\n3. 使用領域事件\n\n[CODE_BLOCK_1]"

程式碼部分（儲存在 metadata）：
[
  {language: "java", code: "public class Order {...}", position: 0},
  {language: "java", code: "// 正確：透過聚合根修改...", position: 1}
]

Embedding 大小減少：約 55-60%
```

### 2. 語意搜尋

```python
# 查詢 Aggregate 相關知識
results = search_knowledge(
    query="如何正確修改 Aggregate 內部狀態",
    top_k=3,
    topic="DDD"  # 可選：限定主題
)
```

**返回結果：**
```json
{
  "results": [
    {
      "id": "abc-123",
      "content": "# Aggregate 實作指南\n\n...\n\n[CODE_BLOCK_0]\n\n...",
      "topic": "DDD",
      "similarity": 0.91,
      "timestamp": "2025-11-23T10:30:00Z",
      "code_blocks": [
        {
          "language": "java",
          "code": "public class Order {\n    private OrderId id;\n    private List<OrderItem> items;\n\n    public void addItem(OrderItem item) {\n        this.items.add(item);\n        apply(new ItemAdded(this.id, item));\n    }\n}",
          "position": 0
        },
        {
          "language": "java",
          "code": "// 正確：透過聚合根修改\norder.addItem(newItem);\n\n// 錯誤：直接修改內部狀態\norder.getItems().add(newItem);  // 不要這樣做！",
          "position": 1
        }
      ]
    }
  ]
}
```

---

## 智能程式碼分離範例

### 範例 1：Event Sourcing 教學文件

**文件內容：**
```markdown
# Event Sourcing 建構子規則

## 業務建構子規範

業務建構子**不可直接設定狀態**，必須透過事件驅動狀態變更。

### 錯誤範例

```java
public Product(ProductId id, ProductName name) {
    this.productId = id;          // ❌ 直接設定狀態
    this.productName = name;      // ❌ 直接設定狀態
    apply(new ProductCreated(...));
}
```

### 正確範例

```java
public Product(ProductId id, ProductName name) {
    apply(new ProductCreated(...));  // ✅ 只發送事件
}
```

事件處理器會負責狀態變更：

```java
@EventHandler
private void on(ProductCreated event) {
    this.productId = event.getProductId();
    this.productName = event.getProductName();
}
```
```

**儲存：**
```python
store_document(
    file_path="./docs/EventSourcing_Constructor.md",
    topic="EventSourcing"
)
```

**查詢：**
```python
# 查詢：Event Sourcing 建構子的正確寫法
results = search_knowledge(
    query="Event Sourcing constructor best practices",
    top_k=2
)
```

**結果分析：**
```
傳統方式（程式碼參與 embedding）：
  Similarity: 0.62
  原因：Java 語法稀釋了 "best practices" 的語意

智能分離（只用文字 embedding）：
  Similarity: 0.93
  原因：精準匹配 "不可直接設定狀態"、"透過事件驅動" 等概念
  同時返回：3 個完整的 Java 程式碼範例
```

### 範例 2：API 文件

**文件內容：**
```markdown
# REST API 使用指南

## 建立訂單 API

**端點：** `POST /api/orders`

**請求格式：**

```json
{
  "customerId": "CUST-001",
  "items": [
    {"productId": "PROD-123", "quantity": 2},
    {"productId": "PROD-456", "quantity": 1}
  ]
}
```

**回應格式：**

```json
{
  "orderId": "ORD-789",
  "status": "CREATED",
  "totalAmount": 15000
}
```

**錯誤處理：**

當庫存不足時，會返回 400 錯誤：

```json
{
  "error": "INSUFFICIENT_STOCK",
  "message": "Product PROD-123 is out of stock"
}
```
```

**查詢：**
```python
results = search_knowledge(
    query="如何處理訂單建立失敗的錯誤",
    top_k=1
)
```

**結果：**
- 精準匹配「錯誤處理」段落
- 返回 3 個 JSON 程式碼範例（請求、回應、錯誤）
- Similarity: 0.89

---

## 進階查詢

### 查詢 1：按主題檢索

```python
# 取得所有 DDD 主題的知識點
ddd_knowledge = retrieve_all_by_topic(topic="DDD")

# 遍歷結果
for knowledge in ddd_knowledge:
    print(f"標題：{knowledge['content'][:50]}...")
    if knowledge.get('code_blocks'):
        print(f"  包含 {len(knowledge['code_blocks'])} 個程式碼範例")
```

### 查詢 2：多輪對話

```python
# 第一輪：查詢概念
q1_results = search_knowledge(
    query="什麼是 Aggregate",
    top_k=3
)

# Claude 閱讀結果並理解概念
# ...

# 第二輪：查詢實作細節
q2_results = search_knowledge(
    query="Aggregate 的工廠方法如何實作",
    top_k=2
)

# 返回結果包含：
# - 概念說明（文字）
# - 工廠方法程式碼範例（code_blocks）
```

### 查詢 3：組合查詢

```python
# 查詢特定模式的實作
patterns = ["UseCase", "Repository", "Event Handler"]

all_results = []
for pattern in patterns:
    results = search_knowledge(
        query=f"{pattern} implementation pattern",
        top_k=2
    )
    all_results.extend(results["results"])

# 整理所有程式碼範例
code_examples = []
for result in all_results:
    if result.get("code_blocks"):
        code_examples.extend(result["code_blocks"])

print(f"找到 {len(code_examples)} 個程式碼範例")
```

---

## 整合到工作流程

### 工作流程 1：撰寫新功能前查閱規範

```python
# 情境：準備實作 CreateOrderUseCase

# Step 1: 查詢 UseCase 實作規範
usecase_rules = search_knowledge(
    query="UseCase 實作規範和原則",
    topic="CleanArchitecture",
    top_k=3
)

# Step 2: 查詢類似的 UseCase 範例
examples = search_knowledge(
    query="Create command UseCase implementation",
    top_k=2
)

# Claude 閱讀結果：
# - 理解 UseCase 原則（從 usecase_rules）
# - 參考類似實作（從 examples 的 code_blocks）
# - 撰寫符合規範的程式碼
```

### 工作流程 2：Code Review 前檢查

```python
# 情境：Review Event Sourcing 相關程式碼

# 查詢 Code Review Checklist
checklist = search_knowledge(
    query="Event Sourcing code review checklist constructor",
    topic="EventSourcing",
    top_k=1
)

# 結果包含：
# - Checklist 項目（content）
# - 正確/錯誤範例（code_blocks）

# Claude 可以：
# 1. 對照 checklist 檢查程式碼
# 2. 比對錯誤範例找出問題
# 3. 參考正確範例給出修改建議
```

### 工作流程 3：技術文件撰寫

```python
# 情境：撰寫 Aggregate 實作指南

# Step 1: 查詢現有知識
existing = search_knowledge(
    query="Aggregate implementation guidelines",
    top_k=5
)

# Step 2: 整理要點
# - 從 content 提取概念說明
# - 從 code_blocks 收集程式碼範例

# Step 3: 撰寫新文件（結合查詢結果）

# Step 4: 儲存到知識庫
store_document(
    file_path="./docs/NEW_Aggregate_Guide.md",
    topic="DDD"
)
```

### 工作流程 4：Debug 時查詢問題

```python
# 情境：遇到 Event Sourcing 建構子錯誤

# 查詢相關的常見錯誤和解決方案
debug_help = search_knowledge(
    query="Event Sourcing constructor common mistakes state management",
    top_k=3
)

# 結果可能包含：
# - "業務建構子不可直接設定狀態"（概念）
# - 錯誤範例：this.id = id（code_blocks）
# - 正確範例：apply(new Event())（code_blocks）

# Claude 比對錯誤範例，快速定位問題
```

---

## 效能最佳化範例

### 範例 1：批次儲存文件

```python
import os
from pathlib import Path

# 批次儲存整個文件夾
docs_dir = Path("./documentation")

for doc_file in docs_dir.glob("**/*.md"):
    # 根據資料夾名稱決定主題
    topic = doc_file.parent.name

    store_document(
        file_path=str(doc_file),
        topic=topic
    )

    print(f"已儲存: {doc_file.name} (主題: {topic})")
```

### 範例 2：增量更新

```python
import hashlib
import json

# 載入已處理檔案的 hash 記錄
with open(".doc_hashes.json", "r") as f:
    processed = json.load(f)

# 檢查檔案是否有變更
def file_changed(file_path):
    with open(file_path, "rb") as f:
        current_hash = hashlib.md5(f.read()).hexdigest()

    return processed.get(file_path) != current_hash

# 只處理有變更的檔案
for doc_file in Path("./docs").glob("**/*.md"):
    if file_changed(str(doc_file)):
        store_document(file_path=str(doc_file), topic="ProjectDocs")

        # 更新 hash
        with open(doc_file, "rb") as f:
            processed[str(doc_file)] = hashlib.md5(f.read()).hexdigest()

# 儲存 hash 記錄
with open(".doc_hashes.json", "w") as f:
    json.dump(processed, f)
```

---

## 最佳實踐總結

### ✅ 建議做法

1. **文件結構化**
   - 使用清晰的標題層級（##, ###）
   - 概念說明在程式碼區塊之前
   - 每個程式碼區塊指定語言

2. **主題分類**
   - 使用一致的主題命名（DDD, EventSourcing, CleanArchitecture）
   - 按主題組織文件

3. **查詢優化**
   - 使用具體的查詢詞
   - 利用 `topic` 參數縮小範圍
   - 適當調整 `top_k` 值

4. **定期更新**
   - 文件變更後重新索引
   - 使用增量更新避免重複處理

### ❌ 避免做法

1. 程式碼區塊沒有指定語言（```）
2. 所有說明都寫在程式碼註解中
3. 一個文件混合多個不相關主題
4. 查詢詞過於模糊或過於具體

---

## 故障排除範例

### 問題：查詢結果沒有程式碼

**檢查步驟：**
```python
# 1. 確認文件已儲存
results = search_knowledge(query="test", top_k=1)
print("找到結果：", len(results["results"]))

# 2. 檢查結果結構
result = results["results"][0]
print("有 content:", "content" in result)
print("有 code_blocks:", "code_blocks" in result)

# 3. 如果沒有 code_blocks，檢查原始文件
# 確保程式碼區塊格式正確：```language
```

### 問題：語意相似度很低

**調整策略：**
```python
# 方法 1：使用更具體的查詢
search_knowledge(query="Event Sourcing constructor rules")  # 更好
# vs
search_knowledge(query="constructor")  # 太模糊

# 方法 2：限定主題
search_knowledge(
    query="constructor rules",
    topic="EventSourcing"  # 縮小範圍
)

# 方法 3：增加返回數量
search_knowledge(query="...", top_k=10)  # 取更多結果
```

---

## 相關資源

- [CODE_SEPARATION.md](./CODE_SEPARATION.md) - 技術細節
- [CHANGELOG.md](../CHANGELOG.md) - 版本變更
- [README.md](../README.md) - 專案概述

**最後更新：** 2025-11-23
