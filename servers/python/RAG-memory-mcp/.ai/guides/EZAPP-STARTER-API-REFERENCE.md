# ezapp-starter API 參考指南 🚀

## 關於 ezapp-starter

`ezapp-starter` 是一個整合框架，版本 1.0.0，包含了所有 EZDDD、CQRS、Event Sourcing 相關的功能。

**Maven 依賴：**
```xml
<dependency>
    <groupId>tw.teddysoft.ezapp</groupId>
    <artifactId>ezapp-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🔥 重要：ezapp-starter 已包含以下所有框架

不需要單獨引入以下依賴，因為 ezapp-starter 已經包含：
- ezddd-core
- ezddd-gateway
- ezddd-postgres  
- ezcqrs
- ezspec
- ucontract

## 📦 核心套件結構與類別

### 1. Entity Layer (tw.teddysoft.ezddd.entity.*)

#### 基礎類別
```java
// Aggregate Root (Event Sourcing 版本)
import tw.teddysoft.ezddd.entity.EsAggregateRoot;
import tw.teddysoft.ezddd.entity.AggregateRoot;

// Domain Events
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;      // 內部領域事件
import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;   // 事件類型映射

// Value Objects & Entity
import tw.teddysoft.ezddd.entity.ValueObject;
import tw.teddysoft.ezddd.entity.Entity;
```

#### 常用 Aggregate Root 方法
```java
// EsAggregateRoot 有兩個泛型參數：ID 類型和 Events 類型
public abstract class EsAggregateRoot<ID, Events extends InternalDomainEvent> {
    protected void addDomainEvent(DomainEvent event);
    protected void clearDomainEvents();
    public List<DomainEvent> getDomainEvents();
    public long getVersion();
    public String getStreamName();
}
```

### 2. Use Case Layer (tw.teddysoft.ezddd.usecase.* & tw.teddysoft.ezddd.cqrs.*)

#### Command/Query 基礎
```java
// CQRS Command/Query Pattern
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;           // CQRS 輸出物件

// Input/Output
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

// Use Case Exceptions & Exit Codes
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;                  // 退出碼枚舉
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;   // Use Case 失敗例外
```

#### Repository Pattern
```java
// 基礎 Repository
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

// Outbox Pattern
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxData;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper;
```

#### Projection Pattern
```java
// Projection 有兩個泛型參數：Input 和 Output
import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;

// 使用範例
public interface ProductsProjection extends Projection<ProductsProjectionInput, List<ProductData>> {
    // query 方法從 Projection 介面繼承
}
```

#### Inquiry Pattern (跨聚合查詢)
```java
// 注意：Inquiry 在實際專案中通常是自定義介面，不繼承框架類別
// 範例：FindPbisBySprintIdInquiry 是專案自定義的介面
// 位置：[rootPackage].[aggregate].usecase.port.out.inquiry
```

#### Archive Pattern (Query Model CRUD)
```java
import tw.teddysoft.ezddd.cqrs.usecase.query.Archive;
```

#### Reactor Pattern
```java
// Reactor 必須繼承 Reactor<DomainEventData>
import tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
```

### 3. Domain Event Support

```java
// Event Data & Mapper
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
```

### 4. Message Support

```java
// Message Bus & Producer
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;

// In-Memory Message Broker (另一套實作)
import tw.teddysoft.ezddd.message.broker.adapter.InMemoryMessageBroker;
import tw.teddysoft.ezddd.message.broker.adapter.PostEventFailureException;
```

### 5. PostgreSQL/Outbox Support (tw.teddysoft.ezddd.data.*)

```java
// PgMessageStore (需透過 JpaRepositoryFactory 創建)
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;

