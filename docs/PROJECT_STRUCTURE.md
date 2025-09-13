# PostgreSQL MCP Server 專案結構說明

本文件說明整理後的專案目錄結構，讓開發者能夠快速了解各檔案的用途與位置。

## 📁 目錄結構概覽

```
postgresql-mcp-server/
├── 📁 src/                     # 核心程式碼
│   ├── 📁 tools/              # MCP 工具實現
│   ├── 📁 db/                 # 資料庫操作層
│   ├── 📁 models/             # 資料模型定義
│   ├── 🐍 server.py           # MCP Server 主程式
│   ├── 🛡️ security.py          # 安全驗證模組
│   ├── 📊 monitoring.py       # 監控與健康檢查
│   └── ⚙️ config.py           # 配置管理
├── 📁 deployment/             # 部署相關檔案
│   ├── 📁 docker/            # Docker 配置
│   │   ├── 🐳 Dockerfile          # 生產環境映像
│   │   ├── 🐳 Dockerfile.dev      # 開發環境映像
│   │   ├── 🐳 docker-compose.yml  # 生產部署配置
│   │   ├── 🐳 docker-compose.dev.yml # 開發環境配置
│   │   └── 📄 .dockerignore       # Docker 忽略檔案
│   └── 📁 db/                # 資料庫初始化
│       ├── 🗃️ init-db.sql         # 生產資料庫初始化
│       ├── 🗃️ init-db-dev.sql     # 開發資料庫初始化
│       └── 🗃️ init-db-test.sql    # 測試資料庫初始化
├── 📁 docs/                   # 文件目錄
│   ├── 📚 README.md           # 文件導覽中心
│   ├── 📖 MCP_SERVER_HANDBOOK.md # 完整使用手冊
│   ├── 📋 PROJECT_STRUCTURE.md   # 專案結構說明 (本文件)
│   └── 📁 guides/            # 使用指南
│       └── 📋 USER_GUIDE.md      # 詳細技術指南
├── 📁 tests/                  # 測試套件
│   ├── 📁 unit/              # 單元測試
│   │   ├── 🧪 test_security.py    # 安全模組測試
│   │   ├── 🧪 test_query_tool.py  # 查詢工具測試
│   │   └── 🧪 test_monitoring.py  # 監控模組測試
│   ├── 📁 integration/       # 整合測試
│   │   └── 🧪 test_server_integration.py # 伺服器整合測試
│   └── ⚙️ conftest.py         # pytest 配置與共用 fixtures
├── 📁 scripts/                # 部署與開發腳本
│   ├── 🚀 deploy.sh          # 生產部署腳本
│   └── 🔧 dev.sh             # 開發環境腳本
├── 📄 README.md              # 專案主要說明文件
├── 📄 pyproject.toml         # Python 專案配置
├── 📄 LICENSE                # MIT 授權條款
├── 📄 CLAUDE.md              # Claude Code 開發指南
├── 📄 .env.example           # 環境變數範例
├── 📄 .gitignore             # Git 忽略檔案設定
└── 🧪 run_tests.py           # 測試執行器
```

## 📂 詳細說明

### 🐍 src/ - 核心程式碼

#### 主要模組
- **`server.py`**: MCP Server 主程式，整合所有功能模組
- **`security.py`**: 安全驗證，SQL 注入防護，查詢限制
- **`monitoring.py`**: 系統監控，健康檢查，性能指標收集
- **`config.py`**: 配置管理，環境變數處理

#### tools/ - MCP 工具實現
- **`connection.py`**: 資料庫連線管理，連線池操作
- **`query.py`**: 查詢執行工具，事務處理，批次操作
- **`schema.py`**: 資料庫結構檢查，表格資訊獲取

#### db/ - 資料庫操作層
- **`pool.py`**: 連線池管理，連線生命週期
- **`executor.py`**: SQL 執行器，查詢優化

