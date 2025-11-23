# Repository 範例與模式

本目錄包含 Repository Pattern 的設計模式說明與實作範例，以及測試用的 GenericInMemoryRepository。

## 📋 概述

Repository 是 Domain 和 Infrastructure 之間的抽象層，定義了如何存取和保存 Aggregate。在 Event Sourcing 架構中，Repository 負責儲存和重建 Aggregate 的事件流。

## 🎯 核心概念

### Repository Pattern
- **抽象介面**：定義在 Domain 層
- **具體實作**：實作在 Infrastructure 層
- **聚合存取**：只針對 Aggregate Root
- **隔離關注**：Domain 不關心持久化細節

### 在 Clean Architecture 中的位置
```
Domain Layer → Port (Repository Interface)
                        ↓
Infrastructure Layer → Adapter (Repository Implementation)
```

## 📁 檔案結構

```
repository/
├── README.md                           # 本文件
├── GenericInMemoryRepository.java      # 測試用的記憶體實作
├── EventStoreRepository.java           # Event Store 實作範例
└── JpaPlanRepository.java              # JPA 實作範例
```

## 🔧 實作要點

### 1. Repository 介面（Domain 層）

```java
package tw.teddysoft.ezddd.usecase.port.out.repository;

public interface Repository<T extends AggregateRoot<ID>, ID> {
    
    void save(T aggregate);
    
    Optional<T> findById(ID id);
    
    void delete(T aggregate);
    
    List<T> findAll();
    
    boolean existsById(ID id);
}
```

### 2. GenericInMemoryRepository（測試用）

```java
package [package].common.adapter.out.repository;

import tw.teddysoft.ezddd.entity.AggregateRoot;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GenericInMemoryRepository<T extends AggregateRoot<ID>, ID> 
        implements Repository<T, ID> {
    
    private final Map<ID, T> storage = new ConcurrentHashMap<>();
    private MessageBus messageBus;
    
    public GenericInMemoryRepository() {
        this(null);
    }
    
    public GenericInMemoryRepository(MessageBus messageBus) {
        this.messageBus = messageBus;
    }
    
    @Override
    public void save(T aggregate) {
        Objects.requireNonNull(aggregate, "Aggregate cannot be null");
        Objects.requireNonNull(aggregate.getId(), "Aggregate ID cannot be null");
        
        // 儲存聚合根
        storage.put(aggregate.getId(), aggregate);
        
        // 發布未提交的事件
        if (messageBus != null) {
            aggregate.getUncommittedChanges().forEach(messageBus::publish);
        }
        
        // 標記事件為已提交
        aggregate.markChangesAsCommitted();
    }
    
    @Override
    public Optional<T> findById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return Optional.ofNullable(storage.get(id));
    }
    
    @Override
    public void delete(T aggregate) {
        Objects.requireNonNull(aggregate, "Aggregate cannot be null");
        storage.remove(aggregate.getId());
    }
    
    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }
    
    @Override
    public boolean existsById(ID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return storage.containsKey(id);
    }
    
    // 測試輔助方法
    public void clear() {
        storage.clear();
    }
    
    public int count() {
        return storage.size();
    }
}
```

### 3. Event Store Repository 實作

```java
package [package].adapter.out.repository;

import tw.teddysoft.ezddd.gateway.eventstore.EventStore;
import tw.teddysoft.ezddd.entity.DomainEvent;

public class EventStoreRepository<T extends EsAggregateRoot<ID, E>, ID, E extends DomainEvent> 
        implements Repository<T, ID> {
    
    private final EventStore eventStore;
    private final Class<T> aggregateClass;
    private final String category;
    
    public EventStoreRepository(EventStore eventStore, Class<T> aggregateClass, String category) {
        this.eventStore = eventStore;
        this.aggregateClass = aggregateClass;
        this.category = category;
    }
    
    @Override
    public void save(T aggregate) {
        List<E> uncommittedEvents = aggregate.getUncommittedChanges();
        
        if (!uncommittedEvents.isEmpty()) {
            // 儲存事件到 Event Store
            String streamId = category + "-" + aggregate.getId();
            eventStore.appendToStream(streamId, uncommittedEvents);
            
            // 標記事件為已提交
            aggregate.markChangesAsCommitted();
        }
    }
    
    @Override
    public Optional<T> findById(ID id) {
        String streamId = category + "-" + id;
        List<E> events = eventStore.readStreamEvents(streamId);
        
        if (events.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            // 使用事件重建聚合根
            Constructor<T> constructor = aggregateClass.getConstructor(List.class);
            T aggregate = constructor.newInstance(events);
            return Optional.of(aggregate);
        } catch (Exception e) {
            throw new RepositoryException("Failed to reconstruct aggregate", e);
        }
    }
}
```

