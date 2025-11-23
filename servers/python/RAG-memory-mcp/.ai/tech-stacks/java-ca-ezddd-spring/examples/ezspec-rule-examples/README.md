# ezSpec Rule 範例 (ezSpec Rule Examples)

本目錄展示如何使用 ezSpec 測試框架的 Rule 功能來改善測試程式碼的重用性和可讀性。

## 📁 目錄內容

- **complete-usecase-with-rules.java** - 完整的 Use Case 測試範例
  - 展示如何使用 Rule 來設置測試環境
  - 包含多個測試場景的完整實作
  - 示範 Rule 的組合使用

- **rule-design-before-after.md** - Rule 設計前後對比
  - 傳統測試設置的問題
  - 使用 Rule 後的改進
  - 效益分析和最佳實踐

- **rule-migration-guide.md** - Rule 遷移指南
  - 如何將現有測試重構為使用 Rule
  - 步驟化的遷移流程
  - 常見問題和解決方案

## 🎯 什麼是 ezSpec Rule？

ezSpec Rule 是一種測試設置機制，用於：
1. **重用測試設置** - 將常用的 Given 步驟封裝為可重用的 Rule
2. **提高可讀性** - 使用描述性的 Rule 名稱取代冗長的設置代碼
3. **維護一致性** - 確保測試資料的一致性和正確性

## 📝 Rule 使用範例

### Before (不使用 Rule)
```java
@EzScenario
public void test_create_task_in_project() {
    // Given - 冗長的設置代碼
    Plan plan = new Plan(PlanId.newId(), "Development Plan", "user123");
    plan.createProject(ProjectName.valueOf("Backend"));
    planRepository.save(plan);
    
    CreateTaskInput input = CreateTaskInput.create();
    input.planId = plan.getId();
    input.projectName = ProjectName.valueOf("Backend");
    input.taskName = "Implement API";
    
    // When
    CqrsOutput output = createTaskUseCase.execute(input);
    
    // Then
    assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
}
```

### After (使用 Rule)
```java
@EzScenario
public void test_create_task_in_project() {
    // Given - 使用 Rule 簡化設置
    givenPlanWithProject();
    givenCreateTaskInput("Implement API");
    
    // When
    CqrsOutput output = createTaskUseCase.execute(input);
    
    // Then
    assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
}

// Rule 定義
@Rule("計畫包含專案")
private void givenPlanWithProject() {
    plan = new Plan(PlanId.newId(), "Development Plan", "user123");
    plan.createProject(ProjectName.valueOf("Backend"));
    planRepository.save(plan);
}

@Rule("建立任務輸入")
private void givenCreateTaskInput(String taskName) {
    input = CreateTaskInput.create();
    input.planId = plan.getId();
    input.projectName = ProjectName.valueOf("Backend");
    input.taskName = taskName;
}
```

## 🚀 Rule 設計原則

### 1. 單一職責
每個 Rule 應該只負責一個特定的設置任務。

### 2. 描述性命名
使用業務語言命名 Rule，而非技術實作細節。

### 3. 參數化設計
```java
@Rule("計畫包含N個專案")
private void givenPlanWithProjects(int count) {
    plan = new Plan(PlanId.newId(), "Test Plan", "user123");
    for (int i = 0; i < count; i++) {
        plan.createProject(ProjectName.valueOf("Project " + i));
    }
    planRepository.save(plan);
}
```

### 4. Rule 組合
```java
@EzScenario
public void test_complex_scenario() {
    // 組合多個 Rule
    givenPlanWithProject();
    givenProjectWithTasks(3);
    givenUserPermissions("admin");
    
    // When & Then...
}
```

## 📊 效益分析

### 使用 Rule 前
- ❌ 重複的設置代碼
- ❌ 測試難以理解
- ❌ 維護成本高
- ❌ 容易出錯

### 使用 Rule 後
- ✅ 代碼重用性高
- ✅ 測試意圖清晰
- ✅ 集中管理測試資料
- ✅ 減少錯誤

## 🔧 最佳實踐

1. **Rule 分層**
   ```
   基礎 Rule → 組合 Rule → 場景 Rule
   ```

2. **Rule 庫**
   - 建立共用的 Rule 基類
   - 按領域分組 Rule
   - 維護 Rule 文檔

3. **Rule 命名規範**
   - `given[業務狀態]()` - 設置初始狀態
   - `and[附加條件]()` - 添加額外條件
   - `with[參數描述]()` - 帶參數的設置

## 📚 相關資源
- [ezSpec 官方文檔](../reference/ezspec-test-template.md)
- [測試設計模式](../test/README.md)
- [BDD 最佳實踐](../../best-practices.md)