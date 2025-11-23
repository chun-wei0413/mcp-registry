# Projection 編碼規範

本文件定義 Projection Pattern 的編碼標準，用於處理查詢需求和資料投影。

## 📌 核心概念

**Projection** 是一種查詢模式，在 CQRS 架構中，專門用於 「Query Model」：
- 複雜查詢需求（Repository 只限定在 Command Model 操作單一 Aggregate 使用）
- 跨聚合查詢
- 報表和統計查詢
- 返回 Data (Persistence Object) 而非領域物件
- Use Cases Layer 的 Query 物件會呼叫 Projection，並將 Projection 回傳的 Data 物件轉成 DTO 傳給呼叫端
 
## 🔴 必須遵守的規則 (MUST FOLLOW)

### 1. Projection Interface 設計

#### 套件位置
```java
// ✅ 正確：Projection 介面定義在 usecase.port.out.projection 套件
package tw.teddysoft.aiscrum.product.usecase.port.out.projection;

// ❌ 錯誤：不要放在其他位置
package tw.teddysoft.aiscrum.product.usecase.port.out;  // 缺少 projection
package tw.teddysoft.aiscrum.product.adapter.out;       // 不應在 adapter 層
```

#### 介面命名規範
```java
// ✅ 正確：使用 XxxProjection 命名（複數形）
public interface ProductsProjection { }
public interface SprintsProjection { }
public interface ProductBacklogItemsProjection { }

// ❌ 錯誤：不要使用其他命名模式
public interface ProductQuery { }        // 不要用 Query
public interface ProductFinder { }       // 不要用 Finder
public interface IProductProjection { }  // 不要加 I 前綴
public interface ProductDtoProjection { } // 舊規範，不要用 DtoProjection
```

#### 介面繼承規範
```java
import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;

// ✅ 正確：繼承 Projection<Input, Output> 介面
public interface ProductsProjection extends Projection<ProductsProjection.ProductsProjectionInput, List<ProductData>> {
    // query 方法由 Projection 介面定義，不需要重複宣告
}

// ❌ 錯誤：不繼承 Projection 介面
public interface ProductsProjection {
    List<ProductData> query(ProductsProjectionInput input);
}
```

#### 方法設計原則
```java
import tw.teddysoft.aiscrum.product.usecase.port.out.ProductData;
import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;
import java.util.List;

public interface ProductsProjection extends Projection<ProductsProjection.ProductsProjectionInput, List<ProductData>> {

    // ✅ 正確：query 方法由 Projection 介面繼承而來
    // 不需要重複宣告
    
    // ❌ 錯誤：不可以自行宣告其他方法
    // List<ProductData> queryAll();
    // List<ProductData> queryByState(String state);
    
    // 輸入參數使用 inner class，必須實作 ProjectionInput
    class ProductsProjectionInput implements ProjectionInput {
        public String productId;
        
        public ProductsProjectionInput() {
            // 預設構造子，查詢所有產品
        }
        
        public ProductsProjectionInput(String productId) {
            this.productId = productId;
        }
    }
}
```

#### 返回類型規範
```java
public interface ProductsProjection {
    
    // ✅ 正確：返回 DATA (Persistence Object) 物件
    List<ProductData> query(ProductsProjectionInput input);
    
    // ❌ 錯誤：不要返回領域物件
    List<Product> query(ProductsProjectionInput input);
    
    // ❌ 錯誤：不要返回 DTO（Use Case 層負責轉換）
    List<ProductDto> query(ProductsProjectionInput input);
}
```

### 2. Projection 實作

#### 實作位置
```java
// ✅ 正確：實作放在 adapter.out.database.springboot.projection 套件
package tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection;
```

#### JPA Projection 實作範例
```java
package tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 當使用 Spring Data JPA 時，可以創建一個介面繼承 JpaRepository

// ⚠️ 重要：不要加 @Repository 註解，Spring Data JPA 會自動產生 bean
public interface JpaProductsProjection extends ProductsProjection, JpaRepository<ProductData, String> {

    @Override
    default List<ProductData> query(ProductsProjectionInput input) {
        return getProducts(input.getProductId());
    }

    @Query(value = """
            SELECT *
            FROM product
            WHERE (:productId IS NULL OR product_id = :productId)
            """,
            nativeQuery = true)
    List<ProductData> getProducts(@Param("productId") String productId);
}
```

#### ⚠️ 重要：JPA Projection Bean 管理方式

JPA Projection 有兩種 bean 管理方式：

##### 方式一：透過 @EnableJpaRepositories 自動掃描（推薦）
```java
@Configuration
@EnableJpaRepositories(basePackages = {
    // ... 其他套件 ...
    "tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection",  // ✅ Spring Data JPA 自動產生 bean
    // ... 其他套件 ...
})
public class JpaConfiguration {
    // Spring Data JPA 會自動為該套件下的 JpaRepository 介面產生實作
}
```

