# Java DDD Spring 反模式

## 概述

這份文件記錄了在使用 Java DDD Spring 技術棧時應該避免的常見錯誤和反模式。

## 領域層反模式

### 1. ❌ Anemic Domain Model（貧血領域模型）
```java
// 錯誤：只有 getter/setter 的實體
public class User {
    private String id;
    private String name;
    private String email;
    
    // 只有 getter/setter...
}

// 業務邏輯散落在 Service 中
public class UserService {
    public void changeEmail(User user, String newEmail) {
        if (!isValidEmail(newEmail)) {
            throw new IllegalArgumentException();
        }
        user.setEmail(newEmail);
    }
}
```

✅ **正確做法**：
```java
public class User extends EsAggregateRoot<UserId, UserEvent> {
    private UserId id;
    private UserName name;
    private Email email;
    
    public void changeEmail(String newEmail) {
        Contract.requireNotNull(newEmail, "Email");
        var email = new Email(newEmail); // 驗證在 Value Object 中
        apply(new UserEmailChanged(id, email));
    }
}
```

### 2. ❌ 過大的 Aggregate
```java
// 錯誤：Company 包含所有員工、部門、專案
public class Company {
    private List<Employee> employees;      // 可能有數千個
    private List<Department> departments;
    private List<Project> projects;
    private List<Customer> customers;
    // ...
}
```

✅ **正確做法**：
```java
// 拆分為多個 Aggregate
public class Company {
    private CompanyId id;
    private CompanyName name;
    // 只保留核心屬性
}

public class Employee {
    private EmployeeId id;
    private CompanyId companyId;  // 通過 ID 引用
    // ...
}
```

### 3. ❌ 直接修改實體狀態
```java
// 錯誤：繞過領域事件直接修改
public class Task {
    public void complete() {
        this.status = TaskStatus.COMPLETED;  // 直接修改
        this.completedAt = LocalDateTime.now();
    }
}
```

✅ **正確做法**：
```java
public class Task extends EsAggregateRoot<TaskId, TaskEvent> {
    public void complete() {
        if (status == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task already completed");
        }
        apply(new TaskCompleted(id, LocalDateTime.now()));
    }
    
    @EventHandler
    private void on(TaskCompleted event) {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = event.getCompletedAt();
    }
}
```

## 應用層反模式

### 4. ❌ Use Case 中包含業務邏輯
```java
// 錯誤：業務邏輯應該在 Domain 層
public class CreateOrderService implements CreateOrderUseCase {
    public void execute(CreateOrderInput input) {
        // 業務邏輯不應該在這裡
        if (input.getItems().isEmpty()) {
            throw new BusinessException("Order must have items");
        }
        
        var totalAmount = input.getItems().stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
            
        if (totalAmount < 10) {
            throw new BusinessException("Minimum order amount is 10");
        }
        
        // ...
    }
}
```

✅ **正確做法**：
```java
public class CreateOrderService implements CreateOrderUseCase {
    public void execute(CreateOrderInput input) {
        // Use Case 只協調，業務邏輯在 Domain
        var order = Order.create(
            input.getCustomerId(),
            input.getItems()  // 驗證在 Order.create() 中
        );
        
        repository.save(order);
        messageBus.publish(order.getEvents());
    }
}
```

### 5. ❌ 跨 Aggregate 事務
```java
// 錯誤：在一個事務中修改多個 Aggregate
@Transactional
public void transferEmployee(String employeeId, String fromDeptId, String toDeptId) {
    var employee = employeeRepo.findById(employeeId);
    var fromDept = departmentRepo.findById(fromDeptId);
    var toDept = departmentRepo.findById(toDeptId);
    
    fromDept.removeEmployee(employee);
    toDept.addEmployee(employee);
    employee.changeDepartment(toDeptId);
    
    // 保存三個 Aggregate - 違反一致性邊界
    departmentRepo.save(fromDept);
    departmentRepo.save(toDept);
    employeeRepo.save(employee);
}
```

