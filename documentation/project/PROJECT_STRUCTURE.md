# MCP Registry 專案結構

本文檔說明 MCP Registry 專案的目錄結構和檔案組織方式。

## 📁 專案目錄結構

```
mcp-registry/
├── 📁 src/                          # 原始碼目錄
│   ├── 📁 postgresql_mcp/           # PostgreSQL MCP Server 實現
│   │   ├── __init__.py
│   │   ├── postgresql_server.py     # 主要服務器類
│   │   ├── 📁 db/                   # 資料庫相關模組
│   │   │   ├── connection_manager.py
│   │   │   ├── query_executor.py
│   │   │   └── schema_inspector.py
│   │   ├── 📁 tools/                # MCP 工具實現
│   │   │   ├── connection_tools.py
│   │   │   ├── query_tools.py
│   │   │   └── schema_tools.py
│   │   └── 📁 models/               # 資料模型
│   │       ├── connection_models.py
│   │       └── query_models.py
│   │
│   └── 📁 mysql_mcp/               # MySQL MCP Server 實現
│       ├── __init__.py
│       ├── mysql_server.py         # 主要服務器類
│       ├── 📁 db/                  # 資料庫相關模組
│       │   ├── connection_manager.py
│       │   ├── query_executor.py
│       │   └── schema_inspector.py
│       ├── 📁 tools/               # MCP 工具實現
│       │   ├── connection_tools.py
│       │   ├── query_tools.py
│       │   └── schema_tools.py
│       └── 📁 models/              # 資料模型
│           ├── connection_models.py
│           └── query_models.py
│
├── 📁 testing/                     # 測試相關檔案
│   ├── TEST_SCENARIOS.md           # 詳細測試場景文檔
│   ├── TESTING_GUIDE.md            # 測試使用指南
│   ├── test_config.json            # 測試配置檔案
│   ├── run_test_scenarios.py       # 自動化測試腳本
│   ├── interactive_test.py         # 互動式測試工具
│   └── quick_test.py               # 快速測試指令產生器
│
├── 📁 deployment/                  # 部署相關檔案
│   ├── DOCKER_HUB_USAGE.md         # Docker Hub 使用指南
│   ├── docker-compose.yml          # Docker Compose 配置
│   ├── 📁 test-postgres-mcp/       # PostgreSQL MCP 測試容器
│   │   ├── README.md
│   │   └── docker-compose.test.yml
│   └── 📁 test-mysql-mcp/          # MySQL MCP 測試容器
│       ├── README.md
│       └── docker-compose.test.yml
│
├── 📁 scripts/                     # 執行腳本
│   ├── run_postgres_mcp.py         # PostgreSQL MCP Server 啟動腳本
│   └── run_mysql_mcp.py            # MySQL MCP Server 啟動腳本
│
├── 📁 docs/                        # 文檔目錄
│   ├── README.md                   # 文檔總覽
│   ├── ARCHITECTURE.md             # 架構設計文檔
│   ├── PROJECT_STRUCTURE.md        # 專案結構說明
│   ├── MODULE_SPECIFICATIONS.md    # 模組規格說明
│   ├── MCP_SERVER_HANDBOOK.md      # MCP Server 使用手冊
│   ├── DOCKER_HUB_GUIDE.md        # Docker Hub 指南
│   └── USE_CASES.md               # 使用案例說明
│
├── 📁 tests/                       # 單元測試 (預留)
│   ├── test_postgresql_mcp/
│   └── test_mysql_mcp/
│
├── 📄 pyproject.toml               # Python 專案配置
├── 📄 README.md                    # 專案說明文檔
├── 📄 CLAUDE.md                    # Claude Code 開發指南
├── 📄 QUICK_START.md              # 快速開始指南
├── 📄 QA.md                       # 常見問題
├── 📄 database-summary-mcp.md     # 資料庫摘要 MCP 文檔
├── 📄 RELEASE_NOTES_v0.2.0.md     # v0.2.0 版本發布說明
└── 📄 RELEASE_NOTES_v0.4.0.md     # v0.4.0 版本發布說明
```

