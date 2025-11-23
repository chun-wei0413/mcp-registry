# 📁 Directory Rules - 目錄使用規則

## 🚨 重要原則：.ai vs .dev 的區別

### `.ai/` 目錄 - 通用 AI Coding 框架
**只能包含**：
- ✅ 可重用於任何專案的內容
- ✅ 通用的編碼標準和最佳實踐
- ✅ 技術棧範本（使用 tw.teddysoft.example 等通用套件名）
- ✅ 通用的工作流程和 prompts
- ✅ 可複製到其他專案的檔案

**絕對不能包含**：
- ❌ 專案名稱（如 AI-SCRUM, AiScrum）
- ❌ 專案特定套件名（如 tw.teddysoft.aiscrum）
- ❌ 專案特定的業務邏輯或領域模型
- ❌ 專案的 ADR、規格、任務
- ❌ 專案特定的測試資料或設定

### `.dev/` 目錄 - 專案特定內容
**應該包含**：
- ✅ AI-SCRUM 專案的所有特定內容
- ✅ 專案的 ADR（架構決策記錄）
- ✅ 領域規格（specs/）
- ✅ 任務定義（tasks/）
- ✅ 專案特定的經驗教訓（lessons/）
- ✅ Code review 報告（reports/）

## 🤖 AI 判斷規則

當 AI 需要創建或修改檔案時，請按以下流程判斷：

### 1. 內容分類判斷
```
問題：這個內容是否可以原封不動地用在其他專案？
├─ 是 → 放在 .ai/
└─ 否 → 放在 .dev/
```

### 2. 具體判斷標準

| 內容類型 | 放置位置 | 範例 |
|---------|---------|------|
| 編碼標準 | `.ai/tech-stacks/*/coding-standards/` | Repository 限制規則 |
| 專案 ADR | `.dev/adr/` | ADR-031-reactor-interface |
| 通用 prompt | `.ai/prompts/` | command-sub-agent-prompt.md |
| 專案規格 | `.dev/specs/` | product-spec.md |
| 工作流程 | `.ai/workflows/` | tdd-implementation.md |
| 任務定義 | `.dev/tasks/` | task-create-product.json |
| 技術範例 | `.ai/tech-stacks/*/examples/` | 使用 tw.teddysoft.example |
| 專案程式碼 | `src/` | 使用 tw.teddysoft.aiscrum |

### 3. 檔案命名規則

#### `.ai/` 目錄檔案
- 使用通用術語（Product, Sprint, Task）
- 避免專案特定術語（AiScrum, PBI）
- 範例程式碼使用 `example` 套件名

#### `.dev/` 目錄檔案
- 可以使用專案特定術語
- 可以包含專案名稱
- 使用實際的套件名稱

## 📝 實際範例

### ❌ 錯誤：將專案特定內容放在 .ai/
```markdown
# 檔案：.ai/guides/AISCRUM-SETUP.md
AiScrum 專案使用 tw.teddysoft.aiscrum 套件...
```

### ✅ 正確：將通用內容放在 .ai/
```markdown
# 檔案：.ai/guides/DDD-PROJECT-SETUP.md
DDD 專案使用 tw.teddysoft.example 套件作為範例...
```

### ✅ 正確：將專案特定內容放在 .dev/
```markdown
# 檔案：.dev/docs/AISCRUM-ARCHITECTURE.md
AiScrum 專案採用 tw.teddysoft.aiscrum 套件...
```

## 🔍 檢查命令

```bash
# 檢查 .ai/ 是否包含專案特定內容
grep -r "aiscrum\|AiScrum\|AI-SCRUM" .ai/ --exclude-dir=.git

# 如果有結果，表示違反規則，需要移到 .dev/ 或改為通用內容
```

## 🚀 AI 行動準則

1. **創建新檔案前**：
   - 先判斷是通用還是專案特定
   - 選擇正確的目錄

2. **修改現有檔案時**：
   - 如果在 `.ai/` 發現專案特定內容，提醒使用者
   - 建議移動到 `.dev/` 或改為通用版本

3. **Code Review 時**：
   - 檢查是否有檔案放錯位置
   - 提醒違反目錄規則的情況

## 📋 Quick Checklist

創建或修改檔案前，AI 應該問自己：
- [ ] 這個內容提到專案名稱嗎？→ 是：放 .dev/
- [ ] 這個內容包含 tw.teddysoft.aiscrum 嗎？→ 是：放 .dev/
- [ ] 這個內容可以給其他專案用嗎？→ 否：放 .dev/
- [ ] 這是 ADR、spec、task 嗎？→ 是：放 .dev/
- [ ] 這是通用的編碼規範嗎？→ 是：放 .ai/

---
最後更新：2025-09-03