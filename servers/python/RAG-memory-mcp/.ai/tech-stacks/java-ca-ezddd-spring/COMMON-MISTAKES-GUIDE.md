# 常見錯誤與解決方案指南

> 本指南收集了開發過程中最常遇到的錯誤及其解決方案，幫助 AI 編碼助手快速診斷和修復問題。

## 📋 目錄

1. [編譯錯誤](#編譯錯誤)
2. [測試錯誤](#測試錯誤)
3. [Spring Boot 錯誤](#spring-boot-錯誤)
4. [JPA 錯誤](#jpa-錯誤)
5. [事件處理錯誤](#事件處理錯誤)
6. [Domain 模型錯誤](#domain-模型錯誤)

## 🔴 編譯錯誤

### 1. Cannot find symbol: DomainEvent

**錯誤訊息**：
```
error: cannot find symbol
import tw.teddysoft.ezddd.entity.DomainEvent;
                                 ^
```

**原因**：Maven 依賴未正確下載或版本錯誤

**解決方案**：
```bash
# 1. 檢查 Maven 設定
cat ~/.m2/settings.xml

# 2. 強制更新依賴
mvn clean install -U

# 3. 確認依賴版本
mvn dependency:tree | grep ezddd
```

### 2. Package does not exist: ucontract

**錯誤訊息**：
```
error: package tw.teddysoft.ucontract does not exist
```

**原因**：uContract 的 artifactId 大小寫錯誤

**解決方案**：
```xml
<!-- ❌ 錯誤 -->
<artifactId>ucontract</artifactId>

<!-- ✅ 正確 -->
<artifactId>uContract</artifactId>
```

### 3. Incompatible types: long cannot be converted to int

**常見位置**：`getVersion()` 方法

**解決方案**：
```java
// ❌ 錯誤
int oldVersion = getVersion();

// ✅ 正確
long oldVersion = getVersion();
```

### 4. Wrong package for Repository

**錯誤訊息**：
```
error: cannot find symbol
import tw.teddysoft.ezddd.entity.Repository;
```

**原因**：Repository 在錯誤的 package

**解決方案**：
```java
// ❌ 錯誤
import tw.teddysoft.ezddd.entity.Repository;
import tw.teddysoft.ezddd.core.aggregate.repository.Repository;

// ✅ 正確
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
```

## 🧪 測試錯誤

### 1. LazyInitializationException

**錯誤訊息**：
```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection
```

**原因**：使用了 LAZY loading

**解決方案**：
```java
// 永遠使用 EAGER loading
@OneToMany(fetch = FetchType.EAGER)
private Set<ProjectData> projectDatas;
```

### 2. DateProvider not mocked

**錯誤訊息**：
```
Expected: <2025-07-30T10:00:00Z>
Actual: <2025-07-30T10:00:01Z>
```

**原因**：測試中未正確 mock DateProvider

**解決方案**：
```java
@BeforeEach
void setUp() {
    DateProvider.setProvider(() -> Instant.parse("2025-07-30T10:00:00Z"));
}

@AfterEach
void tearDown() {
    DateProvider.resetProvider();
}
```

### 3. Repository not initialized

**錯誤訊息**：
```
NullPointerException at repository.save()
```

**解決方案**：
```java
@BeforeEach
void setUp() {
    messageBus = new MessageBus();
    repository = new GenericInMemoryRepository<>(messageBus);
}
```

## 🌱 Spring Boot 錯誤

### 1. Bean creation error

**錯誤訊息**：
```
Error creating bean with name 'planRepository': 
Unsatisfied dependency expressed through constructor
```

**原因**：缺少 @Repository 註解或配置錯誤

**解決方案**：
```java
@Repository
public interface PlanRepository extends JpaRepository<PlanData, String> {
}

// 確保 @ComponentScan 包含正確的 package
@ComponentScan(basePackages = "tw.teddysoft.aiplan")
```

### 2. No qualifying bean found

**錯誤訊息**：
```
No qualifying bean of type 'MessageBus' available
```

**解決方案**：
```java
@Configuration
public class MessageBusConfig {
    @Bean
    public MessageBus messageBus() {
        return new MessageBus();
    }
}
```

## 💾 JPA 錯誤

### 1. Detached entity passed to persist

**錯誤訊息**：
```
org.hibernate.PersistentObjectException: detached entity passed to persist
```

**原因**：嘗試 persist 已存在的實體

**解決方案**：
```java
// ❌ 錯誤
entityManager.persist(existingEntity);

// ✅ 正確
entityManager.merge(existingEntity);
```

### 2. TransientPropertyValueException

**錯誤訊息**：
```
object references an unsaved transient instance - save the transient instance before flushing
```

**原因**：關聯的實體未被保存

**解決方案**：
```java
@OneToMany(cascade = CascadeType.ALL)  // 加入 cascade
private Set<ProjectData> projectDatas;
```

### 3. Column name conflict

**錯誤訊息**：
```
Duplicate column name 'name'
```

**解決方案**：
```java
@Column(name = "project_name")  // 使用明確的欄位名稱
private String name;
```

## 📨 事件處理錯誤

### 1. Event not registered

**錯誤訊息**：
```
Unknown event type: tw.teddysoft.aiplan.plan.entity.PlanEvents$PlanCreated
```

**原因**：事件未在 BootstrapConfig 中註冊

**解決方案**：
```java
@Configuration
public class BootstrapConfig {
    @PostConstruct
    public void init() {
        DomainEventTypeRegistry.register(
            PlanEvents.PlanCreated.class,
            // 其他事件...
        );
    }
}
```

### 2. Event handler not called

**原因**：when() 方法中缺少事件處理

**解決方案**：
```java
@Override
protected void when(DomainEvent event) {
    switch (event) {
        case PlanEvents.PlanCreated e -> {
            this.planId = new PlanId(e.planId());
            this.name = e.name();
        }
        // 處理其他事件
    }
}
```

## 🏛️ Domain 模型錯誤

### 1. Aggregate state not updated

**症狀**：apply() 事件後狀態未改變

**原因**：when() 方法未正確實作

**解決方案**：
```java
// 確保 when() 方法處理所有事件
@Override
protected void when(PlanEvents event) {
    switch (event) {
        case PlanEvents.PlanCreated e -> {
            this.planId = e.planId();
            this.name = e.name();
            this.userId = e.userId();
            this.isDeleted = false;
        }
        case PlanEvents.TaskCreated e -> {
            // 處理任務創建邏輯
            Task task = new Task(e.taskId(), e.taskName());
            this.tasks.put(e.taskId(), task);
        }
        // 處理所有事件類型
        default -> {
            // 處理未知事件類型
        }
    }
}
```

### 2. Value Object equality failure

**症狀**：相同值的 Value Object 不相等

**解決方案**：
```java
public record PlanId(String value) implements ValueObject {
    // record 自動生成正確的 equals() 和 hashCode()
}
```

### 3. Contract violation not detected

**原因**：未正確使用 uContract

**解決方案**：
```java
public Plan(String name, String userId) {
    Contract.requireNotNull(name, "Plan name cannot be null");
    Contract.require(!name.trim().isEmpty(), "Plan name cannot be empty");
    // 繼續建構...
}
```

## 🔧 快速診斷流程

當遇到錯誤時，按以下順序檢查：

1. **編譯錯誤**
   - 檢查 import 語句
   - 確認依賴版本
   - 檢查型別相容性

2. **測試錯誤**
   - 確認測試設置 (@BeforeEach)
   - 檢查 mock 設定
   - 驗證測試資料

3. **運行時錯誤**
   - 檢查 Spring 配置
   - 確認 Bean 註冊
   - 查看 JPA 映射

4. **業務邏輯錯誤**
   - 檢查事件處理
   - 驗證領域規則
   - 確認狀態轉換

## 💡 預防措施

1. **使用範本**：總是從範本開始，避免從零開始
2. **逐步測試**：每次修改後立即編譯和測試
3. **版本一致**：確保所有依賴版本與 VERSION-CONTROL.md 一致
4. **遵循模式**：嚴格遵循既有的程式碼模式

## 📚 相關資源

- [ANTI-PATTERNS.md](./ANTI-PATTERNS.md) - 反模式參考
- [DEPENDENCY-TROUBLESHOOTING.md](../../DEPENDENCY-TROUBLESHOOTING.md) - 依賴問題詳解
- [FAQ.md](./FAQ.md) - 常見問題
- [TEMPLATE-USAGE-GUIDE.md](./TEMPLATE-USAGE-GUIDE.md) - 範本使用指南