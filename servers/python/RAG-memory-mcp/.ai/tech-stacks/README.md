# 技術棧模板庫

## 概述

這個目錄包含各種技術棧的專門指導，讓 AI 助手能夠快速理解和應用特定的技術組合。

## 可用的技術棧

### 1. [java-ca-ezddd-spring](./java-ca-ezddd-spring/)
**技術組合**: Java 21 + Clean Architecture + ezddd Framework + Spring Boot
**來源**: DDD 和事件溯源的最佳實踐
**適用場景**: 企業級應用、複雜業務邏輯、事件驅動架構

### 2. [react-typescript](./react-typescript/) *(待建立)*
**技術組合**: React + TypeScript + Tailwind CSS
**適用場景**: 現代前端應用、SPA、組件化開發

### 3. [node-express-mongo](./node-express-mongo/) *(待建立)*
**技術組合**: Node.js + Express + MongoDB
**適用場景**: REST API、微服務、快速原型開發

## 如何使用

### 1. 在新專案中引入技術棧
```bash
# 複製整個 .ai 目錄到新專案
cp -r .ai /path/to/new-project/

# 在 CLAUDE.md 中指定技術棧
tech_stack: "java-ca-ezddd-spring"
```

### 2. AI 自動載入相應指南
當 AI 讀取 `CLAUDE.md` 時，會自動載入對應的技術棧指南。

### 3. 結合通用 Workflow 使用
技術棧指南會調整 Workflow 的執行方式，使其符合特定技術的最佳實踐。

## 技術棧模板結構

每個技術棧應包含：
```
tech-stack-name/
├── README.md                    # 技術棧概述
├── project-config-template.json # 技術棧專屬配置模板（如需要）
├── coding-guide.md              # AI 編碼指南（類似 AI-CODING-GUIDE.md）
├── examples/                    # 設計模式與實作範例（整合版）
├── schemas/                     # 配置和資料結構 schema
│   └── project-config-schema.json
└── anti-patterns.md             # 需要避免的做法
```

### 專案配置模板

各技術棧可提供專屬的 `project-config-template.json`，包含：
- 技術棧特定的版本管理（如 Maven、npm、pip）
- 相關依賴和插件配置
- 建置工具設定
- 其他技術棧特定配置

使用者可選擇：
1. 使用通用模板：`.ai/templates/project-config-template.json`
2. 使用技術棧專屬模板：`.ai/tech-stacks/[tech-stack]/project-config-template.json`

## 貢獻新技術棧

1. 在此目錄創建新資料夾
2. 遵循標準結構
3. 提供完整的範例
4. 測試 AI 能否正確理解和應用