# Aggregate Testing 指南

本文件說明如何為 DDD Aggregate 撰寫完整的測試案例。

---

## 測試框架要求

### 🎯 設計意圖 (Why)

**問題**：如何測試 Aggregate 的業務邏輯？

**答案**：使用標準 JUnit 5.x 測試 Aggregate：
1. **純領域物件測試**：Aggregate 不依賴 Spring 或 Repository
2. **3A 模式**：Arrange-Act-Assert 結構清晰
3. **DateProvider 控制時間**：使測試可預測、可重複
4. **Event 驗證**：確保正確的 Domain Events 被發出

### 📋 實作規範 (How)

```java
@DisplayName("Aggregate 測試")
class AggregateTest {

    @BeforeEach
    void setUp() {
        DateProvider.setDate("2025-01-15T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        DateProvider.resetDate();  // ✅ 重要：清理 DateProvider
    }

    @Test
    @DisplayName("測試描述")
    void testName() {
        // Arrange: 準備測試數據
        // Act: 執行業務方法
        // Assert: 驗證結果和 Events
    }
}
```

**重要規範**：
- ✅ 使用標準 JUnit 5.x（不需要 ezSpec BDD）
- ✅ Aggregate 是純 POJO，直接 new 物件測試
- ✅ 使用 DateProvider.setDate() 設定測試時間
- ✅ @AfterEach 中清理 DateProvider
- ✅ 使用 AssertJ 的 assertThat() 驗證
- ❌ 不使用 Repository（Aggregate 測試是單元測試）
- ❌ 不使用 Spring DI（Use Case 測試才需要）

### ✅ 正確範例

```java
package tw.teddysoft.ezkanban.plan.entity;

import org.junit.jupiter.api.*;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import tw.teddysoft.ezkanban.common.entity.DateProvider;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Plan Aggregate 測試")
class PlanTest {

    private Plan plan;
    private final PlanId planId = PlanId.valueOf("plan-123");
    private final String userId = "user-456";

    @BeforeEach
    void setUp() {
        // ✅ 設定可測試的時間
        DateProvider.setDate("2025-01-15T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        // ✅ 清理 DateProvider
        DateProvider.resetDate();
    }

    @Test
    @DisplayName("創建 Plan 應設定初始狀態並發出 PlanCreated Event")
    void should_set_initial_state_and_emit_event_when_created() {
        // Arrange
        String planName = "Sprint Planning";
        Instant testTime = Instant.parse("2025-01-15T10:00:00Z");

        // Act
        plan = new Plan(planId, planName, userId);

        // Assert: 驗證狀態
        assertThat(plan.getPlanId()).isEqualTo(planId);
        assertThat(plan.getName()).isEqualTo(planName);
        assertThat(plan.getUserId()).isEqualTo(userId);
        assertThat(plan.isDeleted()).isFalse();

        // Assert: 驗證 Event
        List<InternalDomainEvent> events = plan.getDomainEvents();
        assertThat(events).hasSize(1);

        var event = events.get(0);
        assertThat(event).isInstanceOf(PlanEvents.PlanCreated.class);
        assertThat(event).isInstanceOf(InternalDomainEvent.ConstructionEvent.class);

        var createdEvent = (PlanEvents.PlanCreated) event;
        assertThat(createdEvent.planId()).isEqualTo(planId);
        assertThat(createdEvent.name()).isEqualTo(planName);
        assertThat(createdEvent.occurredOn()).isEqualTo(testTime);
        assertThat(createdEvent.eventId()).isNotNull();
        assertThat(createdEvent.metadata()).containsEntry("creatorId", userId);
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：在 Aggregate 測試中使用 Repository
@SpringBootTest
class PlanTest {

    @Autowired
    private PlanRepository repository;  // ❌ Aggregate 測試不需要 Repository

    @Test
    void testCreatePlan() {
        Plan plan = new Plan(planId, "name", userId);
        repository.save(plan);  // ❌ 不應該使用 Repository
    }
}

// ✅ 正確：Aggregate 測試是純單元測試
class PlanTest {

    @Test
    void should_create_plan_with_valid_data() {
        // ✅ 直接測試 Aggregate，不需要 Repository
        Plan plan = new Plan(planId, "name", userId);
        assertThat(plan.getName()).isEqualTo("name");
    }
}

// ❌ 錯誤：使用 Instant.now() 而非 DateProvider
@Test
void testCreatePlan() {
    Plan plan = new Plan(planId, "name", userId);

    var event = (PlanEvents.PlanCreated) plan.getLastDomainEvent().get();
    assertThat(event.occurredOn()).isEqualTo(Instant.now());  // ❌ 時間不可預測
}

// ✅ 正確：使用 DateProvider.setDate()
@Test
void should_use_date_provider_for_event_time() {
    Instant testTime = Instant.parse("2025-01-15T10:00:00Z");
    DateProvider.setDate(testTime);

    Plan plan = new Plan(planId, "name", userId);

    var event = (PlanEvents.PlanCreated) plan.getLastDomainEvent().get();
    assertThat(event.occurredOn()).isEqualTo(testTime);  // ✅ 時間可預測
}

// ❌ 錯誤：忘記清理 DateProvider
@Test
void testPlan() {
    DateProvider.setDate("2025-01-15T10:00:00Z");
    // 測試...
    // ❌ 忘記重置，影響其他測試
}

// ✅ 正確：在 @AfterEach 中清理
@AfterEach
void tearDown() {
    DateProvider.resetDate();  // ✅ 確保不影響其他測試
}
```

**症狀**：測試不穩定、時間相關測試失敗、測試之間互相影響。

### 🔍 檢查清單

- [ ] 使用標準 JUnit 5.x（@Test, @BeforeEach, @AfterEach）
- [ ] Aggregate 測試不使用 Spring 或 Repository
- [ ] @BeforeEach 中設定 DateProvider
- [ ] @AfterEach 中重置 DateProvider
- [ ] 使用 @DisplayName 提供中文描述
- [ ] 使用 AssertJ 的 assertThat() 驗證
- [ ] 使用 @Nested 組織相關測試

---

## 基本測試結構

### 🎯 設計意圖 (Why)

**問題**：如何組織 Aggregate 測試？

**答案**：使用 @Nested 組織相關測試：
1. **測試分組**：將相關測試歸類在一起
2. **清晰結構**：易於理解和維護
3. **專注測試**：每個測試只驗證一個行為

### 📋 實作規範 (How)

```java
@DisplayName("Aggregate 測試")
class AggregateTest {

    // 共用的測試數據
    private Aggregate aggregate;

    @BeforeEach
    void setUp() {
        DateProvider.setDate("2025-01-15T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        DateProvider.resetDate();
    }

    @Nested
    @DisplayName("狀態轉換測試")
    class StateTransitionTests {
        // 狀態轉換相關測試
    }

    @Nested
    @DisplayName("邊界條件測試")
    class BoundaryConditionTests {
        // 邊界條件相關測試
    }

    @Nested
    @DisplayName("不變式測試")
    class InvariantTests {
        // 不變式相關測試
    }
}
```

### ✅ 正確範例