✅ **正確做法**：
```java
// 使用領域事件和最終一致性
public void transferEmployee(String employeeId, String toDeptId) {
    var employee = employeeRepo.findById(employeeId);
    employee.requestTransfer(toDeptId);  // 發出事件
    employeeRepo.save(employee);
    
    // Reactor 處理跨 Aggregate 的變更
}
```

## 持久化層反模式

### 6. ❌ 使用 Lazy Loading
```java
// 錯誤：依賴 JPA 的 Lazy Loading
@Entity
public class PlanData {
    @OneToMany(fetch = FetchType.LAZY)  // 違反 DDD 原則
    private Set<TaskData> tasks;
}
```

✅ **正確做法**：
```java
@Entity
public class PlanData {
    @OneToMany(fetch = FetchType.EAGER)  // Aggregate 完整載入
    private Set<TaskData> tasks;
}

// 或使用 Projection 進行查詢優化
```

### 7. ❌ Repository 添加自定義查詢方法（嚴重違規）
```java
// 錯誤：違反「Repository 只能有三個方法」的規則
public interface UserRepository extends Repository<User, UserId> {
    // ❌ 以下全部都是錯誤的！
    List<User> findByEmail(Email email);
    Optional<User> findByUsername(String username);
    List<User> findActiveUsers();
    List<User> findByAgeGreaterThan(int age);
    Page<User> findAll(Pageable pageable);
}
```

✅ **正確做法**：
```java
// Repository 嚴格遵守限制
public interface UserRepository extends Repository<User, UserId> {
    // 只繼承 findById, save, delete
    // 絕對不添加任何其他方法！
}

// 方案1：使用 Query Service
@Service
public class UserQueryService {
    private final UserDataJpaRepository jpaRepository;
    
    public List<UserDto> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .stream()
            .map(UserMapper::toDto)
            .collect(Collectors.toList());
    }
}

// 方案2：使用 Projection Repository
@Repository
public interface UserProjectionRepository extends JpaRepository<UserProjection, String> {
    List<UserProjection> findByEmail(String email);
    Page<UserProjection> findByStatus(String status, Pageable pageable);
}
```

### 8. ❌ Repository 包含業務邏輯
```java
// 錯誤：Repository 不應該有業務邏輯
public interface UserRepository extends Repository<User, UserId> {
    void deactivateInactiveUsers(int days);  // 業務邏輯！
    List<User> findUsersEligibleForPromotion();  // 業務規則！
}
```

✅ **正確做法**：
```java
// Repository 只負責持久化
public interface UserRepository extends Repository<User, UserId> {
    // 只有 findById, save, delete
}

// 業務邏輯放在 Use Case
@Service
public class DeactivateInactiveUsersService {
    private final UserRepository repository;
    private final UserQueryService queryService;
    
    public void execute(int days) {
        // 透過 Query Service 找出符合條件的用戶
        var userIds = queryService.findInactiveUserIds(days);
        
        // 逐個處理
        userIds.forEach(id -> {
            repository.findById(id).ifPresent(user -> {
                user.deactivate();  // 領域邏輯
                repository.save(user);
            });
        });
    }
}
```

## 測試反模式

### 9. ❌ 直接實現 InMemoryRepository（違反測試規範）
```java
// 錯誤：為每個 Repository 寫特定的測試實現
public class InMemoryPlanRepository implements PlanRepository {
    private final Map<String, Plan> storage = new HashMap<>();
    private final List<DomainEvent> events = new ArrayList<>();
    
    @Override
    public Optional<Plan> findById(PlanId id) {
        return Optional.ofNullable(storage.get(id.getValue()));
    }
    
    @Override
    public void save(Plan plan) {
        storage.put(plan.getId().getValue(), plan);
        events.addAll(plan.getDomainEvents());
        plan.clearDomainEvents();
    }
    
    @Override
    public void delete(Plan plan) {
        storage.remove(plan.getId().getValue());
    }
    
    // 問題：
    // 1. 重複程式碼
    // 2. 可能忘記處理事件
    // 3. 可能實現不一致
}
```

