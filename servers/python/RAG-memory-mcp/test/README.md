# RAG Memory MCP Server - 測試套件

## 📂 測試結構

```
test/
├── README.md                   # 本文件
└── test_mcp_functions.py       # MCP 功能測試腳本
```

## 🧪 測試內容

### 測試 1: `learn_knowledge` - 新增知識點
- **目的**: 驗證可以將新知識點儲存到向量資料庫
- **測試內容**: 新增一個關於 FastMCP 的知識點
- **驗證點**: Server 返回成功響應

### 測試 2: `search_knowledge` - 搜尋知識
- **目的**: 驗證可以使用自然語言搜尋知識
- **測試內容**: 搜尋 "FastMCP Python SDK"
- **驗證點**: 返回相關的知識點列表及相似度分數

### 測試 3: `retrieve_all_by_topic` - 按主題檢索
- **目的**: 驗證可以檢索特定主題的所有知識點
- **測試內容**:
  1. 新增 3 個 DDD 主題的知識點
  2. 檢索所有 DDD 主題的知識
- **驗證點**: 返回所有相關知識點

## 🚀 執行測試

### 前提條件
1. Docker 容器 `memory-mcp-server` 必須正在運行
2. Python 3.7+ 已安裝

### 執行指令

```bash
# 切換到專案目錄
cd E:\Coding\mcp-registry\servers\python\RAG-memory-mcp

# 確認容器運行中
docker-compose ps

# 執行測試
python test/test_mcp_functions.py
```

### 預期輸出

```
======================================================================
 RAG Memory MCP Server - 功能測試
======================================================================

測試三個核心功能:
  1. learn_knowledge - 新增知識點
  2. search_knowledge - 搜尋知識
  3. retrieve_all_by_topic - 按主題檢索

======================================================================
測試 1: learn_knowledge - 新增知識點
======================================================================

[執行] 呼叫 learn_knowledge...
  主題: FastMCP
  內容: FastMCP 是 Anthropic 提供的...

[成功] 知識點已新增
  響應: Document stored successfully...

======================================================================
測試 2: search_knowledge - 搜尋知識
======================================================================

[執行] 呼叫 search_knowledge...
  查詢: FastMCP Python SDK
  返回數量: 3

[成功] 搜尋完成
  結果預覽:
  找到 X 個結果
    1. 主題: FastMCP
       相似度: 0.8542
       內容: FastMCP 是 Anthropic 提供的...

======================================================================
測試 3: retrieve_all_by_topic - 按主題檢索
======================================================================

[準備] 先新增幾個測試知識點...
  已新增: DDD - Aggregate Root 是...
  已新增: DDD - Bounded Context 定義了...
  已新增: DDD - Entity 和 Value Object...

[執行] 呼叫 resources/read (retrieve_all_by_topic)...
  主題: DDD

[成功] 檢索完成
  找到 3 個資源
    1. URI: knowledge://DDD
       內容預覽: Aggregate Root 是...

======================================================================
 測試結果總結
======================================================================
  learn_knowledge               ✓ 通過
  search_knowledge              ✓ 通過
  retrieve_all_by_topic         ✓ 通過

  總計: 3/3 個測試通過
======================================================================
```

## 🔍 測試細節

### MCP 請求格式

所有測試都使用標準的 JSON-RPC 2.0 格式與 MCP Server 通信：

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "learn_knowledge",
    "arguments": {
      "topic": "主題",
      "content": "內容"
    }
  }
}
```

### 通信方式

測試通過以下方式與 Docker 容器中的 MCP Server 通信：

```python
docker exec -i memory-mcp-server python mcp_server.py
```

輸入透過 stdin 傳遞 JSON-RPC 請求，輸出從 stdout 讀取響應。

## 📊 測試覆蓋範圍

| 功能 | 測試狀態 |
|------|---------|
| `learn_knowledge` | ✓ 已測試 |
| `search_knowledge` | ✓ 已測試 |
| `retrieve_all_by_topic` (Resource) | ✓ 已測試 |
| `store_document` | ⚠️ 未包含（需要檔案系統存取） |

## 🐛 故障排除

### 問題 1: 容器未運行
```bash
# 啟動容器
docker-compose up -d

# 檢查狀態
docker-compose ps
```

### 問題 2: 測試超時
- 原因: MCP Server 響應慢或未啟動完成
- 解決: 增加腳本中的 `timeout` 參數值

### 問題 3: 編碼錯誤 (Windows)
- 原因: Windows 預設使用 cp950 編碼
- 解決: 設定環境變數 `set PYTHONIOENCODING=utf-8`

## 📝 注意事項

1. **資料持久化**: 測試會實際寫入資料到 ChromaDB
2. **清理資料**: 如需清理測試資料，刪除 `chroma_db/` 目錄
3. **並發執行**: 不建議同時執行多個測試實例
4. **網路延遲**: 首次執行可能因模型載入較慢

## 🔗 相關文件

- [專案 README](../README.md)
- [使用演示](../USAGE_DEMO.md)
- [操作證明](../PROOF_OF_OPERATION.md)