```java
@DisplayName("Plan Aggregate 測試")
class PlanTest {

    private Plan plan;
    private final PlanId planId = PlanId.valueOf("plan-123");
    private final String userId = "user-456";

    @BeforeEach
    void setUp() {
        DateProvider.setDate("2025-01-15T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        DateProvider.resetDate();
    }

    @Nested
    @DisplayName("創建測試")
    class CreationTests {

        @Test
        @DisplayName("創建 Plan 應設定初始狀態")
        void should_set_initial_state_when_created() {
            // Arrange
            String planName = "Sprint Planning";

            // Act
            plan = new Plan(planId, planName, userId);

            // Assert
            assertThat(plan.getPlanId()).isEqualTo(planId);
            assertThat(plan.getName()).isEqualTo(planName);
            assertThat(plan.getUserId()).isEqualTo(userId);
            assertThat(plan.getProjects()).isEmpty();
            assertThat(plan.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("創建時名稱為 null 應拋出異常")
        void should_throw_exception_when_name_is_null() {
            // Act & Assert
            assertThatThrownBy(() -> new Plan(planId, null, userId))
                .isInstanceOf(PreconditionViolationException.class)
                .hasMessageContaining("Plan name");
        }
    }

    @Nested
    @DisplayName("修改測試")
    class ModificationTests {

        @BeforeEach
        void setUp() {
            plan = new Plan(planId, "Original Name", userId);
            plan.clearDomainEvents();  // ✅ 清除創建事件，專注測試修改
        }

        @Test
        @DisplayName("重新命名應更新名稱並發出事件")
        void should_update_name_and_emit_event_when_renamed() {
            // Arrange
            String newName = "Updated Name";

            // Act
            plan.rename(newName, userId);

            // Assert
            assertThat(plan.getName()).isEqualTo(newName);

            var event = plan.getLastDomainEvent().orElse(null);
            assertThat(event).isInstanceOf(PlanEvents.PlanRenamed.class);

            var renamedEvent = (PlanEvents.PlanRenamed) event;
            assertThat(renamedEvent.newName()).isEqualTo(newName);
        }
    }

    @Nested
    @DisplayName("軟刪除測試")
    class SoftDeleteTests {

        @BeforeEach
        void setUp() {
            plan = new Plan(planId, "Test Plan", userId);
            plan.clearDomainEvents();
        }

        @Test
        @DisplayName("刪除應設置 deleted 狀態")
        void should_mark_as_deleted_when_delete_called() {
            // Act
            plan.delete(userId);

            // Assert
            assertThat(plan.isDeleted()).isTrue();

            var event = plan.getLastDomainEvent().orElse(null);
            assertThat(event).isInstanceOf(PlanEvents.PlanDeleted.class);
            assertThat(event).isInstanceOf(InternalDomainEvent.DestructionEvent.class);
        }
    }
}
```

### 🔍 檢查清單

- [ ] 使用 @Nested 組織相關測試
- [ ] 每個 @Nested 類別有清晰的 @DisplayName
- [ ] 使用 @BeforeEach 準備測試數據
- [ ] 使用 clearDomainEvents() 清除不相關的事件
- [ ] 每個測試方法只驗證一個行為

---

## 狀態轉換測試

### 🎯 設計意圖 (Why)

**問題**：如何測試 Aggregate 的狀態轉換邏輯？

**答案**：為每個狀態轉換路徑撰寫獨立測試：
1. **完整覆蓋**：測試所有可能的狀態轉換
2. **正常路徑**：驗證業務流程正確執行
3. **回退路徑**：驗證錯誤恢復機制
4. **非法轉換**：驗證錯誤處理

### 📋 實作規範 (How)

```java
@Nested
@DisplayName("狀態轉換測試")
class StateTransitionTests {

    @Test
    @DisplayName("從狀態A到狀態B的轉換")
    void should_transition_from_state_a_to_state_b() {
        // Arrange: 準備初始狀態
        aggregate = createAggregateInStateA();

        // Act: 執行狀態轉換
        aggregate.transitionToStateB();

        // Assert: 驗證新狀態
        assertThat(aggregate.getState()).isEqualTo(StateB);

        // Assert: 驗證 Event
        var event = aggregate.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(StateChangedEvent.class);
    }
}
```

### ✅ 正確範例

```java
@Nested
@DisplayName("狀態轉換測試")
class StateTransitionTests {

    @Test
    @DisplayName("從 BACKLOG 到 IN_PROGRESS 的完整流程")
    void should_transition_through_complete_lifecycle() {
        // BACKLOG → STARTED
        plan = new Plan(planId, "Test Plan", userId);
        assertThat(plan.getState()).isEqualTo(PlanState.BACKLOG);

        plan.start(userId);
        assertThat(plan.getState()).isEqualTo(PlanState.IN_PROGRESS);

        // Verify event
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(PlanEvents.PlanStarted.class);

        var startedEvent = (PlanEvents.PlanStarted) event;
        assertThat(startedEvent.planId()).isEqualTo(planId);
    }

    @Test
    @DisplayName("從 IN_PROGRESS 到 COMPLETED 的轉換")
    void should_transition_to_completed_when_all_work_done() {
        // Arrange
        plan = createPlanInProgress();

        // Act
        plan.complete(userId);

        // Assert
        assertThat(plan.getState()).isEqualTo(PlanState.COMPLETED);

        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(PlanEvents.PlanCompleted.class);

        var completedEvent = (PlanEvents.PlanCompleted) event;
        assertThat(completedEvent.completedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("從 COMPLETED 回退到 IN_PROGRESS")
    void should_regress_to_in_progress_when_work_reopened() {
        // Arrange
        plan = createCompletedPlan();

        // Act
        plan.reopen(userId);

        // Assert
        assertThat(plan.getState()).isEqualTo(PlanState.IN_PROGRESS);

        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(PlanEvents.PlanReopened.class);
    }

    @Test
    @DisplayName("非法狀態轉換應拋出異常")
    void should_throw_exception_for_illegal_transition() {
        // Arrange: BACKLOG 狀態
        plan = new Plan(planId, "Test Plan", userId);

        // Act & Assert: 不能直接從 BACKLOG 完成
        assertThatThrownBy(() -> plan.complete(userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot complete plan in BACKLOG state");
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：測試名稱不清楚
@Test
void testStateChange() {  // ❌ 不知道測試什麼狀態轉換
    plan.start(userId);
    assertThat(plan.getState()).isEqualTo(PlanState.IN_PROGRESS);
}

// ✅ 正確：清楚描述狀態轉換
@Test
@DisplayName("從 BACKLOG 開始計畫應轉換到 IN_PROGRESS 狀態")
void should_transition_to_in_progress_when_started_from_backlog() {
    plan = new Plan(planId, "Test Plan", userId);
    plan.start(userId);
    assertThat(plan.getState()).isEqualTo(PlanState.IN_PROGRESS);
}

// ❌ 錯誤：沒有驗證 Event
@Test
void should_complete_plan() {
    plan = createPlanInProgress();
    plan.complete(userId);
    assertThat(plan.getState()).isEqualTo(PlanState.COMPLETED);  // ❌ 沒驗證 Event
}

// ✅ 正確：同時驗證狀態和 Event
@Test
@DisplayName("完成計畫應轉換狀態並發出 PlanCompleted Event")
void should_transition_and_emit_event_when_completed() {
    plan = createPlanInProgress();

    plan.complete(userId);

    assertThat(plan.getState()).isEqualTo(PlanState.COMPLETED);

    var event = plan.getLastDomainEvent().orElse(null);
    assertThat(event).isInstanceOf(PlanEvents.PlanCompleted.class);
}

// ❌ 錯誤：測試多個轉換但沒有清除事件
@Test
void testMultipleTransitions() {
    plan = new Plan(planId, "Test", userId);
    plan.start(userId);
    plan.complete(userId);

    assertThat(plan.getDomainEvents()).hasSize(3);  // ❌ 混雜了所有事件
}

// ✅ 正確：每個轉換清除不相關的事件
@Test
@DisplayName("完整生命週期轉換")
void should_transition_through_complete_lifecycle() {
    plan = new Plan(planId, "Test", userId);
    plan.clearDomainEvents();  // ✅ 清除創建事件

    plan.start(userId);
    assertThat(plan.getLastDomainEvent().get())
        .isInstanceOf(PlanEvents.PlanStarted.class);
    plan.clearDomainEvents();  // ✅ 清除開始事件

    plan.complete(userId);
    assertThat(plan.getLastDomainEvent().get())
        .isInstanceOf(PlanEvents.PlanCompleted.class);
}
```

