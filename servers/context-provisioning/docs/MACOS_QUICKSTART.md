# macOS 快速開始指南

在 macOS 上快速啟動 RAG Context Provisioning Server，共有三種方式可選。

---

## 方式 1：使用 Docker Compose（推薦）

### 前置需求

- **Docker Desktop** (包含 Docker 和 Docker Compose)
  - 下載：https://www.docker.com/products/docker-desktop
  - 驗證安裝：
    ```bash
    docker --version
    docker-compose --version
    ```

### 啟動步驟

#### 1️⃣ 進入專案目錄
```bash
cd /path/to/mcp-registry/servers/python/RAG-context-provisioning
```

#### 2️⃣ 啟動服務
```bash
docker-compose up -d
```

**第一次啟動會發生什麼：**
- ✅ 下載 Python 3.11-slim 基礎映像（~50MB）
- ✅ 安裝 Python 依賴（包括 sentence-transformers）
- ✅ 下載 Embedding 模型（~120MB）
- ✅ 初始化 ChromaDB
- ⏳ 預計耗時：5-10 分鐘（取決於網速）

#### 3️⃣ 查看啟動日誌
```bash
# 即時查看日誌
docker-compose logs -f rag-context-provisioning

# 預期的成功日誌輸出：
# [*] Starting MCP Server...
# [*] Listening on 0.0.0.0:3031
# [*] Press Ctrl+C to stop
```

#### 4️⃣ 驗證服務已運行
```bash
# 查看容器狀態
docker-compose ps

# 應該看到：
# NAME                    STATUS
# rag-context-provisioning-server   Up X minutes (healthy)
```

#### 5️⃣ 停止服務
```bash
docker-compose down
```

### 常見問題排查

| 問題 | 原因 | 解決方案 |
|------|------|--------|
| `docker: command not found` | Docker 未安裝 | 下載並安裝 Docker Desktop |
| 容器啟動慢 | 首次下載模型 | 耐心等待 60-120 秒 |
| `Cannot connect to Docker daemon` | Docker 未運行 | 打開 Docker Desktop 應用 |
| Port 3031 被佔用 | 其他服務使用該埠 | 在 docker-compose.yml 修改埠號 |
| 磁碟空間不足 | Embedding 模型占用空間 | 確保有 5GB+ 可用空間 |

---

## 方式 2：本地開發（無需 Docker）

### 前置需求

- **Python 3.11+**
  - 驗證版本：`python3 --version`
  - 下載：https://www.python.org/downloads/

### 最快啟動（推薦）

#### 🚀 一個指令啟動伺服器

```bash
cd /path/to/mcp-registry/servers/python/RAG-context-provisioning
bash start.sh
```

**就這樣！** 腳本會自動：
- ✅ 建立虛擬環境
- ✅ 安裝所有依賴
- ✅ 啟動 MCP Server

首次執行預計 5-10 分鐘（下載 embedding 模型），後續啟動只需 10-20 秒。

### 逐步手動啟動（如果你想瞭解細節）

#### 1️⃣ 進入專案目錄
```bash
cd /path/to/mcp-registry/servers/python/RAG-context-provisioning
```

#### 2️⃣ 建立虛擬環境
```bash
python3 -m venv venv
source venv/bin/activate

# 驗證虛擬環境已啟動（命令提示字應顯示 (venv)）
```

#### 3️⃣ 安裝依賴
```bash
pip install -r requirements.txt
```

#### 4️⃣ 啟動 MCP Server
```bash
python mcp_server.py
```

#### 5️⃣ 停止服務
按 `Ctrl+C` 停止伺服器

### start.sh 腳本做了什麼？

```bash
✓ 檢查 Python 版本
✓ 建立虛擬環境（如果不存在）
✓ 升級 pip
✓ 安裝 requirements.txt 中的所有依賴
✓ 驗證關鍵套件（mcp、chromadb、sentence-transformers）
✓ 啟動 MCP Server
```

**腳本優勢：**
- 一行指令即可執行
- 自動錯誤檢查
- 清晰的彩色輸出
- 能告訴你預計要等多久

### 常見問題排查

| 問題 | 原因 | 解決方案 |
|------|------|--------|
| `command not found: python3` | Python 未安裝 | 下載安裝 Python 3.11+ |
| `No module named 'pip'` | pip 未安裝 | `python3 -m ensurepip --upgrade` |
| `ModuleNotFoundError: No module named 'mcp'` | 依賴未安裝 | 執行 `pip install -r requirements.txt` |
| 模型下載失敗 | 網路問題 | 檢查網路連線，重試 |
| `Port 3031 already in use` | 埠被佔用 | 修改 `mcp_server.py` 中的埠號 |

