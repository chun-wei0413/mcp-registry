# Java CA ezddd Spring 常見問題 (FAQ)

## 架構設計問題

### 為什麼要使用 Event Sourcing？
Event Sourcing 提供了以下優勢：
- 完整的審計軌跡（Audit Trail）
- 可以重建任何時間點的狀態
- 支援複雜的業務分析
- 天然支援 CQRS 模式
- 便於實現撤銷/重做功能

### Command 和 Query 的區別是什麼？
- **Command**: 改變系統狀態，返回 `CqrsOutput`（只包含成功/失敗）
- **Query**: 讀取資料，不改變狀態，返回具體的 DTO

```java
// Command 範例
public interface CreatePlanUseCase extends Command<CreatePlanInput, CqrsOutput> {}

// Query 範例  
public interface GetPlanUseCase extends Query<GetPlanInput, PlanOutput> {}
```

### 什麼時候使用 Reactor？
Reactor 用於處理跨 Aggregate 的副作用：
- 當一個 Aggregate 的事件需要觸發另一個 Aggregate 的變更
- 需要異步處理的業務邏輯
- 發送通知、更新快取等副作用

### 為什麼 UseCase 的 Input/Output 要用 public fields？
這是框架的設計決策：
- 簡化程式碼，減少 boilerplate
- Input/Output 是純資料傳輸物件
- 使用 static factory method 創建實例
- 符合 DDD 的 DTO 設計理念

詳見 [UseCase Pattern](./examples/usecase/README.md)

## 開發實作問題

### 找不到 tw.teddysoft.ezddd 相關類別？
這些是專案使用的外部框架，需要：
1. 確認 Maven 設定正確
2. 檢查私有 Repository 認證
3. 執行 `mvn clean install`
4. **絕對不要**自己創建這些類別

### 為什麼不能使用 Lazy Loading？
在 DDD 專案中禁用 Lazy Loading 的原因：
- Aggregate 應該完整載入
- 避免 LazyInitializationException
- 防止 N+1 查詢問題
- 保持領域模型的純粹性

正確做法：
```java
@OneToMany(fetch = FetchType.EAGER)  // 使用 EAGER
private Set<TaskData> tasks;
```

詳見 [Repository Pattern](./examples/repository/README.md)

### 如何處理 N+1 查詢問題？
使用以下策略：
1. **Projection**: 專門的查詢介面
2. **EAGER Loading**: 一次載入所需資料
3. **批次查詢**: 減少查詢次數

範例：
```java
@Query("""
    SELECT p FROM PlanData p
    LEFT JOIN FETCH p.tasks
    WHERE p.userId = :userId
    """)
List<PlanData> findPlansWithTasks(@Param("userId") String userId);
```

詳見 [Query vs Repository Pattern](./examples/projection/README.md)

### 為什麼 Controller 不直接調用 Repository？
遵循 Clean Architecture 原則：
- Controller → UseCase → Repository
- 業務邏輯集中在 UseCase 層
- 便於測試和維護
- 保持層次間的單向依賴

詳見 [Clean Architecture Pattern](./examples/aggregate/README.md)

## 測試相關問題

### 如何寫 ezSpec 測試？
ezSpec 使用 BDD 風格：
```java
@EzScenario
public void test_使用者可以建立計畫() {
    // Given - 準備測試資料
    CreatePlanInput input = CreatePlanInput.create();
    input.userId = "user-123";
    input.planName = "我的計畫";
    
    // When - 執行操作
    CqrsOutput output = createPlanUseCase.execute(input);
    
    // Then - 驗證結果
    assertThat(output.getExitCode()).isEqualTo(ExitCode.SUCCESS);
}
```

詳見 [測試範例](./examples/test-example.md)

### 測試時如何處理外部依賴？
使用 In-Memory 實作：
```java
public class InMemoryPlanRepository implements Repository<Plan, PlanId> {
    private final Map<PlanId, Plan> storage = new HashMap<>();
    
    @Override
    public Optional<Plan> findById(PlanId id) {
        return Optional.ofNullable(storage.get(id));
    }
}
```

## 常見錯誤

### "No qualifying bean of type" 錯誤
**原因**: Spring 找不到對應的 Bean
**解決方案**:
1. 確認 Service 類有 `@Service` 註解
2. 檢查 `@ComponentScan` 範圍
3. 確認介面和實作在正確的套件

### "LazyInitializationException"
**原因**: 在 Session 外存取 Lazy 載入的屬性
**解決方案**:
1. 改用 `FetchType.EAGER`
2. 使用 `@Transactional` 確保在 Session 內
3. 使用 Projection 預先載入

### 事件沒有被 Reactor 處理？
**檢查事項**:
1. 事件是否在 BootstrapConfig 中註冊
2. Reactor 是否有 `@Component` 註解
3. MessageBus 是否正確配置
4. 檢查事件類型對應

## 效能優化問題

### 查詢速度很慢怎麼辦？
**優化策略**:
1. 加入適當的資料庫索引
2. 使用 Projection 而非載入整個 Aggregate
3. 實作快取機制
4. 考慮讀寫分離

### 如何實作快取？
使用 Spring Cache：
```java
@Cacheable(value = "plans", key = "#userId")
public List<PlanDto> getUserPlans(String userId) {
    // 查詢邏輯
}

@CacheEvict(value = "plans", key = "#userId")
public void invalidateUserPlans(String userId) {
    // 清除快取
}
```

## 學習資源

### 推薦書籍
1. Eric Evans - "Domain-Driven Design"
2. Vaughn Vernon - "Implementing Domain-Driven Design"
3. Martin Fowler - "Patterns of Enterprise Application Architecture"

### 技術棧內的資源
- [編碼指南](./coding-guide.md) - 完整的編碼規範
- [examples/](./examples/) - 設計模式與實作範例
- [examples/INDEX.md](./examples/INDEX.md) - 完整範例索引
- [專案結構](./project-structure.md) - 目錄組織指南

## 延伸閱讀

- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [DDD Reference](https://www.domainlanguage.com/ddd/reference/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)