### 🔍 檢查清單

- [ ] 測試所有正常狀態轉換路徑
- [ ] 測試回退路徑（如果有）
- [ ] 測試非法狀態轉換並驗證異常
- [ ] 驗證狀態改變
- [ ] 驗證對應的 Domain Event 被發出
- [ ] 使用 clearDomainEvents() 隔離測試

---

## 邊界條件測試

### 🎯 設計意圖 (Why)

**問題**：如何確保 Aggregate 在極端情況下正確運作？

**答案**：測試邊界條件和特殊情況：
1. **空集合**：沒有子實體時的行為
2. **極端值**：最大、最小、零值
3. **臨界狀態**：接近狀態轉換的情況

### 📋 實作規範 (How)

```java
@Nested
@DisplayName("邊界條件測試")
class BoundaryConditionTests {

    @Test
    @DisplayName("空集合的處理")
    void should_handle_empty_collection() {
        // Test behavior with empty collection
    }

    @Test
    @DisplayName("極端值的處理")
    void should_handle_extreme_values() {
        // Test behavior with max/min values
    }
}
```

### ✅ 正確範例

```java
@Nested
@DisplayName("邊界條件測試")
class BoundaryConditionTests {

    @Test
    @DisplayName("沒有專案時也能創建 Plan")
    void should_create_plan_with_empty_projects() {
        // Act
        plan = new Plan(planId, "Empty Plan", userId);

        // Assert
        assertThat(plan.getProjects()).isEmpty();
        assertThat(plan.getProjects()).isNotNull();  // ✅ 空集合，不是 null
    }

    @Test
    @DisplayName("刪除所有專案後應該有空集合")
    void should_have_empty_projects_after_removing_all() {
        // Arrange
        plan = createPlanWithMultipleProjects();
        List<ProjectId> allProjectIds = new ArrayList<>(plan.getProjects().keySet());

        // Act: 刪除所有專案
        for (ProjectId projectId : allProjectIds) {
            plan.removeProject(projectId, userId);
        }

        // Assert
        assertThat(plan.getProjects()).isEmpty();
        assertThat(plan.getProjects()).isNotNull();
    }

    @Test
    @DisplayName("名稱長度達到最大值時應該接受")
    void should_accept_name_at_maximum_length() {
        // Arrange: 200 字元是最大長度
        String maxLengthName = "A".repeat(200);

        // Act
        plan = new Plan(planId, maxLengthName, userId);

        // Assert
        assertThat(plan.getName()).isEqualTo(maxLengthName);
        assertThat(plan.getName().length()).isEqualTo(200);
    }

    @Test
    @DisplayName("名稱長度超過最大值應拋出異常")
    void should_reject_name_exceeding_maximum_length() {
        // Arrange: 超過 200 字元
        String tooLongName = "A".repeat(201);

        // Act & Assert
        assertThatThrownBy(() -> new Plan(planId, tooLongName, userId))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining("Plan name length");
    }

    @Test
    @DisplayName("空字串名稱應拋出異常")
    void should_reject_empty_name() {
        // Act & Assert
        assertThatThrownBy(() -> new Plan(planId, "", userId))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining("Plan name");
    }

    @Test
    @DisplayName("只有空白的名稱應拋出異常")
    void should_reject_whitespace_only_name() {
        // Act & Assert
        assertThatThrownBy(() -> new Plan(planId, "   ", userId))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining("Plan name");
    }
}
```

### 🔍 檢查清單

- [ ] 測試空集合的處理
- [ ] 測試極端值（最大、最小、零）
- [ ] 測試空字串和空白字串
- [ ] 測試臨界值（最大長度、邊界值）
- [ ] 驗證返回空集合而非 null

---

## 不變式測試

### 🎯 設計意圖 (Why)

**問題**：如何確保 Aggregate 的不變式始終成立？

**答案**：驗證 ensureInvariant() 在所有情況下都通過：
1. **創建後**：新建的 Aggregate 滿足不變式
2. **修改後**：每次狀態改變後不變式仍成立
3. **刪除後**：已刪除的 Aggregate 有不同的不變式

### 📋 實作規範 (How)

```java
@Nested
@DisplayName("不變式測試")
class InvariantTests {

    @Test
    @DisplayName("創建後應滿足不變式")
    void should_satisfy_invariants_after_creation() {
        aggregate = new Aggregate(...);
        assertDoesNotThrow(() -> aggregate.ensureInvariant());
    }
}
```

### ✅ 正確範例

```java
@Nested
@DisplayName("不變式測試")
class InvariantTests {

    @Test
    @DisplayName("創建後應滿足所有不變式")
    void should_satisfy_invariants_after_creation() {
        // Act
        plan = new Plan(planId, "Test Plan", userId);

        // Assert: ensureInvariant() 不應拋出異常
        assertDoesNotThrow(() -> plan.ensureInvariant());

        // Verify specific invariants
        assertThat(plan.getCategory()).isEqualTo("Plan");
        assertThat(plan.getPlanId()).isNotNull();
        assertThat(plan.getName()).isNotNull();
        assertThat(plan.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("重新命名後應仍滿足不變式")
    void should_satisfy_invariants_after_rename() {
        // Arrange
        plan = new Plan(planId, "Original Name", userId);

        // Act
        plan.rename("New Name", userId);

        // Assert
        assertDoesNotThrow(() -> plan.ensureInvariant());
        assertThat(plan.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("新增專案後應仍滿足不變式")
    void should_satisfy_invariants_after_adding_project() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);
        ProjectId projectId = ProjectId.valueOf("project-1");

        // Act
        plan.addProject(projectId, "Project 1", userId);

        // Assert
        assertDoesNotThrow(() -> plan.ensureInvariant());
        assertThat(plan.getProjects()).containsKey(projectId);
    }

    @Test
    @DisplayName("刪除後不變式應考慮 isDeleted 狀態")
    void should_have_different_invariants_when_deleted() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);

        // Act
        plan.delete(userId);

        // Assert: 刪除後仍應滿足不變式（但檢查條件不同）
        assertDoesNotThrow(() -> plan.ensureInvariant());
        assertThat(plan.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("違反不變式應拋出 InvariantException")
    void should_throw_invariant_exception_when_violated() {
        // 這個測試展示如何測試不變式檢查
        // 通常透過反射或特殊方法破壞狀態

        plan = new Plan(planId, "Test Plan", userId);

        // 使用反射設定無效狀態（僅用於測試）
        try {
            var field = Plan.class.getDeclaredField("name");
            field.setAccessible(true);
            field.set(plan, null);  // 破壞不變式

            // Assert: ensureInvariant() 應拋出異常
            assertThatThrownBy(() -> plan.ensureInvariant())
                .isInstanceOf(InvariantException.class);
        } catch (Exception e) {
            fail("反射操作失敗: " + e.getMessage());
        }
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：不測試不變式
@Test
void should_rename_plan() {
    plan = new Plan(planId, "Original", userId);
    plan.rename("New Name", userId);
    assertThat(plan.getName()).isEqualTo("New Name");  // ❌ 沒驗證不變式
}

// ✅ 正確：驗證不變式仍然成立
@Test
@DisplayName("重新命名後應滿足不變式")
void should_satisfy_invariants_after_rename() {
    plan = new Plan(planId, "Original", userId);
    plan.rename("New Name", userId);

    assertThat(plan.getName()).isEqualTo("New Name");
    assertDoesNotThrow(() -> plan.ensureInvariant());  // ✅ 驗證不變式
}
```

