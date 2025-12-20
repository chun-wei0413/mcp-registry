# MCP Registry

企業級 Model Context Protocol (MCP) Server，提供 RAG 智能知識管理系統。

## 🎯 專案概述

**MCP Registry** 提供生產級 MCP Server，讓 AI 助手（如 Claude、Gemini）能夠管理開發知識：

| MCP Server | 功能 | 狀態 | 用途 |
|-----------|------|------|------|
| **Context Provisioning** | RAG 知識庫 | ✅ 生產就緒 | 專案文件語義搜尋與知識管理 |

## 🏗️ 專案結構

```
mcp-registry/
├── 📄 CLAUDE.md                  # Claude AI 規範
│
├── 📁 servers/                         # MCP Server 實作
│   └── 📁 context-provisioning/                  # Context Provisioning Server（RAG 知識庫）
│       ├── mcp_server.py               # FastMCP 伺服器（4 個工具）
│       ├── storage.py                  # ChromaDB + Sentence Transformers
│       ├── docker-compose.yml          # Docker 部署
│       ├── Dockerfile                  # 容器定義
│       ├── requirements.txt            # Python 依賴
│       └── README.md                   # Context Provisioning 說明
│
└── 📄 README.md                  # 本文件
```

## 🛠️ 技術棧

### Context Provisioning Server（Python RAG）
- **語言**: Python 3.11+
- **框架**: FastMCP (Anthropic 官方 SDK)
- **向量搜尋**: ChromaDB, Sentence Transformers (google/embeddinggemma-300m)
- **資料驗證**: Pydantic
- **部署**: Docker Compose

## 🚀 快速開始

### 方法 1: Docker Compose（推薦）

```bash
cd servers/context-provisioning
docker-compose up -d

# 查看日誌
docker-compose logs -f context-provisioning
```

### 方法 2: 本地開發

```bash
cd servers/context-provisioning
pip install -r requirements.txt
python mcp_server.py
```

## 🔧 核心功能

### Context Provisioning Tools（Python RAG 系統）

#### 文件管理
- `store_document` - 讀取並儲存專案文件（.md, .json, .txt）
- `learn_knowledge` - 手動新增知識點

#### 語義搜尋
- `search_knowledge` - 在知識庫上執行語義搜尋
- `retrieve_all_by_topic` - 按主題檢索所有知識點

**範例**:
```python
# 儲存專案規格
store_document(file_path="./Spec.md", topic="ProjectSpec")

# 查詢 Clean Architecture
search_knowledge(query="Clean Architecture", top_k=5)
# 返回: CA 相關的所有文件內容
```

## 📊 技術亮點

### ✅ RAG 知識管理系統（Context Provisioning）
- **向量語義搜尋** 提升搜尋精準度
- **ChromaDB** 內嵌式向量資料庫（零配置）
- **Sentence Transformers** 本地嵌入模型
- **智能程式碼分離** v2.0（提升精準度 ~40%）

## 🧪 測試

```bash
cd servers/context-provisioning
pytest tests/
```

## 🔐 安全配置

```yaml
# config.yaml
mcp:
  memory:
    database:
      type: chromadb                 # 內嵌式向量資料庫
      path: ./data/chroma
    embedding:
      model: google/embeddinggemma-300m       # 本地嵌入模型
      device: cpu                    # 或 cuda
```

## 📚 詳細文檔

- 📖 **開發規範**: [CLAUDE.md](CLAUDE.md) - 完整開發規範
- 🐍 **Context Provisioning**: [servers/context-provisioning/README.md](servers/context-provisioning/README.md) - Python RAG 系統完整說明

## 🎯 使用場景

### 專案知識管理（Context Provisioning）
```python
# AI 助手儲存專案規格到 RAG 系統
store_document(file_path="./Spec.md")

# AI 助手查詢專案規範
search_knowledge(query="Clean Architecture 原則", top_k=3)
```

## 🔄 架構設計

### Context Provisioning 向量搜尋流程
```
使用者查詢: "如何實現登入功能"
    ↓
SentenceTransformer: 文字 → 向量 [0.12, -0.34, 0.56, ...]
    ↓
ChromaDB: 向量相似度搜尋 (Cosine Similarity)
    ↓
返回 Top-K 最相關文件 + 相似度分數
    ↓
排序結果: [
  {"content": "JWT 登入實現...", "similarity": 0.92},
  {"content": "OAuth2 整合...", "similarity": 0.87},
  ...
]
```

## 📦 專案統計

| 指標 | 數值 |
|------|------|
| **Context Provisioning Python 模組** | 4 |
| **MCP Tools** | 4 |
| **Docker Compose 配置** | 1 |
| **文檔文件** | 5+ |

## 🤝 貢獻

歡迎貢獻！請遵循以下步驟：

1. Fork 此專案
2. 建立特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交變更 (`git commit -m '[Feature Addition] Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 💬 支援與聯繫

- 📧 Email: a910413frank@gmail.com
- 📖 完整規範: [CLAUDE.md](CLAUDE.md)

## 📄 授權

此專案使用 MIT 授權 - 詳見 [LICENSE](LICENSE) 檔案

---

**注意**: 這是一個基於 RAG 的知識管理 MCP Server，設計用於與 LLM 配合使用。請確保在生產環境中正確配置安全設定，特別是知識庫存取權限。

## 🌟 如果這個專案對您有幫助，請給我一個 ⭐！
