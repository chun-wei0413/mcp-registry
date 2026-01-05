# Domain Events 設計指南

本文件說明如何設計 Domain Events，包括 Sealed Interface、InternalDomainEvent、Construction/DestructionEvent、metadata 和 TypeMapper。

---

## Sealed Interface 設計

### 🎯 設計意圖

使用 sealed interface 確保所有事件都在一個檔案中定義，便於管理和擴展。

### 📋 實作規範

```java
public sealed interface PlanEvents extends InternalDomainEvent permits
        PlanEvents.PlanCreated,
        PlanEvents.PlanDeleted {

    PlanId planId();  // 所有事件共用的方法

    @Override
    default String source() {
        return planId().value();
    }
}
```

### ✅ 正確範例

```java
public sealed interface PlanEvents extends InternalDomainEvent permits
        PlanEvents.PlanCreated,
        PlanEvents.PlanRenamed,
        PlanEvents.PlanDeleted {

    PlanId planId();

    @Override
    default String source() {
        return planId().value();
    }

    record PlanCreated(...) implements PlanEvents, ConstructionEvent { }
    record PlanRenamed(...) implements PlanEvents { }
    record PlanDeleted(...) implements PlanEvents, DestructionEvent { }
}
```

### 🔍 檢查清單

- [ ] 使用 sealed interface
- [ ] extends InternalDomainEvent
- [ ] permits 列出所有事件
- [ ] 定義共用方法（如 aggregateId）

---

## ConstructionEvent 和 DestructionEvent

### 🎯 設計意圖

標記 Aggregate 的創建和刪除事件，框架可據此做特殊處理。

### 📋 實作規範

```java
// ConstructionEvent：Aggregate 創建
record PlanCreated(...)
    implements PlanEvents, InternalDomainEvent.ConstructionEvent {
}

// DestructionEvent：Aggregate 刪除（軟刪除）
record PlanDeleted(...)
    implements PlanEvents, InternalDomainEvent.DestructionEvent {
}
```

### 🔍 檢查清單

- [ ] 創建事件實作 ConstructionEvent
- [ ] 刪除事件實作 DestructionEvent
- [ ] 一個 Aggregate 只有一個 ConstructionEvent

---

## metadata 使用

### 🎯 設計意圖

metadata 用於存儲審計資訊（如 creatorId, userId）和其他 meta 資料。

### 📋 實作規範

1. **必須使用可變 Map**：
   ```java
   new HashMap<>()  // ✅ 可變
   Map.of()         // ❌ 不可變
   ```

2. **metadata 參數順序**：
   ```java
   record PlanCreated(
       PlanId planId,
       String name,
       // ... 業務參數
       Map<String, String> metadata,  // metadata
       UUID id,                       // event id
       Instant occurredOn             // 時間戳
   ) implements PlanEvents, ConstructionEvent {
   }
   ```

### ✅ 正確範例

```java
apply(new PlanEvents.PlanCreated(
    planId,
    name,
    userId,
    new HashMap<>(),        // ✅ 可變的 HashMap
    UUID.randomUUID(),
    DateProvider.now()
));
```

### ❌ 常見錯誤

```java
apply(new PlanEvents.PlanCreated(
    planId,
    name,
    userId,
    Map.of(),  // ❌ 不可變！框架無法添加 metadata
    UUID.randomUUID(),
    Instant.now()  // ❌ 應該用 DateProvider.now()
));
```

### 🔍 檢查清單

- [ ] metadata 使用 `new HashMap<>()`
- [ ] 使用 `DateProvider.now()`
- [ ] 使用 `UUID.randomUUID()`

---

## TypeMapper 實作

### 🎯 設計意圖

TypeMapper 用於事件的序列化/反序列化，將事件類型名稱映射到 Class。

### 📋 實作規範

```java
class TypeMapper extends DomainEventTypeMapper.DefaultMapper {
    public static final String MAPPING_TYPE_PREFIX = "PlanEvents$";
    public static final String PLAN_CREATED = MAPPING_TYPE_PREFIX + "PlanCreated";
    public static final String PLAN_DELETED = MAPPING_TYPE_PREFIX + "PlanDeleted";

    private static final DomainEventTypeMapper mapper;

    static {
        mapper = DomainEventTypeMapper.create();
        mapper.put(PLAN_CREATED, PlanEvents.PlanCreated.class);
        mapper.put(PLAN_DELETED, PlanEvents.PlanDeleted.class);
    }

    public static DomainEventTypeMapper getInstance() {
        return mapper;
    }
}

static DomainEventTypeMapper mapper() {
    return TypeMapper.getInstance();
}
```

### 🔍 檢查清單

- [ ] TypeMapper 在 Events interface 內部
- [ ] 所有事件都註冊到 mapper
- [ ] 提供 static mapper() 方法

---

## 總結

Domain Events 設計要點：
1. 使用 sealed interface 定義所有事件
2. 標記 ConstructionEvent 和 DestructionEvent
3. metadata 使用可變 HashMap
4. 實作 TypeMapper