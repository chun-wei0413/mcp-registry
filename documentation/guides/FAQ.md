# 常見問題 FAQ

## 🎯 一般問題

### Q: MCP Registry 是什麼？

**A**: MCP Registry 提供三個 MCP Server，讓 LLM（如 Claude）能夠：
- 直接查詢 PostgreSQL 和 MySQL 資料庫
- 智能搜尋和管理開發日誌

### Q: 我應該用哪個 MCP Server？

**A**:
- 有 PostgreSQL？ → 用 **PostgreSQL MCP**
- 有 MySQL？ → 用 **MySQL MCP**
- 想管理開發記錄？ → 用 **ContextCore MCP**

---

## 🚀 安裝與啟動

### Q: 如何快速啟動？

**A**: 參考 [GETTING_STARTED.md](../GETTING_STARTED.md)，3 個步驟即可啟動。

### Q: 需要什麼環境？

**A**:
- Java 17+
- Maven 3.8+
- （PostgreSQL/MySQL MCP）資料庫伺服器
- （ContextCore MCP）Docker

### Q: 啟動後如何驗證？

**A**:
```bash
curl http://localhost:8080/actuator/health
```
回傳 `{"status":"UP"}` 表示成功。

---

## 💾 PostgreSQL / MySQL MCP

### Q: 連線失敗怎麼辦？

**A**: 檢查以下項目：
1. 資料庫是否執行中？
   ```bash
   # PostgreSQL
   sudo systemctl status postgresql

   # MySQL
   sudo systemctl status mysql
   ```
2. 連線參數是否正確？（host, port, database）
3. 防火牆是否開放連接埠？

### Q: 如何安全地儲存密碼？

**A**: 使用環境變數：
```bash
export DB_PASSWORD="your_password"
```
然後在程式中讀取：
```javascript
password: process.env.DB_PASSWORD
```

### Q: 查詢時出現 SQL Injection 錯誤？

**A**: 必須使用參數化查詢：
```javascript
// ✅ 正確
sql: "SELECT * FROM users WHERE id = ?",
parameters: [userId]

// ❌ 錯誤
sql: `SELECT * FROM users WHERE id = ${userId}`
```

### Q: 如何查看表結構？

**A**:
```javascript
await mcp.callTool("get_table_schema", {
  connectionId: "my_db",
  tableName: "users"
});
```

---

## 🧠 ContextCore MCP

### Q: Qdrant 是什麼？為什麼需要它？

**A**: Qdrant 是向量資料庫，用於語義搜尋。ContextCore MCP 用它來理解日誌的「意義」，而不只是關鍵字匹配。

### Q: 搜尋結果不準確？

**A**:
1. 確認 Embedding 模型已下載：
   ```bash
   docker exec memory-ollama ollama list
   ```
2. 新增更多相關日誌（提升訓練資料）
3. 調整搜尋參數（tags, module 過濾）

### Q: 新增日誌後多久能搜尋到？

**A**: 約 1-2 秒（向量化處理時間）。

### Q: 可以刪除或修改日誌嗎？

**A**: 目前版本不支援，請等待未來更新。

---

## 🔧 進階問題

### Q: 如何啟用只讀模式？

**A**:
```javascript
await mcp.callTool("add_connection", {
  connectionId: "readonly_db",
  // ... 其他參數
  readonly: true  // 只讀模式
});
```

### Q: 支援事務嗎？

**A**: 支援！
```javascript
await mcp.callTool("transaction", {
  connectionId: "my_db",
  queries: [
    { sql: "INSERT INTO ...", parameters: [...] },
    { sql: "UPDATE ...", parameters: [...] }
  ]
});
```

### Q: 如何處理大量資料查詢？

**A**: 使用批次操作：
```javascript
await mcp.callTool("batch_execute", {
  connectionId: "my_db",
  sql: "INSERT INTO users VALUES (?, ?)",
  parametersList: [
    [1, "Alice"],
    [2, "Bob"],
    // ... 更多資料
  ]
});
```

---

## 🐛 錯誤處理

### Q: "Connection timeout" 錯誤？

**A**:
1. 增加連線超時時間（環境變數）
2. 檢查網路連線
3. 確認資料庫負載

### Q: "Out of memory" 錯誤？

**A**:
1. 減少 `fetchSize`（每次抓取行數）
2. 使用分頁查詢
3. 增加 JVM 記憶體：
   ```bash
   java -Xmx1G -jar mcp-server.jar
   ```

### Q: 日誌顯示什麼錯誤？

**A**: 查看日誌檔案：
```bash
tail -f logs/mcp-server.log
```

---

## 📖 更多資訊

### Q: 如何了解架構設計？

**A**: 閱讀 [ARCHITECTURE.md](../advanced/ARCHITECTURE.md)

### Q: 如何貢獻程式碼？

**A**: 閱讀 [DEVELOPMENT.md](../advanced/DEVELOPMENT.md)

### Q: 有更多範例嗎？

**A**: 查看各 MCP Server 文檔：
- [PostgreSQL MCP](../mcp-servers/postgresql-mcp.md)
- [MySQL MCP](../mcp-servers/mysql-mcp.md)
- [ContextCore MCP](../mcp-servers/contextcore-mcp.md)

---

## 📞 還有問題？

- 📧 Email: a910413frank@gmail.com
- 🐛 GitHub Issues: [回報問題](https://github.com/your-org/mcp-registry/issues)
- 💬 Discussions: [討論區](https://github.com/your-org/mcp-registry/discussions)

**找不到答案？** 請直接發 Issue 或 Email 聯繫我們！
