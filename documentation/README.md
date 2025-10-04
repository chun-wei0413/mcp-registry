# MCP Registry - 文檔中心

歡迎來到 MCP Registry 文檔中心！這裡包含所有 MCP Server 的技術文檔、使用指南和架構設計。

## 📚 文檔導覽

### 🚀 MCP Servers（核心產品）

我們提供三個功能強大的 MCP Server，每個都解決特定領域的問題：

#### 1. [PostgreSQL MCP Server](mcp-servers/postgresql-mcp/OVERVIEW.md)
**企業級 PostgreSQL 資料庫管理工具**
- 🎯 **解決問題**: LLM 無法直接操作 PostgreSQL 資料庫
- 💡 **核心功能**:
  - 資料庫連線管理
  - 安全的查詢執行（防 SQL Injection）
  - Schema 探索和分析
  - 事務和批次操作
- 🔧 **技術棧**: Java 17, Spring Boot, HikariCP, PostgreSQL JDBC
- 📖 [完整文檔 →](mcp-servers/postgresql-mcp/OVERVIEW.md)

#### 2. [MySQL MCP Server](mcp-servers/mysql-mcp/OVERVIEW.md)
**MySQL 原生特性支援工具**
- 🎯 **解決問題**: MySQL 特有功能缺乏標準化介面
- 💡 **核心功能**:
  - MySQL 方言和特性支援
  - InnoDB 儲存引擎管理
  - JSON 函數和 CTE (8.0+)
  - 索引優化和效能監控
- 🔧 **技術棧**: Java 17, Spring Boot, HikariCP, MySQL Connector/J
- 📖 [完整文檔 →](mcp-servers/mysql-mcp/OVERVIEW.md)

#### 3. [ContextCore MCP](mcp-servers/contextcore-mcp/OVERVIEW.md)
**智能開發日誌管理系統**
- 🎯 **解決問題**: Context 過載、檢索效率低、資訊組織困難
- 💡 **核心功能**:
  - 語義向量搜尋（理解查詢意圖）
  - 智能日誌檢索（不需載入所有歷史）
  - 多維度過濾（標籤、模組、時間、類型）
  - 本地部署、隱私安全
- 🔧 **技術棧**: Java, Qdrant, Ollama, SQLite
- 📖 [完整文檔 →](mcp-servers/contextcore-mcp/OVERVIEW.md)

---

### 🏗️ 架構設計

深入了解專案的技術架構和設計原則：

- **[系統架構](architecture/SYSTEM_ARCHITECTURE.md)** - 整體系統架構和技術選型
- **[Clean Architecture 實現](architecture/CLEAN_ARCHITECTURE.md)** - 清潔架構的具體實踐
- **[模組規格](architecture/MODULE_SPECIFICATIONS.md)** - 各模組的詳細規格說明

---

### 📖 使用指南

從入門到精通的完整指南：

- **[快速開始](guides/QUICK_START.md)** - 5 分鐘內啟動 MCP Server
- **[使用者指南](guides/USER_GUIDE.md)** - 詳細的使用說明和 API 參考
- **[常見問題 FAQ](guides/QA.md)** - 常見問題解答
- **[Docker Hub 指南](guides/DOCKER_HUB_GUIDE.md)** - Docker 部署完整教學

---

### 💻 開發文檔

開發者必讀的技術文檔：

- **[Java 專案總覽](development/JAVA_PROJECT_OVERVIEW.md)** - Java 版本的專案結構和模組介紹
- **[MCP 客戶端整合範例](examples/MCP_CLIENT_EXAMPLES.md)** - Python、Node.js、Claude Desktop 整合示範
- **[使用案例](examples/USE_CASES.md)** - 實際應用場景和最佳實踐

---

### 📁 專案資訊

了解專案的組織和演進：

- **[專案結構說明](project/PROJECT_STRUCTURE.md)** - 目錄結構和檔案組織
- **[Java 遷移計畫](project/JAVA_MIGRATION_PLAN.md)** - 從 Python 到 Java 的遷移歷程
- **[專案總結](PROJECT_SUMMARY.md)** - 專案的核心價值和技術亮點

---

### 📋 版本歷史

追蹤專案的發展歷程：

- **[v0.4.0 發布說明](release-notes/RELEASE_NOTES_v0.4.0.md)** - 最新版本
- **[v0.2.0 發布說明](release-notes/RELEASE_NOTES_v0.2.0.md)** - 里程碑版本

---

## 🎯 推薦閱讀路徑

### 新手入門 👋
1. [快速開始指南](guides/QUICK_START.md) - 快速上手
2. 選擇你需要的 MCP Server：
   - [PostgreSQL MCP](mcp-servers/postgresql-mcp/OVERVIEW.md)
   - [MySQL MCP](mcp-servers/mysql-mcp/OVERVIEW.md)
   - [ContextCore MCP](mcp-servers/contextcore-mcp/OVERVIEW.md)
3. [常見問題 FAQ](guides/QA.md) - 解決疑問

### 開發者 💻
1. [Java 專案總覽](development/JAVA_PROJECT_OVERVIEW.md) - 理解專案結構
2. [系統架構](architecture/SYSTEM_ARCHITECTURE.md) - 掌握架構設計
3. [Clean Architecture 實現](architecture/CLEAN_ARCHITECTURE.md) - 學習最佳實踐
4. [MCP 客戶端整合範例](examples/MCP_CLIENT_EXAMPLES.md) - 動手實作

---

## 🔧 MCP Tools 速查表

### PostgreSQL MCP Server

