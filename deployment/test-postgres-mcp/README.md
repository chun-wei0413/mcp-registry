# PostgreSQL MCP Server 測試環境

這個資料夾用於測試 PostgreSQL MCP Server 的功能。

## 快速開始

### 使用 Docker Hub 預建映像

```bash
# 拉取 PostgreSQL MCP Server
docker pull russellli/postgresql-mcp-server:latest

# 運行容器
docker run -d \
  --name postgres-mcp-test \
  -p 3001:3001 \
  -e MCP_SERVER_PORT=3001 \
  -e MCP_LOG_LEVEL=INFO \
  russellli/postgresql-mcp-server:latest
```

### 使用 docker-compose

```yaml
version: '3.8'
services:
  postgres-mcp:
    image: russellli/postgresql-mcp-server:latest
    ports:
      - "3001:3001"
    environment:
      - MCP_SERVER_PORT=3001
      - MCP_LOG_LEVEL=INFO
    networks:
      - mcp-network

  postgres-db:
    image: postgres:15
    environment:
      - POSTGRES_DB=testdb
      - POSTGRES_USER=testuser
      - POSTGRES_PASSWORD=testpass
    ports:
      - "5432:5432"
    networks:
      - mcp-network

networks:
  mcp-network:
    driver: bridge
```

## 測試案例

1. **連線測試**: 驗證 MCP Server 能成功連接到 PostgreSQL
2. **查詢測試**: 執行基本的 SELECT、INSERT、UPDATE、DELETE 操作
3. **事務測試**: 測試事務的提交和回滾功能
4. **Schema 檢查**: 獲取表結構和 metadata
5. **批次操作**: 測試批次插入和更新功能

## 使用情境

### 情境 1: 資料遷移
- 從舊系統遷移數據到新的 PostgreSQL 數據庫
- LLM 分析源資料結構，自動生成遷移腳本
- 透過 MCP 工具執行批次資料轉換

### 情境 2: 資料分析
- 連接現有 PostgreSQL 資料庫
- LLM 根據用戶需求生成複雜查詢
- 自動化報表生成和數據探索

### 情境 3: 數據庫維護
- 定期檢查數據庫健康狀況
- 自動化索引優化建議
- 性能分析和查詢優化