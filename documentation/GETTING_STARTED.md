# 5 分鐘快速上手

> 這份指南幫助你在 5 分鐘內開始使用 MCP Registry 的任一 MCP Server。

## 📋 前置準備

- Java 17 或以上
- Maven 3.8+
- PostgreSQL 或 MySQL（如果要使用資料庫 MCP）

## 🚀 快速啟動

### 選項 1：使用 PostgreSQL MCP

```bash
# 1. 進入專案目錄
cd mcp-registry-java/mcp-postgresql-server

# 2. 建置專案
mvn clean package

# 3. 啟動服務
java -jar target/mcp-postgresql-server-0.5.0.jar

# 4. 驗證服務
curl http://localhost:8080/actuator/health
```

### 選項 2：使用 MySQL MCP

```bash
# 1. 進入專案目錄
cd mcp-registry-java/mcp-mysql-server

# 2. 建置專案
mvn clean package

# 3. 啟動服務
java -jar target/mcp-mysql-server-0.5.0.jar
```

### 選項 3：使用 ContextCore MCP

```bash
# 1. 啟動依賴服務（Qdrant + Ollama）
docker-compose -f deployment/contextcore-docker-compose.yml up -d

# 2. 下載 Embedding 模型
docker exec memory-ollama ollama pull nomic-embed-text

# 3. 啟動 ContextCore MCP
cd mcp-registry-java/mcp-contextcore-server
mvn clean package
java -jar target/mcp-contextcore-server-0.5.0.jar
```

## 💡 第一次使用

### PostgreSQL MCP - 基本查詢

```javascript
// 1. 建立連線
await mcp.callTool("add_connection", {
  connectionId: "test_db",
  host: "localhost",
  port: 5432,
  database: "testdb",
  username: "postgres",
  password: "your_password"
});

// 2. 執行查詢
const result = await mcp.callTool("query", {
  connectionId: "test_db",
  sql: "SELECT * FROM users LIMIT 10"
});

console.log(result.rows);
```

### MySQL MCP - JSON 查詢

```javascript
// 使用 MySQL JSON 函數
const result = await mcp.callTool("query", {
  connectionId: "mysql_db",
  sql: `
    SELECT
      id,
      JSON_EXTRACT(data, '$.name') as name
    FROM products
    WHERE JSON_EXTRACT(data, '$.category') = 'electronics'
  `
});
```

### ContextCore MCP - 搜尋日誌

```javascript
// 1. 新增日誌
await mcp.callTool("add_log", {
  title: "實現用戶註冊功能",
  content: "使用 Spring Security + JWT...",
  tags: ["auth", "backend"],
  module: "user-service",
  type: "feature"
});

// 2. 搜尋相關日誌
const logs = await mcp.callTool("search_logs", {
  query: "用戶認證",
  tags: ["auth"],
  limit: 3
});
```

## 🔧 常見問題

### 連線失敗？

1. 檢查資料庫是否正在執行
2. 確認連線參數（host, port, database）
3. 檢查防火牆設定

### 查詢錯誤？

1. 使用參數化查詢（`?` 佔位符）
2. 檢查 SQL 語法
3. 確認資料表存在

### ContextCore MCP 搜尋不到結果？

1. 確認 Ollama 和 Qdrant 已啟動
2. 檢查 Embedding 模型是否下載完成
3. 等待日誌向量化完成（約 1-2 秒）

## 📖 下一步

- **詳細功能** → [PostgreSQL MCP](mcp-servers/postgresql-mcp.md)
- **常見問題** → [FAQ](guides/FAQ.md)
- **架構設計** → [ARCHITECTURE](advanced/ARCHITECTURE.md)

## 🛡️ 安全建議

- ✅ 使用環境變數儲存密碼
- ✅ 啟用只讀模式測試
- ✅ 定期檢查日誌

---

**遇到問題？** → [FAQ](guides/FAQ.md) | Email: a910413frank@gmail.com
