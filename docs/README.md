# PostgreSQL MCP Server 文件中心

歡迎使用 PostgreSQL MCP Server！此文件中心提供完整的使用指南和參考資料。

## 📚 文件導覽

### 🚀 快速開始
- ⚡ [**快速開始指南**](../QUICK_START.md) - **5分鐘內開始使用！**
- 🐳 [**Docker Hub 使用指南**](DOCKER_HUB_GUIDE.md) - **官方映像檔部署指南**

### 📖 完整指南
- 📚 [**MCP Server 完整使用手冊**](MCP_SERVER_HANDBOOK.md) - **一站式完整指南**
  - 快速開始與部署
  - 詳細的安裝配置步驟
  - 所有 MCP 工具使用說明
  - 實際應用場景範例
  - 安全配置與最佳實務
  - 部署與維運指南
  - 故障排除與性能調優
  - 擴展與客製化方法

### 💡 實用範例與應用
- 🔌 [**MCP 客戶端整合範例**](examples/MCP_CLIENT_EXAMPLES.md) - **Python、Node.js、Claude Desktop 整合**
- 🎯 [**常見使用場景**](USE_CASES.md) - **資料遷移、分析、監控等實際應用**

### 📋 技術參考
- 📋 [**使用者指南**](guides/USER_GUIDE.md) - **技術使用指南**
  - 系統架構概覽
  - MCP 工具詳細說明
  - API 參考文件
  - 安全配置選項
  - 環境變數說明

### 📁 專案資訊
- 📁 [**專案結構說明**](PROJECT_STRUCTURE.md) - **專案組織架構**
  - 完整的目錄結構圖
  - 各檔案的用途說明
  - 模組分層設計理念
  - 新手與開發者指引

## 🎯 快速導覽

### 🆕 我是新手，想要快速開始
👉 [⚡ 快速開始指南](../QUICK_START.md) - 5分鐘內開始使用！
👉 [🐳 Docker Hub 指南](DOCKER_HUB_GUIDE.md) - 直接使用官方映像檔

### 🔧 我需要了解系統架構
👉 [使用者指南 - 系統架構](guides/USER_GUIDE.md#系統架構)

### 💼 我要看實際應用範例
👉 [🔌 MCP 客戶端整合範例](examples/MCP_CLIENT_EXAMPLES.md) - Python、Node.js 範例
👉 [🎯 常見使用場景](USE_CASES.md) - 資料遷移、分析、監控

### 🏭 我要設定生產環境
👉 [MCP Server 完整使用手冊 - 部署與維運](MCP_SERVER_HANDBOOK.md#部署與維運)

### 🚨 我遇到了問題
👉 [MCP Server 完整使用手冊 - 故障排除](MCP_SERVER_HANDBOOK.md#故障排除)

### 🛠️ 我想要自定義功能
👉 [MCP Server 完整使用手冊 - 擴展與客製化](MCP_SERVER_HANDBOOK.md#擴展與客製化)

### 📚 我需要 API 參考
👉 [使用者指南 - API 參考](guides/USER_GUIDE.md#api-參考)

## 🔧 工具速查

| 工具類別 | 主要工具 | 用途 |
|---------|---------|------|
| 連線管理 | `add_connection`, `test_connection` | 建立和管理資料庫連線 |
| 查詢執行 | `execute_query`, `execute_transaction` | 執行 SQL 查詢和事務 |
| Schema 檢查 | `get_table_schema`, `list_tables` | 檢視資料庫結構 |
| 監控工具 | `health_check`, `get_metrics` | 系統監控和性能指標 |

## 📖 範例場景

### 資料分析場景
```bash
# 1. 建立分析資料庫連線
await add_connection("analytics_db", host="...", database="warehouse")

# 2. 執行分析查詢
result = await execute_query(
    "analytics_db",
    "SELECT DATE_TRUNC('month', date) as month, SUM(revenue) FROM sales GROUP BY month"
)
```

### 資料遷移場景
```bash
# 1. 建立來源和目標連線
await add_connection("source_db", ...)
await add_connection("target_db", ...)

# 2. 批次遷移資料
await batch_execute("target_db", "INSERT INTO ...", data_batches)
```

## 🛡️ 安全提醒

- ✅ 永遠使用參數化查詢
- ✅ 設定適當的安全配置
- ✅ 定期檢查權限設定
- ✅ 啟用查詢日誌記錄
- ❌ 不要停用安全驗證
- ❌ 不要使用字串拼接查詢

## 🔗 快速連結

- [GitHub 專案](../../)
- [Issues 回報](../../issues)
- [討論區](../../discussions)
- [更新日誌](../../CHANGELOG.md)

## 📞 需要幫助？

- 📧 **Email**: a910413frank@gmail.com
- 🐛 **Bug 回報**: [GitHub Issues](../../issues)
- 💬 **功能討論**: [GitHub Discussions](../../discussions)

---

**💡 提示**: 建議新使用者先閱讀 [MCP Server 完整使用手冊](MCP_SERVER_HANDBOOK.md)，它提供了從入門到進階的完整指導。