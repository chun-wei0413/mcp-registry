# Scripts 工具腳本

本目錄包含 RAG Memory MCP Server 的工具腳本。

## 📜 腳本列表

### 1. `ingest_ai_docs.py`

**用途**: 處理 `.ai/` 目錄下的所有文檔，進行 chunking 並 embedding 後存入 ChromaDB。

**執行方式**:
```bash
cd servers/python/RAG-memory-mcp
python scripts/ingest_ai_docs.py
```

**功能**:
- 遞迴讀取 `.ai/` 目錄下所有 `.md` 檔案
- 根據檔案大小智能選擇 chunking 策略
- 生成豐富的元數據（分類、優先級、主題標籤）
- 存入 ChromaDB 的 `ai_documentation` 集合

**何時使用**:
- 首次設置系統時
- `.ai/` 目錄有重大更新時
- 需要重建 ChromaDB 資料時

---

### 2. `verify_ai_docs.py`

**用途**: 驗證 ChromaDB 中的資料完整性並測試檢索功能。

**執行方式**:
```bash
cd servers/python/RAG-memory-mcp
python scripts/verify_ai_docs.py
```

**功能**:
- 統計 ChromaDB 中的 chunks 數量
- 測試多個查詢範例的檢索效果
- 顯示分類統計
- 驗證元數據正確性

**何時使用**:
- 執行 `ingest_ai_docs.py` 後驗證結果
- 複製 `chroma_db/` 到新裝置後確認資料完整
- 排查檢索問題時

---

### 3. `check_paths.py`

**用途**: 檢查 ChromaDB 中所有檔案路徑的格式是否符合跨平台規範。

**執行方式**:
```bash
cd servers/python/RAG-memory-mcp
python scripts/check_paths.py
```

**功能**:
- 檢查是否所有路徑都使用 `/` (forward slash)
- 驗證沒有殘留 `full_path` 欄位
- 統計路徑格式分布

**何時使用**:
- 更新 embedding 腳本後驗證路徑格式
- 排查跨平台相容性問題時

---

## 🛠️ 常見使用場景

### 場景 1: 首次設置系統

```bash
# 1. 處理所有文檔並存入 ChromaDB
python scripts/ingest_ai_docs.py

# 2. 驗證資料正確性
python scripts/verify_ai_docs.py

# 3. 檢查路徑格式
python scripts/check_paths.py
```

### 場景 2: 複製到新裝置

```bash
# 1. 解壓 chroma_db.tar.gz
tar -xzf chroma_db.tar.gz

# 2. 驗證資料完整性
python scripts/verify_ai_docs.py
```

### 場景 3: 更新 .ai 文檔

```bash
# 1. 重新處理文檔
python scripts/ingest_ai_docs.py

# 2. 驗證更新結果
python scripts/verify_ai_docs.py
```

---

## 📝 注意事項

1. **環境需求**: 確保已安裝 `requirements.txt` 中的所有依賴
2. **工作目錄**: 所有腳本都假設從 `servers/python/RAG-memory-mcp/` 目錄執行
3. **資料備份**: 執行 `ingest_ai_docs.py` 前建議先備份 `chroma_db/` 目錄
4. **跨平台**: 所有腳本在 Windows、Linux、macOS 上都可正常運作
