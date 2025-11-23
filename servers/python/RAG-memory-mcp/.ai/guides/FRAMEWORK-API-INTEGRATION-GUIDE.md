# ezddd Framework API 整合完整指南 🏗️

## 🔴 關鍵問題總覽

框架 API 整合的四大挑戰：
1. **PgMessageDbClient 創建方式錯誤**
2. **Outbox Pattern 實作不符合規範**
3. **Import 路徑和註解使用錯誤**
4. **事件傳遞架構混淆（InMemory vs Outbox）**

## 🔥 重要：InMemory vs Outbox 事件傳遞架構差異

### InMemory Profile 事件流
```
Repository.save() → MessageBus<DomainEvent> → Reactors
```
- **需要**: MessageBus<DomainEvent> bean (BlockingMessageBus)
- **不需要**: MessageBroker, MessageProducer, PgMessageDbClient
- **特點**: 同步、直接傳遞、無持久化

### Outbox Profile 事件流  
```
Repository.save() → PostgreSQL → EzesCatchUpRelay → MessageProducer → MessageBroker → Reactors
```
- **需要**: PgMessageDbClient, MessageBroker, MessageProducer
- **不需要**: MessageBus<DomainEvent> bean
- **特點**: 異步、持久化、保證傳遞

### 關鍵配置差異
| Component | InMemory Profile | Outbox Profile |
|-----------|-----------------|----------------|
| MessageBus<DomainEvent> | ✅ 必要 | ❌ 不需要 |
| MessageBroker | ❌ 不需要 | ✅ 必要 |
| MessageProducer | ❌ 不需要 | ✅ 必要 |
| PgMessageDbClient | ❌ 不需要 | ✅ 必要 |
| EzesCatchUpRelay | ❌ 不需要 | ✅ 自動啟動 |

## 問題 1：PgMessageDbClient 創建問題

### ❌ 錯誤方式（會導致運行時錯誤）
```java
@Bean
public PgMessageDbClient pgMessageDbClient(DataSource dataSource) {
    // 這樣會失敗！缺少 Spring Data JPA 的代理和攔截器
    return new PgMessageDbClient(dataSource);
}
```

### ✅ 正確方式（唯一可行的方法）
```java
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

@Configuration
public class OutboxInfrastructureConfig {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Bean
    public PgMessageDbClient pgMessageDbClient() {
        // 必須透過 JpaRepositoryFactory 創建
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
}
```

### 🔍 為什麼必須這樣做？
1. **Spring Data JPA 代理**：PgMessageDbClient 需要 Spring Data JPA 的動態代理來處理資料庫操作
2. **事務管理**：需要 Spring 的事務攔截器
3. **查詢方法解析**：Repository 方法需要被解析成 SQL 查詢
4. **EntityManager 整合**：需要與 JPA EntityManager 正確整合

## 問題 2：Outbox Pattern 實作規範（ADR-019）

### 1. OutboxMapper 必須是內部類別

#### ❌ 錯誤：獨立類別
```java
// ProductOutboxMapper.java - 錯誤！
public class ProductOutboxMapper implements OutboxMapper<Product, ProductData> {
    // 獨立的 mapper 類別會導致框架無法正確處理
}
```

#### ✅ 正確：內部類別
```java
// ProductMapper.java - 正確！
public class ProductMapper {
    
    private static final OutboxMapper<Product, ProductData> mapper = 
        new ProductMapper.Mapper();
    
    public static OutboxMapper<Product, ProductData> newMapper() {
        return mapper;
    }
    
    // 必須是內部類別
    static class Mapper implements OutboxMapper<Product, ProductData> {
        @Override
        public Product toDomain(ProductData data) {
            return ProductMapper.toDomain(data);
        }
        
        @Override
        public ProductData toData(Product aggregateRoot) {
            return ProductMapper.toData(aggregateRoot);
        }
    }
}
```

### 2. 必須使用 Jakarta Persistence

#### ❌ 錯誤：使用 javax
```java
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
```

#### ✅ 正確：使用 jakarta
```java
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
```

### 3. @Transient 註解的關鍵欄位

#### ❌ 錯誤：忘記 @Transient
```java
@Entity
public class ProductData implements OutboxData<String> {
    
    // 錯誤！這些欄位不應該被持久化
    private List<DomainEventData> domainEventDatas;
    private String streamName;
}
```

#### ✅ 正確：必須加 @Transient
```java
@Entity
public class ProductData implements OutboxData<String> {
    
    @Transient  // 關鍵！
    private List<DomainEventData> domainEventDatas;
    
    @Transient  // 關鍵！
    private String streamName;
    
    @Id
    private String productId;
    
    @Version
    private long version;
}
```

## 🛡️ 完整的防護檢查清單

### 創建 PgMessageDbClient 時
- [ ] 使用 `@PersistenceContext` 注入 EntityManager
- [ ] 使用 `JpaRepositoryFactory` 創建
- [ ] 透過 `factory.getRepository()` 取得實例
- [ ] **絕不使用** `new PgMessageDbClient()`

### 實作 OutboxMapper 時
- [ ] Mapper 是主類別的內部類別
- [ ] 提供 static `newMapper()` 方法
- [ ] 實作 `toDomain()` 和 `toData()` 方法
- [ ] **絕不創建**獨立的 Mapper 類別

