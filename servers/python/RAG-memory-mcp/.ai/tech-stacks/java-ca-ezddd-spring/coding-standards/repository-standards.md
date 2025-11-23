# Repository 編碼規範

本文件定義 Repository 的編碼標準，包含介面設計、實作原則、JPA Entity 設計等規範。

> **Projection 規範**: 複雜查詢相關規範請參考 [Projection 編碼規範](./projection-standards.md)

## 🔴 必須遵守的規則 (MUST FOLLOW)

### 1. Repository Interface 設計

```java
// ✅ 正確：直接使用 ezddd 框架的 generic Repository interface
// 不需要另外宣告 ProductRepository interface
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

// 在 Service 或 Configuration 中直接使用
@Service
public class CreateProductService {
    private final Repository<Product, ProductId> productRepository;
    
    public CreateProductService(Repository<Product, ProductId> productRepository) {
        this.productRepository = productRepository;
    }
}

// ❌ 錯誤：不要創建自定義的 Repository interface
public interface ProductRepository extends Repository<Product, ProductId> {
    // 不需要這樣做
}
```

**重要原則**：
- 所有 Aggregate 都使用 `tw.teddysoft.ezddd.usecase.port.out.repository.Repository` generic interface
- 不需要另外宣告特定的 Repository interface
- 例如：`Repository<Plan, PlanId>`、`Repository<Tag, TagId>`、`Repository<Product, ProductId>`
- Aggregate repository 的具體實作類別透過 Spring Boot 的 @Bean 注入

### 2. Repository 實作原則
**重要原則**：
- Repository 有三種實作：
  - OutboxRepository, 支援 Outbox 設計模式，來自 `tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository`
  - EsRepository, 支援 Event Sourcing 設計模式，來自 `tw.teddysoft.ezddd.usecase.port.out.repository.impl.es.EsRepository`
  - GenericInMemoryRepository，測試時使用

#### OutboxRepository 實作
- 使用 OutboxRepository 需注入兩個參數：
  - RepositoryPeer
      - 預設使用 OutboxRepositoryPeerAdapter, 來自 `tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeerAdapter`
  - OutboxMapper
    - 呼叫 [AggregateMapper].newMapper()
  
```java
// ✅ 正確：實作 Outbox 設計模式的 Plan Repository, 符合 ezddd 框架規範
@Bean
public Repository<Plan, PlanId> planRepository() {
    return new OutboxRepository<>(new OutboxRepositoryPeerAdapter<>(planOutboxStore()), PlanMapper.newMapper());
}
```

#### EsRepository 實作
- 使用 EsRepository 需注入三個參數：
    - RepositoryPeer
        - 預設使用 EsRepositoryPeerAdapter, 來自 `tw.teddysoft.ezddd.data.adapter.repository.es.EsRepositoryPeerAdapter`
    - Class: [Aggregate].class
    - category: [Aggregate].CATEGORY

```java
// ✅ 正確：實作 Event Sourcing 設計模式的 Plan Repository, 符合 ezddd 框架規範
@Bean
public Repository<Plan, PlanId> planRepository() {
    return new EsRepository<>(new EsRepositoryPeerAdapter(eventStore()), Plan.class, Plan.CATEGORY);
}
```


```java
// ✅ 正確：JPA 實作在 adapter 層，實作 generic Repository interface
package tw.teddysoft.aiscrum.product.adapter.out.repository;

import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

@Component
public class JpaProductRepository implements Repository<Product, ProductId> {
    
    private final SpringDataProductRepository springDataRepo;
    private final ProductDataMapper mapper;
    
    public JpaProductRepository(SpringDataProductRepository springDataRepo, 
                                ProductDataMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }
    
    @Override
    public void save(Product product) {
        ProductData data = mapper.toData(product);
        springDataRepo.save(data);
        // 發布 domain events
        product.getDomainEvents().forEach(messageBus::post);
        product.clearDomainEvents();
    }
    
    @Override
    public Optional<Product> findById(ProductId id) {
        return springDataRepo.findById(id.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public void delete(Product product) {
        // 發布 domain events（delete 也要發布事件！）
        product.getDomainEvents().forEach(messageBus::post);
        product.clearDomainEvents();
        
        springDataRepo.deleteById(product.getId().value());
    }
}

// Spring Data JPA 介面（內部使用）
interface SpringDataProductRepository extends JpaRepository<ProductData, String> {
    // 複雜查詢透過 Projection 處理，不在 Repository 層
}
```

#### GenericInMemoryRepository 使用（測試用）
```java
// ✅ 正確：直接使用 GenericInMemoryRepository，不要創建自定義的 InMemory 實作
// 在 Configuration 中：
@Bean
public Repository<Product, ProductId> productRepository(MessageBus<DomainEvent> messageBus) {
    return new GenericInMemoryRepository<>(messageBus);
}

// ❌ 錯誤：不要創建多餘的 InMemory 實作類別
public class InMemoryProductRepository extends GenericInMemoryRepository<Product, ProductId> {
    // 如果只是空的繼承，這是多餘的！
    public InMemoryProductRepository(MessageBus<DomainEvent> messageBus) {
        super(messageBus);
    }
}
```