## 🗂️ 目錄說明

### `/src/` - 原始碼目錄
包含所有 MCP Server 的核心實現程式碼：

- **`postgresql_mcp/`**: PostgreSQL MCP Server 的完整實現
- **`mysql_mcp/`**: MySQL MCP Server 的完整實現
- 每個 MCP Server 都包含：
  - `db/`: 資料庫連線和操作邏輯
  - `tools/`: MCP 工具實現
  - `models/`: 資料模型定義

### `/testing/` - 測試目錄
包含所有測試相關的檔案和工具：

- **測試文檔**: 詳細的測試場景和使用指南
- **測試工具**: 自動化、互動式和快速測試工具
- **測試配置**: 測試用的連線參數和資料配置

### `/deployment/` - 部署目錄
包含部署和容器化相關的檔案：

- **Docker 配置**: Docker Compose 檔案和容器配置
- **部署文檔**: Docker Hub 使用指南
- **測試容器**: 獨立的 Docker 測試環境

### `/scripts/` - 腳本目錄
包含各種執行和管理腳本：

- **啟動腳本**: MCP Server 的本地執行腳本
- **管理腳本**: 專案管理和維護工具

### `/docs/` - 文檔目錄
包含所有技術文檔和說明：

- **設計文檔**: 架構設計和模組規格
- **使用手冊**: 操作指南和最佳實務
- **開發文檔**: 開發者指南和 API 說明

## 🎯 檔案命名規範

### Python 檔案
- **模組**: `snake_case.py` (例: `connection_manager.py`)
- **類別**: `ClassName` (例: `PostgreSQLMCPServer`)
- **函數**: `function_name()` (例: `execute_query()`)

### 文檔檔案
- **README**: `README.md`
- **指南類**: `*_GUIDE.md` (例: `TESTING_GUIDE.md`)
- **說明類**: `*_NOTES.md` (例: `RELEASE_NOTES_*.md`)
- **規格類**: `*_SPECIFICATIONS.md`

### 配置檔案
- **Python**: `pyproject.toml`
- **Docker**: `docker-compose.yml`, `Dockerfile`
- **測試**: `test_config.json`

## 🔄 目錄遷移說明

### 已完成的整理

1. **測試檔案整理**:
   ```
   測試相關檔案 → /testing/
   - TEST_SCENARIOS.md
   - TESTING_GUIDE.md
   - test_config.json
   - *.py (測試腳本)
   ```

2. **部署檔案整理**:
   ```
   部署相關檔案 → /deployment/
   - DOCKER_HUB_USAGE.md
   - docker-compose.yml
   - test-*-mcp/ (測試容器)
   ```

3. **腳本檔案整理**:
   ```
   執行腳本 → /scripts/
   - run_postgres_mcp.py
   - run_mysql_mcp.py
   ```

## 🚀 如何使用新結構

### 執行測試
```bash
# 快速測試
python testing/quick_test.py --all

# 互動式測試
python testing/interactive_test.py

# 自動化測試
python testing/run_test_scenarios.py
```

### 啟動 MCP Server
```bash
# PostgreSQL MCP Server
python scripts/run_postgres_mcp.py

# MySQL MCP Server
python scripts/run_mysql_mcp.py
```

### 部署容器
```bash
# 使用 Docker Compose
cd deployment/
docker-compose up -d

# 測試特定服務
cd deployment/test-postgres-mcp/
docker-compose -f docker-compose.test.yml up
```

## 📚 相關文檔

- [測試指南](testing/TESTING_GUIDE.md) - 如何執行和配置測試
- [部署指南](deployment/DOCKER_HUB_USAGE.md) - Docker 部署說明
- [架構設計](docs/ARCHITECTURE.md) - 系統架構說明
- [快速開始](QUICK_START.md) - 快速上手指南

---

這個新的專案結構讓檔案更有組織，便於維護和擴展。每個目錄都有明確的責任範圍，讓開發者能快速找到需要的檔案。