// Event Store
import tw.teddysoft.ezddd.data.io.ezes.relay.EzesCatchUpRelay;
```

### 6. 專案自訂共用類別

⚠️ **注意**：以下類別不是 ezapp-starter 的一部分，需要在專案中自行實作：

```java
// DateProvider - 統一的日期時間管理（放在 [rootPackage].common.entity）
// 範例：tw.teddysoft.aiscrum.common.entity.DateProvider
public class DateProvider {
    public static Instant now() { /* 實作 */ }
    public static void useFixedInstant(Instant instant) { /* 測試用 */ }
    public static void useSystemTime() { /* 恢復系統時間 */ }
}

// GenericInMemoryRepository - Repository 的記憶體實作（放在 [rootPackage].common.adapter.out.repository）
// 用於 test-inmemory profile

// MyInMemoryMessageBroker - 異步訊息傳遞（放在 [rootPackage].common）
// 基於 Google EventBus 實作

// MyInMemoryMessageProducer - MessageProducer 實作（放在 [rootPackage].common）
// 用於 Outbox Pattern
```

詳細實作請參考：`.ai/tech-stacks/java-ca-ezddd-spring/examples/generation-templates/local-utils.md`

### 7. Testing Support (tw.teddysoft.ezspec.*)

```java
// BDD Testing - JUnit 5 Extension
import tw.teddysoft.ezspec.extension.junit5.EzScenario;

// Feature & Reporting
import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.EzFeatureReport;
import tw.teddysoft.ezspec.keyword.Feature;
import tw.teddysoft.ezspec.visitor.PlainTextReport;

// Legacy Annotations (可能較少使用)
import tw.teddysoft.ezspec.annotation.Spec;
```

### 8. Design by Contract (tw.teddysoft.ucontract.*)

```java
// 🔴 重要：僅用於 EsAggregateRoot 及其子類別！
// ValueObject、Entity、Domain Events 使用 Objects.requireNonNull
import static tw.teddysoft.ucontract.Contract.*;

// 主要方法：
// - requireNotNull(String name, Object obj)  // 前置條件：檢查非空
// - require(String message, Supplier<Boolean> condition)  // 前置條件：檢查條件
// - ensureNotNull(String name, Object obj)  // 後置條件：檢查非空  
// - ensure(String message, Supplier<Boolean> condition)  // 後置條件：檢查條件
// - invariantNotNull(String name, Object obj)  // 不變式：檢查非空
// - invariant(String message, Supplier<Boolean> condition)  // 不變式：檢查條件
```

#### 使用規則：
- **EsAggregateRoot 及其子類別**：使用 `Contract.requireNotNull()`
- **ValueObject (record)**：使用 `Objects.requireNonNull()`  
- **Entity**：使用 `Objects.requireNonNull()`
- **Domain Events (record)**：使用 `Objects.requireNonNull()`
- **UseCase Service**：使用 `Contract.requireNotNull()`（因為 Service 需要 DBC）
- **Controller**：使用 `Objects.requireNonNull()`

## 🎯 實作範例

### 1. Aggregate Root 實作
```java
import tw.teddysoft.ezddd.entity.EsAggregateRoot;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import static tw.teddysoft.ucontract.Contract.*;

// EsAggregateRoot 需要兩個泛型參數
public class Product extends EsAggregateRoot<ProductId, ProductEvents> {
    private ProductId id;
    private ProductName name;
    
    public Product(ProductId id, ProductName name) {
        requireNotNull("id", id);
        requireNotNull("name", name);
        
        this.id = id;
        this.name = name;
        
        // 使用 apply() 發出事件（Event Sourcing 模式）
        apply(new ProductEvents.ProductCreated(
            id,
            name,
            Map.of("creatorId", "system"),
            UUID.randomUUID(),
            DateProvider.now()  // 🔴 重要：使用 DateProvider.now()，不要用 Instant.now()
        ));
    }
    
    @Override
    public ProductId getId() {
        return id;
    }
    