**重要**：GenericInMemoryRepository 已經提供了完整的記憶體儲存功能，包括：
- 基本 CRUD 操作
- Domain Event 發布（save() 和 delete() 都會發布事件）
- Outbox Pattern 支援
- 不需要為每個 Aggregate 創建專屬的 InMemory 實作

**⚠️ 關鍵規則：save() 和 delete() 都必須發布 Domain Events！**

### 3. Spring Bean 配置

```java
// ✅ 正確：透過 @Bean 注入 Repository 實作
@Configuration
public class RepositoryConfiguration {
    
    @Bean
    public Repository<Product, ProductId> productRepository(
            MessageBus<DomainEvent> messageBus) {
        // 可根據 profile 或環境變數選擇實作
        return new InMemoryProductRepository(messageBus);
        // 或
        // return new JpaProductRepository(springDataRepo, mapper);
    }
    
    @Bean
    public Repository<Sprint, SprintId> sprintRepository(
            MessageBus<DomainEvent> messageBus) {
        return new InMemorySprintRepository(messageBus);
    }
    
    @Bean
    public Repository<ProductBacklogItem, ProductBacklogItemId> pbiRepository(
            MessageBus<DomainEvent> messageBus) {
        return new InMemoryProductBacklogItemRepository(messageBus);
    }
}

// 在 Service 中使用
@Service
public class CreateProductService {
    private final Repository<Product, ProductId> repository;
    
    // Spring 會自動注入對應的 Bean
    public CreateProductService(Repository<Product, ProductId> repository) {
        this.repository = repository;
    }
}
```


## 🎯 JPA Entity (Data Model) 設計

### 1. JPA Entity 結構
```java
@Entity
@Table(name = "products")
public class ProductData {

    public ProductData() {}

    public ProductData(String id, String name, ProductStateData state,
                       String creatorId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    @Id
    @Column(name = "product_id", length = 50)
    private String id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "state", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProductStateData state;
    
    @Column(name = "creator_id", nullable = false, length = 50)
    private String creatorId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 關聯關係
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    private List<TaskData> tasks = new ArrayList<>();
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "goal_id")
    private ProductGoalData goal;
    
    // 版本控制（樂觀鎖）
    @Version
    private Long version;
}
```

### 2. Data Mapper 設計
```java
@Component
public class ProductDataMapper {
    
    // Domain to Data
    public ProductData toData(Product product) {
        return ProductData.builder()
            .id(product.getId().value())
            .name(product.getName())
            .state(mapState(product.getState()))
            .creatorId(product.getCreatorId().value())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .tasks(mapTasks(product.getTasks()))
            .goal(mapGoal(product.getGoal()))
            .build();
    }
    
    // Data to Domain
    public Product toDomain(ProductData data) {
        // Event Sourcing 場景：從事件重建
        if (isEventSourced()) {
            return rebuildFromEvents(data.getId());
        }
        
        // State-based 場景：直接映射
        return Product.builder()
            .id(ProductId.valueOf(data.getId()))
            .name(data.getName())
            .state(mapState(data.getState()))
            .creatorId(UserId.valueOf(data.getCreatorId()))
            .createdAt(data.getCreatedAt())
            .updatedAt(data.getUpdatedAt())
            .tasks(mapTasks(data.getTasks()))
            .goal(mapGoal(data.getGoal()))
            .build();
    }
}
```

## 🎯 查詢方法命名規範

### 1. Repository 查詢限制
```java
// ⚠️ 重要：Repository 只有三個基本方法
public interface Repository<T, ID> {
    Optional<T> findById(ID id);
    void save(T aggregate);
    void delete(T aggregate);
}

// ❌ 錯誤：不要在 Repository 加入額外查詢方法
public interface ProductRepository extends Repository<Product, ProductId> {
    List<Product> findByName(String name);  // 不應該在這裡
    List<Product> findByState(String state); // 應該用 Projection
}

// ✅ 正確：複雜查詢使用 Projection Pattern
// 請參考：[Projection 編碼規範](./projection-standards.md)
```

### 2. Spring Data JPA 內部介面命名
```java
// Spring Data JPA 介面（僅供內部 Repository 實作使用）
interface SpringDataProductRepository extends JpaRepository<ProductData, String> {
    // 這些方法僅供 Repository 實作類別內部使用
    // 不對外暴露，複雜查詢透過 Projection 處理
}

// 如需複雜查詢，使用 @Query（但僅限內部使用）
@Query("SELECT p FROM ProductData p WHERE p.name LIKE %:keyword%")
List<ProductData> searchByKeyword(@Param("keyword") String keyword);
```

