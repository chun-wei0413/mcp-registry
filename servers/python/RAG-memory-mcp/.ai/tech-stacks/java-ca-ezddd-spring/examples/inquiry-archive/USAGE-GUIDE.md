# Inquiry 與 Archive 使用指南

## 📖 快速開始

本指南提供 Inquiry 和 Archive 模式的實際使用範例和最佳實踐。

## 🔍 Inquiry 模式使用指南

### 1. 定義 Inquiry 介面

```java
// 位置：{aggregate}/usecase/port/out/inquiry/
package tw.teddysoft.aiscrum.sprint.usecase.port.out.inquiry;

public interface FindPbisBySprintIdInquiry {
    List<String> findBySprintId(SprintId sprintId);
    List<String> findBySprintIdAndStates(SprintId sprintId, List<String> states);
    int countBySprintId(SprintId sprintId);
}
```

### 2. 實作 Inquiry

```java
// 位置：{aggregate}/adapter/out/persistence/inquiry/
package tw.teddysoft.aiscrum.sprint.adapter.out.persistence.inquiry;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * JPA Inquiry 使用 interface 繼承 CrudRepository
 * 重要：必須是 interface，不是 class
 */
public interface JpaFindPbisBySprintIdInquiry 
        extends FindPbisBySprintIdInquiry,
                CrudRepository<ProductBacklogItemData, String> {
    
    @Override
    default List<String> findBySprintId(SprintId sprintId) {
        // 使用 default method 實作業務介面方法
        return getPbisBySprintId(sprintId.value());
    }
    
    @Query(value = """
            SELECT p.pbi_id 
            FROM product_backlog_item_data p 
            WHERE p.sprint_id = :sprintId 
            AND p.deleted = false 
            ORDER BY p.order_id
            """, nativeQuery = true)
    List<String> getPbisBySprintId(@Param("sprintId") String sprintId);
}
```

### 3. 在 Reactor 中使用 Inquiry

```java
public class NotifyPbiWhenSprintStartedService implements NotifyPbiWhenSprintStartedReactor {
    
    private final FindPbisBySprintIdInquiry findPbisBySprintIdInquiry;
    private final StartPbiUseCase startPbiUseCase;
    
    @Override
    public void execute(Object event) {
        if (event instanceof SprintEvents.SprintStarted sprintStarted) {
            // 使用 Inquiry 查詢相關 PBI
            List<String> pbiIds = findPbisBySprintIdInquiry.findBySprintId(
                SprintId.valueOf(sprintStarted.sprintId())
            );
            
            // 處理每個 PBI
            pbiIds.forEach(pbiId -> startPbi(pbiId, sprintStarted));
        }
    }
    
    private void startPbi(String pbiId, SprintEvents.SprintStarted event) {
        try {
            var input = new StartPbiInput();
            input.setPbiId(pbiId);
            input.setSprintId(event.sprintId());
            input.setStartedBy(event.startedBy());
            
            startPbiUseCase.execute(input);
        } catch (Exception e) {
            logger.error("Failed to start PBI: " + pbiId, e);
        }
    }
}
```

### 4. Spring 配置注入

```java
@Configuration
public class SprintReactorConfig {
    
    @Bean
    public NotifyPbiWhenSprintStartedReactor notifyPbiWhenSprintStartedReactor(
            FindPbisBySprintIdInquiry inquiry,
            StartPbiUseCase useCase) {
        return new NotifyPbiWhenSprintStartedService(inquiry, useCase);
    }
}
```

## 🗄️ Archive 模式使用指南

### 1. 定義 Archive 介面

```java
// 位置：{aggregate}/usecase/port/out/archive/
package tw.teddysoft.aiscrum.product.usecase.port.out.archive;

public interface ProductArchive {
    void archive(Product product, String reason, String archivedBy);
    Optional<ArchivedProduct> findArchivedById(ProductId productId);
    List<ArchivedProduct> findArchivedBetween(LocalDateTime from, LocalDateTime to);
    Optional<Product> restore(ProductId productId, String restoredBy);
    boolean permanentlyDelete(ProductId productId, String deletedBy);
    boolean isArchived(ProductId productId);
}
```

### 2. 定義歸檔資料 Entity

```java
// 位置：{aggregate}/adapter/out/persistence/archive/
@Entity
@Table(name = "archived_products")
public class ArchivedProductData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "goal", length = 1000)
    private String goal;
    
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
    
    @Column(name = "archived_by", nullable = false)
    private String archivedBy;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Lob
    @Column(name = "original_data", nullable = false)
    private String originalData;  // JSON 格式的完整資料
    
    // getters and setters
}
```

### 3. 實作 Archive