### 🔍 檢查清單

- [ ] 創建後驗證 ensureInvariant()
- [ ] 每次狀態改變後驗證 ensureInvariant()
- [ ] 驗證刪除狀態的不變式
- [ ] 驗證特定不變式條件（Category, ID, 必要欄位）
- [ ] 使用 assertDoesNotThrow() 驗證

---

## Domain Event 測試

### 🎯 設計意圖 (Why)

**問題**：如何驗證正確的 Domain Events 被發出？

**答案**：驗證 Event 的類型、內容和時間：
1. **Event 類型**：確認發出正確的 Event
2. **Event 內容**：驗證 Event 包含正確的資料
3. **Event 時間**：驗證使用 DateProvider
4. **Event 順序**：驗證 Events 的發出順序

### 📋 實作規範 (How)

```java
@Nested
@DisplayName("Domain Event 測試")
class DomainEventTests {

    @Test
    @DisplayName("操作應發出對應的 Event")
    void should_emit_event_for_operation() {
        // Act
        aggregate.performOperation();

        // Assert
        var event = aggregate.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(ExpectedEvent.class);
        assertThat(event.occurredOn()).isEqualTo(testTime);
    }
}
```

### ✅ 正確範例

```java
@Nested
@DisplayName("Domain Event 測試")
class DomainEventTests {

    @Test
    @DisplayName("PlanCreated event 應包含完整資訊")
    void created_event_should_contain_complete_information() {
        // Arrange
        Instant testTime = Instant.parse("2025-01-15T10:00:00Z");
        DateProvider.setDate(testTime);

        // Act
        plan = new Plan(planId, "Test Plan", userId);

        // Assert
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isNotNull();
        assertThat(event).isInstanceOf(PlanEvents.PlanCreated.class);
        assertThat(event).isInstanceOf(InternalDomainEvent.ConstructionEvent.class);

        var createdEvent = (PlanEvents.PlanCreated) event;
        assertThat(createdEvent.planId()).isEqualTo(planId);
        assertThat(createdEvent.name()).isEqualTo("Test Plan");
        assertThat(createdEvent.userId()).isEqualTo(userId);
        assertThat(createdEvent.occurredOn()).isEqualTo(testTime);
        assertThat(createdEvent.eventId()).isNotNull();
        assertThat(createdEvent.metadata()).isNotNull();
        assertThat(createdEvent.metadata()).containsEntry("creatorId", userId);
    }

    @Test
    @DisplayName("PlanRenamed event 應包含新舊名稱")
    void renamed_event_should_contain_old_and_new_names() {
        // Arrange
        plan = new Plan(planId, "Old Name", userId);
        plan.clearDomainEvents();
        Instant testTime = Instant.parse("2025-01-15T10:30:00Z");
        DateProvider.setDate(testTime);

        // Act
        plan.rename("New Name", userId);

        // Assert
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(PlanEvents.PlanRenamed.class);

        var renamedEvent = (PlanEvents.PlanRenamed) event;
        assertThat(renamedEvent.planId()).isEqualTo(planId);
        assertThat(renamedEvent.oldName()).isEqualTo("Old Name");
        assertThat(renamedEvent.newName()).isEqualTo("New Name");
        assertThat(renamedEvent.occurredOn()).isEqualTo(testTime);
    }

    @Test
    @DisplayName("多個操作應按順序發出多個 Events")
    void should_emit_events_in_sequence() {
        // Act
        plan = new Plan(planId, "Test Plan", userId);
        plan.addProject(ProjectId.valueOf("p1"), "Project 1", userId);
        plan.addProject(ProjectId.valueOf("p2"), "Project 2", userId);

        // Assert
        List<InternalDomainEvent> events = plan.getDomainEvents();
        assertThat(events).hasSize(3);

        assertThat(events.get(0)).isInstanceOf(PlanEvents.PlanCreated.class);
        assertThat(events.get(1)).isInstanceOf(PlanEvents.ProjectAdded.class);
        assertThat(events.get(2)).isInstanceOf(PlanEvents.ProjectAdded.class);
    }

    @Test
    @DisplayName("PlanDeleted event 應實作 DestructionEvent")
    void deleted_event_should_implement_destruction_event() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);
        plan.clearDomainEvents();

        // Act
        plan.delete(userId);

        // Assert
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(PlanEvents.PlanDeleted.class);
        assertThat(event).isInstanceOf(InternalDomainEvent.DestructionEvent.class);

        var deletedEvent = (PlanEvents.PlanDeleted) event;
        assertThat(deletedEvent.planId()).isEqualTo(planId);
    }

    @Test
    @DisplayName("所有 Events 都應使用 DateProvider")
    void all_events_should_use_date_provider() {
        // Arrange
        Instant t1 = Instant.parse("2025-01-15T10:00:00Z");
        Instant t2 = Instant.parse("2025-01-15T11:00:00Z");

        // Act & Assert: 創建
        DateProvider.setDate(t1);
        plan = new Plan(planId, "Test Plan", userId);
        assertThat(plan.getLastDomainEvent().get().occurredOn()).isEqualTo(t1);

        plan.clearDomainEvents();

        // Act & Assert: 修改
        DateProvider.setDate(t2);
        plan.rename("New Name", userId);
        assertThat(plan.getLastDomainEvent().get().occurredOn()).isEqualTo(t2);
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：只驗證 Event 類型，不驗證內容
@Test
void should_emit_created_event() {
    plan = new Plan(planId, "Test", userId);

    var event = plan.getLastDomainEvent().get();
    assertThat(event).isInstanceOf(PlanEvents.PlanCreated.class);  // ❌ 沒驗證內容
}

// ✅ 正確：驗證 Event 的完整內容
@Test
@DisplayName("PlanCreated event 應包含完整資訊")
void created_event_should_contain_complete_information() {
    Instant testTime = Instant.parse("2025-01-15T10:00:00Z");
    DateProvider.setDate(testTime);

    plan = new Plan(planId, "Test Plan", userId);

    var event = (PlanEvents.PlanCreated) plan.getLastDomainEvent().get();
    assertThat(event.planId()).isEqualTo(planId);
    assertThat(event.name()).isEqualTo("Test Plan");
    assertThat(event.userId()).isEqualTo(userId);
    assertThat(event.occurredOn()).isEqualTo(testTime);
    assertThat(event.eventId()).isNotNull();
    assertThat(event.metadata()).containsEntry("creatorId", userId);
}

// ❌ 錯誤：不驗證 Event 時間
@Test
void should_emit_event() {
    plan = new Plan(planId, "Test", userId);

    var event = plan.getLastDomainEvent().get();
    assertThat(event).isInstanceOf(PlanEvents.PlanCreated.class);  // ❌ 沒驗證時間
}

// ✅ 正確：驗證 Event 使用 DateProvider
@Test
@DisplayName("Event 應使用 DateProvider 的時間")
void event_should_use_date_provider_time() {
    Instant testTime = Instant.parse("2025-01-15T10:00:00Z");
    DateProvider.setDate(testTime);

    plan = new Plan(planId, "Test", userId);

    var event = plan.getLastDomainEvent().get();
    assertThat(event.occurredOn()).isEqualTo(testTime);  // ✅ 驗證使用 DateProvider
}
```

