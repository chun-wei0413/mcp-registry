# Mapper Implementation Standards

## 📋 Overview
Mapper 類別負責在不同層級之間轉換資料物件，是 Clean Architecture 中的重要元件。本文檔定義了實作 Mapper 的完整標準和最佳實踐。

## ⚠️ Critical Requirements

### 1. Jackson ObjectMapper 配置
**必須正確配置 ObjectMapper 以支援 Java 8+ 時間類型**

```java
// ✅ 正確：配置 JavaTimeModule
private static final ObjectMapper objectMapper = createObjectMapper();

private static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());  // 必須！
    return mapper;
}

// ❌ 錯誤：使用預設 ObjectMapper
private static final ObjectMapper objectMapper = new ObjectMapper();
```

### 2. 完整的雙向轉換
**toData() 和 toDomain() 必須是對稱的**

```java
// ✅ 正確：toData 序列化 DoD，toDomain 反序列化 DoD
public static ProductData toData(Product product) {
    // ... 序列化 DefinitionOfDone
    if (product.getDefinitionOfDone() != null) {
        productData.setDefinitionOfDone(
            objectMapper.writeValueAsString(product.getDefinitionOfDone())
        );
    }
}

public static Product toDomain(ProductData data) {
    // ... 反序列化 DefinitionOfDone
    if (data.getDefinitionOfDone() != null) {
        DefinitionOfDone dod = objectMapper.readValue(
            data.getDefinitionOfDone(), 
            DefinitionOfDone.class
        );
        product.defineDefinitionOfDone(/*...*/);
    }
}
```

## 🏗️ Mapper Class Structure

### 完整的 Mapper 類別模板