    // Event Sourcing: 處理事件
    @Override
    protected void when(ProductEvents event) {
        switch (event) {
            case ProductEvents.ProductCreated e -> {
                this.id = e.productId();
                this.name = e.name();
            }
            default -> {}
        }
    }
}

// Domain Events 使用 sealed interface（🔴 重要：所有 events 在同一個檔案中）
public sealed interface ProductEvents extends InternalDomainEvent permits
        ProductEvents.ProductCreated,
        ProductEvents.ProductDeleted {
    
    // 共用方法：獲取 aggregate ID
    ProductId productId();
    
    // 🔴 重要：必須實作 aggregateId() 返回 String
    @Override
    default String aggregateId() {
        return productId().value();
    }
    
    // 🔴 [Aggregate]Created 必須額外實作 ConstructionEvent
    record ProductCreated(
        ProductId productId,
        ProductName name,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements ProductEvents, InternalDomainEvent.ConstructionEvent {
        
        // 建構子可以加入驗證（Domain Events 也使用 Objects.requireNonNull）
        public ProductCreated {
            Objects.requireNonNull(productId, "productId cannot be null");
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(metadata, "metadata cannot be null");
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(occurredOn, "occurredOn cannot be null");
        }
    }
    
    // 🔴 [Aggregate]Deleted 必須額外實作 DestructionEvent
    record ProductDeleted(
        ProductId productId,
        String reason,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements ProductEvents, InternalDomainEvent.DestructionEvent { }
}
```

### 2. Value Object 實作
```java
import tw.teddysoft.ezddd.entity.ValueObject;
import java.util.Objects;

// 使用 record 並 implements ValueObject（不是 extends）
// 🔴 重要：ValueObject 使用 Objects.requireNonNull，不用 Contract
public record ProductId(String value) implements ValueObject {
    
    public ProductId {
        Objects.requireNonNull(value, "ProductId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductId value cannot be empty");
        }
    }
    
    // 🔴 重要：必須覆寫 toString() 返回純值（對 Outbox Pattern 的 stream name 生成很重要）
    @Override
    public String toString() {
        return value;
    }
    
    // 必須提供 valueOf 方法（框架序列化需要）
    public static ProductId valueOf(String value) {
        return new ProductId(value);
    }
    
    public static ProductId create() {
        return new ProductId(UUID.randomUUID().toString());
    }
}
```

### 3. Entity 實作（Aggregate 內的 Entity）
```java
import tw.teddysoft.ezddd.entity.Entity;
import java.util.Objects;

// Entity 有泛型參數，使用 implements
// 🔴 重要：Entity 也使用 Objects.requireNonNull，不用 Contract
public class Task implements Entity<TaskId> {
    private final TaskId id;
    private String name;
    private TaskStatus status;
    
    public Task(TaskId id, String name) {
        this.id = Objects.requireNonNull(id, "Task id cannot be null");
        this.name = Objects.requireNonNull(name, "Task name cannot be null");
        this.status = TaskStatus.TODO;
    }
    
    @Override
    public TaskId getId() {
        return id;
    }
    
    public void moveToInProgress() {
        if (status != TaskStatus.TODO) {
            throw new IllegalStateException("Task must be TODO to move to IN_PROGRESS");
        }
        this.status = TaskStatus.IN_PROGRESS;
    }
}
```

### 4. Use Case 實作
```java
// UseCase 介面定義（extends Command 或 Query）
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface CreateProductUseCase extends Command<CreateProductUseCase.CreateProductInput, CqrsOutput> {
    
    class CreateProductInput implements Input {
        public String id;
        public String name;
        public String userId;
    }
}

// Service 實作
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

public class CreateProductService implements CreateProductUseCase {
    private final Repository<Product, ProductId> repository;
    
    public CreateProductService(Repository<Product, ProductId> repository) {
        this.repository = Objects.requireNonNull(repository);
    }
    
    @Override
    public CqrsOutput execute(CreateProductInput input) {
        Product product = new Product(
            ProductId.valueOf(input.id),
            new ProductName(input.name)
        );
        repository.save(product);
        return CqrsOutput.create().setId(product.getId().value());
    }
}
```

### 5. Repository 配置

#### InMemory Repository
```java
@Bean
@Profile({"inmemory", "test-inmemory"})
public Repository<Product, ProductId> productRepository(
        MessageBus<DomainEvent> messageBus) {
    return new GenericInMemoryRepository<>(messageBus);
}
```

#### Outbox Repository
```java
@Bean
@Profile({"outbox", "test-outbox"})
public Repository<Product, ProductId> productOutboxRepository(
        PgMessageDbClient pgClient) {
    return new OutboxRepository<>(
        pgClient,
        ProductMapper.newMapper(),
        "Product"
    );
}
```

### 6. Reactor 實作
```java
import tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;

@Component
public class NotifyTeamReactor extends Reactor<DomainEventData> {
    
    @Override
    public void execute(Object event) {
        if (event instanceof SprintCreatedData) {
            // 處理事件
        }
    }
}
```

## 🔧 Spring Configuration

### 必要的 Configuration 類別
```java
@Configuration
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.*.adapter.out.database.springboot.projection",
    "tw.teddysoft.aiscrum.*.adapter.out.database.springboot.inquiry",
    "tw.teddysoft.ezddd.data.io.ezes.store"  // PgMessageDbClient
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum.*.usecase.port.out",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
public class JpaConfiguration {
}
```

## 🎯 專案自定義類別（不是框架提供）

以下是專案需要自行實作的類別：

### 1. DateProvider
```java
package [rootPackage].common.entity;

import java.time.Instant;

/**
 * 提供統一的時間管理機制
 * 🔴 重要：Domain Events 必須使用 DateProvider.now()，不要用 Instant.now()
 */
public class DateProvider {
    private static Instant fixedInstant;
    
    public static Instant now() {
        return fixedInstant != null ? fixedInstant : Instant.now();
    }
    
    // 測試用：固定時間
    public static void useFixedClockAt(Instant instant) {
        fixedInstant = instant;
    }
    
    // 測試用：重置為系統時間
    public static void useSystemDefaultZoneClock() {
        fixedInstant = null;
    }
}
```

### 2. GenericInMemoryRepository
```java
package [rootPackage].common.adapter.out.repository;

import tw.teddysoft.ezddd.entity.Aggregate;
import tw.teddysoft.ezddd.entity.AggregateRoot;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository 的記憶體實作，用於 test-inmemory profile
 * 🔴 重要：測試中不要手動建立，要使用 Spring DI 注入
 */
public class GenericInMemoryRepository<T extends AggregateRoot<ID>, ID> implements Repository<T, ID> {
    private final Map<ID, T> store = new ConcurrentHashMap<>();
    private final MessageBus<DomainEvent> messageBus;
    
    public GenericInMemoryRepository(MessageBus<DomainEvent> messageBus) {
        this.messageBus = messageBus;
    }
    
    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }
    
    @Override
    public T save(T aggregate) {
        store.put(aggregate.getId(), aggregate);
        // 發布領域事件
        aggregate.getDomainEvents().forEach(messageBus::publish);
        aggregate.clearDomainEvents();
        return aggregate;
    }
    
    @Override
    public void delete(T aggregate) {
        store.remove(aggregate.getId());
    }
    
    // 測試用：清空資料
    public void clear() {
        store.clear();
    }
}
```

### 3. Inquiry Pattern（查詢模式）
```java
// Inquiry 介面（專案自定義，不是框架提供）
package [rootPackage].[aggregate].usecase.port.out.inquiry;

