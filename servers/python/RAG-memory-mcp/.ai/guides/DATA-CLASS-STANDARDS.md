# Data Class Standards Guide

## 📋 Overview
Data classes in the EZDDD framework serve as DTOs (Data Transfer Objects) for persisting aggregate state in the Outbox pattern. They are JPA entities that represent the database schema.

## ⚠️ Critical Rules

### 1. NO @Enumerated on String Fields
This is the **#1 cause of JPA configuration errors**.

```java
// ❌ WRONG - Will cause JPA error
@Enumerated(EnumType.STRING)
@Column(name = "state", nullable = false)
private String state;  // String cannot have @Enumerated!

// ✅ CORRECT
@Column(name = "state", nullable = false)
private String state;  // Just @Column, no @Enumerated
```

### 2. NO Enum Type Fields
Data classes should **never** have enum type fields.

```java
// ❌ WRONG - Don't use enum types
@Column(name = "state")
private ProductLifecycleState state;  // Enum type

// ✅ CORRECT - Use String instead
@Column(name = "state", nullable = false)
private String state;  // String representation of enum
```

## 📐 Data Class Structure

### Basic Template
```java
package tw.teddysoft.aiscrum.{aggregate}.usecase.port.out;

import jakarta.persistence.*;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxData;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "{aggregate}s")
public class {Aggregate}Data implements OutboxData<String> {
    
    // Transient fields for Outbox pattern
    @Transient
    private List<DomainEventData> domainEventDatas = new ArrayList<>();
    
    @Transient
    private String streamName;
    
    // Entity fields
    @Id
    @Column(name = "{aggregate}_id")
    private String {aggregate}Id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "state", nullable = false)
    private String state;  // Enum stored as String
    
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
    
    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;
    
    // Getters and setters...
    
    // OutboxData interface methods
    @Override
    @Transient
    public String getId() {
        return {aggregate}Id;
    }
    
    @Override
    @Transient
    public void setId(String id) {
        this.{aggregate}Id = id;
    }
    
    @Override
    @Transient
    public List<DomainEventData> getDomainEventDatas() {
        return domainEventDatas;
    }
    
    @Override
    @Transient
    public void setDomainEventDatas(List<DomainEventData> domainEventDatas) {
        this.domainEventDatas = domainEventDatas;
    }
    
    @Override
    @Transient
    public String getStreamName() {
        return streamName;
    }
    
    @Override
    @Transient
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }
}
```

## 🔄 Type Conversion Rules

### Enum to String Conversion
```java
// In Mapper.toData() method
data.setState(aggregate.getState().name());  // Convert enum to String

// In Mapper.toDomain() method
ProductLifecycleState.valueOf(data.getState());  // Convert String to enum
```

### Complex Types to JSON
```java
// For complex value objects
@Column(name = "metadata", columnDefinition = "TEXT")
private String metadata;  // Store as JSON string

// In Mapper
data.setMetadata(JsonUtils.toJson(aggregate.getMetadata()));
aggregate.setMetadata(JsonUtils.fromJson(data.getMetadata(), Metadata.class));
```

### Collections
```java
// Store as JSON array
@Column(name = "tags", columnDefinition = "TEXT")
private String tags;  // JSON array string

// In Mapper
data.setTags(JsonUtils.toJson(aggregate.getTags()));
aggregate.setTags(JsonUtils.fromJsonList(data.getTags(), Tag.class));
```

## 📝 Annotation Guidelines

### Required Annotations
1. **@Entity** - Mark as JPA entity
2. **@Table** - Specify table name
3. **@Id** - Mark primary key
4. **@Column** - Specify column mapping for all fields
5. **@Version** - For optimistic locking
6. **@Transient** - For non-persistent fields

### Column Naming Convention
```java
@Column(name = "product_id")      // snake_case for database
private String productId;          // camelCase for Java

@Column(name = "is_deleted")      // boolean prefix with is_
private boolean isDeleted;

@Column(name = "created_at")      // timestamps use _at suffix
private Instant createdAt;
```

## 🚫 Common Mistakes

### Mistake 1: Using @Enumerated with String
```java
// ❌ WRONG
@Enumerated(EnumType.STRING)
private String status;

// ✅ CORRECT
@Column(name = "status")
private String status;
```

### Mistake 2: Missing @Transient on Outbox fields
```java
// ❌ WRONG - Will try to persist to database
private List<DomainEventData> domainEventDatas;

// ✅ CORRECT
@Transient
private List<DomainEventData> domainEventDatas;
```

### Mistake 3: Using enum types directly
```java
// ❌ WRONG
private TaskStatus status;

// ✅ CORRECT
private String status;
```

### Mistake 4: Missing @Column annotations
```java
// ❌ WRONG - Relies on naming strategy
private String productName;

// ✅ CORRECT - Explicit mapping
@Column(name = "product_name")
private String productName;
```

## ✅ Validation Checklist

Before committing a Data class:

- [ ] No @Enumerated annotations on String fields
- [ ] No enum type fields (all converted to String)
- [ ] All persistent fields have @Column annotations
- [ ] Transient fields marked with @Transient
- [ ] Implements OutboxData<String> interface
- [ ] Has @Entity and @Table annotations
- [ ] Has @Version field for optimistic locking
- [ ] Column names use snake_case

## 🔧 Validation Script

Run this script to check Data class compliance:
```bash
.ai/scripts/check-data-class-annotations.sh
```

## 📚 Related Documents
- [DUAL-PROFILE-TEST-CHECKLIST.md](../checklists/DUAL-PROFILE-TEST-CHECKLIST.md)
- [FRAMEWORK-API-INTEGRATION-GUIDE.md](./FRAMEWORK-API-INTEGRATION-GUIDE.md)
- [Outbox Pattern Implementation](../tech-stacks/java-ca-ezddd-spring/examples/outbox/README.md)