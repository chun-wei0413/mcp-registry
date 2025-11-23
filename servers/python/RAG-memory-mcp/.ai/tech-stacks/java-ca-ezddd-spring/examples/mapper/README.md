# Mapper 設計模式與實作範例

## 概述

Mapper 模式在 Clean Architecture 中負責在不同層級之間轉換物件。主要用於：
- **Domain → Data**: 將領域物件轉換為持久化物件
- **Data → Domain**: 將持久化物件還原為領域物件
- **Domain → DTO**: 將領域物件轉換為資料傳輸物件
- **Data → DTO**: 將持久化物件轉換為資料傳輸物件

## 核心概念

### 1. 為什麼需要 Mapper？

在 Clean Architecture 中，不同層級有不同的資料表示：
- **Entity Layer**: 純粹的業務物件（Aggregate、Entity、Value Object）
- **Use Case Layer**: 資料傳輸物件（DTO、Input、Output）
- **Adapter Layer**: 持久化物件（Data、JPA Entity）

Mapper 確保各層之間的轉換邏輯集中管理，保持關注點分離。

### 2. Mapper 類型

#### Aggregate Mapper
- 包含 `newMapper()` 靜態方法
- 實作 `OutboxMapper` 介面
- 處理複雜的聚合根轉換
- 包含事件和版本資訊

#### Entity Mapper
- **不包含** `newMapper()` 方法
- 只處理實體內部的轉換
- 通常更簡單直接

## 實作模式

### 1. Aggregate Mapper 模式

```java
package [rootPackage].[aggregate].usecase.port;

import [rootPackage].common.entity.DateProvider;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper;

import static tw.teddysoft.ucontract.Contract.requireNotNull;

public class [Aggregate]Mapper {
    
    // Domain → Data (for persistence)
    public static [Aggregate]Data toData([Aggregate] aggregate) {
        requireNotNull("Aggregate", aggregate);
        
        [Aggregate]Data data = new [Aggregate]Data(
            aggregate.getId().id(),
            aggregate.getName(),
            // ... other fields
            aggregate.getVersion()
        );
        
        // 設定 Event Sourcing 必要欄位
        data.setLastUpdated(DateProvider.now());
        data.setStreamName(aggregate.getStreamName());
        data.setDomainEventDatas(
            aggregate.getDomainEvents().stream()
                .map(DomainEventMapper::toData)
                .collect(Collectors.toList())
        );
        
        return data;
    }
    
    // Data → Domain (from persistence)
    public static [Aggregate] toDomain([Aggregate]Data data) {
        requireNotNull("[Aggregate]Data", data);
        
        [Aggregate] aggregate = new [Aggregate](
            [Aggregate]Id.valueOf(data.get[Aggregate]Id()),
            data.getName()
            // ... other fields
        );
        
        aggregate.setVersion(data.getVersion());
        aggregate.clearDomainEvents(); // 重要：清除重建時的事件
        
        return aggregate;
    }
    
    // Domain → DTO (for API response)
    public static [Aggregate]Dto toDto([Aggregate] aggregate) {
        requireNotNull("Aggregate", aggregate);
        
        return new [Aggregate]Dto(
            aggregate.getId().id(),
            aggregate.getName(),
            // ... other fields
            aggregate.getVersion()
        );
    }
    
    // 批次轉換方法
    public static List<[Aggregate]Data> toData(List<[Aggregate]> aggregates) {
        return aggregates.stream()
            .map([Aggregate]Mapper::toData)
            .collect(Collectors.toList());
    }
    
    // OutboxMapper 實作（只有 Aggregate 需要）
    private static final OutboxMapper mapper = new Mapper();
    
    public static OutboxMapper newMapper() {
        return mapper;
    }
    
    static class Mapper implements OutboxMapper<[Aggregate], [Aggregate]Data> {
        @Override
        public [Aggregate] toDomain([Aggregate]Data data) {
            return [Aggregate]Mapper.toDomain(data);
        }
        
        @Override
        public [Aggregate]Data toData([Aggregate] aggregateRoot) {
            return [Aggregate]Mapper.toData(aggregateRoot);
        }
    }
}
```

### 2. Entity Mapper 模式

```java
package [rootPackage].[aggregate].usecase.port;

public class [Entity]Mapper {
    
    // Entity → Data
    public static [Entity]Data toData([Entity] entity) {
        return new [Entity]Data(
            entity.getId().id(),
            entity.getName()
            // ... other fields
        );
    }
    
    // Data → Entity
    public static [Entity] toDomain([Entity]Data data) {
        return new [Entity](
            [Entity]Id.valueOf(data.getId()),
            data.getName()
            // ... other fields
        );
    }
    
    // Entity → DTO
    public static [Entity]Dto toDto([Entity] entity) {
        return new [Entity]Dto(
            entity.getId().id(),
            entity.getName()
            // ... other fields
        );
    }
    
    // 注意：Entity Mapper 沒有 newMapper() 方法
}
```

## 實際範例

### 1. Plan Aggregate Mapper

參見 [PlanMapper.java](./PlanMapper.java)

重點特性：
- 處理複雜的巢狀結構（Projects、Tasks）
- 轉換領域事件
- 維護 Event Sourcing 必要資訊

### 2. Task Entity Mapper

參見 [TaskMapper.java](./TaskMapper.java)

重點特性：
- 簡單的實體轉換
- 沒有 `newMapper()` 方法
- 處理 Value Object 轉換

## 最佳實踐

### 1. 使用靜態方法
```java
// ✅ 正確：使用靜態方法
PlanData data = PlanMapper.toData(plan);

// ❌ 錯誤：不要實例化 Mapper
PlanMapper mapper = new PlanMapper();
```

### 2. Null 檢查
```java
public static PlanData toData(Plan plan) {
    requireNotNull("Plan", plan);  // 使用 Contract
    // ...
}
```

### 3. 集合轉換
```java
// 提供便利的批次轉換方法
public static List<PlanDto> toDto(List<Plan> plans) {
    return plans.stream()
        .map(PlanMapper::toDto)
        .collect(Collectors.toList());
}
```

### 4. Event Sourcing 必要欄位
```java
// Aggregate Mapper 必須設定這些欄位
data.setLastUpdated(DateProvider.now());
data.setStreamName(aggregate.getStreamName());
data.setDomainEventDatas(...);
```

### 5. 清除重建時的事件
```java
// 從資料庫重建時必須清除事件
aggregate.clearDomainEvents();
```

## 常見錯誤

### 1. Entity Mapper 包含 newMapper()
```java
// ❌ 錯誤：Entity 不需要 OutboxMapper
public class TaskMapper {
    public static OutboxMapper newMapper() { ... }
}
```

### 2. 忘記設定 Event Sourcing 欄位
```java
// ❌ 錯誤：缺少必要欄位
public static PlanData toData(Plan plan) {
    PlanData data = new PlanData(...);
    // 忘記設定 streamName 和 domainEventDatas
    return data;
}
```

### 3. 直接使用建構子
```java
// ❌ 錯誤：應該使用 valueOf
PlanId.new(data.getPlanId())

// ✅ 正確：使用 valueOf
PlanId.valueOf(data.getPlanId())
```

## 相關資源

- [Clean Architecture 概述](../aggregate/README.md)
- [Repository 模式](../repository/README.md)
- [Domain Event 處理](../aggregate/README.md#domain-events)