| 工具類別 | 主要工具 | 用途 |
|---------|---------|------|
| **連線管理** | `add_connection`, `test_connection`, `remove_connection` | 建立和管理資料庫連線 |
| **查詢執行** | `query`, `execute`, `transaction`, `batch` | 執行 SQL 查詢和事務 |
| **Schema 管理** | `get_table_schema`, `list_tables`, `explain_query` | 檢視資料庫結構 |

### MySQL MCP Server

| 工具類別 | 主要工具 | 用途 |
|---------|---------|------|
| **連線管理** | `add_connection`, `get_server_info` | MySQL 連線和伺服器資訊 |
| **查詢執行** | `query`, `transaction` | MySQL 特定查詢和事務 |
| **Schema 管理** | `get_table_schema`, `show_index_usage`, `optimize_table` | MySQL Schema 和優化 |
| **儲存引擎** | `get_engine_status` | InnoDB/MyISAM 狀態監控 |

### ContextCore MCP

| 工具類別 | 主要工具 | 用途 |
|---------|---------|------|
| **日誌管理** | `add_log` | 新增開發日誌 |
| **智能搜尋** | `search_logs` | 語義向量搜尋 |
| **日誌檢索** | `get_log`, `list_log_summaries` | 獲取日誌詳情 |
| **專案上下文** | `get_project_context` | 獲取關鍵決策和重要日誌 |

---

## 📖 使用範例速覽

### PostgreSQL 資料分析

```javascript
// 1. 建立分析資料庫連線
await mcp.callTool("postgresql_connection_management", {
  action: "add_connection",
  connectionId: "analytics_db",
  host: "localhost",
  database: "warehouse",
  readonly: true
});

// 2. 執行分析查詢
const result = await mcp.callTool("postgresql_query_execution", {
  action: "query",
  connectionId: "analytics_db",
  sql: `
    SELECT DATE_TRUNC('month', date) as month,
           SUM(revenue) as total_revenue
    FROM sales
    WHERE date >= ?
    GROUP BY month
  `,
  parameters: ["2024-01-01"]
});
```

### MySQL JSON 資料查詢

```javascript
// 使用 MySQL JSON 函數分析
const sentimentAnalysis = await mcp.callTool("mysql_query_execution", {
  action: "query",
  connectionId: "reviews_db",
  sql: `
    SELECT
      JSON_UNQUOTE(JSON_EXTRACT(review_data, '$.sentiment')) as sentiment,
      COUNT(*) as count,
      AVG(JSON_EXTRACT(review_data, '$.rating')) as avg_rating
    FROM product_reviews
    WHERE JSON_EXTRACT(review_data, '$.verified') = true
    GROUP BY sentiment
  `
});
```

### ContextCore 智能日誌搜尋

```javascript
// 語義搜尋歷史開發記錄
const logs = await mcp.callTool("search_logs", {
  query: "用戶認證處理方式",
  limit: 3,
  tags: ["auth"]
});

// 回傳結果（按相似度排序）：
// 1. "實現 JWT 登入功能" (相似度: 0.92)
// 2. "OAuth2 整合" (相似度: 0.87)
// 3. "Session 管理機制" (相似度: 0.81)
```

---

## 🛡️ 安全提醒

所有 MCP Server 都遵循嚴格的安全標準：

- ✅ **永遠使用參數化查詢** - 防止 SQL Injection
- ✅ **設定適當的安全配置** - 最小權限原則
- ✅ **定期檢查權限設定** - 避免權限過大
- ✅ **啟用查詢日誌記錄** - 審計和追蹤
- ✅ **本地部署優先** - 保護資料隱私（ContextCore MCP）
- ❌ **不要停用安全驗證** - 即使在開發環境
- ❌ **不要使用字串拼接查詢** - 使用 PreparedStatement

---

## 🔗 快速連結

- 🏠 [專案主頁](../)
- 🐛 [Issues 回報](https://github.com/your-org/mcp-registry/issues)
- 💬 [討論區](https://github.com/your-org/mcp-registry/discussions)
- 📝 [更新日誌](../CHANGELOG.md)

---

## 📞 需要幫助？

- 📧 **Email**: a910413frank@gmail.com
- 🐛 **Bug 回報**: [GitHub Issues](https://github.com/your-org/mcp-registry/issues)
- 💬 **功能討論**: [GitHub Discussions](https://github.com/your-org/mcp-registry/discussions)
- 📚 **文檔問題**: [文檔 Issues](https://github.com/your-org/mcp-registry/issues?q=is%3Aissue+label%3Adocumentation)

---

## 🌟 專案特色

### 🎯 三個強大的 MCP Server
- **PostgreSQL MCP**: 企業級 PostgreSQL 管理
- **MySQL MCP**: MySQL 原生特性完整支援
- **ContextCore MCP**: 智能開發日誌管理

### 🏗️ 企業級架構
- Clean Architecture + DDD 設計
- SOLID 原則實踐
- 完整的測試覆蓋

### 🔒 安全第一
- SQL Injection 防護
- 參數化查詢強制執行
- 完整的審計日誌
- 本地部署保護隱私

### 🚀 高效能
- 連線池優化
- 批次操作支援
- 響應式程式設計（R2DBC）
- 語義向量搜尋（ContextCore）

### 📊 可觀測性
- 結構化日誌
- 效能指標收集
- 健康檢查端點

---

**💡 提示**:
- 新使用者建議從 [快速開始指南](guides/QUICK_START.md) 開始
- 開發者可直接查看 [Java 專案總覽](development/JAVA_PROJECT_OVERVIEW.md)
- 每個 MCP Server 都有獨立的 OVERVIEW.md，包含完整的設計思路和實作細節

**🚀 立即開始**: [快速開始 →](guides/QUICK_START.md)
