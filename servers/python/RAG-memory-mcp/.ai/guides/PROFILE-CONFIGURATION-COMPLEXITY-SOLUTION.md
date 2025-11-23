# Profile 配置複雜性完整解決方案 🎯

## 問題總覽

Profile 配置複雜性導致的三大挑戰：
1. **條件化 Bean 載入**：不同 profile 需要不同的 Repository 實作
2. **JPA 配置衝突**：inmemory 不需要 JPA，但框架仍嘗試初始化
3. **依賴注入複雜度**：Outbox 模式需要多層依賴注入

## 🏗️ Profile 架構與隔離策略

```
Profile 隔離架構
├── Configuration 類別隔離
│   ├── InMemoryConfiguration (只載入於 inmemory profiles)
│   ├── OutboxConfiguration (只載入於 outbox profiles)
│   └── CommonConfiguration (所有 profiles 共用)
│
├── Properties 檔案隔離
│   ├── application-inmemory.properties
│   ├── application-outbox.properties
│   ├── application-test-inmemory.properties
│   └── application-test-outbox.properties
│
└── Bean 定義隔離
    ├── Repository Beans (profile-specific)
    ├── DataSource Beans (outbox only)
    └── UseCase Beans (profile-independent)
```

## 解決方案 1：條件化 Bean 載入 🔧

### 策略：使用專屬 Configuration 類別

```java
// ===== CommonConfiguration.java =====
// 所有 Profile 共用的配置
@Configuration
public class CommonConfiguration {
    
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> repository) {
        // Repository 由 Profile-specific 配置提供
        return new CreateProductService(repository);
    }
    
    @Bean
    public GetProductsUseCase getProductsUseCase(
            ProductsProjection projection) {
        return new GetProductsService(projection);
    }
}

// ===== InMemoryConfiguration.java =====
// 只在 InMemory profiles 載入
@Configuration
@Profile({"inmemory", "test-inmemory", "default"})
@ConditionalOnMissingBean(DataSource.class)  // 額外保護
public class InMemoryConfiguration {
    
    @Bean
    public MessageBus messageBus() {
        return new MyInMemoryMessageBroker();
    }
    
    @Bean
    public Repository<Product, ProductId> productRepository(MessageBus messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    @Bean
    public ProductsProjection productsProjection() {
        return new InMemoryProductsProjection();
    }
}

// ===== OutboxConfiguration.java =====
// 只在 Outbox profiles 載入
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.io.springboot.config.orm",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
public class OutboxConfiguration {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    // Outbox 基礎設施
    @Bean
    public PgMessageDbClient pgMessageDbClient() {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
    
    @Bean
    public EzOutboxClient<ProductData, String> productOutboxClient(
            ProductOrmClient ormClient,
            PgMessageDbClient pgMessageDbClient) {
        return new EzOutboxClient<>(ormClient, pgMessageDbClient);
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
    
    @Bean
    public ProductsProjection productsProjection() {
        return new JpaProductsProjection();
    }
}
```

## 解決方案 2：JPA 配置衝突 🛡️

### 策略：Profile-specific 自動配置排除

```properties
# ===== application-inmemory.properties =====
# 完全排除所有 JPA 和 DataSource 相關配置
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,\
  org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration

# 明確停用 JPA
spring.jpa.enabled=false
spring.data.jpa.repositories.enabled=false

# ===== application-outbox.properties =====
# 啟用所有 JPA 功能
spring.jpa.enabled=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# DataSource 配置
spring.datasource.url=jdbc:postgresql://localhost:5432/aiscrum
spring.datasource.username=postgres
spring.datasource.password=root
```

### 進階保護：條件化註解

```java
// 使用 @ConditionalOnProperty 進行更精細的控制
@Configuration
@ConditionalOnProperty(
    prefix = "spring.jpa",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class JpaSpecificConfiguration {
    // 只在 JPA 啟用時載入的配置
}
```

## 解決方案 3：依賴注入複雜度 📦

### 策略：分層配置與延遲初始化

```java
// ===== Layer 1: 基礎設施層 =====
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@Order(1)  // 確保最先載入
public class OutboxInfrastructureConfiguration {
    
    @Bean
    @Primary  // 標記為主要 DataSource
    public DataSource dataSource() {
        // DataSource 配置
    }
    
    @Bean
    public EntityManagerFactory entityManagerFactory(DataSource dataSource) {
        // EntityManagerFactory 配置
    }
}

// ===== Layer 2: ORM 層 =====
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@Order(2)
@DependsOn("outboxInfrastructureConfiguration")
public class OutboxOrmConfiguration {
    
    @Bean
    public PgMessageDbClient pgMessageDbClient(EntityManager entityManager) {
        // PgMessageDbClient 配置
    }
    
    // OrmClient interfaces 會由 @EnableJpaRepositories 自動產生
}

// ===== Layer 3: Repository 層 =====
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@Order(3)
@DependsOn("outboxOrmConfiguration")
public class OutboxRepositoryConfiguration {
    
    @Bean
    @Lazy  // 延遲初始化，避免循環依賴
    public Repository<Product, ProductId> productRepository(
            @Qualifier("productOutboxStore") OutboxStore<ProductData, String> outboxStore) {
        return new OutboxRepository<>(
            new OutboxRepositoryPeerAdapter<>(outboxStore),
            ProductMapper.newMapper()
        );
    }
}
```

## 🔍 Profile 衝突診斷工具

### 診斷腳本：diagnose-profile-conflicts.sh

