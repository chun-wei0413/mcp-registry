# Docker 環境設定指南

## 目錄結構

```
mcp-contextcore-server/
├── docker-compose.yml          # Docker 服務配置
├── docker-volumes/             # 本地資料儲存目錄
│   ├── ollama/                # Ollama 模型資料 (~1-2 GB)
│   └── qdrant/                # Qdrant 向量資料庫
├── data/                      # SQLite 資料庫
└── logs/                      # 應用程式日誌
```

## 啟動服務

### 1. 啟動 Ollama 和 Qdrant

```bash
cd /Users/frankli/Coding/mcp-registry/mcp-registry-java/mcp-contextcore-server

# 啟動所有服務 (背景執行)
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看即時日誌
docker-compose logs -f
```

預期輸出:
```
NAME                  IMAGE                  STATUS         PORTS
contextcore-ollama    ollama/ollama:latest   Up 10 seconds  0.0.0.0:11434->11434/tcp
contextcore-qdrant    qdrant/qdrant:latest   Up 10 seconds  0.0.0.0:6333-6334->6333-6334/tcp
```

### 2. 下載 Ollama 嵌入模型

```bash
# 方法一: 直接執行 (推薦)
docker exec -it contextcore-ollama ollama pull nomic-embed-text

# 方法二: 進入 container 內部
docker exec -it contextcore-ollama bash
ollama pull nomic-embed-text
exit
```

下載進度:
```
pulling manifest
pulling 970aa74c0a90... 100% ▕████████████████▏ 274 MB
pulling c71d239df917... 100% ▕████████████████▏  11 KB
pulling ce4a164fc046... 100% ▕████████████████▏   17 B
pulling 31df23ea7173... 100% ▕████████████████▏  420 B
verifying sha256 digest
writing manifest
success
```

### 3. 驗證服務運行

```bash
# 檢查 Ollama
curl http://localhost:11434/api/tags

# 預期輸出 (確認模型已下載)
{
  "models": [
    {
      "name": "nomic-embed-text:latest",
      "size": 274302450,
      ...
    }
  ]
}

# 檢查 Qdrant
curl http://localhost:6333/healthz

# 預期輸出
{"title":"healthz","version":"1.x.x"}
```

## 管理介面

### Qdrant Web UI

開啟瀏覽器訪問: http://localhost:6333/dashboard

功能:
- 查看所有 collections
- 瀏覽向量資料
- 執行搜尋測試
- 監控效能指標

## 常用命令

### 查看服務狀態

```bash
# 查看運行中的服務
docker-compose ps

# 查看資源使用情況
docker stats contextcore-ollama contextcore-qdrant
```

### 查看日誌

```bash
# 查看所有服務日誌
docker-compose logs

# 只查看 Ollama 日誌
docker-compose logs ollama

# 只查看 Qdrant 日誌
docker-compose logs qdrant

# 即時追蹤日誌
docker-compose logs -f --tail=100
```

### 重啟服務

```bash
# 重啟所有服務
docker-compose restart

# 只重啟 Ollama
docker-compose restart ollama

# 只重啟 Qdrant
docker-compose restart qdrant
```

### 停止服務

```bash
# 停止服務 (保留資料)
docker-compose stop

# 停止並移除 containers (保留資料)
docker-compose down

# 停止並刪除所有資料 (危險!)
docker-compose down -v
rm -rf docker-volumes/
```

## 測試嵌入功能

### 使用 Ollama 生成嵌入向量

```bash
# 測試嵌入 API
curl http://localhost:11434/api/embeddings -d '{
  "model": "nomic-embed-text",
  "prompt": "Hello, world!"
}'

# 預期輸出: 768 維度的向量
{
  "embedding": [0.123, 0.456, ..., 0.789]
}
```

## 資料備份

### 備份 Qdrant 資料

```bash
# 建立備份目錄
mkdir -p backups/qdrant

# 複製資料
cp -r docker-volumes/qdrant backups/qdrant/backup-$(date +%Y%m%d)

# 或使用 Qdrant 快照功能
curl -X POST http://localhost:6333/collections/contextcore_logs/snapshots
```

### 備份 Ollama 模型

```bash
# 模型檔案位置
ls -lh docker-volumes/ollama/models/

# 備份 (可選,因為可以重新下載)
cp -r docker-volumes/ollama backups/ollama/backup-$(date +%Y%m%d)
```

## 效能調整

### Ollama 記憶體設定

編輯 `docker-compose.yml`:

```yaml
ollama:
  image: ollama/ollama:latest
  environment:
    - OLLAMA_NUM_PARALLEL=2      # 並行請求數
    - OLLAMA_MAX_LOADED_MODELS=1  # 最多載入幾個模型
  deploy:
    resources:
      limits:
        memory: 4G  # 限制記憶體使用
```

### Qdrant 效能設定

```yaml
qdrant:
  image: qdrant/qdrant:latest
  deploy:
    resources:
      limits:
        memory: 2G
        cpus: '2.0'
```

## 疑難排解

### 問題 1: Ollama 無法啟動

```bash
# 檢查日誌
docker-compose logs ollama

# 重新啟動
docker-compose restart ollama
```

### 問題 2: Qdrant 連線錯誤

```bash
# 檢查 port 是否被佔用
lsof -i :6334
lsof -i :6333

# 重新啟動
docker-compose restart qdrant
```

### 問題 3: 磁碟空間不足

```bash
# 檢查 volume 大小
du -sh docker-volumes/*

# Ollama 模型通常約 1-2 GB
# Qdrant 資料視使用量而定
```

### 問題 4: 模型下載失敗

```bash
# 刪除已下載的不完整模型
docker exec -it contextcore-ollama rm -rf /root/.ollama/models/manifests

# 重新下載
docker exec -it contextcore-ollama ollama pull nomic-embed-text
```

## 清理資源

### 完全清理並重新開始

```bash
# 1. 停止並移除 containers
docker-compose down

# 2. 刪除本地資料 (小心!)
rm -rf docker-volumes/ollama
rm -rf docker-volumes/qdrant

# 3. 重新啟動
docker-compose up -d

# 4. 重新下載模型
docker exec -it contextcore-ollama ollama pull nomic-embed-text
```

## 監控建議

### 使用 Docker Stats

```bash
# 即時監控資源使用
docker stats contextcore-ollama contextcore-qdrant
```

預期輸出:
```
CONTAINER           CPU %   MEM USAGE / LIMIT   MEM %   NET I/O
contextcore-ollama  5.0%    1.2GiB / 4GiB      30.0%   1.5MB / 2MB
contextcore-qdrant  2.0%    500MiB / 2GiB      25.0%   500KB / 1MB
```

---

**資料位置**: 所有 Docker 資料都儲存在 `./docker-volumes/` 目錄下,方便管理和備份。
