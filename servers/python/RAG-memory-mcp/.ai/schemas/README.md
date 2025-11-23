# Schema 定義目錄

## 概述

這個目錄包含通用的 JSON Schema 定義，用於標準化各種配置和數據結構。Schema 提供了結構驗證和文檔化的雙重功能。

## 通用 Schemas

### 1. workflow-schema.json
定義工作流程（Workflow）的結構。

### 2. ai-config-schema.json
定義 AI 協作配置的結構。

### 3. project-config-schema.json
定義專案配置的結構。

## 使用方式

### 1. 驗證數據
```bash
# 使用 JSON Schema 驗證工具
ajv validate -s workflow-schema.json -d my-workflow.json
```

### 2. 生成文檔
```bash
# 從 schema 生成文檔
json-schema-to-markdown workflow-schema.json > workflow-structure.md
```

### 3. IDE 支援
大多數現代 IDE 支援 JSON Schema：
- VS Code: 在 JSON 文件中添加 `"$schema": "./schema/workflow-schema.json"`
- IntelliJ: 自動識別 schema 文件

## 創建新 Schema

當需要標準化新的數據結構時：

1. 創建 schema 文件：`[name]-schema.json`
2. 定義結構、類型和驗證規則
3. 提供範例和說明
4. 更新此 README

## Schema 設計原則

1. **簡潔性**：只包含必要的驗證規則
2. **可擴展性**：使用 `additionalProperties` 允許擴展
3. **文檔化**：每個字段都包含 `description`
4. **範例**：提供 `examples` 幫助理解

## 技術特定 Schemas

技術特定的 schemas（如 Java、Python 等）應該放在對應的 tech-stack 目錄中：
- `tech-stacks/java-ca-ezddd-spring/schemas/`
- `tech-stacks/python-fastapi/schemas/`
- 等等

## 工具推薦

- **ajv**: JSON Schema 驗證工具
- **json-schema-faker**: 從 schema 生成測試數據
- **quicktype**: 從 schema 生成類型定義