```java
package tw.teddysoft.aiscrum.[aggregate].usecase.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tw.teddysoft.aiscrum.common.entity.DateProvider;
import tw.teddysoft.aiscrum.[aggregate].entity.*;
import tw.teddysoft.aiscrum.[aggregate].usecase.port.out.[Aggregate]Data;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;

import java.util.List;
import java.util.stream.Collectors;

import static tw.teddysoft.ucontract.Contract.requireNotNull;

public class [Aggregate]Mapper {
    
    // 1. 依賴配置
    private final [SubMapper] subMapper = new [SubMapper]();
    private static final ObjectMapper objectMapper = createObjectMapper();
    private static final OutboxMapper<[Aggregate], [Aggregate]Data> outboxMapper = 
        new [Aggregate]Mapper.Mapper();
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 其他配置（如需要）
        return mapper;
    }
    
    // 2. Instance method: Domain -> DTO (for API layer)
    public [Aggregate]Dto toDto([Aggregate] aggregate) {
        if (aggregate == null) {
            return null;
        }
        
        [Aggregate]Dto dto = new [Aggregate]Dto();
        // 基本欄位映射
        dto.setId(aggregate.getId().value());
        dto.setName(aggregate.getName().value());
        
        // 複雜物件映射
        if (aggregate.getComplexObject() != null) {
            dto.setComplexObject(subMapper.toDto(aggregate.getComplexObject()));
        }
        
        dto.setState(aggregate.getState().name());
        return dto;
    }
    
    // 3. Static method: Data -> DTO (for Projection)
    public static [Aggregate]Dto toDto([Aggregate]Data data) {
        if (data == null) {
            return null;
        }
        
        [Aggregate]Dto dto = new [Aggregate]Dto();
        dto.setId(data.get[Aggregate]Id());
        dto.setName(data.getName());
        
        // 從 JSON 還原複雜物件
        if (data.getComplexObjectJson() != null) {
            try {
                ComplexObject obj = objectMapper.readValue(
                    data.getComplexObjectJson(), 
                    ComplexObject.class
                );
                dto.setComplexObject(new SubMapper().toDto(obj));
            } catch (Exception e) {
                // 優雅降級
                dto.setComplexObject(null);
            }
        }
        
        dto.setState(data.getState());
        return dto;
    }
    
    // 4. Domain -> Data (for persistence)
    public static [Aggregate]Data toData([Aggregate] aggregate) {
        requireNotNull("[Aggregate]", aggregate);
        
        [Aggregate]Data data = new [Aggregate]Data(aggregate.getVersion());
        
        // 基本欄位
        data.set[Aggregate]Id(aggregate.getId().value());
        data.setName(aggregate.getName().value());
        
        // 複雜物件序列化為 JSON
        if (aggregate.getComplexObject() != null) {
            try {
                data.setComplexObjectJson(
                    objectMapper.writeValueAsString(aggregate.getComplexObject())
                );
            } catch (Exception e) {
                // 記錄錯誤但不中斷流程
                data.setComplexObjectJson(null);
            }
        }
        
        data.setState(aggregate.getState().name());
        
        // 時間戳記
        if (!aggregate.getDomainEvents().isEmpty()) {
            data.setCreatedAt(aggregate.getDomainEvents().get(0).occurredOn());
            data.setLastUpdated(
                aggregate.getDomainEvents()
                    .get(aggregate.getDomainEvents().size() - 1)
                    .occurredOn()
            );
        } else {
            data.setCreatedAt(DateProvider.now());
            data.setLastUpdated(DateProvider.now());
        }
        
        // Domain events 和 stream
        data.setStreamName(aggregate.getStreamName());
        data.setDomainEventDatas(
            aggregate.getDomainEvents().stream()
                .map(DomainEventMapper::toData)
                .collect(Collectors.toList())
        );
        
        return data;
    }
    
    // 5. Data -> Domain (from persistence)
    public static [Aggregate] toDomain([Aggregate]Data data) {
        requireNotNull("[Aggregate]Data", data);
        
        // 優先從 events 重建（Event Sourcing）
        if (data.getDomainEventDatas() != null && !data.getDomainEventDatas().isEmpty()) {
            var domainEvents = data.getDomainEventDatas().stream()
                .map(DomainEventMapper::toDomain)
                .map(event -> ([Aggregate]Events) event)
                .collect(Collectors.toList());
            
            [Aggregate] aggregate = new [Aggregate](domainEvents);
            aggregate.setVersion(data.getVersion());
            aggregate.clearDomainEvents();
            return aggregate;
        }
        
        // 從當前狀態重建
        [Aggregate] aggregate = new [Aggregate](
            [Aggregate]Id.valueOf(data.get[Aggregate]Id()),
            [Aggregate]Name.valueOf(data.getName())
        );
        
        // 還原複雜物件
        if (data.getComplexObjectJson() != null) {
            try {
                ComplexObject obj = objectMapper.readValue(
                    data.getComplexObjectJson(), 
                    ComplexObject.class
                );
                // 使用 aggregate 的方法設定複雜物件
                aggregate.setComplexObject(obj);
            } catch (Exception e) {
                // 反序列化失敗，優雅降級
            }
        }
        
        aggregate.setVersion(data.getVersion());
        aggregate.clearDomainEvents();
        return aggregate;
    }
    
    // 6. OutboxMapper support
    public static OutboxMapper<[Aggregate], [Aggregate]Data> newMapper() {
        return outboxMapper;
    }
    
    // 7. Inner OutboxMapper implementation
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

## 🔍 Implementation Guidelines

### 1. 處理 Value Objects

#### Records (Java 14+)
```java
// DefinitionOfDone 是 record
DefinitionOfDone dod = product.getDefinitionOfDone();

// 使用 record 的 accessor methods（沒有 get 前綴）
String name = dod.name();           // ✅ 正確
String name = dod.getName();        // ❌ 錯誤

// 重建時使用適當的方法
product.defineDefinitionOfDone(
    dod.name(),
    dod.criteria(),
    dod.note(),
    dod.definedAt()
);
```

#### 傳統 Value Objects
```java
// ProductName 是傳統 Value Object
ProductName name = product.getName();

// 使用 value() 方法取得原始值
String nameValue = name.value();    // ✅ 正確
```

### 2. 處理集合類型

#### SequencedSet (Java 21+)
```java
// 序列化時保持順序
SequencedSet<DoneCriterion> criteria = dod.criteria();
String json = objectMapper.writeValueAsString(criteria);