#### models/ - 資料模型
- **`types.py`**: Pydantic 資料模型定義，型別驗證

### 🚀 deployment/ - 部署配置

#### docker/ - Docker 相關
- **生產環境**:
  - `Dockerfile`: 優化的生產映像
  - `docker-compose.yml`: 生產部署配置
- **開發環境**:
  - `Dockerfile.dev`: 包含開發工具的映像
  - `docker-compose.dev.yml`: 開發環境配置，支援熱重載

#### db/ - 資料庫初始化
- **`init-db.sql`**: 生產環境資料庫初始化
- **`init-db-dev.sql`**: 開發環境測試資料
- **`init-db-test.sql`**: 單元測試資料庫

### 📚 docs/ - 文件系統

#### 文件層級
1. **`README.md`**: 文件導覽中心，快速導引
2. **`MCP_SERVER_HANDBOOK.md`**: 完整使用手冊 (推薦新手閱讀)
3. **`guides/USER_GUIDE.md`**: 技術詳細指南
4. **`PROJECT_STRUCTURE.md`**: 專案結構說明 (本文件)

#### 文件特色
- **分層設計**: 從簡介到深入的漸進式文件
- **場景驅動**: 提供實際使用案例和範例
- **完整涵蓋**: 從安裝到進階客製化的全面指導

### 🧪 tests/ - 測試套件

#### 測試分類
- **`unit/`**: 單元測試，測試個別模組功能
- **`integration/`**: 整合測試，測試模組間協作
- **`conftest.py`**: pytest 配置，共用測試設備

#### 測試覆蓋
- 安全驗證邏輯
- 查詢執行功能
- 監控與健康檢查
- 伺服器整合流程

### 🔧 scripts/ - 自動化腳本

#### 部署腳本
- **`deploy.sh`**: 生產部署自動化
  - 環境檢查
  - 映像建置
  - 服務部署
  - 健康檢查
  - 狀態報告

- **`dev.sh`**: 開發環境管理
  - 開發服務啟動
  - 測試資料庫管理
  - 日誌監控
  - 資料庫連線

## 🎯 使用指南

### 新手入門
1. 📚 閱讀 [文件中心](README.md)
2. 🚀 參考 [完整使用手冊](MCP_SERVER_HANDBOOK.md)
3. 💻 使用 `./scripts/deploy.sh` 快速部署

### 開發者
1. 🔧 使用 `./scripts/dev.sh start` 啟動開發環境
2. 🧪 執行 `python run_tests.py all` 運行測試
3. 📖 參考 [使用者指南](guides/USER_GUIDE.md) 了解 API 細節

### 維運人員
1. 🚀 使用 `./scripts/deploy.sh deploy` 部署生產環境
2. 🔍 使用 `./scripts/deploy.sh status` 檢查服務狀態
3. 📊 使用 `curl http://localhost:3000/health` 監控健康狀態

## 🌟 專案特色

### 清晰的模組分離
- **工具層**: MCP 工具實現，專注於功能提供
- **安全層**: 獨立的安全驗證，可複用的安全規則
- **監控層**: 完整的可觀測性，便於維運監控

### 完善的部署支援
- **多環境支援**: 開發、測試、生產環境分離
- **自動化腳本**: 一鍵部署，減少人為錯誤
- **健康檢查**: 自動驗證服務狀態

### 豐富的文件系統
- **分層文件**: 滿足不同角色的需求
- **實用範例**: 提供真實場景的使用案例
- **維護友好**: 結構化的文件組織

## 🔄 文件更新

本專案結構文件會隨著專案演進持續更新。如有疑問或建議，請：

- 📧 **聯絡**: a910413frank@gmail.com
- 🐛 **回報問題**: [GitHub Issues](../../issues)
- 💬 **功能討論**: [GitHub Discussions](../../discussions)

---

**💡 提示**: 建議將此文件收藏，作為快速查找檔案位置的參考。