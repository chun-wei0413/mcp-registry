# Common Pitfalls and Solutions

本文件記錄在使用 Sub-agent Workflow 實作時常見的錯誤和解決方案。

## 🔴 Spring Configuration 問題（最嚴重！）

### 1. 重複的 @EnableJpaRepositories
**錯誤訊息**: `BeanDefinitionOverrideException: Invalid bean definition with name 'jpaXXX'`

**根本原因**: 多個類別都有 @EnableJpaRepositories
- DataSourceConfig.java 有
- JpaConfiguration.java 也有（重複！）
- 個別 Projection 類別也有（錯誤！）

**解決方案**:
1. 只在 DataSourceConfig 保留 @EnableJpaRepositories
2. 刪除 JpaConfiguration.java
3. 移除 Projection 類別的 @EnableJpaRepositories
4. 執行 `.ai/scripts/check-spring-config.sh` 驗證

**預防措施**:
```bash
# 建立新配置前先檢查
grep -n "@EnableJpaRepositories" src/main/java/**/*.java
```

### 2. UseCase Input 欄位名稱不一致
**錯誤訊息**: 編譯錯誤 `cannot find symbol: variable productId`

**根本原因**: Controller 假設 UseCase Input 有 `productId` 但實際上是 `id`
- REST DTO 使用: `productId`, `sprintId`, `taskId`
- UseCase Input 使用: `id`（通用名稱）

**解決方案**:
```java
// 錯誤
input.productId = request.productId;

// 正確
input.id = request.productId;  // 對應 DTO 欄位到實際 Input 欄位
```

**預防措施**:
```bash
# 編碼前檢查實際欄位名稱
grep -A5 "class.*Input" src/main/java/**/usecase/*UseCase.java
```

### 3. OutboxInfrastructureConfig 錯誤的 Import
**錯誤訊息**: `cannot find symbol: class JpaConfiguration`

**根本原因**: OutboxInfrastructureConfig 有 `@AutoConfigureAfter(JpaConfiguration.class)`
但 JpaConfiguration 已被刪除以修復重複 @EnableJpaRepositories

**解決方案**:
```java
// 錯誤
@AutoConfigureAfter(JpaConfiguration.class)

// 正確
@AutoConfigureAfter(DataSourceConfig.class)
```

## 🔴 Framework API 使用錯誤

### 1. PgMessageDbClient 建立錯誤

#### ❌ 錯誤方式
```java
@Bean
public PgMessageDbClient pgMessageDbClient(DataSource dataSource) {
    return new PgMessageDbClient(dataSource);  // PgMessageDbClient is abstract!
}
```

#### ✅ 正確方式
```java
@Bean
public PgMessageDbClient pgMessageDbClient() {
    RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
    return factory.getRepository(PgMessageDbClient.class);
}
```

### 2. Outbox Import 路徑錯誤

#### ❌ 錯誤 Import
```java
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepositoryPeerAdapter;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxStore;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.springjpa.SpringJpaClient;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.AbstractOutboxRepository;
```

#### ✅ 正確 Import
```java
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeerAdapter;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore;
import tw.teddysoft.ezddd.data.io.ezoutbox.SpringJpaClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxStoreAdapter;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;
```

### 3. Value Object 缺少 valueOf 方法

#### ❌ 缺少 valueOf
```java
public record ProductId(UUID value) implements ValueObject {
    public ProductId {
        Objects.requireNonNull(value);
    }
    // 缺少 valueOf 方法，導致 Mapper 無法轉換
}
```

#### ✅ 包含 valueOf
```java
public record ProductId(UUID value) implements ValueObject {
    public ProductId {
        Objects.requireNonNull(value);
    }
    
    public static ProductId valueOf(String value) {
        return new ProductId(UUID.fromString(value));
    }
}
```

### 4. Projection 實作錯誤的方法

#### ❌ 實作 execute 方法
```java
@Override
public List<ProductDto> execute(ProductDtoProjectionInput input) {
    return queryAll();  // 錯誤！應該是 query 方法
}
```

#### ✅ 實作 query 方法
```java
@Override
public List<ProductDto> query(ProductDtoProjectionInput input) {
    return queryAll();  // 正確！
}
```

### 5. Projection Repository 衝突

#### ❌ 直接覆寫 findAll
```java
public interface ProductDtoProjectionImpl extends JpaRepository<ProductData, String>, ProductDtoProjection {
    @Override
    default List<ProductDto> findAll() {  // 錯誤！返回類型不相容
        // ...
    }
}
```

