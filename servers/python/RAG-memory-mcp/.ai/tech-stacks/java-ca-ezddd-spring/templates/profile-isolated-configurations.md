# Profile 隔離配置模板集 🏗️

## 目的
提供完整的 Java Configuration 類別模板，實現 Profile 完全隔離，避免配置衝突。

## 📁 建議的套件結構

```
src/main/java/tw/teddysoft/aiscrum/
└── config/
    ├── CommonConfiguration.java           # 所有 Profile 共用
    ├── inmemory/
    │   ├── InMemoryConfiguration.java    # InMemory 主配置
    │   └── InMemoryProjectionConfig.java # InMemory Projection
    └── outbox/
        ├── OutboxInfrastructureConfig.java  # 基礎設施
        ├── OutboxRepositoryConfig.java      # Repository 層
        └── OutboxProjectionConfig.java      # Projection 層
```

## 1️⃣ CommonConfiguration（所有 Profile 共用）

```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.usecase.*;
import tw.teddysoft.aiscrum.product.usecase.port.out.projection.ProductsProjection;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import java.util.Objects;

/**
 * 所有 Profile 共用的配置
 * 注意：不包含任何 @Profile 註解
 */
@Configuration
public class CommonConfiguration {
    
    // ========== Product UseCase Beans ==========
    
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> repository) {
        // Repository 由 Profile-specific 配置提供
        return new CreateProductService(Objects.requireNonNull(repository));
    }
    
    @Bean
    public GetProductsUseCase getProductsUseCase(
            ProductsProjection projection) {
        // Projection 由 Profile-specific 配置提供
        return new GetProductsService(Objects.requireNonNull(projection));
    }
    
    @Bean
    public GetProductUseCase getProductUseCase(
            Repository<Product, ProductId> repository) {
        return new GetProductService(Objects.requireNonNull(repository));
    }
    
    @Bean
    public DeleteProductUseCase deleteProductUseCase(
            Repository<Product, ProductId> repository) {
        return new DeleteProductService(Objects.requireNonNull(repository));
    }
    
    // 其他共用的 UseCase beans...
}
```

## 2️⃣ InMemory Profile 配置

### InMemoryConfiguration.java

```java
package tw.teddysoft.aiscrum.config.inmemory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.aiscrum.common.GenericInMemoryRepository;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageBroker;
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.ezddd.cqrs.usecase.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import javax.sql.DataSource;

/**
 * InMemory Profile 專用配置
 * 特點：
 * 1. 不需要資料庫
 * 2. 使用記憶體儲存
 * 3. 需要 MessageBus
 */
@Configuration
@Profile({"default", "inmemory", "test-inmemory"})
@ConditionalOnMissingBean(DataSource.class)  // 確保沒有 DataSource
@ConditionalOnProperty(
    prefix = "spring.jpa",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true  // 預設為 false
)
public class InMemoryConfiguration {
    
    // ========== 基礎設施 ==========
    
    @Bean
    public MessageBus messageBus() {
        MyInMemoryMessageBroker broker = new MyInMemoryMessageBroker();
        Thread brokerThread = new Thread(broker);
        brokerThread.setDaemon(true);
        brokerThread.start();
        return broker;
    }
    
    // ========== Product Aggregate ==========
    
    @Bean
    public Repository<Product, ProductId> productRepository(MessageBus messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    // ========== 其他 Aggregate Repositories ==========
    
    // @Bean
    // public Repository<Sprint, SprintId> sprintRepository(MessageBus messageBus) {
    //     return new GenericInMemoryRepository<>(messageBus);
    // }
}
```

### InMemoryProjectionConfig.java

```java
package tw.teddysoft.aiscrum.config.inmemory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection.InMemoryProductsProjection;
import tw.teddysoft.aiscrum.product.usecase.port.out.projection.ProductsProjection;

/**
 * InMemory Projection 配置
 */
@Configuration
@Profile({"default", "inmemory", "test-inmemory"})
public class InMemoryProjectionConfig {
    
    @Bean
    public ProductsProjection productsProjection() {
        return new InMemoryProductsProjection();
    }
    
    // 其他 Projection beans...
}
```

## 3️⃣ Outbox Profile 配置

### OutboxInfrastructureConfig.java