### 🔍 檢查清單

- [ ] 驗證 Event 類型正確
- [ ] 驗證 Event 包含所有必要欄位
- [ ] 驗證 Event 使用 DateProvider 時間
- [ ] 驗證 Event 的 eventId 不為 null
- [ ] 驗證 Event 的 metadata 包含審計資訊
- [ ] 驗證 Created Event 實作 ConstructionEvent
- [ ] 驗證 Deleted Event 實作 DestructionEvent
- [ ] 驗證多個操作產生正確的 Event 順序

---

## 軟刪除測試

### 🎯 設計意圖 (Why)

**問題**：如何測試 Aggregate 的軟刪除功能？

**答案**：驗證軟刪除的三個關鍵行為：
1. **刪除標記**：isDeleted() 返回 true
2. **刪除事件**：發出 DestructionEvent
3. **操作禁止**：已刪除的 Aggregate 不能再執行業務操作

### 📋 實作規範 (How)

```java
@Nested
@DisplayName("軟刪除測試")
class SoftDeleteTests {

    @Test
    @DisplayName("刪除應設置 deleted 狀態並發出 DestructionEvent")
    void should_mark_as_deleted_and_emit_event() {
        // Arrange
        aggregate = createAggregate();

        // Act
        aggregate.delete(userId);

        // Assert
        assertThat(aggregate.isDeleted()).isTrue();
        var event = aggregate.getLastDomainEvent().get();
        assertThat(event).isInstanceOf(InternalDomainEvent.DestructionEvent.class);
    }

    @Test
    @DisplayName("已刪除的 Aggregate 不能執行業務操作")
    void should_not_allow_operations_on_deleted_aggregate() {
        // Arrange
        aggregate = createAggregate();
        aggregate.delete(userId);

        // Act & Assert
        assertThatThrownBy(() -> aggregate.performOperation())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("deleted");
    }
}
```

### ✅ 正確範例

```java
@Nested
@DisplayName("軟刪除測試")
class SoftDeleteTests {

    @Test
    @DisplayName("刪除 Plan 應設置 deleted 狀態")
    void should_mark_as_deleted_when_delete_called() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);
        assertThat(plan.isDeleted()).isFalse();

        // Act
        plan.delete(userId);

        // Assert
        assertThat(plan.isDeleted()).isTrue();

        // Verify deletion event
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isInstanceOf(PlanEvents.PlanDeleted.class);
        assertThat(event).isInstanceOf(InternalDomainEvent.DestructionEvent.class);

        var deletedEvent = (PlanEvents.PlanDeleted) event;
        assertThat(deletedEvent.planId()).isEqualTo(planId);
        assertThat(deletedEvent.metadata()).containsEntry("deleterId", userId);
    }

    @Test
    @DisplayName("已刪除的 Plan 不能重新命名")
    void should_not_allow_rename_on_deleted_plan() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);
        plan.delete(userId);

        // Act & Assert
        assertThatThrownBy(() -> plan.rename("New Name", userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot modify a deleted Plan");
    }

    @Test
    @DisplayName("已刪除的 Plan 不能新增專案")
    void should_not_allow_adding_project_on_deleted_plan() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);
        plan.delete(userId);

        // Act & Assert
        assertThatThrownBy(() -> plan.addProject(
            ProjectId.valueOf("p1"), "Project 1", userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("deleted");
    }

    @Test
    @DisplayName("已刪除的 Plan 不能開始")
    void should_not_allow_start_on_deleted_plan() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);
        plan.delete(userId);

        // Act & Assert
        assertThatThrownBy(() -> plan.start(userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("deleted");
    }

    @Test
    @DisplayName("不能重複刪除已刪除的 Plan")
    void should_not_allow_double_delete() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);
        plan.delete(userId);

        // Act & Assert
        assertThatThrownBy(() -> plan.delete(userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already deleted");
    }

    @Test
    @DisplayName("刪除後 ensureInvariant 應仍然通過")
    void should_satisfy_invariants_when_deleted() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);

        // Act
        plan.delete(userId);

        // Assert: 刪除狀態有不同的不變式檢查
        assertDoesNotThrow(() -> plan.ensureInvariant());
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：不驗證 DestructionEvent
@Test
void should_delete_plan() {
    plan = new Plan(planId, "Test", userId);
    plan.delete(userId);

    assertThat(plan.isDeleted()).isTrue();  // ❌ 沒驗證 DestructionEvent
}

// ✅ 正確：驗證 DestructionEvent
@Test
@DisplayName("刪除應發出 DestructionEvent")
void should_emit_destruction_event_when_deleted() {
    plan = new Plan(planId, "Test", userId);
    plan.delete(userId);

    assertThat(plan.isDeleted()).isTrue();
    var event = plan.getLastDomainEvent().get();
    assertThat(event).isInstanceOf(InternalDomainEvent.DestructionEvent.class);
}

// ❌ 錯誤：不測試已刪除後的操作限制
@Test
void should_delete_plan() {
    plan = new Plan(planId, "Test", userId);
    plan.delete(userId);
    assertThat(plan.isDeleted()).isTrue();  // ❌ 沒測試操作限制
}

// ✅ 正確：測試所有業務操作都被禁止
@Test
@DisplayName("已刪除的 Plan 應禁止所有業務操作")
void should_prohibit_all_operations_on_deleted_plan() {
    plan = new Plan(planId, "Test", userId);
    plan.delete(userId);

    // 驗證各種操作都被禁止
    assertThatThrownBy(() -> plan.rename("New", userId))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> plan.start(userId))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> plan.addProject(ProjectId.create(), "P1", userId))
        .isInstanceOf(IllegalStateException.class);
}
```

### 🔍 檢查清單

- [ ] 驗證 isDeleted() 返回 true
- [ ] 驗證 Deleted Event 實作 DestructionEvent
- [ ] 驗證 Deleted Event 包含 deleterId
- [ ] 測試所有業務操作在刪除後都被禁止
- [ ] 測試不能重複刪除
- [ ] 驗證刪除後 ensureInvariant() 仍通過

---