#### ✅ 使用 Adapter 模式
```java
// Repository 介面
@Repository
public interface ProductDtoProjectionRepository extends JpaRepository<ProductData, String> {
    @Query("SELECT p FROM ProductData p")
    List<ProductData> findAllProducts();
}

// Adapter 實作
@Component
public class ProductDtoProjectionAdapter implements ProductDtoProjection {
    private final ProductDtoProjectionRepository repository;
    
    @Override
    public List<ProductDto> queryAll() {
        return repository.findAllProducts().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductDto> query(ProductDtoProjectionInput input) {
        return queryAll();
    }
}
```

## 🔴 Configuration 錯誤

### 1. 缺少 @EnableJpaRepositories 配置

#### ❌ 遺漏套件路徑
```java
@Configuration
@EnableJpaRepositories(basePackages = "tw.teddysoft.aiscrum.io.springboot.config.orm")
public class UseCaseInjection {
    // 忘記加入 projection 套件路徑
}
```

#### ✅ 包含所有必要路徑
```java
@Configuration
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.io.springboot.config.orm",
    "tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection"
})
public class UseCaseInjection {
    // 包含所有 JPA repository 套件
}
```

### 2. Outbox Configuration 順序錯誤

#### ❌ 錯誤的 Bean 依賴
```java
@Bean
public OutboxStore<ProductData, String> productOutboxStore(
        ProductOrmClient productOrmClient,
        PgMessageDbClient pgMessageDbClient) {
    return new OutboxStore<>(productOrmClient, ProductMapper.newMapper());  // 錯誤！
}
```

#### ✅ 正確的 Bean 鏈
```java
@Bean
public EzOutboxClient<ProductData, String> productOutboxClient(
        ProductOrmClient productOrmClient,
        PgMessageDbClient pgMessageDbClient) {
    return new EzOutboxClient<>(productOrmClient, pgMessageDbClient);
}

@Bean
public OutboxStore<ProductData, String> productOutboxStore(
        EzOutboxClient<ProductData, String> outboxClient) {
    return EzOutboxStoreAdapter.createOutboxStore(outboxClient);
}

@Bean  
public Repository<Product, ProductId> productRepository(
        OutboxStore<ProductData, String> outboxStore) {
    return new OutboxRepository<>(
        new OutboxRepositoryPeerAdapter<>(outboxStore),
        ProductMapper.newMapper()
    );
}
```

## 🔴 Controller 實作錯誤

### 1. 忘記使用 Objects.requireNonNull

#### ❌ 缺少 null 檢查
```java
public CreateProductController(CreateProductUseCase createProductUseCase) {
    this.createProductUseCase = createProductUseCase;  // 錯誤！應該要檢查 null
}
```

#### ✅ 使用 Objects.requireNonNull
```java
public CreateProductController(CreateProductUseCase createProductUseCase) {
    this.createProductUseCase = Objects.requireNonNull(createProductUseCase);
}
```

### 2. Command 操作檢查 ExitCode

#### ❌ 不必要的 ExitCode 檢查
```java
CqrsOutput output = createProductUseCase.execute(input);
if (output.getExitCode() == ExitCode.SUCCESS) {  // 非同步操作不需要檢查
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
}
```

#### ✅ 直接返回 202 ACCEPTED
```java
createProductUseCase.execute(input);  // 執行即可
return ResponseEntity.status(HttpStatus.ACCEPTED)
        .location(location)
        .body(response);
```

## 🔴 測試配置錯誤

### 1. 使用 test-outbox profile 執行測試

#### ❌ 觸發 PIT Mutation Testing
```bash
mvn test -Dtest=ClassName -q  # 如果預設是 test-outbox，會超時
```

#### ✅ 明確指定 test-inmemory
```bash
SPRING_PROFILES_ACTIVE=test-inmemory mvn test -Dtest=ClassName -q
```

## 預防措施

1. **使用 Sub-agent Workflow**：讓專門的 sub-agent 處理特定類型的程式碼生成
2. **參考範例程式碼**：查看 `.ai/tech-stacks/java-ca-ezddd-spring/examples/` 的正確實作
3. **執行自動檢查**：使用 `.ai/scripts/` 下的檢查腳本驗證實作
4. **查看 CLAUDE.md**：了解最新的框架 API 使用方式
5. **測試先行**：實作後立即執行測試確認正確性

## 更新紀錄

- 2025-09-03: 初版建立，記錄 Outbox Pattern 實作中的常見錯誤