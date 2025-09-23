# PostgreSQL MCP Server FAQ

## 📋 用戶提問

### **問題1: PostgreSQL 部署和參數配置**

**Q: 請問部署這個 MCP Server 還需要部署 PostgreSQL 嗎？是的話我需要給予這個 MCP Server 什麼參數？參數是要在部署時給嗎？還是透過 LLM 給即可？**

**A:**
- **部署方式選擇**：
  - **包含 PostgreSQL (推薦)**: 使用我們的 Docker Compose，會自動部署 PostgreSQL
    ```bash
    docker-compose -f deployment/docker/docker-compose.yml up -d
    ```
  - **僅 MCP Server**: 如果你已有 PostgreSQL，可以只部署 MCP Server

- **參數配置方式**：
  ```bash
  # 方式1: 部署時配置 (.env 檔案)
  DB_HOST=localhost
  DB_PORT=5432
  DB_DATABASE=your_database
  DB_USER=your_username
  DB_PASSWORD=your_password

  # 方式2: 透過 LLM 動態給予 (執行時配置)
  await add_connection(
      connection_id="my_db",
      host="localhost",
      database="my_database",
      user="my_user",
      password="my_password"
  )
  ```

- **建議做法**:
  - 固定連線用 `.env` 配置
  - 動態連線透過 LLM 的 `add_connection` 工具

### **問題2: 資料遷移參數**

**Q: 如果我需要從 PostgreSQL table1 的資料 migrate 到 table2，我是不是要給兩個 table 的相關參數？**

**A:**
不用給 table 參數，只需要給**連線參數**。MCP Server 會透過工具自動處理：

```python
# 1. 建立連線 (一次即可)
await add_connection("main_db", host="...", database="...")

# 2. 查看來源表結構
source_schema = await get_table_schema("main_db", "table1")

# 3. 查看目標表結構
target_schema = await get_table_schema("main_db", "table2")

# 4. 提取資料
source_data = await execute_query("main_db", "SELECT * FROM table1")

# 5. 批次插入目標表
await batch_execute("main_db", "INSERT INTO table2 VALUES (...)", data)
```

### **問題3: CREATE/INSERT 權限問題**

**Q: 原本這個 MCP Server 不能使用 INSERT 和 CREATE TABLE 的指令是因為什麼原因，然後後來怎麼解決的？**

**A:**
**問題原因：**
- MCP Server 內建安全驗證機制，預設只允許 SELECT, UPDATE, DELETE 等安全操作
- CREATE 操作被歸類為「危險操作」，預設被阻擋以保護資料庫安全
- 環境變數配置中的 `SECURITY_ALLOWED_OPERATIONS` 沒有包含 CREATE

**解決方法：**
```yaml
# docker-compose.yml 中更新環境變數
environment:
  - SECURITY_ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN,CREATE
  - SECURITY_BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
  - ALLOWED_OPERATIONS=SELECT,INSERT,UPDATE,DELETE,WITH,EXPLAIN,CREATE
  - BLOCKED_KEYWORDS=DROP,TRUNCATE,ALTER
```

**注意事項：**
- 需要重新啟動 Docker 容器讓環境變數生效
- 可以根據需求調整允許的操作類型
- 生產環境建議謹慎開放 CREATE 權限

### **問題4: 跨資料庫遷移設定**

**Q: 如果今天做 data migration 的 source 和 destination 的 DB 不一樣了，那我要怎麼設定資料庫的參數使得 Claude 能夠調度 pg_mcp 去對這兩個 DB 做 data migration？**

**A:**
建立多個連線，每個資料庫使用不同的 connection_id：

```python
# 1. 建立來源資料庫連線
await add_connection(
    connection_id="source_db",
    host="source-server.com",
    port=5432,
    database="production_db",
    user="source_user",
    password="source_password"
)

# 2. 建立目標資料庫連線
await add_connection(
    connection_id="target_db",
    host="target-server.com",
    port=5432,
    database="backup_db",
    user="target_user",
    password="target_password"
)

# 3. 跨資料庫遷移操作
# 從來源讀取資料
source_data = await execute_query("source_db", "SELECT * FROM users")

# 檢查目標表結構
target_schema = await get_table_schema("target_db", "users")

# 寫入目標資料庫
await execute_query("target_db", "INSERT INTO users (...) VALUES (...)")
```

**優勢：**
- 同時管理多個資料庫連線
- 支援不同主機、不同認證的資料庫
- 可以在遷移過程中比較兩邊的資料結構

### **問題5: pg_mcp vs docker exec 的本質差異**

**Q: 我讓 LLM 去使用 pg_mcp 去做 Data migration 和叫 LLM 對 DB 做 docker exec 直接對資料庫下指令有沒有什麼本質上的差別使我一定要透過 pg_mcp？**

**A:**
**本質差異對比：**

