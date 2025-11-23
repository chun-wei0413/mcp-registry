# Code Review Standard Operating Procedure (SOP)

## 🎯 目標：避免表面審查，確保深度檢查

## 📋 Code Review 執行步驟

### Step 1: 自動化檢查（但不能只依賴它）
```bash
# 執行完整的 code review
.ai/scripts/code-review.sh [component-name]

# 如果是 Aggregate，額外執行
.ai/scripts/check-event-sourcing-patterns.sh
```

### Step 2: 架構原則檢查（人工審查）

#### 🔴 Event Sourcing 檢查
參考：`.ai/checklists/EVENT-SOURCING-REVIEW-CHECKLIST.md`

1. **建構子審查**
   - [ ] 業務建構子是否直接設定狀態？（不應該）
   - [ ] ES 建構子是否呼叫 super(events)？（應該）
   - [ ] 是否有重複的狀態設定？

2. **狀態管理審查**
   - [ ] 狀態是否只在 when() 方法中設定？
   - [ ] 是否有其他地方修改狀態？

#### 🟡 DDD 原則檢查
1. **Aggregate 邊界**
   - [ ] 是否維護了正確的邊界？
   - [ ] 是否有跨 Aggregate 的直接引用？

2. **Value Objects**
   - [ ] 是否為不可變？
   - [ ] 是否有 valueOf() 方法？

#### 🟢 Clean Architecture 檢查
1. **依賴方向**
   - [ ] 是否只向內依賴？
   - [ ] 是否有違反層次的依賴？

### Step 3: 程式碼品質檢查

1. **重複程式碼**
   - [ ] 是否有重複的邏輯？
   - [ ] 是否可以抽取共用方法？

2. **錯誤處理**
   - [ ] 是否有適當的錯誤處理？
   - [ ] 錯誤訊息是否明確？

### Step 4: 測試覆蓋檢查

1. **單元測試**
   - [ ] 是否有對應的測試？
   - [ ] 測試是否覆蓋主要場景？

2. **邊界條件**
   - [ ] 是否測試了邊界條件？
   - [ ] 是否測試了錯誤情況？

## 🚨 Red Flags（看到這些要特別警覺）

### Event Sourcing Red Flags
- 🔴 建構子中有 `this.field = value`
- 🔴 when() 方法外有狀態修改
- 🔴 直接呼叫 when() 而非 apply()
- 🔴 狀態被設定多次

### Architecture Red Flags
- 🔴 Repository 有自定義方法
- 🔴 Service 使用 @Component
- 🔴 Aggregate 依賴外部服務
- 🔴 跨層的不當依賴

## 📝 Review 報告模板

```markdown
## Code Review Report: [Component Name]

### ✅ Strengths
- [列出做得好的地方]

### ❌ Critical Issues
- [列出必須修復的問題]

### ⚠️ Warnings
- [列出應該改善的地方]

### 📊 Score: X/10

### 🔧 Action Items
1. [具體的修復建議]
2. [改進方向]
```

## 🎯 記住：不是能跑就是對的！

> "Code Review 不是跑跑腳本就好，要真正理解架構原則，要有勇氣指出問題。"