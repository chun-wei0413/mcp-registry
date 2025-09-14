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
3. 參考 [故障排除指南](docs/MCP_SERVER_HANDBOOK.md#故障排除)

---

## 📚 更多資源

- 📖 [完整使用手冊](docs/MCP_SERVER_HANDBOOK.md) - 從入門到進階的完整指南
- 📋 [技術指南](docs/guides/USER_GUIDE.md) - 詳細的 API 參考和技術細節
- 🏗️ [專案結構說明](docs/PROJECT_STRUCTURE.md) - 了解專案組織架構
- 📚 [文件導覽](docs/README.md) - 文件中心和快速導引

## 📞 需要協助？

- 📧 **Email**: a910413frank@gmail.com
- 🐛 **問題回報**: [GitHub Issues](../../issues)
- 💬 **功能討論**: [GitHub Discussions](../../discussions)