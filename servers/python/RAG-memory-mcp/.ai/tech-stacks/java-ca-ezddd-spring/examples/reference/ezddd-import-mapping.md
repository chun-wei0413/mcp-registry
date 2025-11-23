# EZDDD Framework Import Mapping Guide

This guide provides the correct import statements for the ezddd framework (version 3.0.1) to prevent import errors.

## ⚠️ 重要：Package 結構變更

### ezddd 3.0.1 模組結構
- `tw.teddysoft.ezddd:ezddd-core` 是 aggregator POM（只有 2KB），不包含類別
- 實際類別分布在子模組中：
  - `ezddd-entity` - Entity, ValueObject, DomainEvent, AggregateRoot 等
  - `ezddd-common` - 共用工具類別
  - `ezddd-usecase` - UseCase 相關介面
  - `ezcqrs` - Command 和 Query 介面

### ❌ 常見錯誤
**絕對沒有 `tw.teddysoft.ezddd.core.*` 這樣的套件路徑！**

## Entity Layer Imports

### Entities
```java
import tw.teddysoft.ezddd.entity.Entity;
```
**用途**: Entity 是具有身份識別的領域物件，必須實作 `Entity<ID>` 介面
- 用於 Aggregate 內部的實體（如 Project、Task）
- ID 參數是該 Entity 的識別碼類型（如 ProjectId、TaskId）

### Value Objects
```java
import tw.teddysoft.ezddd.entity.ValueObject;
```
**用途**: ValueObject 是不可變的值物件，必須實作 `ValueObject` 介面
- 用於表達業務概念（如 ProjectName、Email、Money）
- 用於識別碼（如 PlanId、ProjectId、TaskId）

### Domain Events
```java
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;  // For internal events
// Note: ConstructionEvent and DestructionEvent are inner interfaces
// Use as: DomainEvent.ConstructionEvent, DomainEvent.DestructionEvent
```
**ezddd 3.0.1 重要變更**: DomainEvent 需要實作 `metadata()` 方法：
```java
@Override
public Map<String, Object> metadata() {
    return Map.of();  // 至少返回空 Map
}
```

### Aggregate Root
```java
import tw.teddysoft.ezddd.entity.AggregateRoot;     // 標準 Aggregate
import tw.teddysoft.ezddd.entity.EsAggregateRoot;   // Event Sourcing Aggregate
```
**用途**: 
- `AggregateRoot<ID, Event>` - 不需要 Event Sourcing 的聚合根
- `EsAggregateRoot<ID, Events>` - 需要 Event Sourcing 的聚合根（必須實作 getCategory() 方法）

### Domain Event Type Mapper
```java
import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
```

## CQRS Layer Imports

### Command Interface
```java
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
```

### Query Interface
```java
import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
```

### CQRS Output
```java
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
```

### Projection
```java
import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;
```

## Use Case Layer Imports

### Input Interface
```java
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;
```

### Exit Code
```java
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
```

### Use Case Failure Exception
```java
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;
```

## Repository and Messaging Imports

### Repository
```java
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
```

### MessageBus
```java
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
```

### BlockingMessageBus (for testing)
```java
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
```

## Common Mistakes to Avoid

### ❌ 最常見的錯誤：使用不存在的 core 套件
1. **Wrong**: `import tw.teddysoft.ezddd.core.entity.DomainEvent;`  
   **Correct**: `import tw.teddysoft.ezddd.entity.DomainEvent;`

2. **Wrong**: `import tw.teddysoft.ezddd.core.aggregate.Aggregate;`  
   **Correct**: `import tw.teddysoft.ezddd.entity.AggregateRoot;`

3. **Wrong**: `import tw.teddysoft.ezddd.core.entity.EntityId;`  
   **Correct**: 使用 `import tw.teddysoft.ezddd.entity.ValueObject;` (ID 通常實作 ValueObject)

### 其他常見錯誤

1. **Wrong**: `import tw.teddysoft.ezddd.domain.ValueObject;`  
   **Correct**: `import tw.teddysoft.ezddd.entity.ValueObject;`

2. **Wrong**: `import tw.teddysoft.ezddd.domain.event.DomainEvent;`  
   **Correct**: `import tw.teddysoft.ezddd.entity.DomainEvent;`

3. **Wrong**: `import tw.teddysoft.ezddd.domain.event.ConstructionEvent;`  
   **Correct**: Use `DomainEvent.ConstructionEvent` (it's an inner interface)

4. **Wrong**: `import tw.teddysoft.ezddd.domain.aggregate.EsAggregateRoot;`  
   **Correct**: `import tw.teddysoft.ezddd.entity.EsAggregateRoot;`

5. **Wrong**: `import tw.teddysoft.ezddd.usecase.port.in.interactor.Command;`  
   **Correct**: `import tw.teddysoft.ezddd.cqrs.usecase.command.Command;`

6. **Wrong**: `import tw.teddysoft.ezddd.usecase.CqrsOutput;`  
   **Correct**: `import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;`

7. **Wrong**: `import tw.teddysoft.ezddd.domain.Repository;`  
   **Correct**: `import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;`

8. **Wrong**: `import tw.teddysoft.ezddd.message.MessageBus;`  
   **Correct**: `import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;`

9. **Wrong**: `import tw.teddysoft.ezddd.usecase.ExitCode;`  
   **Correct**: `import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;`

10. **Wrong**: `import tw.teddysoft.ezddd.adapter.port.in.interactor.UseCaseFailureException;`  
    **Correct**: `import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;`

## Usage Examples

### Domain Event with ConstructionEvent
```java
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;

public sealed interface PlanEvents extends InternalDomainEvent {
    record PlanCreated(...) implements PlanEvents, ConstructionEvent {
        // implementation
    }
}
```

### Use Case Implementation
```java
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.ExitCode;
import tw.teddysoft.ezddd.usecase.port.in.interactor.UseCaseFailureException;

public interface CreatePlanUseCase extends Command<CreatePlanInput, CqrsOutput> {
}
```

### Projection Implementation
```java
import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;

public interface PlanDtosProjection extends Projection<PlanDtosProjection.PlanDtosProjectionInput, List<PlanDto>> {
    class PlanDtosProjectionInput extends ProjectionInput {
        public String userId;
        public String sortBy;
        public String sortOrder;
    }
}
```

## Note on TypeMapper

The domain event pattern has been updated. Instead of extending `tw.teddysoft.ezddd.utils.TypeMapper`, use:
```java
class TypeMapper extends DomainEventTypeMapper.DefaultMapper {
    // implementation
}
```

## Additional Notes

1. **InternalDomainEvent**: Used for domain events that are internal to the aggregate
2. **DomainEvent**: Used for domain events that can be published externally
3. **Contract Utilities**: For preconditions and postconditions, use:
   ```java
   import static tw.teddysoft.ucontract.Contract.*;
   ```