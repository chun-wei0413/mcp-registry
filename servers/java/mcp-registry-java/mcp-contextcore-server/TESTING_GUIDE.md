# ContextCore MCP Server - 測試指南

## 🎯 如何查看和測試 Vector Database

### 1. Qdrant Web UI (推薦)

Qdrant 提供了內建的 Web 管理介面，你可以直接在瀏覽器中查看和測試。

#### 訪問方式：
```
http://localhost:6333/dashboard
```

#### 功能：
- 📊 **查看 Collections**: 所有向量集合的列表
- 🔍 **瀏覽數據**: 查看儲存的向量和 payload
- 🎯 **執行搜尋**: 測試相似度搜尋
- 📈 **監控狀態**: 查看資料庫統計資訊

---

### 2. Qdrant REST API

#### 查看所有 Collections
```bash
curl http://localhost:6333/collections
```

#### 查看特定 Collection 資訊
```bash
# ContextCore 使用的 collection name: contextcore_logs
curl http://localhost:6333/collections/contextcore_logs
```

#### 查看 Collection 中的點 (Points)
```bash
# 列出所有向量點
curl -X POST http://localhost:6333/collections/contextcore_logs/points/scroll \
  -H "Content-Type: application/json" \
  -d '{
    "limit": 10,
    "with_payload": true,
    "with_vector": false
  }'
```

#### 搜尋相似向量
```bash
# 使用範例向量搜尋 (需要先有真實的 embedding vector)
curl -X POST http://localhost:6333/collections/contextcore_logs/points/search \
  -H "Content-Type: application/json" \
  -d '{
    "vector": [0.1, 0.2, 0.3, ...],  # 768 維向量
    "limit": 5,
    "with_payload": true
  }'
```

---

### 3. 測試流程

#### Step 1: 啟動服務
```bash
# 1. 啟動 Docker 容器
cd mcp-contextcore-server
docker-compose up -d

# 2. 確認服務狀態
docker ps --filter "name=contextcore"

# 3. 下載 Ollama 嵌入模型 (首次使用)
docker exec -it contextcore-ollama ollama pull nomic-embed-text
```

#### Step 2: 運行 ContextCore MCP Server
```bash
# 在 IntelliJ 中運行
ContextCoreMCPApplication.java

# 或使用 Maven
./mvnw spring-boot:run -pl mcp-contextcore-server
```

#### Step 3: 使用 MCP Tools 添加測試數據

通過 MCP Client (如 Claude Desktop) 調用以下 tools:

**1. 添加日誌 (addLog)**
```json
{
  "title": "測試：實現用戶登入功能",
  "content": "完成了用戶登入的後端 API，使用 JWT 進行身份驗證。包含密碼加密和 token 刷新機制。",
  "tags": "backend,authentication,jwt",
  "module": "user-service",
  "type": "FEATURE"
}
```

**2. 搜尋日誌 (searchLogs)**
```json
{
  "query": "用戶登入",
  "limit": 5
}
```

**3. 查看專案上下文 (getProjectContext)**
```json
{
  "limit": 10
}
```

#### Step 4: 在 Qdrant Dashboard 查看結果

1. 打開瀏覽器: `http://localhost:6333/dashboard`
2. 點擊 **Collections** → 選擇 `contextcore_logs`
3. 查看:
   - **Points Count**: 總共儲存的日誌數量
   - **Vector Dimension**: 768 (nomic-embed-text 的維度)
   - **Points**: 查看實際儲存的向量和 metadata

---

### 4. 檢查 SQLite 資料庫

ContextCore 使用 SQLite 儲存結構化日誌數據。

#### 位置
```
mcp-contextcore-server/data/contextcore.db
```

#### 查詢工具
```bash
# 使用 sqlite3 命令行
sqlite3 mcp-contextcore-server/data/contextcore.db

# 查看表結構
.schema logs

# 查詢所有日誌
SELECT id, title, module, type, timestamp FROM logs;

# 退出
.quit
```

或使用 GUI 工具:
- [DB Browser for SQLite](https://sqlitebrowser.org/)
- [DBeaver](https://dbeaver.io/)

---

### 5. 常用檢查命令

#### 快速檢查 Qdrant 狀態
```bash
# 健康檢查
curl http://localhost:6333/healthz

# 查看集群資訊
curl http://localhost:6333/cluster

# 查看 collection 統計
curl http://localhost:6333/collections/contextcore_logs
```

#### 快速檢查 Ollama 狀態
```bash
# 查看已下載的模型
docker exec contextcore-ollama ollama list

# 測試嵌入模型
docker exec contextcore-ollama ollama run nomic-embed-text "test embedding"
```

---

### 6. 故障排除

#### Qdrant 無法連線
```bash
# 重啟容器
docker-compose restart qdrant

# 查看日誌
docker logs contextcore-qdrant
```

#### Ollama 模型未下載
```bash
# 下載 nomic-embed-text 模型
docker exec -it contextcore-ollama ollama pull nomic-embed-text

# 確認模型存在
docker exec contextcore-ollama ollama list
```

#### Collection 不存在
```bash
# ContextCore 會在第一次添加日誌時自動建立 collection
# 如果需要手動建立:
curl -X PUT http://localhost:6333/collections/contextcore_logs \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": {
      "size": 768,
      "distance": "Cosine"
    }
  }'
```

---

### 7. 資料清理

#### 刪除測試資料
```bash
# 刪除 Qdrant collection
curl -X DELETE http://localhost:6333/collections/contextcore_logs

# 刪除 SQLite 資料庫
rm mcp-contextcore-server/data/contextcore.db

# 清理 Docker volumes
docker-compose down -v
rm -rf mcp-contextcore-server/docker-volumes/*
```

---

## 📊 預期的測試結果

### Qdrant Collection 結構
```json
{
  "name": "contextcore_logs",
  "config": {
    "params": {
      "vectors": {
        "size": 768,
        "distance": "Cosine"
      }
    }
  },
  "points_count": 10,  // 儲存的日誌數量
  "status": "green"
}
```

### Point (Vector) 結構
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "vector": [0.1, 0.2, ...],  // 768 維向量
  "payload": {
    "tags": ["backend", "authentication"],
    "module": "user-service",
    "type": "FEATURE"
  }
}
```

### Search Result 範例
```json
{
  "result": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "score": 0.95,  // 相似度分數 (0-1)
      "payload": {
        "tags": ["backend", "authentication"],
        "module": "user-service",
        "type": "FEATURE"
      }
    }
  ]
}
```

---

## 🎉 成功指標

✅ Qdrant Dashboard 可以訪問
✅ Collection `contextcore_logs` 存在
✅ Points count > 0
✅ 可以執行向量搜尋並得到結果
✅ SQLite 資料庫中有對應的日誌記錄
✅ 搜尋結果的 score 在合理範圍內 (0.7-1.0)

---

**提示**: 建議先添加 5-10 條不同主題的測試日誌，然後使用 `searchLogs` 工具測試語義搜尋的準確度。
