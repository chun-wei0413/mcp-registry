# Code Review Checklist 快速索引

## 🚨 當你收到 Code Review 請求時

**格式**: `code review [FileName]`

### ⚠️ 強制執行步驟（不可跳過）

1. **讀取此檔案** - 你正在讀它 ✓
2. **識別檔案類型** - 根據下表找到對應章節
3. **讀取對應章節** - 從 CODE-REVIEW-CHECKLIST.md 讀取完整內容
4. **執行檢查** - 逐項對照，建立檢查結果表格
5. **標記問題等級** - CRITICAL / MUST FIX / SHOULD FIX

### 📋 檔案類型快速對應表

| 檔案模式 | 檢查清單位置 | 檢查重點 | 優先級 |
|---------|------------|---------|--------|
| `**/entity/*Sprint.java`<br>`**/entity/*Product.java`<br>`**/entity/*Aggregate*.java` | [CODE-REVIEW-CHECKLIST.md](../tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md#domain-層檢查)<br>→ **Event Sourcing 合規性檢查** (第 48-156 行) | 1. Constructor 是否直接設定狀態？<br>2. 是否只透過 apply(event) 設定？<br>3. when() 是否為唯一設定狀態的位置？ | **CRITICAL** ⭐⭐⭐ |
| `**/usecase/service/*.java` | [CODE-REVIEW-CHECKLIST.md](../tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md#usecase-層檢查)<br>→ **Use Case Service 實作結構** (第 773-849 行) | 1. Service 是否有 @Component？<br>2. 是否使用 try-catch 結構？<br>3. 是否使用 findById().orElse(null)？ | HIGH |
| `**/controller/*.java` | [CODE-REVIEW-CHECKLIST.md](../tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md#adapter-層檢查)<br>→ **REST API 路徑設計檢查** (第 893-906 行) | 1. 路徑設計是否正確？<br>2. @RequestMapping 使用是否正確？<br>3. 錯誤處理是否完整？ | MEDIUM |
| `**/*Test.java` | [CODE-REVIEW-CHECKLIST.md](../tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md#測試檢查)<br>→ **Use Case 測試規範** (第 1096-1195 行) | 1. Given/When 是否只用 Use Case？<br>2. 是否有 .Execute() 結尾？<br>3. 測試 ID 是否使用 UUID？ | HIGH |
| `**/usecase/port/*.java`<br>(Mapper) | [CODE-REVIEW-CHECKLIST.md](../tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md#mapper-實作檢查)<br>→ **Mapper 套件位置與設計規範** (第 1599-1676 行) | 1. 是否在 usecase.port 套件？<br>2. 方法是否都是 static？<br>3. 是否有 @Component 註解？ | MEDIUM |

### 🔴 Sprint.java 具體檢查步驟（範例）

當收到 `code review Sprint.java` 時：

#### Step 1: 讀取檢查清單
```
我現在要 code review Sprint.java
這是一個 Aggregate Root，根據 CODE-REVIEW-INDEX.md，
我必須先讀取 CODE-REVIEW-CHECKLIST.md 的 "Event Sourcing 合規性檢查" 章節。
```

#### Step 2: 執行 CRITICAL 檢查項目

建立檢查表格：

| 檢查項目 | 結果 | 位置 | 問題描述 |
|---------|------|------|---------|
| Constructor 是否直接設定狀態欄位？ | ❌ FAIL | 61-74行 | 直接賦值 `this.id = sprintId` |
| 是否透過 apply(event) 觸發 when()？ | ✅ PASS | 106行 | 有呼叫 apply(event) |
| 狀態欄位賦值是否只在 when() 中？ | ❌ FAIL | 61-74行 & when() | 狀態被設定兩次 |
| 事件參數是否使用 constructor 參數？ | ❌ FAIL | 74-105行 | 使用 `this.xxx` 而非參數 |
| Collections 是否在宣告時初始化？ | ✅ PASS | N/A | 無 collection 欄位 |

#### Step 3: 總結與評分

**Critical 問題數量**: 3
**評分**: ⭐ (1/5) - MUST FIX IMMEDIATELY

**核心問題**:
違反 Event Sourcing 基本原則 - Constructor 直接設定狀態，導致：
1. 狀態被設定兩次（Constructor + when()）
2. Event Store 重建會失敗
3. 無法保證狀態完全來自事件

**修正建議**:
移除 Constructor 中第 61-74 行的直接賦值，只保留 apply(event) 呼叫。

---

### ⚠️ 違反檢查流程的後果

如果你（AI）在 Code Review 時：
- ❌ 沒有先讀取 CODE-REVIEW-INDEX.md
- ❌ 沒有讀取對應的 CODE-REVIEW-CHECKLIST.md 章節
- ❌ 沒有建立檢查項目對照表
- ❌ 直接給出評價和建議

**後果**：
- Code Review 結果無效（需要重做）
- 可能遺漏關鍵錯誤（如 Sprint.java 的 Event Sourcing 違規）
- 不符合專案規範要求

### 📚 相關文件

- **完整檢查清單**: [CODE-REVIEW-CHECKLIST.md](../tech-stacks/java-ca-ezddd-spring/CODE-REVIEW-CHECKLIST.md)
- **Aggregate 編碼規範**: [aggregate-standards.md](../tech-stacks/java-ca-ezddd-spring/coding-standards/aggregate-standards.md)
- **編碼標準**: [coding-standards.md](../tech-stacks/java-ca-ezddd-spring/coding-standards.md)

---

## 💡 提醒

每次執行 Code Review 前，請先在腦海中默念：

> "我必須先讀取 CODE-REVIEW-INDEX.md，識別檔案類型，讀取對應的檢查清單章節，然後逐項檢查。絕對不能跳過這個流程。"