✅ **正確做法**：
```java
// 使用框架提供的 GenericInMemoryRepository
import tw.teddysoft.example.common.adapter.out.repository.GenericInMemoryRepository;

@BeforeEach
void setUp() {
    messageBus = new BlockingMessageBus();
    // 所有測試都使用統一的 GenericInMemoryRepository
    planRepository = new GenericInMemoryRepository<>(messageBus);
    userRepository = new GenericInMemoryRepository<>(messageBus);
    tagRepository = new GenericInMemoryRepository<>(messageBus);
}

// 優點：
// 1. 不需要重複實現每個 Repository
// 2. 行為一致且正確
// 3. 自動處理事件發布
// 4. 遵守 Repository 三個方法限制
```

### 10. ❌ 測試實現細節
```java
// 錯誤：測試內部實現而非行為
@Test
void should_set_completed_flag() {
    var task = new Task();
    task.complete();
    
    // 測試私有字段
    assertTrue(ReflectionTestUtils.getField(task, "isCompleted"));
}
```

✅ **正確做法**：
```java
@Test
void should_emit_completed_event_when_task_completed() {
    // Given
    var task = Task.create("Test task");
    
    // When  
    task.complete();
    
    // Then - 測試行為和事件
    var events = task.getEvents();
    assertThat(events).hasSize(2);  // Created + Completed
    assertThat(events.get(1)).isInstanceOf(TaskCompleted.class);
}
```

### 11. ❌ 過度 Mock
```java
// 錯誤：Mock 所有依賴
@Test
void test_with_too_many_mocks() {
    var repo = mock(Repository.class);
    var bus = mock(MessageBus.class);
    var mapper = mock(Mapper.class);
    var validator = mock(Validator.class);
    // ... 10 個 mocks
}
```

✅ **正確做法**：
```java
// 使用真實對象和 GenericInMemoryRepository
@Test
void test_with_real_objects() {
    var messageBus = new BlockingMessageBus();
    var repo = new GenericInMemoryRepository<Plan, PlanId>(messageBus);
    var service = new CreatePlanService(repo);
    // ...
}
```

## 架構反模式

### 12. ❌ 跳過架構層次
```java
// 錯誤：Controller 直接調用 Repository
@RestController
public class UserController {
    @Autowired
    private UserRepository repository;  // 違反架構原則
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        return repository.findById(id);  // Domain 物件洩漏
    }
}
```

✅ **正確做法**：
```java
@RestController
public class UserController {
    private final GetUserUseCase getUserUseCase;
    
    @GetMapping("/users/{id}")
    public UserDto getUser(@PathVariable String id) {
        return getUserUseCase.execute(id).getData();
    }
}
```

## 效能反模式

### 13. ❌ N+1 查詢問題
```java
// 錯誤：載入 Plan 後逐個載入 Task
var plans = planRepository.findAll();
for (Plan plan : plans) {
    var tasks = taskRepository.findByPlanId(plan.getId());  // N 次查詢
    // ...
}
```

✅ **正確做法**：
```java
// 使用 Projection 或 Join Fetch
@Query("SELECT p FROM PlanData p JOIN FETCH p.tasks WHERE p.userId = :userId")
List<PlanData> findByUserIdWithTasks(@Param("userId") String userId);
```

### 14. ❌ 過度使用 Event Sourcing
```java
// 錯誤：簡單的查詢也重建整個 Aggregate
public UserDto getUser(String id) {
    var user = repository.findById(id);  // 載入所有歷史事件
    return UserMapper.toDto(user);
}
```

✅ **正確做法**：
```java
// 使用 CQRS - 查詢用 Projection
public UserDto getUser(String id) {
    return userProjection.findById(id);  // 直接查詢讀模型
}
```

## 11. ❌ 不使用 DateProvider 產生時間戳記

在 Domain Events 或任何需要時間戳記的地方直接使用 Java 時間 API。

