# 文檔一致性檢查指南

本文檔定義了 .ai 目錄中所有文檔的一致性檢查規則和流程。

## 🎯 檢查原則

**每次修改文件後，AI 助手應自動執行以下檢查：**

1. 版本號一致性檢查
2. 範例程式碼一致性檢查
3. 術語和命名一致性檢查
4. 檔案路徑一致性檢查

## 📋 版本號一致性檢查清單

### 核心依賴版本（以 .dev/project-config.json 為準）
需要在以下文件中保持一致：

| 依賴項 | 當前版本 | 檢查文件列表 |
|--------|----------|--------------|
| Spring Boot | 3.5.3 | - .dev/project-config.json<br>- config/VERSION-CONTROL.md<br>- 所有 workflow 文檔 |
| Java | 21 | - .dev/project-config.json<br>- pom.xml<br>- CLAUDE.md |
| ezapp-starter | 3.0.1 | - .dev/project-config.json<br>- config/VERSION-CONTROL.md<br>- NEW-PROJECT-GUIDE.md |
| ezspec | 0.0.5 | - .dev/project-config.json<br>- config/VERSION-CONTROL.md |
| ucontract | 2.0.0 | - .dev/project-config.json<br>- config/VERSION-CONTROL.md<br>- CLAUDE.md |
| JUnit | 5.10.2 | - .dev/project-config.json<br>- 由 Spring Boot 管理 |
| Mockito | 5.11.0 | - .dev/project-config.json<br>- 由 Spring Boot 管理 |

**重要**：`.dev/project-config.json` 是版本號的唯一權威來源！

### 檢查指令
```bash
# 檢查 ezddd 版本一致性
grep -r "ezddd.*3\.[0-9]\.[0-9]" .ai/

# 檢查 Spring Boot 版本
grep -r "Spring Boot.*3\.5\.[0-9]+" .ai/

# 檢查 uContract 版本
grep -r "ucontract.*2\.[0-9]\.[0-9]" .ai/
```

## 🔧 範例程式碼一致性

### AiScrum 專案範例
所有範例都應使用 AiScrum 的領域模型：

| 正確範例 | 避免使用 |
|----------|----------|
| Product | Plan, Order, User |
| Sprint | Iteration, Release |
| ProductBacklogItem (PBI) | Task, Story, Feature |
| CreateProductUseCase | CreatePlanUseCase |
| SprintEvents | TeamEvents |