```bash
#!/bin/bash

echo "=== Profile 配置衝突診斷 ==="

# 1. 檢查 Active Profile
ACTIVE_PROFILE=$(grep "spring.profiles.active" application.properties | cut -d'=' -f2)
echo "Active Profile: $ACTIVE_PROFILE"

# 2. 檢查 Configuration 類別
echo -e "\n配置類別檢查："
for config in $(find src -name "*Configuration.java"); do
    profile=$(grep "@Profile" "$config" | head -1)
    if [ ! -z "$profile" ]; then
        echo "  $config -> $profile"
    fi
done

# 3. 檢查 Bean 衝突
echo -e "\nBean 定義檢查："
echo "Repository Beans:"
grep -r "@Bean.*Repository" src/ | wc -l

echo "DataSource Beans:"
grep -r "@Bean.*DataSource" src/ | wc -l

# 4. 檢查自動配置排除
echo -e "\n自動配置排除檢查："
for props in application*.properties; do
    if grep -q "spring.autoconfigure.exclude" "$props"; then
        echo "  ✅ $props 有排除配置"
    else
        echo "  ⚠️  $props 沒有排除配置"
    fi
done

# 5. 檢查 JPA 設定
echo -e "\nJPA 設定檢查："
grep -h "spring.jpa.enabled" application*.properties 2>/dev/null || echo "  未明確設定 JPA 狀態"
```

## 📊 Profile 載入決策矩陣

| Profile | DataSource | JPA | Repository Type | MessageBus | Projection Type |
|---------|------------|-----|----------------|------------|-----------------|
| inmemory | ❌ 排除 | ❌ 停用 | GenericInMemoryRepository | ✅ 需要 | InMemoryProjection |
| outbox | ✅ 需要 | ✅ 啟用 | OutboxRepository | ❌ 不需要 | JpaProjection |
| test-inmemory | ❌ 排除 | ❌ 停用 | GenericInMemoryRepository | ✅ 需要 | InMemoryProjection |
| test-outbox | ✅ 需要 | ✅ 啟用 | OutboxRepository | ❌ 不需要 | JpaProjection |

## 🚀 最佳實踐

### 1. Configuration 類別組織
```
config/
├── common/
│   └── CommonConfiguration.java
├── inmemory/
│   └── InMemoryConfiguration.java
└── outbox/
    ├── OutboxInfrastructureConfiguration.java
    ├── OutboxOrmConfiguration.java
    └── OutboxRepositoryConfiguration.java
```

### 2. Profile 啟用規則
```java
// 使用複合條件確保正確載入
@Configuration
@Profile({"inmemory", "test-inmemory"})
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnProperty(
    prefix = "spring.jpa",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class InMemoryOnlyConfiguration {
    // 只在真正的 InMemory 環境載入
}
```

### 3. 測試 Profile 隔離
```java
// InMemory 測試
@SpringBootTest
@ActiveProfiles("test-inmemory")
public class InMemoryIntegrationTest {
    // 不應該有任何 DataSource 或 JPA 相關 Bean
}

// Outbox 測試
@SpringBootTest
@ActiveProfiles("test-outbox")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.jpa.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb"  // 使用 H2 測試
})
public class OutboxIntegrationTest {
    // 應該有完整的 Outbox 配置鏈
}
```

## ⚠️ 常見陷阱與解決

### 陷阱 1：Profile 繼承問題
```properties
# ❌ 錯誤：test-inmemory 會繼承 test 的配置
spring.profiles.active=test,test-inmemory

# ✅ 正確：只使用單一 profile
spring.profiles.active=test-inmemory
```

### 陷阱 2：Bean 名稱衝突
```java
// ❌ 錯誤：不同 Profile 使用相同 Bean 名稱可能衝突
@Bean
@Profile("inmemory")
public Repository repository() { }

@Bean
@Profile("outbox")
public Repository repository() { }

// ✅ 正確：使用明確的名稱或相同的方法簽名
@Bean
@Profile("inmemory")
public Repository<Product, ProductId> productRepository() { }

@Bean
@Profile("outbox")
public Repository<Product, ProductId> productRepository() { }
```

### 陷阱 3：隱式依賴
```java
// ❌ 錯誤：InMemory Configuration 意外依賴 JPA
@Configuration
@Profile("inmemory")
@EnableJpaRepositories  // 不應該在這裡！
public class InMemoryConfiguration { }

// ✅ 正確：完全隔離
@Configuration
@Profile("inmemory")
public class InMemoryConfiguration { 
    // 不包含任何 JPA 相關註解
}
```

## 📝 快速檢查清單

### InMemory Profile 檢查
- [ ] 已排除所有 DataSource 自動配置
- [ ] 已排除所有 JPA 自動配置
- [ ] 有專屬的 Configuration 類別
- [ ] 沒有 @EnableJpaRepositories
- [ ] 沒有 @EntityScan
- [ ] 有 MessageBus Bean
- [ ] 有 InMemoryRepository Bean

### Outbox Profile 檢查
- [ ] 有完整的 DataSource 配置
- [ ] 有 JPA 配置
- [ ] 有 @EnableJpaRepositories
- [ ] 有 @EntityScan
- [ ] 有 PgMessageDbClient Bean
- [ ] 有完整的 Outbox 依賴鏈
- [ ] 沒有 MessageBus Bean（不需要）

透過以上完整的解決方案，可以有效管理 Profile 配置的複雜性，避免衝突和錯誤。