```java
package tw.teddysoft.aiscrum.config.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;

/**
 * Outbox 基礎設施配置
 * 優先載入（Order = 1）
 */
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@Order(1)
@ConditionalOnProperty(
    prefix = "spring.jpa",
    name = "enabled",
    havingValue = "true"
)
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.io.springboot.config.orm",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
public class OutboxInfrastructureConfig {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Bean
    public PgMessageDbClient pgMessageDbClient() {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
    
    // 如果需要自定義 EntityManagerFactory
    // @Bean
    // public LocalContainerEntityManagerFactoryBean entityManagerFactory(
    //         DataSource dataSource) {
    //     // 配置...
    // }
}
```

### OutboxRepositoryConfig.java

```java
package tw.teddysoft.aiscrum.config.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import tw.teddysoft.aiscrum.io.springboot.config.orm.ProductOrmClient;
import tw.teddysoft.aiscrum.product.entity.Product;
import tw.teddysoft.aiscrum.product.entity.ProductId;
import tw.teddysoft.aiscrum.product.usecase.port.ProductMapper;
import tw.teddysoft.aiscrum.product.usecase.port.out.ProductData;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeerAdapter;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxStoreAdapter;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;

/**
 * Outbox Repository 配置
 * 依賴基礎設施配置（Order = 2）
 */
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@Order(2)
@DependsOn("outboxInfrastructureConfig")
public class OutboxRepositoryConfig {
    
    // ========== Product Aggregate ==========
    
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
    
    // ========== 其他 Aggregate Repositories ==========
    
    // Sprint, PBI, ScrumTeam 等...
}
```

### OutboxProjectionConfig.java

```java
package tw.teddysoft.aiscrum.config.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection.JpaProductsProjection;
import tw.teddysoft.aiscrum.product.usecase.port.out.projection.ProductsProjection;

/**
 * Outbox Projection 配置
 */
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.product.adapter.out.database.springboot.projection"
    // 其他 projection packages...
})
public class OutboxProjectionConfig {
    
    @Bean
    public ProductsProjection productsProjection(
            JpaProductsProjection jpaProjection) {
        return jpaProjection;
    }
    
    // 其他 Projection beans...
}
```

## 4️⃣ 測試專用配置

### TestConfiguration.java

```java
package tw.teddysoft.aiscrum.config.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import static org.mockito.Mockito.mock;

/**
 * 測試專用配置
 * 可以覆蓋生產配置
 */
@TestConfiguration
@Profile({"test-inmemory", "test-outbox"})
public class TestSpecificConfiguration {
    
    // 可以提供 Mock beans 或測試專用實作
    
    // @Bean
    // @Primary
    // public SomeService mockService() {
    //     return mock(SomeService.class);
    // }
}
```

## 🔍 配置驗證工具

### ConfigurationValidator.java

```java
package tw.teddysoft.aiscrum.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 配置驗證工具
 * 在開發環境啟動時檢查配置正確性
 */
@Configuration
@Profile({"dev", "debug"})
public class ConfigurationValidator {
    
    @Bean
    public CommandLineRunner validateConfiguration(ApplicationContext ctx) {
        return args -> {
            System.out.println("=== Configuration Validation ===");
            
            // 檢查關鍵 Beans
            String[] profiles = ctx.getEnvironment().getActiveProfiles();
            System.out.println("Active Profiles: " + String.join(", ", profiles));
            
            // 檢查 Repository beans
            String[] repositoryBeans = ctx.getBeanNamesForType(Repository.class);
            System.out.println("Repository Beans: " + repositoryBeans.length);
            
            // 檢查 DataSource（Outbox only）
            try {
                ctx.getBean(javax.sql.DataSource.class);
                System.out.println("DataSource: ✅ Found");
            } catch (Exception e) {
                System.out.println("DataSource: ❌ Not found (OK for InMemory)");
            }
            
            // 檢查 MessageBus（InMemory only）
            try {
                ctx.getBean(MessageBus.class);
                System.out.println("MessageBus: ✅ Found");
            } catch (Exception e) {
                System.out.println("MessageBus: ❌ Not found (OK for Outbox)");
            }
            
            System.out.println("=== Validation Complete ===");
        };
    }
}
```

## ⚠️ 重要提醒

1. **套件隔離**：InMemory 和 Outbox 配置放在不同套件
2. **Profile 明確**：每個 Configuration 類別都要有明確的 @Profile
3. **條件載入**：使用 @ConditionalOn* 註解增加保護
4. **載入順序**：使用 @Order 和 @DependsOn 控制載入順序
5. **Bean 命名**：避免相同名稱的 Bean 在不同 Profile 中衝突

## 使用方式

1. 複製對應的模板到專案
2. 修改套件名稱和具體實作
3. 確保 application.properties 設定正確的 Profile
4. 執行驗證確保配置正確載入