##### 方式二：明確宣告 Bean（當需要特殊配置時）
```java
@Configuration
@Profile("outbox")
public class OutboxProjectionConfig {
    
    @Autowired
    private JpaProductsProjection jpaProductsProjection;  // Spring Data JPA 自動注入
    
    @Bean
    @Primary
    public ProductsProjection productsProjection() {
        return jpaProductsProjection;  // 包裝為 Projection 介面
    }
}
```

**常見錯誤**：
- ❌ 在 JPA Projection 介面上加 `@Repository` 註解（不需要）
- ❌ 忘記在 `@EnableJpaRepositories` 中加入套件路徑
- ❌ 嘗試手動實例化 JPA interface（如 `new JpaProductsProjection()`）

#### InMemory Projection 實作範例
```java
package tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProductsProjection implements ProductsProjection {

    private final Map<String, ProductData> store;

    // 透過建構子注入
    public InMemoryProductsProjection(Map<String, ProductData> store) {
        this.store = store;
    }

    @Override
    public List<ProductData> query(ProductsProjectionInput input) {
        String productId = input.getProductId();
        if (productId == null) {
            return new ArrayList<>(store.values()); // 回傳全部
        }
        ProductData product = store.get(productId);
        if (product == null) {
            return Collections.emptyList();
        }
        return List.of(product);
    }

    // 測試或初始化方便用的方法
    public void save(ProductData product) {
        store.put(product.getProductId(), product);
    }

    public void delete(String productId) {
        store.remove(productId);
    }

    public void clear() {
        store.clear();
    }
}
```

### 3. Spring Configuration

#### Profile-based 配置
```java
@Configuration
@Profile("outbox")
public class OutboxProjectionConfig {
    
    private JpaProductDataProjection jpaProductsProjection;

    @Autowired
    public OutboxProjectionConfig(JpaProductDataProjection jpaProductsProjection){
        this.jpaProductsProjection = jpaProductsProjection;
    }
    
    @Bean
    @Primary
    public ProductsProjection productsProjection() {
        // Outbox profile 使用 JPA 實作
        return jpaProductsProjection;
    }
}

@Configuration
public class UseCaseConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(ProductDtoProjection.class)
    public ProductDtoProjection productDtoProjection(Repository<Product, ProductId> productRepository) {
        // 預設使用 InMemory 實作
        return new InMemoryProductsProjection(productRepository);
    }
}
```

## 🎯 使用場景指南

### 1. 何時使用 Projection
- ✅ 複雜查詢需求（JOIN、聚合、統計）
- ✅ 跨聚合查詢
- ✅ 報表和分析查詢
- ✅ UI 特定的查詢需求
- ❌ Write Model 的 CRUD 操作（使用 Repository）

### 2. 與 Repository 的區別
```java
// Repository：Write Model 的 Aggregate 持久化
Repository<Product, ProductId> repository;
repository.findById(id);  // 返回 Product 領域物件
repository.save(product); // 儲存領域物件

// Projection：Read Model 的查詢和資料投影
ProductsProjection projection;
List<ProductData> projection.query(input);  // 返回 ProductDto
```

### 3. 與 Inquiry 的區別
- **Projection**: 用於 Read Model Query 的查詢需求
- **Inquiry**: 用於 Write Model Command 的查詢需求

## 🔍 檢查清單

### Projection Interface
- [ ] 定義在 `usecase.port.out.projection` 套件
- [ ] 使用 `XxxProjection` 命名（複數形）
- [ ] 繼承 `Projection<Input, Output>` 介面
- [ ] Input 類別實作 `ProjectionInput` 介面
- [ ] 只依賴繼承的 `query` 方法，不自行宣告其他方法
- [ ] 使用具名參數類別作為輸入（inner class）
- [ ] 返回 Data (Persistence Object) 而非領域物件或 DTO

### Projection 實作
- [ ] 實作在 `adapter.out.database.springboot.projection` 套件
- [ ] **JPA Projection 不要加 `@Repository` 註解**（Spring Data JPA 自動管理）
- [ ] 處理 null 值和空集合

### Spring Configuration
- [ ] 使用 @Profile 區分不同環境
- [ ] 使用 @ConditionalOnMissingBean 避免衝突
- [ ] 正確配置 Bean 優先順序
- [ ] **JPA Projection: 確認套件路徑已加入 `JpaConfiguration` 的 `@EnableJpaRepositories`**
- [ ] **JPA Projection: 選擇合適的 bean 管理方式（自動掃描 or 明確宣告）**

## 📚 相關文件
- [Repository 規範](./repository-standards.md)
- [Use Case 規範](./usecase-standards.md)
- [Inquiry Pattern 指南](../examples/inquiry-archive/README.md)
- [Query Use Case 實作指引](../../prompts/query-sub-agent-prompt.md)