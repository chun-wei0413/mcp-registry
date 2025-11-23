# Docker 部署指南

本指南說明如何使用 Docker 和 Docker Compose 部署 RAG Memory MCP Server。

---

## 快速開始

### 使用 Docker Compose（推薦）

```bash
# 1. 啟動服務
docker-compose up -d

# 2. 查看日誌
docker-compose logs -f rag-memory-mcp

# 3. 停止服務
docker-compose down

# 4. 重新建置並啟動
docker-compose up -d --build
```

### 使用 Docker CLI

```bash
# 1. 建置映像
docker build -t rag-memory-mcp .

# 2. 執行容器
docker run -d \
  --name rag-memory-mcp \
  -p 3031:3031 \
  -v $(pwd)/chroma_db:/app/chroma_db \
  -e EMBEDDING_MODEL=paraphrase-multilingual-MiniLM-L12-v2 \
  rag-memory-mcp

# 3. 查看日誌
docker logs -f rag-memory-mcp

# 4. 停止容器
docker stop rag-memory-mcp
docker rm rag-memory-mcp
```

---

## 環境變數配置

### 必要配置

| 環境變數 | 預設值 | 說明 |
|---------|--------|------|
| `CHROMA_DB_PATH` | `/app/chroma_db` | ChromaDB 資料庫路徑 |

### 可選配置

| 環境變數 | 預設值 | 說明 |
|---------|--------|------|
| `EMBEDDING_MODEL` | `paraphrase-multilingual-MiniLM-L12-v2` | Embedding 模型名稱 |
| `COLLECTION_NAME` | `mcp_knowledge_base` | ChromaDB 集合名稱 |
| `MCP_SERVER_HOST` | `0.0.0.0` | Server 監聽位址 |
| `MCP_SERVER_PORT` | `3031` | Server 監聽埠 |

### 配置範例

```yaml
# docker-compose.yml
environment:
  # 使用較快的英文模型
  - EMBEDDING_MODEL=all-MiniLM-L6-v2

  # 自訂資料庫路徑
  - CHROMA_DB_PATH=/data/vector_db

  # 自訂 Server 設定
  - MCP_SERVER_PORT=8080
```

---

## Volume 掛載

### ChromaDB 資料持久化

```yaml
volumes:
  - ./chroma_db:/app/chroma_db
```

**說明：** 將本地 `chroma_db` 目錄掛載到容器，確保向量資料庫在容器重啟後保留。

### 掛載文件供批次索引

```yaml
volumes:
  # 掛載您的文件目錄（唯讀）
  - /path/to/your/docs:/app/docs:ro
  - /path/to/your/notes:/app/notes:ro
```

**使用範例：**
```python
# 透過 MCP Tool 批次索引掛載的資料夾
batch_index_folder(source_dir="/app/docs")
batch_index_folder(source_dir="/app/notes")
```

---

## Health Check

Docker Compose 自動配置健康檢查：

```yaml
healthcheck:
  test: ["CMD", "python", "-c", "from app import create_app; app = create_app(); print('OK')"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

**檢查容器健康狀態：**
```bash
docker-compose ps
# 或
docker inspect rag-memory-mcp-server --format='{{.State.Health.Status}}'
```

---

## 資源限制

### 配置建議

```yaml
deploy:
  resources:
    limits:
      cpus: '2'       # 最多使用 2 個 CPU 核心
      memory: 2G      # 最多使用 2GB 記憶體
    reservations:
      memory: 512M    # 保留 512MB 記憶體
```

### 不同場景的資源配置

| 場景 | CPU | 記憶體 | 說明 |
|------|-----|--------|------|
| **開發測試** | 1 核 | 512MB | 使用 `all-MiniLM-L6-v2` 模型 |
| **小型生產** | 2 核 | 1GB | 使用多語言模型，少量檔案 |
| **中型生產** | 2 核 | 2GB | 推薦配置，處理數百個檔案 |
| **大型生產** | 4 核 | 4GB | 處理數千個檔案，高併發 |

---

## 網路配置

### 預設網路

```yaml
networks:
  mcp-network:
    driver: bridge