---

## 方式 3：使用 Docker CLI（高級）

適合需要更細粒度控制的開發者。

### 建置映像

```bash
cd /path/to/mcp-registry/servers/python/RAG-context-provisioning

# 建置映像
docker build -t rag-context-provisioning .

# 驗證建置成功
docker images | grep rag-context-provisioning
```

### 執行容器

```bash
docker run -d \
  --name rag-context-provisioning-server \
  -p 3031:3031 \
  -v $(pwd)/chroma_db:/app/chroma_db \
  -e EMBEDDING_MODEL=paraphrase-multilingual-MiniLM-L12-v2 \
  rag-context-provisioning
```

### 容器管理命令

```bash
# 查看容器日誌
docker logs -f rag-context-provisioning-server

# 進入容器 shell（除錯用）
docker exec -it rag-context-provisioning-server /bin/bash

# 檢查容器資訊
docker inspect rag-context-provisioning-server

# 停止容器
docker stop rag-context-provisioning-server

# 刪除容器
docker rm rag-context-provisioning-server

# 查看所有容器
docker ps -a
```

---

## 驗證安裝成功

無論使用哪種方式啟動，都可以驗證伺服器是否正常運行：

### 1. 檢查服務可達性

```bash
# 在另一個終端執行
curl http://localhost:3031

# 如果服務正常，應該看到回應（可能是 JSON 或文字）
```

### 2. 檢查 MCP Server 狀態

```bash
# 查看伺服器日誌中是否有 "Listening on 0.0.0.0:3031" 的訊息
docker-compose logs | grep "Listening"
# 或本地開發：查看啟動時的輸出日誌
```

### 3. 查看 ChromaDB 資料庫

```bash
# Docker 方式
docker-compose exec rag-context-provisioning python -c "from storage import get_collection; print(get_collection().count())"

# 本地開發方式
python -c "from storage import get_collection; print(get_collection().count())"
```

---

## 常用操作速查表

### Docker Compose 操作

| 操作 | 命令 |
|------|------|
| **啟動** | `docker-compose up -d` |
| **停止** | `docker-compose down` |
| **查看日誌** | `docker-compose logs -f rag-context-provisioning` |
| **重新啟動** | `docker-compose restart` |
| **重新構建** | `docker-compose up -d --build` |
| **查看容器狀態** | `docker-compose ps` |
| **進入容器** | `docker-compose exec rag-context-provisioning /bin/bash` |
| **清除所有資料** | `docker-compose down -v` |

### 本地開發操作

| 操作 | 命令 |
|------|------|
| **一鍵啟動** (推薦) | `bash start.sh` |
| **啟動虛擬環境** | `source venv/bin/activate` |
| **停止虛擬環境** | `deactivate` |
| **安裝依賴** | `pip install -r requirements.txt` |
| **啟動服務** | `python mcp_server.py` |
| **停止服務** | `Ctrl+C` |
| **升級 pip** | `pip install --upgrade pip` |

---

## 資料夾結構說明

```
RAG-context-provisioning/
├── mcp_server.py              # ← MCP Server 入口點（本地開發執行此檔案）
├── app.py                     # FastMCP 應用工廠
├── docker-compose.yml         # ← Docker Compose 配置
├── Dockerfile                 # Docker 映像定義
├── requirements.txt           # Python 依賴清單
├── chroma_db/                 # ChromaDB 資料庫（自動建立）
│   ├── 0/                    # 資料庫內部檔案
│   └── ...
├── controllers/               # MCP 工具實現
├── models/                    # 資料模型
├── services/                  # 業務邏輯服務
├── docs/                      # 文檔（包含本檔案）
└── venv/                      # 虛擬環境（本地開發自動建立）
```

---

## 環境變數配置

### Docker Compose 方式

編輯 `docker-compose.yml`，修改 `environment` 段落：

```yaml
environment:
  - MCP_SERVER_NAME=RAG Context Provisioning Server
  - CHROMA_DB_PATH=/app/chroma_db
  - EMBEDDING_MODEL=all-MiniLM-L6-v2  # 改為快速模型
  - PYTHONUNBUFFERED=1
```

### 本地開發方式

在啟動 `mcp_server.py` 前設定環境變數：