// 反序列化時使用 LinkedHashSet 保持順序
LinkedHashSet<DoneCriterion> criteria = 
    objectMapper.readValue(json, 
        objectMapper.getTypeFactory().constructCollectionType(
            LinkedHashSet.class, DoneCriterion.class
        )
    );
```

### 3. 錯誤處理策略

#### 優雅降級原則
```java
// ✅ 正確：捕獲異常，記錄但不中斷
try {
    data.setComplexObjectJson(
        objectMapper.writeValueAsString(complexObject)
    );
} catch (Exception e) {
    // 可選：記錄錯誤用於除錯
    // logger.warn("Failed to serialize complex object", e);
    data.setComplexObjectJson(null);
}

// ❌ 錯誤：讓異常傳播
data.setComplexObjectJson(
    objectMapper.writeValueAsString(complexObject)  // 可能拋出異常
);
```

### 4. Event Sourcing 支援

#### 優先順序
```java
public static [Aggregate] toDomain([Aggregate]Data data) {
    // 1. 優先從 events 重建（最準確）
    if (hasEvents(data)) {
        return reconstructFromEvents(data);
    }
    
    // 2. 其次從當前狀態重建（可能遺失歷史）
    return reconstructFromState(data);
}
```

## 📝 Checklist for Mapper Implementation

### 必要檢查項目
- [ ] **ObjectMapper 配置**
  - [ ] 註冊 JavaTimeModule
  - [ ] 處理其他必要的序列化模組
  
- [ ] **toDto 方法**
  - [ ] Instance method 用於 domain -> DTO
  - [ ] Static method 用於 data -> DTO
  - [ ] 處理 null 輸入
  
- [ ] **toData 方法**
  - [ ] 映射所有基本欄位
  - [ ] 序列化複雜物件為 JSON
  - [ ] 包含 domain events
  - [ ] 設定時間戳記
  - [ ] 處理序列化錯誤
  
- [ ] **toDomain 方法**
  - [ ] 優先從 events 重建
  - [ ] 支援從狀態重建
  - [ ] 反序列化所有複雜物件
  - [ ] 設定版本號
  - [ ] 清除 domain events
  
- [ ] **OutboxMapper 支援**
  - [ ] 實作內部 Mapper 類別
  - [ ] 提供 newMapper() 方法

### 測試檢查項目
- [ ] 測試 null 輸入處理
- [ ] 測試複雜物件的序列化/反序列化
- [ ] 測試時間類型的處理
- [ ] 測試 toData -> toDomain 的對稱性
- [ ] 測試錯誤情況的優雅降級

## ⚠️ Common Pitfalls

### 1. 忘記配置 JavaTimeModule
```java
// ❌ 會導致 Instant、LocalDateTime 等無法序列化
private static final ObjectMapper objectMapper = new ObjectMapper();

// ✅ 正確配置
private static final ObjectMapper objectMapper = createObjectMapper();
```

### 2. 不對稱的轉換
```java
// ❌ toData 序列化了 DoD，但 toDomain 沒有反序列化
public static ProductData toData(Product product) {
    data.setDefinitionOfDone(serialize(product.getDefinitionOfDone()));
}

public static Product toDomain(ProductData data) {
    // 遺漏了 DefinitionOfDone 的還原！
    return product;
}
```

### 3. 使用錯誤的 accessor methods
```java
// ❌ 對 record 使用 get 前綴
String name = record.getName();

// ✅ record 使用無前綴的方法
String name = record.name();
```

### 4. 不處理序列化錯誤
```java
// ❌ 異常會中斷整個流程
data.setJson(objectMapper.writeValueAsString(object));

// ✅ 優雅處理錯誤
try {
    data.setJson(objectMapper.writeValueAsString(object));
} catch (Exception e) {
    data.setJson(null);
}
```

## 📚 Related Documents
- ADR-019: Outbox Pattern Implementation
- ADR-020: Archive Pattern Implementation
- ADR-022: Mapper Serialization Requirements
- `.ai/tech-stacks/java-ca-ezddd-spring/coding-standards.md`