public interface Find[Entity]By[Criteria]Inquiry {
    List<[Entity]Data> findBy[Criteria](String criteria);
}

// JPA 實作
package [rootPackage].[aggregate].adapter.out.persistence.inquiry;

@Repository  // Inquiry 可以加 @Repository
public class JpaFind[Entity]By[Criteria]Inquiry implements Find[Entity]By[Criteria]Inquiry {
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<[Entity]Data> findBy[Criteria](String criteria) {
        // JPQL 查詢實作
    }
}
```

## ⚠️ 重要提醒

1. **不要嘗試重新實作框架類別** - 這些都由 ezapp-starter 提供
2. **Import 路徑必須正確** - 使用上述指定的 import 路徑
3. **PgMessageDbClient 必須透過 JpaRepositoryFactory 創建** - 不能用 new
4. **OutboxMapper 必須是內部類別** - 參考 FRAMEWORK-API-INTEGRATION-GUIDE.md
5. **使用 jakarta.persistence 而非 javax.persistence**

## 🔍 如何讓 AI 使用這份文件

在 CLAUDE.md 或 prompt 中加入：
```markdown
### 框架 API 參考
當需要使用 ezapp-starter 的類別時，請參考：
- `.ai/guides/EZAPP-STARTER-API-REFERENCE.md` - 完整的 API 參考與 import 路徑
- 不要嘗試猜測或創建框架類別，直接使用文件中的 import
```

## 📊 快速查詢表

| 功能 | Import 路徑 | 用途 |
|-----|-----------|------|
| **Entity Layer** | | |
| EsAggregateRoot | tw.teddysoft.ezddd.entity.EsAggregateRoot | Event Sourcing 聚合根 |
| DomainEvent | tw.teddysoft.ezddd.entity.DomainEvent | 領域事件 |
| InternalDomainEvent | tw.teddysoft.ezddd.entity.InternalDomainEvent | 內部領域事件 |
| DomainEventTypeMapper | tw.teddysoft.ezddd.entity.DomainEventTypeMapper | 事件類型映射 |
| ValueObject | tw.teddysoft.ezddd.entity.ValueObject | 值物件基礎類別 |
| Entity | tw.teddysoft.ezddd.entity.Entity | 實體基礎類別 |
| **CQRS Layer** | | |
| Command | tw.teddysoft.ezddd.cqrs.usecase.command.Command | Command 註解 |
| Query | tw.teddysoft.ezddd.cqrs.usecase.query.Query | Query 註解 |
| CqrsOutput | tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput | CQRS 輸出物件 |
| Projection | tw.teddysoft.ezddd.cqrs.usecase.query.Projection | 查詢投影 |
| Archive | tw.teddysoft.ezddd.cqrs.usecase.query.Archive | Query Model CRUD |
| **Use Case Layer** | | |
| Repository | tw.teddysoft.ezddd.usecase.port.out.repository.Repository | Repository 介面 |
| Reactor | tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor | 事件處理器 |
| Input | tw.teddysoft.ezddd.usecase.port.in.interactor.Input | Use Case 輸入介面 |
| ExitCode | tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode | 退出碼枚舉 |
| UseCaseFailureException | tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException | Use Case 失敗例外 |
| DomainEventData | tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData | 事件資料 |
| DomainEventMapper | tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper | 事件映射器 |
| MessageBus | tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus | 訊息匯流排 |
| BlockingMessageBus | tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus | 阻塞式訊息匯流排 |
| **Testing** | | |
| EzScenario | tw.teddysoft.ezspec.extension.junit5.EzScenario | JUnit 5 BDD 測試 |
| @Spec | tw.teddysoft.ezspec.annotation.Spec | BDD 測試註解（舊版） |
| **專案自訂** | | |
| DateProvider | [rootPackage].common.entity.DateProvider | 日期時間管理（需自行實作） |

## 🚀 使用建議

1. **建立專案時**：複製此文件到專案的 `.ai/` 目錄
2. **更新 sub-agent prompts**：確保所有 sub-agent 都引用此文件
3. **定期更新**：當 ezapp-starter 版本更新時，同步更新此文件