| 特性 | pg_mcp 方式 | docker exec 方式 |
|------|-------------|------------------|
| **安全性** | ✅ 參數化查詢防 SQL Injection<br>✅ 權限控制和操作限制 | ❌ 直接執行 SQL，易受攻擊<br>❌ 無內建安全檢查 |
| **錯誤處理** | ✅ 結構化錯誤回應<br>✅ 自動重試和回滾 | ❌ 命令行錯誤，難以解析<br>❌ 手動處理異常 |
| **資料型別** | ✅ 自動型別轉換和驗證<br>✅ JSON 格式標準化 | ❌ 字串解析，易出錯<br>❌ 需手動處理格式 |
| **監控審計** | ✅ 完整查詢日誌和指標<br>✅ 健康檢查和監控 | ❌ 有限的日誌記錄<br>❌ 難以追蹤操作歷史 |
| **連線管理** | ✅ 連線池優化性能<br>✅ 自動連線健康檢查 | ❌ 每次新建連線<br>❌ 無連線池機制 |
| **跨平台** | ✅ 標準 MCP 協定<br>✅ 與任何 MCP 客戶端整合 | ❌ 依賴 Docker 環境<br>❌ 平台綁定 |

**實際範例比較：**

```python
# pg_mcp 方式 (推薦)
result = await execute_query(
    "main_db",
    "SELECT * FROM users WHERE id = $1",
    [user_id]  # 自動防 SQL Injection
)

# docker exec 方式 (不推薦)
command = f"docker exec postgres psql -U user -d db -c \"SELECT * FROM users WHERE id = '{user_id}'\""
# 風險：如果 user_id 包含惡意 SQL，可能導致 SQL Injection
```

**結論：**
pg_mcp 提供了**生產級的安全性、可靠性和可維護性**，而 docker exec 更像是**開發階段的臨時工具**。對於正式的資料遷移任務，強烈建議使用 pg_mcp。

---

## 🤔 常見問題擴展

### **Q3: MCP Server 支援哪些資料庫？**
**A:** 目前專門針對 PostgreSQL 設計，未來可擴展支援 MySQL、SQL Server 等。

### **Q4: 如何確保資料遷移的安全性？**
**A:**
- 內建 SQL 注入防護 (參數化查詢)
- 可設定只讀模式限制操作
- 支援事務回滾機制
- 查詢日誌記錄和審計

### **Q5: 支援多個資料庫同時連線嗎？**
**A:** 是的！可以建立多個連線：
```python
await add_connection("db1", host="server1", database="app1")
await add_connection("db2", host="server2", database="app2")

# 跨資料庫查詢
data1 = await execute_query("db1", "SELECT * FROM users")
data2 = await execute_query("db2", "SELECT * FROM customers")
```

### **Q6: 大量資料遷移會不會有性能問題？**
**A:**
- 支援批次操作 (`batch_execute`)
- 可設定 `fetch_size` 分頁處理
- 異步連線池優化性能
- 建議大表使用分批遷移策略

### **Q7: 如何監控 MCP Server 的運行狀態？**
**A:**
```python
# 健康檢查
health = await health_check()

# 性能指標
metrics = await get_metrics()

# HTTP 端點監控
curl http://localhost:3000/health
curl http://localhost:3000/metrics
```

### **Q8: 支援哪些 SQL 操作？**
**A:**
- **預設支援**: SELECT, INSERT, UPDATE, DELETE, WITH, EXPLAIN
- **可設定阻擋**: DROP, TRUNCATE, ALTER, CREATE 等危險操作
- **安全配置**: 透過環境變數調整允許的操作類型

### **Q9: 如何處理連線失敗或異常？**
**A:**
- 自動重試機制
- 連線池健康檢查
- 事務自動回滾
- 詳細錯誤日誌記錄

### **Q10: 可以在生產環境使用嗎？**
**A:**
是的，具備生產級特性：
- Docker 部署支援
- 完整的安全配置
- 監控和健康檢查
- 錯誤處理和日誌記錄
- 性能優化機制

### **Q11: 如何快速開始使用？**
**A:**
```bash
# 1. 克隆專案
git clone <repository-url>
cd postgresql-mcp-server

# 2. 快速部署
cp .env.example .env
docker-compose -f deployment/docker/docker-compose.yml up -d

# 3. 驗證服務
curl http://localhost:3000/health
```

### **Q12: 遇到問題該如何排除？**
**A:**
1. 查看服務日誌: `docker-compose -f deployment/docker/docker-compose.yml logs -f`
2. 檢查健康狀態: `curl http://localhost:3000/health`
3. 參考 [故障排除指南](../MCP_SERVER_HANDBOOK.md#故障排除)

---

## 📚 更多資源

- 📖 [完整使用手冊](../MCP_SERVER_HANDBOOK.md) - 從入門到進階的完整指南
- 📋 [技術指南](USER_GUIDE.md) - 詳細的 API 參考和技術細節
- 🏗️ [專案結構說明](../project/PROJECT_STRUCTURE.md) - 了解專案組織架構
- 📚 [文件導覽](../README.md) - 文件中心和快速導引

## 📞 需要協助？

- 📧 **Email**: a910413frank@gmail.com
- 🐛 **問題回報**: [GitHub Issues](../../issues)
- 💬 **功能討論**: [GitHub Discussions](../../discussions)