## 🎯 事務管理

### 1. Repository 層不管理事務
```java
// ✅ 正確：Repository 不加 @Transactional
public class JpaProductRepository implements ProductRepository {
    @Override
    public void save(Product product) {
        // 不加 @Transactional，由 Service 層管理
    }
}

// ✅ 正確：Service 層管理事務
@Service
@Transactional
public class CreateProductService {
    public CqrsOutput<ProductDto> execute(CreateProductInput input) {
        // 事務在這裡管理
    }
}
```

### 2. 讀寫分離
```java
@Transactional(readOnly = true)  // 查詢操作
public class GetProductService {
    // ...
}

@Transactional(readOnly = false) // 寫入操作（預設）
public class CreateProductService {
    // ...
}
```

## 🎯 效能優化

### 1. 避免 N+1 查詢
```java
// ❌ 錯誤：會產生 N+1 查詢
@Entity
public class ProductData {
    @OneToMany(fetch = FetchType.EAGER)  // 避免 EAGER
    private List<TaskData> tasks;
}

// ✅ 正確：使用 JOIN FETCH
@Query("SELECT p FROM ProductData p LEFT JOIN FETCH p.tasks WHERE p.id = :id")
Optional<ProductData> findByIdWithTasks(@Param("id") String id);
```

### 2. 使用分頁
```java
Page<ProductDto> findAll(Pageable pageable);

// 使用
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
Page<ProductDto> page = projection.findAll(pageable);
```

### 3. 使用索引
```java
@Entity
@Table(name = "products",
       indexes = {
           @Index(name = "idx_product_name", columnList = "name"),
           @Index(name = "idx_product_state", columnList = "state"),
           @Index(name = "idx_product_creator", columnList = "creator_id")
       })
public class ProductData {
    // ...
}
```

## 🔍 檢查清單

### Repository Interface
- [ ] 直接使用 ezddd 框架的 generic Repository interface
- [ ] 不創建自定義的 Repository interface
- [ ] 使用 Repository<Aggregate, AggregateId> 泛型形式
- [ ] 透過 @Bean 配置注入具體實作

### Repository 實作
- [ ] 實作在 adapter 層
- [ ] 正確映射 domain 和 data model
- [ ] 處理 null 值
- [ ] 沒有事務註解


### JPA Entity
- [ ] 使用 @Entity 註解
- [ ] 有主鍵 @Id
- [ ] 欄位映射正確
- [ ] 關聯關係設定適當
- [ ] 有版本控制（如需要）

### 效能
- [ ] 避免 N+1 查詢
- [ ] 使用適當的 Fetch 策略
- [ ] 有必要的索引
- [ ] 支援分頁查詢

## 🔴 Outbox Repository 測試要求

### 必要測試案例（強制性）
每個實作 OutboxRepository 的 Aggregate **必須**包含以下標準測試案例：

1. **資料持久化測試** (`should_persist_[aggregate]_to_database_with_all_fields`)
   - 驗證所有欄位正確儲存到資料庫
   - 包括複雜物件的 JSON 序列化

2. **資料讀取測試** (`should_retrieve_[aggregate]_with_complete_data`)
   - 驗證從資料庫讀取的完整性
   - 確認複雜物件正確反序列化

3. **軟刪除測試** (`should_soft_delete_[aggregate]`)
   - 驗證使用 `save()` 而非 `delete()` 執行軟刪除
   - 確認 `isDeleted` 標記設置正確

4. **版本控制測試** (`should_handle_version_control_for_optimistic_locking`)
   - 驗證樂觀鎖機制
   - 確認版本號正確遞增

### 測試配置
```java
@SpringBootTest
@Transactional
@ActiveProfiles("test-outbox")  // 使用專門的測試 profile
@EzFeature
@EzFeatureReport
public class YourOutboxRepositoryTest {
    // 參考 ProductOutboxRepositoryTest.java 實作
}
```

### 標準測試範本
**必須參考**: `ProductOutboxRepositoryTest.java` - 所有 OutboxRepository 的標準測試範本
- 位置: `.ai/tech-stacks/java-ca-ezddd-spring/examples/outbox/ProductOutboxRepositoryTest.java`

## 相關文件
- [JPA 最佳實踐](../coding-standards.md#-jpa-最佳實踐)
- [包結構規範](../coding-standards.md#-包結構規範)
- [Repository 範例](../examples/repository/README.md)
- [Projection 編碼規範](./projection-standards.md)
- [Outbox Pattern 實作指南](../examples/outbox/README.md)
- [Outbox 測試配置指南](../examples/outbox/OUTBOX-TEST-CONFIGURATION.md)
- [ProductOutboxRepositoryTest 標準範本](../examples/outbox/ProductOutboxRepositoryTest.java)