```java
@Repository
public class JpaProductArchive implements ProductArchive {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @Transactional
    public void archive(Product product, String reason, String archivedBy) {
        requireNotNull("Product", product);
        requireNotNull("Reason", reason);
        requireNotNull("Archived by", archivedBy);
        
        // 檢查是否已歸檔
        if (isArchived(product.getId())) {
            throw new IllegalStateException(
                "Product " + product.getId().id() + " is already archived"
            );
        }
        
        // 創建歸檔記錄
        ArchivedProductData archived = createArchivedData(product, reason, archivedBy);
        entityManager.persist(archived);
        
        // 軟刪除主表記錄
        softDeleteProduct(product.getId());
    }
    
    private ArchivedProductData createArchivedData(
            Product product, String reason, String archivedBy) {
        var archived = new ArchivedProductData();
        archived.setProductId(product.getId().id());
        archived.setName(product.getName());
        archived.setGoal(product.getGoal());
        archived.setArchivedAt(LocalDateTime.now());
        archived.setArchivedBy(archivedBy);
        archived.setReason(reason);
        archived.setOriginalData(JsonSerializer.toJson(product));
        return archived;
    }
    
    private void softDeleteProduct(ProductId productId) {
        String updateJpql = """
            UPDATE ProductData p 
            SET p.deleted = true, p.deletedAt = :deletedAt 
            WHERE p.id = :productId
            """;
        
        entityManager.createQuery(updateJpql)
            .setParameter("productId", productId.id())
            .setParameter("deletedAt", LocalDateTime.now())
            .executeUpdate();
    }
}
```

### 4. 在 Use Case 中使用 Archive

```java
public class DeleteProductService implements DeleteProductUseCase {
    
    private final Repository<Product, ProductId> productRepository;
    private final ProductArchive productArchive;
    private final DomainEventBus eventBus;
    
    @Override
    public DeleteProductOutput execute(DeleteProductInput input) {
        requireNotNull("Input", input);
        
        // 查找產品
        Product product = productRepository.findById(
            ProductId.valueOf(input.getProductId())
        ).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        // 歸檔而非直接刪除
        productArchive.archive(
            product,
            input.getReason() != null ? input.getReason() : "User deletion",
            input.getDeletedBy()
        );
        
        // 發布領域事件
        eventBus.publish(new ProductEvents.ProductDeleted(
            product.getId().id(),
            input.getDeletedBy(),
            LocalDateTime.now()
        ));
        
        return new DeleteProductOutput(
            input.getProductId(),
            "Product archived successfully"
        );
    }
}
```

### 5. 恢復歸檔資料

```java
public class RestoreProductService implements RestoreProductUseCase {
    
    private final ProductArchive productArchive;
    private final DomainEventBus eventBus;
    
    @Override
    public RestoreProductOutput execute(RestoreProductInput input) {
        requireNotNull("Input", input);
        
        // 恢復產品
        Product product = productArchive.restore(
            ProductId.valueOf(input.getProductId()),
            input.getRestoredBy()
        ).orElseThrow(() -> new IllegalArgumentException(
            "Archived product not found"
        ));
        
        // 發布恢復事件
        eventBus.publish(new ProductEvents.ProductRestored(
            product.getId().id(),
            input.getRestoredBy(),
            LocalDateTime.now()
        ));
        
        return new RestoreProductOutput(
            product.getId().id(),
            "Product restored successfully"
        );
    }
}
```

## 💡 實用技巧

### Inquiry 實用技巧

#### 1. 批量查詢優化

```java
public interface FindPbisByMultipleSprintsInquiry {
    
    // 使用 IN 查詢批量獲取
    Map<String, List<String>> findBySprintIds(Set<SprintId> sprintIds);
}

/**
 * 複雜查詢也可以使用 interface，但需要更多的輔助方法
 */
public interface JpaFindPbisByMultipleSprintsInquiry 
        extends FindPbisByMultipleSprintsInquiry,
                CrudRepository<ProductBacklogItemData, String> {
    
    @Override
    default Map<String, List<String>> findBySprintIds(Set<SprintId> sprintIds) {
        if (sprintIds.isEmpty()) {
            return Map.of();
        }
        
        // 將 SprintId 轉換為 String Set
        Set<String> sprintIdStrings = sprintIds.stream()
            .map(SprintId::value)
            .collect(Collectors.toSet());
        
        // 查詢並分組
        List<PbiSprintPair> pairs = getPbisByMultipleSprintIds(sprintIdStrings);
        
        return pairs.stream()
            .collect(Collectors.groupingBy(
                PbiSprintPair::sprintId,
                Collectors.mapping(PbiSprintPair::pbiId, Collectors.toList())
            ));
    }
    
    @Query(value = """
            SELECT p.sprint_id as sprintId, p.pbi_id as pbiId
            FROM product_backlog_item_data p 
            WHERE p.sprint_id IN :sprintIds 
            AND p.deleted = false
            """, nativeQuery = true)
    List<PbiSprintPair> getPbisByMultipleSprintIds(@Param("sprintIds") Set<String> sprintIds);
    
    // Projection interface for query result
    interface PbiSprintPair {
        String getSprintId();
        String getPbiId();
    }
}
```