### 實作 OutboxData 時
- [ ] 使用 `jakarta.persistence.*` imports
- [ ] `domainEventDatas` 標記 `@Transient`
- [ ] `streamName` 標記 `@Transient`
- [ ] 包含 `@Version` 欄位
- [ ] 實作 `OutboxData<String>` 介面

## 📝 完整範例：正確的 Outbox 配置

### Step 1: Data 類別
```java
package tw.teddysoft.aiscrum.product.usecase.port.out;

import jakarta.persistence.*;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxData;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "products")
public class ProductData implements OutboxData<String> {
    
    @Transient
    private List<DomainEventData> domainEventDatas = new ArrayList<>();
    
    @Transient
    private String streamName;
    
    @Id
    @Column(name = "id")
    private String productId;
    
    @Column(name = "name")
    private String name;
    
    @Version
    @Column(columnDefinition = "bigint DEFAULT 0", nullable = false)
    private long version;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
    
    // Getters and Setters...
    
    @Override
    @Transient
    public String getId() {
        return productId;
    }
    
    @Override
    @Transient
    public void setId(String id) {
        this.productId = id;
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

### Step 2: Mapper 類別（含內部類別）
```java
package tw.teddysoft.aiscrum.product.usecase.port;

import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxMapper;

public class ProductMapper {
    
    private static final OutboxMapper<Product, ProductData> mapper = 
        new ProductMapper.Mapper();
    
    public static OutboxMapper<Product, ProductData> newMapper() {
        return mapper;
    }
    
    public static ProductData toData(Product product) {
        ProductData data = new ProductData();
        data.setProductId(product.getId().value());
        data.setName(product.getName().value());
        data.setVersion(product.getVersion());
        data.setDeleted(product.isDeleted());
        
        // Outbox 欄位
        data.setStreamName(product.getStreamName());
        data.setDomainEventDatas(
            product.getDomainEvents().stream()
                .map(DomainEventMapper::toData)
                .collect(Collectors.toList())
        );
        
        return data;
    }
    
    public static Product toDomain(ProductData data) {
        // 實作從 data 重建 domain 物件
        // ...
    }
    
    // 內部類別 - 這是關鍵！
    static class Mapper implements OutboxMapper<Product, ProductData> {
        @Override
        public Product toDomain(ProductData data) {
            return ProductMapper.toDomain(data);
        }
        
        @Override
        public ProductData toData(Product aggregateRoot) {
            return ProductMapper.toData(aggregateRoot);
        }
    }
}
```

### Step 3: 基礎設施配置
```java
package tw.teddysoft.aiscrum.config.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;

@Configuration
@Profile({"outbox", "test-outbox"})
public class OutboxInfrastructureConfig {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Bean
    public PgMessageDbClient pgMessageDbClient() {
        // 正確的創建方式
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
}
```

## 🚨 常見錯誤診斷

### 錯誤 1：No qualifying bean of type 'PgMessageDbClient'
**原因**：使用 new 創建而不是 JpaRepositoryFactory
**解決**：使用本指南的正確創建方式

### 錯誤 2：OutboxMapper not found
**原因**：OutboxMapper 是獨立類別而非內部類別
**解決**：將 Mapper 改為內部類別

### 錯誤 3：Package 'javax.persistence' does not exist
**原因**：使用舊版 javax 而非 jakarta
**解決**：全部改用 jakarta.persistence

### 錯誤 4：Column 'domain_event_datas' not found
**原因**：忘記加 @Transient 註解
**解決**：為 domainEventDatas 和 streamName 加上 @Transient

## 📊 驗證矩陣

| 元件 | 檢查項目 | 正確做法 |
|-----|---------|---------|
| PgMessageDbClient | 創建方式 | JpaRepositoryFactory |
| OutboxMapper | 類別結構 | 內部類別 |
| OutboxData | Import | jakarta.persistence |
| OutboxData | Transient 欄位 | @Transient 註解 |
| Configuration | Profile | @Profile("outbox") |
| Configuration | EntityManager | @PersistenceContext |

## 🔗 相關資源

- [ADR-019: Outbox Pattern 實作規範](.dev/adr/ADR-019-outbox-pattern-implementation.md)
- [Outbox Sub-agent Prompt](.ai/prompts/outbox-sub-agent-prompt.md)
- [完整 Outbox 範例](.ai/tech-stacks/java-ca-ezddd-spring/examples/outbox/)

## 📝 Quick Checklist

使用這個快速檢查清單確保框架 API 整合正確：

```bash
# 1. 檢查 PgMessageDbClient 創建
grep -r "new PgMessageDbClient" src/ && echo "❌ Found direct instantiation" || echo "✅ No direct instantiation"

# 2. 檢查 OutboxMapper 是否為內部類別
find src -name "*OutboxMapper.java" && echo "❌ Found standalone mapper" || echo "✅ No standalone mapper"

# 3. 檢查 javax vs jakarta
grep -r "javax.persistence" src/ && echo "❌ Found javax imports" || echo "✅ Using jakarta"

# 4. 檢查 @Transient 註解
grep -r "domainEventDatas" src/ | grep -v "@Transient" && echo "❌ Missing @Transient" || echo "✅ Has @Transient"
```