### 需要檢查的文件
1. workflows/tdd-implementation.md
2. guides/LEARNING-PATH.md
3. guides/NEW-PROJECT-GUIDE.md
4. lessons/*.md
5. 所有 prompt 檔案

### 檢查指令
```bash
# 檢查是否還有舊的 AI-Plan 範例
grep -r "CreatePlan\|CreateUser\|Plan\|Order" .ai/ --include="*.md"

# 檢查是否使用正確的 AiScrum 範例
grep -r "CreateProduct\|Sprint\|ProductBacklogItem" .ai/ --include="*.md"
```

## 📝 術語一致性

### 標準術語對照表
| 正確術語 | 避免使用 |
|----------|----------|
| AiScrum | AI-Plan, AI-PLAN |
| ProductBacklogItem | Task (當指 PBI 時) |
| Task | SubTask (當指 PBI 內的任務時) |
| test-inmemory | testInMemory |
| test-outbox | testOutbox |
| application.yml | application.properties |

## 🗓️ 日期一致性

### 日期格式
- 使用 2024 年，不是 2025 年（除非真的是未來規劃）
- ISO 8601 格式：`2024-08-17T10:30:00+08:00`

### 檢查指令
```bash
# 檢查錯誤的 2025 年日期
grep -r "2025-" .ai/ --include="*.md"
```

## 🚀 執行指令

### 快速檢查指令

#### 完整一致性檢查
```
請執行完整的文檔一致性檢查：
1. 根據 .dev/project-config.json 檢查所有版本號
2. 檢查範例程式碼是否都使用 AiScrum（不是 AI-Plan）
3. 檢查術語一致性
4. 檢查日期格式（2024 不是 2025）
5. 生成檢查報告
```

#### 版本號一致性檢查
```
請檢查以下版本號在所有文檔中的一致性：
- Spring Boot: 3.5.3
- ezapp-starter: 3.0.1
- uContract: 2.0.0
- ezSpec: 0.0.5
報告任何不一致的地方
```

#### 範例程式碼檢查
```
請檢查所有文檔中的範例程式碼：
- 應該使用 Product, Sprint, ProductBacklogItem
- 不應該有 Plan, Order, User
- 列出需要修正的文件
```

### 特定場景檢查

#### 更新版本號後的檢查
```
我剛更新了 [依賴名] 的版本從 [舊版本] 到 [新版本]
請：
1. 更新 .dev/project-config.json
2. 更新 config/VERSION-CONTROL.md（加註警告）
3. 根據 CONSISTENCY-CHECK.md 更新所有相關文檔
4. 執行一致性檢查並報告結果
```

#### 新增文檔後的檢查
```
我新增了文檔 [檔案名]
請檢查：
1. 版本號是否與 .dev/project-config.json 一致
2. 範例是否使用 AiScrum 領域模型
3. 日期是否正確（2024）
4. 術語是否符合標準
```

## 🔄 更新流程

### 當版本號需要更新時：

1. **更新中心配置**
   ```bash
   # 1. 更新 .dev/project-config.json（權威來源）
   # 2. 更新 config/VERSION-CONTROL.md（加註說明）
   ```

2. **執行一致性檢查**
   ```bash
   # 使用上述檢查指令
   ```

3. **批量更新**
   使用 AI 助手執行：
   ```
   請根據 CONSISTENCY-CHECK.md 執行版本號一致性更新：
   - 將 [依賴名] 從 [舊版本] 更新到 [新版本]
   - 檢查並更新所有相關文檔
   ```

## 🤖 自動檢查

### 觸發條件
1. 修改 `.dev/project-config.json`
2. 修改 `config/VERSION-CONTROL.md`
3. 修改任何 workflow 文檔
4. 新增或修改 lessons/ 文檔

### 檢查報告格式

#### 標準檢查報告模板
```markdown
## 文檔一致性檢查報告

### 檢查時間
2024-09-02 HH:MM:SS

### ✅ 通過項目
- Spring Boot 版本: 所有文檔一致 (3.5.3)
- 範例程式碼: 全部使用 AiScrum 領域模型
- 日期格式: 正確使用 2024 年

### ❌ 發現問題
1. **uContract 版本不一致**
   - config/VERSION-CONTROL.md: 1.0.2
   - CLAUDE.md: 2.0.0
   - 建議: 統一為 2.0.0（根據 .dev/project-config.json）

### 🔧 修正建議
1. 更新 config/VERSION-CONTROL.md 第 XX 行
2. 執行版本同步更新

### 📈 檢查統計
- 檢查文件數: 30
- 發現問題數: 1
- 建議優先級: 高
```

## 🛠️ 工具指令

### 使用 grep 批量搜尋
```bash
# 搜尋所有 Spring Boot 版本
grep -r "Spring Boot.*[0-9]\.[0-9]\.[0-9]" .ai/ | grep -v project-config

# 搜尋舊的範例
grep -r "CreatePlan\|AI-Plan\|User\|Order" .ai/ --include="*.md"

# 搜尋錯誤的年份
grep -r "2025-\|2025年" .ai/ --include="*.md"
```

### 使用 sed 批量替換（僅供參考）
```bash
# 替換版本號
find .ai -name "*.md" -exec sed -i '' 's/Spring Boot 2\.7\.[0-9]+/Spring Boot 3.5.3/g' {} \;

# 替換範例程式碼
find .ai -name "*.md" -exec sed -i '' 's/CreatePlan/CreateProduct/g' {} \;

# 修正年份
find .ai -name "*.md" -exec sed -i '' 's/2025-/2024-/g' {} \;
```

## 🚨 常見不一致問題

1. **版本號不同步**
   - 原因：只更新了部分文檔
   - 解決：以 .dev/project-config.json 為準，批量更新

2. **範例程式碼混用**
   - 原因：從舊專案複製貼上
   - 解決：統一使用 AiScrum 領域模型

3. **日期錯誤**
   - 原因：複製模板時未修正日期
   - 解決：全域搜尋 2025 並替換為 2024

4. **uContract 方法名變更**
   - 原因：2.0.0 版本 reject() 改為 ignore()
   - 解決：更新所有相關文檔和程式碼

## 📚 相關文檔

- [專案配置](.dev/project-config.json) - 版本號權威來源
- [版本控制文檔](config/VERSION-CONTROL.md) - 版本說明與警告
- [CLAUDE.md](../CLAUDE.md) - 專案記憶文檔
- [INDEX.md](../INDEX.md) - 文檔索引

---

💡 **記住**: 
- `.dev/project-config.json` 是版本號的唯一權威來源
- 所有範例使用 AiScrum 領域模型（Product, Sprint, PBI）
- 日期使用 2024 年（除非真的是未來規劃）