#### 2. 分頁查詢

```java
public interface PagedProductInquiry {
    
    record Page<T>(List<T> content, int page, int size, long total) {}
    
    Page<ProductInfo> findProducts(int page, int size, String sortBy);
}

/**
 * 分頁查詢使用 Spring Data JPA 的 PagingAndSortingRepository
 */
public interface JpaPagedProductInquiry 
        extends PagedProductInquiry,
                PagingAndSortingRepository<ProductData, String>,
                CrudRepository<ProductData, String> {
    
    @Override
    default Page<ProductInfo> findProducts(int page, int size, String sortBy) {
        // 使用 Spring Data JPA 的分頁功能
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortBy));
        
        // 查詢未刪除的產品
        org.springframework.data.domain.Page<ProductData> dataPage = 
            findByDeletedFalse(pageRequest);
        
        // 轉換為 DTO
        List<ProductInfo> content = dataPage.getContent().stream()
            .map(p -> new ProductInfo(p.getId(), p.getName(), p.getGoal()))
            .collect(Collectors.toList());
        
        return new Page<>(content, page, size, dataPage.getTotalElements());
    }
    
    // Spring Data JPA 會自動產生實作
    org.springframework.data.domain.Page<ProductData> findByDeletedFalse(Pageable pageable);
}
```

### Archive 實用技巧

#### 1. 批量歸檔

```java
@Transactional
public void archiveMultiple(List<Product> products, String reason, String archivedBy) {
    // 使用批量插入提升效能
    products.forEach(product -> {
        ArchivedProductData archived = createArchivedData(product, reason, archivedBy);
        entityManager.persist(archived);
    });
    
    // 批量軟刪除
    Set<String> productIds = products.stream()
        .map(p -> p.getId().id())
        .collect(Collectors.toSet());
    
    String updateJpql = """
        UPDATE ProductData p 
        SET p.deleted = true, p.deletedAt = :deletedAt 
        WHERE p.id IN :productIds
        """;
    
    entityManager.createQuery(updateJpql)
        .setParameter("productIds", productIds)
        .setParameter("deletedAt", LocalDateTime.now())
        .executeUpdate();
}
```

#### 2. 定期清理策略

```java
@Component
public class ArchiveCleanupService {
    
    private final ProductArchive productArchive;
    
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 點執行
    public void cleanupOldArchives() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(7); // 保留 7 年
        
        List<ArchivedProduct> oldArchives = productArchive.findArchivedBefore(cutoffDate);
        
        oldArchives.forEach(archived -> {
            productArchive.permanentlyDelete(
                ProductId.valueOf(archived.productId()),
                "SYSTEM_CLEANUP"
            );
        });
        
        logger.info("Cleaned up {} old archives", oldArchives.size());
    }
}
```

## ⚠️ 常見問題

### Q1: 何時使用 Inquiry vs Projection？

**使用 Inquiry 當：**
- 在 Reactor 中需要查詢其他聚合
- 查詢邏輯太複雜，不適合放在 Projection
- 需要跨多個表的複雜 JOIN

**使用 Projection 當：**
- 標準的 CQRS 查詢端需求
- 簡單的列表或詳情查詢
- 可以預先計算的視圖資料

### Q2: Archive 資料該保存多久？

根據不同的需求：
- **法規要求**：依照當地法規（如 GDPR 要求）
- **審計需求**：通常 3-7 年
- **業務需求**：根據業務價值決定

### Q3: 如何處理歸檔資料的查詢效能？

1. **分離存儲**：使用獨立的歸檔資料表
2. **建立索引**：為常用查詢欄位建立索引
3. **資料分區**：按時間分區歸檔表
4. **冷熱分離**：舊資料移至冷存儲

## 📋 檢查清單

### Inquiry 實作檢查清單
- [ ] 介面命名遵循 `Find[What]By[Condition]Inquiry` 格式
- [ ] 每個介面只負責一種查詢
- [ ] 返回簡單類型（ID 列表或 DTO）
- [ ] 包含參數驗證（requireNotNull）
- [ ] 提供清晰的 JavaDoc 文檔
- [ ] 實作類標註 @Component
- [ ] 使用 TypedQuery 確保類型安全

### Archive 實作檢查清單
- [ ] 保存完整的聚合狀態（JSON 序列化）
- [ ] 記錄歸檔元資料（時間、原因、操作者）
- [ ] 實作軟刪除機制
- [ ] 提供恢復功能
- [ ] 包含永久刪除選項
- [ ] 事務控制（@Transactional）
- [ ] 防止重複歸檔

## 🔗 相關資源

- [Inquiry 介面範例](./FindPbisBySprintIdInquiry.java)
- [Inquiry JPA 實作範例](./JpaFindPbisBySprintIdInquiry.java)
- [Archive 介面範例](./ProductArchive.java)
- [Archive JPA 實作範例](./JpaProductArchive.java)
- [完整文檔](./README.md)