```bash
# Bash / Zsh
export EMBEDDING_MODEL=all-MiniLM-L6-v2
export CHROMA_DB_PATH=./chroma_db
python mcp_server.py

# 或一行設定
EMBEDDING_MODEL=all-MiniLM-L6-v2 python mcp_server.py
```

---

## 效能優化建議

### 如果啟動很慢

1. **使用更小的 Embedding 模型**
   ```yaml
   # 在 docker-compose.yml 或環境變數設定
   EMBEDDING_MODEL=all-MiniLM-L6-v2
   ```
   節省 40MB 下載和初始化時間

2. **使用本地開發（跳過 Docker 開銷）**
   ```bash
   source venv/bin/activate
   python mcp_server.py
   ```

3. **提前預熱模型**
   ```bash
   # 首次下載完模型後，後續啟動會更快
   ```

### 記憶體使用

- **Docker 方式**：~500MB-1GB
- **本地開發方式**：~400MB-800MB

如果記憶體不足：
```yaml
# 在 docker-compose.yml 減少資源限制
deploy:
  resources:
    limits:
      memory: 1G  # 改為 512M
```

---

## 下一步操作

### 1. 測試 MCP Tools

啟動後可以使用以下工具：

```bash
# 儲存文件
store_document(file_path="./README.md", topic="Overview")

# 搜尋知識
search_knowledge(query="如何使用本伺服器？", top_k=3)

# 新增知識點
learn_knowledge(topic="MCP", content="MCP 是 Model Context Protocol...")

# 按主題檢索
retrieve_all_by_topic(topic="Overview")
```

### 2. 整合到 Claude CLI

參考 [claude-code 文檔](https://github.com/anthropics/claude-code) 配置 MCP Server。

### 3. 使用文件索引

```bash
# 如果需要索引更多文檔，使用提供的腳本
cd scripts/
python ingest_ai_docs.py --source /path/to/docs
```

---

## 常見疑問

### Q: Docker 和本地開發方式有什麼差異？

| 特性 | Docker | 本地開發 |
|------|--------|---------|
| **啟動時間** | 5-10 分鐘（首次） | 2-5 分鐘（首次） |
| **系統隔離** | ✅ 完全隔離 | ❌ 影響系統 |
| **效能** | 略低 | 略高 |
| **推薦用途** | 生產部署 | 本地開發 |
| **依賴管理** | 容器內 | 虛擬環境 |

### Q: 可以同時運行多個伺服器嗎？

可以，但需要使用不同的埠和資料庫路徑：

```bash
# 方式 1：修改 docker-compose.yml 中的埠
ports:
  - "3032:3031"  # 使用不同的埠

# 方式 2：本地開發啟動多個終端
terminal1$ python mcp_server.py          # 預設埠 3031
terminal2$ PORT=3032 python mcp_server.py  # 自訂埠 3032
```

### Q: 如何更新到最新版本？

```bash
# Docker 方式
git pull
docker-compose up -d --build

# 本地開發方式
git pull
source venv/bin/activate
pip install -r requirements.txt --upgrade
python mcp_server.py
```

### Q: 資料是否會保留？

✅ 是的，ChromaDB 資料會持久化：
- **Docker**: `./chroma_db/` 目錄中保留
- **本地開發**: `./chroma_db/` 目錄中保留

即使重啟伺服器，資料也會保存。

---

## 進階主題

### 自訂 Embedding 模型

支援的模型列表：
- `all-MiniLM-L6-v2`（推薦，80MB，快速）
- `paraphrase-multilingual-MiniLM-L12-v2`（120MB，多語言）
- `all-mpnet-base-v2`（430MB，準確但慢）

```bash
# 設定不同模型
export EMBEDDING_MODEL=all-MiniLM-L6-v2
python mcp_server.py
```

### 備份與還原

```bash
# 備份資料庫
cp -r chroma_db/ chroma_db.backup/

# 還原資料庫
rm -rf chroma_db/
cp -r chroma_db.backup/ chroma_db/
```

### 連接到外部資料庫

參考 [DOCKER.md](./DOCKER.md) 和 [ARCHITECTURE.md](./ARCHITECTURE.md) 的進階配置部分。

---

## 支援與反饋

遇到問題？參考以下資源：

- 📖 [完整文檔](./README.md)
- 🏗️ [架構說明](./ARCHITECTURE.md)
- 🐳 [Docker 部署指南](./DOCKER.md)
- 💡 [使用範例](./USAGE_EXAMPLES.md)

---

**版本：** 1.0
**最後更新：** 2025-11-24
**適用版本：** v2.0+
