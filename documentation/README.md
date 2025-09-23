# MCP Registry - 文檔中心

這裡包含 MCP Registry Java Edition 的所有技術文檔和指南。

## 📚 文檔結構

### 🚀 快速開始
- [快速開始指南](guides/QUICK_START.md) - 5分鐘內啟動 Java MCP Server
- [常見問題 FAQ](guides/QA.md) - 常見問題解答

### 📖 使用指南
- [MCP Server 完整手冊](MCP_SERVER_HANDBOOK.md) - 從入門到進階的完整指南
- [使用者指南](guides/USER_GUIDE.md) - 技術細節和 API 參考
- [Docker Hub 指南](DOCKER_HUB_GUIDE.md) - Docker 部署指南
- [使用案例](USE_CASES.md) - 實際應用場景

### 🏗️ 架構設計
- [系統架構](ARCHITECTURE.md) - 技術架構和設計原則
- [模組規格](MODULE_SPECIFICATIONS.md) - 各模組詳細規格

### 💻 開發文檔
- [MCP 客戶端整合範例](examples/MCP_CLIENT_EXAMPLES.md) - Python、Node.js、Claude Desktop 整合

### 📁 專案資訊
- [專案結構說明](project/PROJECT_STRUCTURE.md) - 目錄結構和檔案說明
- [Java 遷移計畫](project/JAVA_MIGRATION_PLAN.md) - 從 Python 到 Java 的遷移
- [Claude Code 指令](project/CLAUDE.md) - Claude Code 開發指令和規範
- [資料庫摘要](project/database-summary-mcp.md) - 資料庫設計摘要

### 📋 版本歷史
- [v0.2.0 發布說明](release-notes/RELEASE_NOTES_v0.2.0.md)
- [v0.4.0 發布說明](release-notes/RELEASE_NOTES_v0.4.0.md)

## 🎯 推薦閱讀順序

### 新手入門
1. [快速開始指南](guides/QUICK_START.md)
2. [MCP Server 完整手冊](MCP_SERVER_HANDBOOK.md)
3. [常見問題 FAQ](guides/QA.md)

### 開發者
1. [系統架構](ARCHITECTURE.md)
2. [Java 遷移計畫](project/JAVA_MIGRATION_PLAN.md)
3. [MCP 客戶端整合範例](examples/MCP_CLIENT_EXAMPLES.md)

### 運維人員
1. [Docker Hub 指南](DOCKER_HUB_GUIDE.md)
2. [使用案例](USE_CASES.md)
3. [使用者指南](guides/USER_GUIDE.md)

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