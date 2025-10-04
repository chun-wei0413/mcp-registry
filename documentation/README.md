# MCP Registry 文檔

> 📌 **2 分鐘快速了解** MCP Registry 提供三個 MCP Server，讓 LLM 能夠操作資料庫和管理開發日誌。

## 🎯 這是什麼？

**MCP Registry** 提供三個 MCP (Model Context Protocol) Server：

| MCP Server | 用途 | 解決什麼問題 |
|-----------|------|------------|
| **PostgreSQL MCP** | 資料庫管理 | LLM 無法直接查詢 PostgreSQL |
| **MySQL MCP** | 資料庫管理 | LLM 無法直接查詢 MySQL |
| **ContextCore MCP** | 智能日誌 | 開發日誌太多，查找困難 |

## 🚀 快速開始

**5 分鐘上手** → [GETTING_STARTED.md](GETTING_STARTED.md)

## 📖 詳細文檔

### 三個 MCP Server 說明

- **[PostgreSQL MCP](mcp-servers/postgresql-mcp.md)** - 連線、查詢、Schema 探索
- **[MySQL MCP](mcp-servers/mysql-mcp.md)** - MySQL 特有功能（JSON、InnoDB）
- **[ContextCore MCP](mcp-servers/contextcore-mcp.md)** - 語義搜尋開發日誌

### 使用指南

- **[常見問題 FAQ](guides/FAQ.md)** - 遇到問題先看這裡

### 進階資訊（給開發者）

- **[系統架構](advanced/ARCHITECTURE.md)** - Clean Architecture + DDD 設計
- **[開發指南](advanced/DEVELOPMENT.md)** - 如何開發和擴展

## 💡 使用範例

### PostgreSQL MCP - 查詢資料

```javascript
// 1. 建立連線
await mcp.callTool("add_connection", {
  connectionId: "my_db",
  host: "localhost",
  database: "myapp"
});

// 2. 查詢資料
const result = await mcp.callTool("query", {
  connectionId: "my_db",
  sql: "SELECT * FROM users WHERE age > ?",
  parameters: [18]
});
```

### ContextCore MCP - 搜尋歷史記錄

```javascript
// 語義搜尋：找到相關的開發記錄
const logs = await mcp.callTool("search_logs", {
  query: "如何實現登入功能",
  limit: 3
});

// 回傳：
// 1. "實現 JWT 登入" (相似度 0.92)
// 2. "OAuth2 整合" (相似度 0.87)
// 3. "Session 管理" (相似度 0.81)
```

## 🛡️ 安全提醒

- ✅ 永遠使用參數化查詢（防止 SQL Injection）
- ✅ 本地部署優先（保護資料隱私）
- ❌ 不要使用字串拼接 SQL

## 📞 需要幫助？

- 📧 Email: a910413frank@gmail.com
- 🐛 問題回報: [GitHub Issues](https://github.com/your-org/mcp-registry/issues)

---

**下一步**: [5 分鐘快速上手 →](GETTING_STARTED.md)