## 審計資訊測試

### 🎯 設計意圖 (Why)

**問題**：如何驗證審計資訊正確記錄？

**答案**：驗證審計資訊在 Event metadata 中：
1. **創建資訊**：creatorId 在 Created Event metadata
2. **修改資訊**：updaterId 在修改 Event metadata
3. **Aggregate 無審計欄位**：不在 Aggregate 中保存審計資訊

### 📋 實作規範 (How)

```java
@Nested
@DisplayName("審計資訊測試")
class AuditInformationTests {

    @Test
    @DisplayName("Created event 應在 metadata 中包含 creatorId")
    void should_include_creator_in_metadata() {
        aggregate = new Aggregate(id, name, creatorId);

        var event = aggregate.getLastDomainEvent().get();
        assertThat(event.metadata()).containsEntry("creatorId", creatorId);
    }

    @Test
    @DisplayName("Aggregate 不應包含審計欄位")
    void should_not_have_audit_fields() {
        // 使用反射檢查沒有審計欄位
    }
}
```

### ✅ 正確範例

```java
@Nested
@DisplayName("審計資訊測試")
class AuditInformationTests {

    @Test
    @DisplayName("Created event 應在 metadata 中包含 creatorId")
    void should_include_creator_in_metadata() {
        // Act
        plan = new Plan(planId, "Test Plan", userId);

        // Assert
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isNotNull();
        assertThat(event.metadata()).isNotNull();
        assertThat(event.metadata()).containsEntry("creatorId", userId);
    }

    @Test
    @DisplayName("Renamed event 應在 metadata 中記錄 updaterId")
    void should_record_updater_in_metadata() {
        // Arrange
        plan = new Plan(planId, "Test Plan", "creator-123");
        plan.clearDomainEvents();

        // Act
        plan.rename("New Name", "updater-456");

        // Assert
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isNotNull();
        assertThat(event.metadata()).containsEntry("updaterId", "updater-456");
    }

    @Test
    @DisplayName("Deleted event 應在 metadata 中記錄 deleterId")
    void should_record_deleter_in_metadata() {
        // Arrange
        plan = new Plan(planId, "Test Plan", "creator-123");
        plan.clearDomainEvents();

        // Act
        plan.delete("deleter-789");

        // Assert
        var event = plan.getLastDomainEvent().orElse(null);
        assertThat(event).isNotNull();
        assertThat(event.metadata()).containsEntry("deleterId", "deleter-789");
    }

    @Test
    @DisplayName("Plan Aggregate 不應包含審計欄位")
    void should_not_have_audit_fields_in_aggregate() {
        // Arrange
        plan = new Plan(planId, "Test Plan", userId);

        // Assert: 使用反射檢查沒有審計欄位
        var fields = plan.getClass().getDeclaredFields();
        for (var field : fields) {
            String fieldName = field.getName().toLowerCase();
            assertThat(fieldName)
                .doesNotContain("creator")
                .doesNotContain("updater")
                .doesNotContain("deleter")
                .doesNotContain("createdat")
                .doesNotContain("updatedat")
                .doesNotContain("deletedat")
                .doesNotContain("createdby")
                .doesNotContain("modifiedby")
                .doesNotContain("deletedby");
        }
    }

    @Test
    @DisplayName("所有修改操作都應在 metadata 中記錄 userId")
    void should_record_user_in_all_modification_events() {
        // Arrange
        plan = new Plan(planId, "Test Plan", "creator-123");
        plan.clearDomainEvents();

        // Act: 執行多個修改操作
        plan.rename("New Name", "user-1");
        plan.addProject(ProjectId.valueOf("p1"), "Project 1", "user-2");
        plan.start("user-3");

        // Assert: 每個 Event 都應有 userId
        List<InternalDomainEvent> events = plan.getDomainEvents();

        assertThat(events.get(0).metadata()).containsEntry("updaterId", "user-1");
        assertThat(events.get(1).metadata()).containsEntry("userId", "user-2");
        assertThat(events.get(2).metadata()).containsEntry("userId", "user-3");
    }
}
```

### 🔍 檢查清單

- [ ] 驗證 Created Event metadata 包含 creatorId
- [ ] 驗證修改 Events metadata 包含 updaterId/userId
- [ ] 驗證 Deleted Event metadata 包含 deleterId
- [ ] 使用反射驗證 Aggregate 不包含審計欄位
- [ ] 驗證所有操作都記錄相關的 userId

---

## Domain Event 序列化測試

### 🎯 設計意圖 (Why)

**問題**：如何確保 Domain Events 可以正確序列化和反序列化？

**答案**：為每個 Aggregate 撰寫 Event 序列化測試：
1. **JSON 序列化**：Event 可以轉換為 JSON
2. **JSON 反序列化**：JSON 可以轉回 Event 物件
3. **欄位完整性**：所有欄位正確保留
4. **無多餘欄位**：不包含不需要的欄位（如 "empty"）

### 📋 實作規範 (How)

```java
@DisplayName("Domain Events 序列化測試")
public class EventSerializationTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Event 序列化與反序列化")
    void testEvent_SerializationAndDeserialization() throws Exception {
        // Given: 完整的 Event
        var event = Events.EventCreated.create(...);

        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(event);

        // Then: 不應包含多餘欄位
        assertFalse(json.contains("\"empty\""));

        // When: Deserialize back
        var deserialized = objectMapper.readValue(json, Events.EventCreated.class);

        // Then: 欄位應正確
        assertEquals(event.id(), deserialized.id());
    }
}
```

### ✅ 正確範例