### 錯誤範例：
```java
// ❌ 錯誤：直接使用 Instant.now()
apply(new PlanCreated(
    planId,
    name,
    userId,
    UUID.randomUUID(),
    Instant.now()  // 錯誤！不可測試
));

// ❌ 錯誤：使用 LocalDateTime.now()
public class TaskEvent {
    private final LocalDateTime occurredAt = LocalDateTime.now();  // 錯誤！
}

// ❌ 錯誤：使用 System.currentTimeMillis()
long timestamp = System.currentTimeMillis();  // 錯誤！
```

### 為什麼這是反模式：
1. **不可測試**：無法在測試中控制時間，導致測試結果不穩定
2. **時間不一致**：系統不同部分可能使用不同的時間
3. **除錯困難**：無法重現特定時間點的行為

### ✅ 正確做法：
```java
// ✅ 正確：使用 DateProvider.now()
apply(new PlanCreated(
    planId,
    name,
    userId,
    UUID.randomUUID(),
    DateProvider.now()  // 正確！可測試
));

// ✅ 正確：在測試中控制時間
@Test
void should_create_event_with_specific_time() {
    // Given
    DateProvider.setDate("2024-01-01T10:00:00Z");
    
    // When
    var event = createEvent();
    
    // Then
    assertThat(event.getOccurredOn()).isEqualTo("2024-01-01T10:00:00Z");
    
    // Cleanup
    DateProvider.resetDate();
}
```

## 12. ❌ 測試失敗時直接修改 ezSpec 測試規格

當測試執行失敗時，直接修改 Given-When-Then 的內容而不與人類確認。

### 錯誤範例：
```java
// ❌ 錯誤：測試失敗後直接修改 Given-When-Then
@EzScenario
public void create_plan_successfully() {
    feature.newScenario("Create plan with valid input")
        .Given("valid plan input", env -> {
            // 原本：預期某個業務規則
            // 錯誤：因為測試失敗就改成別的規則
            var input = CreatePlanInput.builder()
                .name("Plan")  // 改了！原本可能有長度限制
                .build();
        })
        .When("creating the plan", env -> {
            // 測試邏輯
        })
        .Then("plan should be created", env -> {
            // 錯誤：降低了期望值來通過測試
            assertThat(result.isSuccessful()).isTrue();  // 移除了其他驗證
        });
}
```

### 為什麼這是反模式：
1. **破壞業務規格**：ezSpec 測試代表業務需求和規格，不是實現細節
2. **掩蓋真實問題**：測試失敗可能代表業務邏輯有問題
3. **降低品質標準**：為了通過測試而降低期望值
4. **規格失真**：Given-When-Then 不再反映真實的業務場景

### ✅ 正確做法：
```java
// ✅ 正確：測試失敗時先分析原因
@Test
public void when_test_fails() {
    // 1. 停止並分析測試失敗的原因
    // 2. 與人類確認：是業務規格錯誤？還是實現錯誤？
    // 3. 如果是規格錯誤，與產品負責人確認修改
    // 4. 如果是實現錯誤，修改 production code
}
```

### 🛑 AI 必須遵循的流程：
1. **測試失敗時暫停**：不要立即修改測試內容
2. **分析失敗原因**：記錄具體的失敗資訊
3. **尋求人類確認**：
   ```
   測試失敗了，失敗原因是：[具體說明]
   
   請確認：
   - 是測試的 Given-When-Then 規格有誤？
   - 還是 production code 實現有問題？
   
   我應該修改測試規格還是修改實現代碼？
   ```
4. **等待明確指示**：不得自行決定修改測試規格

## 總結

避免這些反模式的關鍵：
1. 保持領域模型的豐富性
2. 遵守架構層次和邊界
3. 正確使用 Event Sourcing 和 CQRS
4. 編寫測試驗證行為而非實現
5. 注意性能影響並適當優化
6. 使用 DateProvider 確保時間的可測試性
7. 尊重測試規格，失敗時先確認再修改