```

### 與其他服務整合

```yaml
services:
  rag-memory-mcp:
    # ... 其他配置
    networks:
      - mcp-network
      - shared-network  # 與其他服務共享的網路

  other-service:
    # ... 其他服務配置
    networks:
      - shared-network

networks:
  mcp-network:
    driver: bridge
  shared-network:
    external: true  # 外部網路
```

---

## 常見問題

### Q1: 如何更換 Embedding 模型？

**方式 1：修改 docker-compose.yml**
```yaml
environment:
  - EMBEDDING_MODEL=all-MiniLM-L6-v2
```

**方式 2：使用環境變數檔案**
```bash
# .env
EMBEDDING_MODEL=all-MiniLM-L6-v2
```

然後重新啟動：
```bash
docker-compose down
docker-compose up -d
```

### Q2: 容器啟動慢怎麼辦？

**原因：** 首次啟動需要下載 Embedding 模型（約 120MB）

**解決：**
1. 使用較小的模型：`all-MiniLM-L6-v2`（80MB）
2. 等待 60 秒讓 healthcheck 完成
3. 查看啟動日誌：
   ```bash
   docker-compose logs -f rag-memory-mcp
   ```

### Q3: 如何清除資料庫重新開始？

```bash
# 停止服務
docker-compose down

# 刪除資料庫
rm -rf chroma_db/

# 重新啟動
docker-compose up -d
```

### Q4: 如何進入容器除錯？

```bash
# 執行 shell
docker-compose exec rag-memory-mcp /bin/bash

# 或使用 docker
docker exec -it rag-memory-mcp-server /bin/bash
```

### Q5: 如何升級到新版本？

```bash
# 1. 拉取最新程式碼
git pull

# 2. 重新建置映像
docker-compose build

# 3. 重新啟動服務
docker-compose up -d
```

---

## 監控與日誌

### 查看即時日誌

```bash
# 查看所有日誌
docker-compose logs -f

# 查看最近 100 行
docker-compose logs --tail=100

# 只看錯誤
docker-compose logs | grep ERROR
```

### 日誌格式

```
[*] Initializing services...
    - Database: /app/chroma_db
    - Collection: mcp_knowledge_base
    - Embedding model: paraphrase-multilingual-MiniLM-L12-v2
[OK] Loaded embedding model: paraphrase-multilingual-MiniLM-L12-v2
[OK] Services initialized

[*] Creating MCP Server...
    - Host: 0.0.0.0
    - Port: 3031

[*] Registering controllers...
    - Knowledge tools registered
    - Document tools registered
    - Indexing tools registered
    - Resources registered
[OK] All controllers registered

[*] Starting MCP Server...
[*] Listening on 0.0.0.0:3031
```

---

## 生產部署建議

### 1. 使用 Docker Secrets（敏感資料）

```yaml
# docker-compose.yml
services:
  rag-memory-mcp:
    secrets:
      - db_password
    environment:
      - DB_PASSWORD_FILE=/run/secrets/db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt
```

### 2. 啟用日誌輪轉

```yaml
services:
  rag-memory-mcp:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 3. 設定自動重啟

```yaml
services:
  rag-memory-mcp:
    restart: unless-stopped
```

### 4. 使用 Docker Compose Profiles

```yaml
# docker-compose.yml
services:
  rag-memory-mcp:
    profiles: ["production"]

# 啟動生產環境
docker-compose --profile production up -d
```

---

## 效能優化

### 1. 使用 CPU-only PyTorch

Dockerfile 已配置 CPU-only PyTorch：
```dockerfile
RUN pip install --no-cache-dir torch torchvision torchaudio \
    --index-url https://download.pytorch.org/whl/cpu
```

**節省：** ~1.5GB 空間和 5-10 分鐘建置時間

### 2. Multi-stage Build（未來改進）

```dockerfile
# Stage 1: Build
FROM python:3.11-slim AS builder
# ... 安裝依賴

# Stage 2: Runtime
FROM python:3.11-slim
# ... 只複製必要檔案
```

---

## 相關文件

- [README.md](./README.md) - 專案總覽
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 架構說明
- [docker-compose.yml](./docker-compose.yml) - Compose 配置檔
- [Dockerfile](./Dockerfile) - Docker 映像定義

---

**版本：** 2.0
**最後更新：** 2025-01-19