```java
package tw.teddysoft.ezkanban.plan.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tw.teddysoft.ezkanban.common.entity.DateProvider;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 每個 Aggregate 都必須有對應的 Domain Event 序列化測試
 * 確保所有 Domain Events 都可以正確轉成 JSON 再轉回原本的物件
 */
@DisplayName("Plan Domain Events 序列化測試")
public class PlanEventSerializationTest {
    private ObjectMapper objectMapper;
    private final PlanId planId = PlanId.valueOf("plan-123");
    private final String userId = "user-456";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DateProvider.setDate("2025-01-15T10:00:00Z");
    }

    @Test
    @DisplayName("PlanCreated 序列化與反序列化")
    void testPlanCreated_SerializationAndDeserialization() throws Exception {
        // Given: 完整的 Domain Event
        var event = PlanEvents.PlanCreated.create(
            planId,
            "Test Plan",
            userId
        );

        // When: Serialize to JSON
        String json = objectMapper.writeValueAsString(event);

        // Then: 不應包含多餘欄位
        assertFalse(json.contains("\"empty\""), "JSON should not contain 'empty' field");

        // When: Deserialize back to object
        var deserialized = objectMapper.readValue(json, PlanEvents.PlanCreated.class);

        // Then: 所有欄位應正確反序列化
        assertEquals(event.planId(), deserialized.planId());
        assertEquals(event.name(), deserialized.name());
        assertEquals(event.userId(), deserialized.userId());
        assertEquals(event.occurredOn(), deserialized.occurredOn());
        assertEquals(event.eventId(), deserialized.eventId());
    }

    @Test
    @DisplayName("PlanRenamed 序列化與反序列化")
    void testPlanRenamed_SerializationAndDeserialization() throws Exception {
        // Given
        var event = PlanEvents.PlanRenamed.create(
            planId,
            "Old Name",
            "New Name",
            userId
        );

        // When/Then
        String json = objectMapper.writeValueAsString(event);
        assertFalse(json.contains("\"empty\""));

        var deserialized = objectMapper.readValue(json, PlanEvents.PlanRenamed.class);
        assertEquals(event.planId(), deserialized.planId());
        assertEquals(event.oldName(), deserialized.oldName());
        assertEquals(event.newName(), deserialized.newName());
    }

    @Test
    @DisplayName("ProjectAdded 序列化與反序列化")
    void testProjectAdded_SerializationAndDeserialization() throws Exception {
        // Given
        ProjectId projectId = ProjectId.valueOf("project-1");
        var event = PlanEvents.ProjectAdded.create(
            planId,
            projectId,
            "Project Name",
            userId
        );

        // When/Then
        String json = objectMapper.writeValueAsString(event);
        assertFalse(json.contains("\"empty\""));

        var deserialized = objectMapper.readValue(json, PlanEvents.ProjectAdded.class);
        assertEquals(event.planId(), deserialized.planId());
        assertEquals(event.projectId(), deserialized.projectId());
        assertEquals(event.projectName(), deserialized.projectName());
    }

    @Test
    @DisplayName("PlanDeleted 序列化與反序列化")
    void testPlanDeleted_SerializationAndDeserialization() throws Exception {
        // Given
        var event = PlanEvents.PlanDeleted.create(planId, userId);

        // When/Then
        String json = objectMapper.writeValueAsString(event);
        assertFalse(json.contains("\"empty\""));

        var deserialized = objectMapper.readValue(json, PlanEvents.PlanDeleted.class);
        assertEquals(event.planId(), deserialized.planId());
        assertEquals(event.metadata().get("deleterId"), deserialized.metadata().get("deleterId"));
    }

    @Test
    @DisplayName("所有 Plan Events 都實作必要介面")
    void testAllEventsImplementRequiredInterfaces() {
        // 確保所有 Domain Events 都實作必要介面
        Stream.of(
            PlanEvents.PlanCreated.class,
            PlanEvents.PlanRenamed.class,
            PlanEvents.ProjectAdded.class,
            PlanEvents.ProjectRemoved.class,
            PlanEvents.PlanStarted.class,
            PlanEvents.PlanCompleted.class,
            PlanEvents.PlanDeleted.class
        ).forEach(eventClass -> {
            assertTrue(DomainEvent.class.isAssignableFrom(eventClass),
                eventClass.getSimpleName() + " should implement DomainEvent");
            assertTrue(InternalDomainEvent.class.isAssignableFrom(eventClass),
                eventClass.getSimpleName() + " should implement InternalDomainEvent");
        });
    }

    @Test
    @DisplayName("PlanCreated 應實作 ConstructionEvent")
    void testPlanCreatedImplementsConstructionEvent() {
        assertTrue(InternalDomainEvent.ConstructionEvent.class
            .isAssignableFrom(PlanEvents.PlanCreated.class));
    }

    @Test
    @DisplayName("PlanDeleted 應實作 DestructionEvent")
    void testPlanDeletedImplementsDestructionEvent() {
        assertTrue(InternalDomainEvent.DestructionEvent.class
            .isAssignableFrom(PlanEvents.PlanDeleted.class));
    }
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：忘記測試序列化
// 沒有序列化測試類別

// ✅ 正確：每個 Aggregate 都有序列化測試
@DisplayName("Plan Domain Events 序列化測試")
public class PlanEventSerializationTest {
    // 測試所有 Events 的序列化...
}

// ❌ 錯誤：不檢查多餘欄位
@Test
void testSerialization() throws Exception {
    var event = PlanEvents.PlanCreated.create(...);
    String json = objectMapper.writeValueAsString(event);
    // ❌ 沒檢查是否有 "empty" 等多餘欄位
}

// ✅ 正確：檢查沒有多餘欄位
@Test
@DisplayName("序列化不應包含 empty 欄位")
void testSerializationWithoutEmptyField() throws Exception {
    var event = PlanEvents.PlanCreated.create(...);
    String json = objectMapper.writeValueAsString(event);
    assertFalse(json.contains("\"empty\""));  // ✅ 檢查沒有多餘欄位
}

// ❌ 錯誤：不註冊 JavaTimeModule
@BeforeEach
void setUp() {
    objectMapper = new ObjectMapper();  // ❌ 忘記註冊 JavaTimeModule
}

// ✅ 正確：註冊 JavaTimeModule
@BeforeEach
void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());  // ✅ 支援 Instant 序列化
}
```

### 🔍 檢查清單

- [ ] 每個 Aggregate 都有對應的序列化測試類別
- [ ] 測試所有 Domain Events 的序列化和反序列化
- [ ] 檢查 JSON 不包含 "empty" 等多餘欄位
- [ ] 驗證所有欄位正確保留
- [ ] 註冊 JavaTimeModule 以支援時間類型
- [ ] 驗證 Events 實作必要介面（DomainEvent, InternalDomainEvent）
- [ ] 驗證 Created Event 實作 ConstructionEvent
- [ ] 驗證 Deleted Event 實作 DestructionEvent

---

## 測試輔助方法

### 🎯 設計意圖 (Why)

**問題**：如何減少測試中的重複程式碼？

**答案**：建立測試輔助方法（Test Fixtures）：
1. **工廠方法**：建立處於特定狀態的 Aggregate
2. **資料建立**：產生測試數據
3. **狀態準備**：快速準備測試所需的初始狀態

### 📋 實作規範 (How)

```java
// 測試輔助方法
private Aggregate createAggregate() {
    return new Aggregate(id, name, userId);
}

private Aggregate createAggregateInStateX() {
    var aggregate = createAggregate();
    aggregate.transitionToStateX();
    aggregate.clearDomainEvents();  // 清除設置事件
    return aggregate;
}
```

### ✅ 正確範例

```java
// ✅ 測試輔助方法範例

// 基本建立方法
private Plan createPlan() {
    return new Plan(planId, "Test Plan", userId);
}

private Plan createPlan(String name) {
    return new Plan(PlanId.create(), name, userId);
}

// 特定狀態的建立方法
private Plan createPlanInProgress() {
    Plan plan = createPlan();
    plan.start(userId);
    plan.clearDomainEvents();  // ✅ 清除設置事件，專注測試
    return plan;
}

private Plan createCompletedPlan() {
    Plan plan = createPlanInProgress();
    plan.complete(userId);
    plan.clearDomainEvents();
    return plan;
}

private Plan createPlanWithProjects(int projectCount) {
    Plan plan = createPlan();
    for (int i = 1; i <= projectCount; i++) {
        ProjectId projectId = ProjectId.valueOf("project-" + i);
        plan.addProject(projectId, "Project " + i, userId);
    }
    plan.clearDomainEvents();
    return plan;
}

private Plan createDeletedPlan() {
    Plan plan = createPlan();
    plan.delete(userId);
    plan.clearDomainEvents();
    return plan;
}

// 使用範例
@Test
@DisplayName("重新開啟已完成的計畫")
void should_reopen_completed_plan() {
    // Arrange: 使用輔助方法快速準備狀態
    plan = createCompletedPlan();

    // Act
    plan.reopen(userId);

    // Assert
    assertThat(plan.getState()).isEqualTo(PlanState.IN_PROGRESS);
}

@Test
@DisplayName("刪除包含多個專案的計畫")
void should_delete_plan_with_multiple_projects() {
    // Arrange: 使用輔助方法建立複雜狀態
    plan = createPlanWithProjects(5);

    // Act
    plan.delete(userId);

    // Assert
    assertThat(plan.isDeleted()).isTrue();
    assertThat(plan.getProjects()).hasSize(5);  // 專案仍在，只是計畫被刪除
}
```