### 4. JPA Repository 實作（用於 Projection）

```java
package [package].adapter.out.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPlanRepository extends JpaRepository<PlanData, String> {
    
    List<PlanData> findByUserId(String userId);
    
    boolean existsByIdAndUserId(String id, String userId);
    
    @Query("SELECT p FROM PlanData p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    Page<PlanData> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId, Pageable pageable);
}

// 對應的實作轉換
@Component
public class PlanRepositoryAdapter implements PlanRepository {
    
    private final JpaPlanRepository jpaRepository;
    private final EventStoreRepository<Plan, PlanId, PlanEvents> eventStoreRepository;
    
    @Override
    public void save(Plan plan) {
        // 儲存到 Event Store
        eventStoreRepository.save(plan);
        
        // 更新 Projection
        PlanData data = PlanMapper.toData(plan);
        jpaRepository.save(data);
    }
    
    @Override
    public Optional<Plan> findById(PlanId id) {
        // 從 Event Store 重建
        return eventStoreRepository.findById(id);
    }
}
```

## 💡 設計原則

### 1. 聚合一致性
- Repository 以 Aggregate 為單位
- 保證事務邊界
- 維護不變條件

### 2. 技術無關性
- Domain 層只定義介面
- Infrastructure 層處理技術細節
- 易於切換實作

### 3. 測試友好
- 提供 In-Memory 實作
- 支援單元測試
- 隔離外部依賴

## 📝 使用範例

### 在 Use Case 中使用

```java
public class CreatePlanService implements CreatePlanUseCase {
    
    private final Repository<Plan, PlanId> planRepository;
    
    public CreatePlanService(Repository<Plan, PlanId> planRepository) {
        this.planRepository = planRepository;
    }
    
    @Override
    public CqrsOutput execute(CreatePlanInput input) {
        // 創建聚合根
        Plan plan = new Plan(
            PlanId.create(),
            input.name,
            input.userId
        );
        
        // 儲存聚合根
        planRepository.save(plan);
        
        return CqrsOutput.create()
            .setId(plan.getId().value())
            .setExitCode(ExitCode.SUCCESS);
    }
}
```

### 在測試中使用

```java
@Test
public void testSaveAndFind() {
    // Given
    Repository<Plan, PlanId> repository = new GenericInMemoryRepository<>();
    Plan plan = new Plan(PlanId.create(), "Test Plan", "user123");
    
    // When
    repository.save(plan);
    
    // Then
    Optional<Plan> found = repository.findById(plan.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Plan");
}
```

### Spring 配置

```java
@Configuration
public class RepositoryConfig {
    
    @Bean
    @Profile("test")
    public Repository<Plan, PlanId> testPlanRepository(MessageBus messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    @Bean
    @Profile("!test")
    public Repository<Plan, PlanId> planRepository(
            EventStore eventStore,
            JpaPlanRepository jpaPlanRepository) {
        
        EventStoreRepository<Plan, PlanId, PlanEvents> esRepository = 
            new EventStoreRepository<>(eventStore, Plan.class, Plan.CATEGORY);
            
        return new PlanRepositoryAdapter(esRepository, jpaPlanRepository);
    }
}
```

## ⚠️ 注意事項

1. **避免貧血 Repository**
   - 不要加入業務邏輯
   - 只處理持久化相關操作

2. **事件發布時機**
   - 在 save() 時發布事件
   - 確保事務一致性

3. **效能考量**
   - 考慮快取策略
   - 優化事件讀取

4. **並發控制**
   - 實作樂觀鎖
   - 處理版本衝突

## 🔗 相關資源

- [Aggregate 範例](../aggregate/)
- [Event Sourcing 模式](../event-sourcing/)
- [Projection 範例](../projection/)
- [測試範例](../test/)