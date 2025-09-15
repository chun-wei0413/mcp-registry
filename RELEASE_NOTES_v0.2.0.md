# PostgreSQL MCP Server - Release Notes v0.2.0

發布日期：2025-09-15

## 🎯 版本概要

PostgreSQL MCP Server v0.2.0 是一個重要的穩定性和兼容性更新版本，主要修復了關鍵的 Pydantic 驗證錯誤，並大幅改善了與 Claude Code 的整合體驗。

## 🐛 重要 Bug 修復

### 1. Pydantic 驗證錯誤修復
- **問題描述**: `QueryResult` 模型中的 `query_id` 和 `execution_time_ms` 欄位驗證失敗
- **解決方案**:
  - 將 `query_id` 改為 `Optional[str] = None`，允許空值
  - 將 `execution_time_ms` 強制轉換為整數類型，避免浮點數驗證錯誤
- **影響**: 解決了所有 MCP 工具呼叫時的驗證失敗問題

### 2. 欄位名稱一致性修復
- **問題描述**: 模型欄位名稱不一致導致的屬性錯誤
- **解決方案**:
  - 統一使用 `row_count` 替代 `affected_rows`
  - 統一使用 `message` 替代 `error_message`
- **影響**: 確保所有查詢結果的結構一致性

### 3. 浮點數精度問題修復
- **問題描述**: 執行時間計算產生浮點數，但模型期望整數
- **解決方案**: 在所有時間計算處使用 `int()` 進行類型轉換
- **影響**: 避免了 Pydantic 的型別驗證錯誤

## ✨ 新增功能

### 1. 增強 Docker HTTP 支援
```python
def run_sync_http(self, host="0.0.0.0", port=3000):
    """Run the MCP server with HTTP transport synchronously for Docker."""
```
- 新增專用的同步 HTTP 運行方法
- 改善與 Claude Code 的整合體驗
- 使用 `stateless_http=True` 提供更好的會話管理

### 2. 改善安全驗證器
- 優化正則表達式模式，減少誤報
- 移除過度嚴格的 CREATE 操作限制
- 改善 SQL 注入檢測的精確度

### 3. 完善錯誤處理
- 更準確的錯誤訊息結構
- 統一的異常處理機制
- 更清晰的錯誤回應格式

## 🔧 改善項目

### 1. 更穩定的批次操作
- 修復批次執行中的邊界情況
- 改善事務回滾機制
- 優化參數處理流程

### 2. 更好的 Claude Code 整合
- 使用 `FastMCP` 的 `stateless_http=True` 設定
- 改善 HTTP 端點的可靠性
- 優化會話管理機制

### 3. 優化 Docker 部署
- 改善容器啟動流程
- 更穩定的網路連線處理
- 優化資源使用效率

## ✅ 驗證結果

### 功能驗證
- ✅ 成功執行 10 筆測試資料的 MCP 寫入操作
- ✅ 批次操作和事務處理正常運作
- ✅ 查詢執行和結果回傳穩定

### 兼容性驗證
- ✅ Docker 網路連線穩定
- ✅ Claude Code 整合無誤
- ✅ PostgreSQL 各版本兼容

### 型別安全驗證
- ✅ 所有 Pydantic 模型驗證通過
- ✅ 資料完整性確保
- ✅ API 回應格式統一

## 📦 Docker Hub 發布

### 可用映像
```bash
# 拉取最新版本
docker pull russellli/postgresql-mcp-server:0.2.0
docker pull russellli/postgresql-mcp-server:latest

# 直接運行
docker run -d -p 3000:3000 russellli/postgresql-mcp-server:0.2.0
```

### 完整部署範例
```bash
# 下載 docker-compose.yml
curl -O https://raw.githubusercontent.com/chun-wei0413/pg-mcp/main/deployment/docker/docker-compose.yml

# 啟動完整環境
docker-compose up -d

# 驗證運行狀態
curl http://localhost:3000/health
```

## 🔄 升級指南

### 從 v0.1.0 升級
1. **停止現有服務**:
   ```bash
   docker-compose down
   ```

2. **更新映像**:
   ```bash
   docker pull russellli/postgresql-mcp-server:0.2.0
   ```

3. **重新啟動**:
   ```bash
   docker-compose up -d
   ```

### 注意事項
- 本版本向後兼容，無需修改配置文件
- 所有 API 端點保持不變
- 資料格式完全兼容

## 🐛 已知問題

目前版本沒有已知的重大問題。

## 🔮 下一版本預告 (v0.3.0)

- 添加更多 PostgreSQL 高級功能支援
- 增加連線池監控和管理
- 改善查詢性能分析工具
- 添加更豐富的日誌和監控功能

## 📧 問題回報

如果您遇到任何問題，請在 GitHub Issues 中回報：
https://github.com/chun-wei0413/pg-mcp/issues

## 🙏 致謝

感謝所有測試和回饋的用戶，您的貢獻讓這個版本更加穩定可靠。