### ❌ 常見錯誤

```java
// ❌ 錯誤：在每個測試中重複相同的設置程式碼
@Test
void test1() {
    Plan plan = new Plan(planId, "Test", userId);
    plan.start(userId);
    plan.clearDomainEvents();
    // 測試...
}

@Test
void test2() {
    Plan plan = new Plan(planId, "Test", userId);  // ❌ 重複程式碼
    plan.start(userId);
    plan.clearDomainEvents();
    // 測試...
}

// ✅ 正確：使用輔助方法
private Plan createPlanInProgress() {
    Plan plan = new Plan(planId, "Test", userId);
    plan.start(userId);
    plan.clearDomainEvents();
    return plan;
}

@Test
void test1() {
    plan = createPlanInProgress();  // ✅ 簡潔
    // 測試...
}

@Test
void test2() {
    plan = createPlanInProgress();  // ✅ 簡潔
    // 測試...
}

// ❌ 錯誤：輔助方法沒有清除事件
private Plan createPlanInProgress() {
    Plan plan = new Plan(planId, "Test", userId);
    plan.start(userId);
    return plan;  // ❌ 沒清除事件，測試時會有額外事件
}

// ✅ 正確：輔助方法清除設置事件
private Plan createPlanInProgress() {
    Plan plan = new Plan(planId, "Test", userId);
    plan.start(userId);
    plan.clearDomainEvents();  // ✅ 清除設置事件
    return plan;
}
```

### 🔍 檢查清單

- [ ] 建立基本的工廠方法（createAggregate）
- [ ] 建立特定狀態的工廠方法（createAggregateInStateX）
- [ ] 輔助方法清除設置事件（clearDomainEvents）
- [ ] 輔助方法命名清楚描述狀態
- [ ] 避免在測試中重複相同的設置程式碼

---

## 測試覆蓋要求

### 🎯 設計意圖 (Why)

**問題**：如何確保測試完整覆蓋 Aggregate 的行為？

**答案**：遵循測試覆蓋檢查清單：
1. **狀態轉換**：所有正常和非法轉換
2. **邊界條件**：極端值和特殊情況
3. **業務規則**：不變式和約束條件
4. **軟刪除**：刪除標記和操作限制
5. **審計資訊**：Event metadata
6. **序列化**：JSON 轉換正確性

### 📋 測試覆蓋檢查清單

**必須測試的場景**：

#### 1. 狀態轉換測試
- [ ] 所有正常狀態轉換路徑
- [ ] 回退路徑（如果有）
- [ ] 取消/中止路徑
- [ ] 非法狀態轉換（驗證異常）
- [ ] 完整生命週期流程

#### 2. 邊界條件測試
- [ ] 空集合的處理
- [ ] 極端值（最大、最小、零）
- [ ] 空字串和空白字串
- [ ] 長度臨界值
- [ ] null 參數（驗證異常）

#### 3. 業務規則測試
- [ ] 所有不變式條件
- [ ] 業務約束驗證
- [ ] 資料完整性檢查

#### 4. Domain Event 測試
- [ ] Event 類型正確
- [ ] Event 內容完整
- [ ] Event 使用 DateProvider
- [ ] Event 順序正確
- [ ] Created Event 實作 ConstructionEvent
- [ ] Deleted Event 實作 DestructionEvent

#### 5. 軟刪除測試
- [ ] delete() 設置 isDeleted = true
- [ ] 發出 DestructionEvent
- [ ] 所有業務操作被禁止
- [ ] 不能重複刪除
- [ ] 刪除後 ensureInvariant() 通過

#### 6. 審計資訊測試
- [ ] Created Event metadata 包含 creatorId
- [ ] 修改 Events metadata 包含 updaterId/userId
- [ ] Deleted Event metadata 包含 deleterId
- [ ] Aggregate 不包含審計欄位

#### 7. 序列化測試
- [ ] 每個 Aggregate 有序列化測試類別
- [ ] 所有 Events 可序列化和反序列化
- [ ] JSON 不包含多餘欄位
- [ ] 所有欄位正確保留
- [ ] Events 實作必要介面

### ✅ 完整測試結構範例

```java
@DisplayName("Plan Aggregate 測試")
class PlanTest {

    private Plan plan;
    private final PlanId planId = PlanId.valueOf("plan-123");
    private final String userId = "user-456";

    @BeforeEach
    void setUp() {
        DateProvider.setDate("2025-01-15T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        DateProvider.resetDate();
    }

    @Nested
    @DisplayName("創建測試")
    class CreationTests {
        // 測試創建行為
    }

    @Nested
    @DisplayName("狀態轉換測試")
    class StateTransitionTests {
        // 測試所有狀態轉換
    }

    @Nested
    @DisplayName("邊界條件測試")
    class BoundaryConditionTests {
        // 測試邊界條件
    }

    @Nested
    @DisplayName("不變式測試")
    class InvariantTests {
        // 測試不變式
    }

    @Nested
    @DisplayName("Domain Event 測試")
    class DomainEventTests {
        // 測試 Events
    }

    @Nested
    @DisplayName("軟刪除測試")
    class SoftDeleteTests {
        // 測試軟刪除
    }

    @Nested
    @DisplayName("審計資訊測試")
    class AuditInformationTests {
        // 測試審計資訊
    }

    // 測試輔助方法
    private Plan createPlan() { ... }
    private Plan createPlanInProgress() { ... }
    private Plan createCompletedPlan() { ... }
}

@DisplayName("Plan Domain Events 序列化測試")
class PlanEventSerializationTest {
    // 測試所有 Events 的序列化
}
```

### 🔍 總檢查清單

- [ ] 使用標準 JUnit 5.x（不使用 Spring 或 Repository）
- [ ] 設定和清理 DateProvider
- [ ] 使用 @Nested 組織測試
- [ ] 使用 @DisplayName 提供中文描述
- [ ] 測試所有狀態轉換（正常和非法）
- [ ] 測試邊界條件和極端值
- [ ] 驗證所有不變式
- [ ] 驗證 Domain Events（類型、內容、時間）
- [ ] 測試軟刪除功能
- [ ] 驗證審計資訊在 metadata
- [ ] 撰寫序列化測試
- [ ] 建立測試輔助方法減少重複

---

## 總結

Aggregate Testing 的要點：
1. **純單元測試**：不使用 Spring 或 Repository
2. **DateProvider 控制**：設定測試時間，測試後清理
3. **3A 模式**：Arrange-Act-Assert 結構
4. **Event 驗證**：驗證類型、內容、時間、順序
5. **軟刪除**：測試刪除標記、事件、操作限制
6. **審計資訊**：驗證在 Event metadata，不在 Aggregate
7. **序列化**：確保所有 Events 可正確序列化
8. **完整覆蓋**：狀態轉換、邊界條件、不變式、Events