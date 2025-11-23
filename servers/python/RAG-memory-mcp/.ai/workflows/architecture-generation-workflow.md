# Architecture Generation Workflow

## 目的
為專案自動生成架構文檔，包括系統架構圖、領域模型圖、數據流圖等技術文檔。

## 觸發方式

### 使用 Workflow
```
請套用 architecture-generation-workflow 為我的專案生成架構文檔
```

### 直接指令
```
為專案生成架構文檔
```

## 執行步驟

### 1. 專案分析階段
- 識別專案使用的技術棧
- 分析專案結構和模組
- 識別主要的架構模式（如 MVC、Clean Architecture、Event-Driven 等）
- 收集領域模型資訊

### 2. 架構選擇階段
詢問用戶需要哪些架構文檔：
- [ ] 系統架構圖 (System Architecture)
- [ ] 領域模型圖 (Domain Model) 
- [ ] 數據流圖 (Data Flow)
- [ ] 部署架構 (Deployment)
- [ ] 安全架構 (Security)
- [ ] API 架構 (API Design)
- [ ] 前端架構 (Frontend Architecture)
- [ ] 微服務架構 (Microservices)

### 3. 文檔生成階段

#### 3.1 創建 architecture 目錄
```bash
mkdir -p .ai/architecture
```

#### 3.2 根據選擇生成對應文檔

**系統架構圖 (SYSTEM-ARCHITECTURE.md)**
```markdown
# 系統架構圖

## 整體架構
[Mermaid 圖表展示系統層次和組件關係]

## 技術棧
[列出使用的主要技術]

## 架構模式
[說明採用的架構模式]
```

**領域模型圖 (DOMAIN-MODEL.md)**
```markdown
# 領域模型圖

## 核心領域模型
[Mermaid 類圖展示聚合根、實體、值對象]

## 領域事件
[列出主要的領域事件]

## 聚合邊界
[說明聚合的劃分原則]
```

**數據流圖 (DATA-FLOW.md)**
```markdown
# 數據流圖

## 請求處理流程
[展示從用戶請求到響應的完整流程]

## 事件流
[展示事件的發布和處理流程]
```

### 4. 模板適配階段
根據技術棧調整圖表內容：
- **Java Spring**: 包含 Controller、Service、Repository 層
- **Node.js**: 包含 Routes、Controllers、Models
- **React**: 包含 Components、Hooks、Context
- **微服務**: 包含服務間通信、API Gateway

### 5. 圖表生成規則

#### Mermaid 圖表類型選擇
- **架構層次**: 使用 `graph TB` 或 `graph LR`
- **類關係**: 使用 `classDiagram`
- **流程**: 使用 `sequenceDiagram` 或 `flowchart`
- **狀態**: 使用 `stateDiagram-v2`
- **部署**: 使用 `graph TB` 配合 subgraph

#### 命名規範
- 使用清晰的業務術語
- 保持一致的命名風格
- 添加必要的註釋說明

## 輸出結果

### 目錄結構
```
.ai/
└── architecture/
    ├── README.md              # 架構文檔索引
    ├── SYSTEM-ARCHITECTURE.md # 系統架構
    ├── DOMAIN-MODEL.md        # 領域模型
    ├── DATA-FLOW.md           # 數據流
    ├── DEPLOYMENT.md          # 部署架構
    └── API-DESIGN.md          # API 設計
```

### 使用方式
1. 開發時參考架構設計
2. Code Review 時對照架構
3. 新成員入職時了解系統
4. 架構演進時更新文檔

## 進階功能

### 1. 架構決策記錄 (ADR)
```bash
.ai/architecture/decisions/
├── ADR-001-architecture-style.md
├── ADR-002-database-choice.md
└── ADR-003-api-design.md
```

### 2. 架構演進追蹤
- 版本化架構文檔
- 記錄架構變更歷史
- 標註過時的設計

### 3. 架構驗證
- 檢查實際代碼是否符合架構設計
- 識別架構違規
- 生成架構適應度函數

## 與 Tech Stacks 整合

如果專案使用了 tech-stacks 中的模式：
1. 引用 tech-stack 的架構模式
2. 基於 tech-stack 生成專門的架構圖
3. 標註使用的設計模式

## 範例指令

```
# 為 Java DDD 專案生成完整架構文檔
請套用 architecture-generation-workflow，使用 java-ca-ezddd-spring tech-stack

# 只生成領域模型圖
請為我的專案生成領域模型架構圖

# 生成微服務架構文檔
請生成微服務架構文檔，包含服務間通信和部署架構
```

## 注意事項

1. **保持更新**: 架構文檔應隨專案演進更新
2. **適度詳細**: 不要過度設計，保持實用性
3. **可視化優先**: 使用圖表而非冗長文字
4. **業務導向**: 從業務視角描述技術架構