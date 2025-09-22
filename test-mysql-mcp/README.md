# MySQL MCP Server 測試環境

這個資料夾用於測試 MySQL MCP Server 的功能。

## 快速開始

### 使用 Docker Hub 預建映像

```bash
# 拉取 MySQL MCP Server
docker pull russellli/mysql-mcp-server:latest

# 運行容器
docker run -d \
  --name mysql-mcp-test \
  -p 3002:3002 \
  -e MCP_SERVER_PORT=3002 \
  -e MCP_LOG_LEVEL=INFO \
  russellli/mysql-mcp-server:latest
```

### 使用 docker-compose

```yaml
version: '3.8'
services:
  mysql-mcp:
    image: russellli/mysql-mcp-server:latest
    ports:
      - "3002:3002"
    environment:
      - MCP_SERVER_PORT=3002
      - MCP_LOG_LEVEL=INFO
    networks:
      - mcp-network

  mysql-db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=rootpass
      - MYSQL_DATABASE=testdb
      - MYSQL_USER=testuser
      - MYSQL_PASSWORD=testpass
    ports:
      - "3306:3306"
    networks:
      - mcp-network

networks:
  mcp-network:
    driver: bridge
```

## 測試案例

1. **連線測試**: 驗證 MCP Server 能成功連接到 MySQL
2. **查詢測試**: 執行基本的 SELECT、INSERT、UPDATE、DELETE 操作
3. **事務測試**: 測試事務的提交和回滾功能
4. **Schema 檢查**: 獲取表結構和 metadata
5. **批次操作**: 測試批次插入和更新功能

## 使用情境

### 情境 1: 電商數據遷移
- 從舊版 MySQL 遷移到新版本
- 處理商品、訂單、用戶數據的結構轉換
- 自動化數據一致性檢查

### 情境 2: 業務數據分析
- 連接現有 MySQL 業務數據庫
- 生成銷售報表和用戶行為分析
- 自動化 KPI 監控和預警

### 情境 3: 數據同步
- 多個 MySQL 實例之間的數據同步
- 主從複製配置優化
- 跨區域數據一致性保證