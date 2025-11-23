# 防止 Spring Boot 啟動失敗完整指南 🔴

## 問題描述

當 AI 執行新專案初始化任務時，經常會遇到兩個致命錯誤：

### 錯誤 1：Repository Bean 缺失
```
Parameter 0 of method createProductUseCase required a bean of type
'tw.teddysoft.ezddd.usecase.port.out.repository.Repository' that could not be found.
```

### 錯誤 2：DataSource 配置問題
```
Failed to configure a DataSource: 'url' attribute is not specified
and no embedded datasource could be configured.
```

## 根本原因分析

### 1. Repository Bean 問題
- 使用 `outbox` profile 時需要完整的 Outbox 配置鏈
- 缺少任何一個環節都會導致 Repository bean 無法創建

### 2. DataSource 自動配置問題
- Spring Boot 預設會嘗試配置 DataSource
- InMemory profile 不需要資料庫，但 JPA 自動配置仍被觸發
- 沒有正確排除自動配置會導致啟動失敗

### 3. 依賴鏈分析
```
CreateProductUseCase
    └── Repository<Product, ProductId>
        └── OutboxRepository
            └── OutboxRepositoryPeerAdapter
                └── OutboxStore
                    └── EzOutboxClient
                        ├── ProductOrmClient
                        └── PgMessageDbClient
```

## 問題 3：Profile 配置複雜性

### 具體挑戰
1. **條件化 Bean 載入**：不同 profile 需要不同的 Repository 實作
2. **JPA 配置衝突**：inmemory 不需要 JPA，但框架仍嘗試初始化
3. **依賴注入複雜度**：Outbox 模式需要多層依賴注入

### 解決方案
- 查看 [Profile 配置複雜性完整解決方案](.ai/guides/PROFILE-CONFIGURATION-COMPLEXITY-SOLUTION.md)
- 使用 [Profile 隔離配置模板](.ai/tech-stacks/java-ca-ezddd-spring/templates/profile-isolated-configurations.md)

## 🛡️ 防護機制

### Step 1: 前置檢查清單

在執行任何新專案初始化前，AI 必須確認：

#### 1.1 檢查 Profile 策略
```bash
# 檢查要使用的 profile
echo "計劃使用的 Profile: [inmemory/outbox]"
```

#### 1.2 如果選擇 Outbox Profile
必須確認以下檔案都會被創建：
- [ ] `ProductData.java` (實作 OutboxData)
- [ ] `ProductMapper.java` (包含 OutboxMapper 內部類)
- [ ] `ProductOrmClient.java` (extends SpringJpaClient)
- [ ] `OutboxRepositoryConfig.java` (配置所有 beans)
- [ ] `OutboxInfrastructureConfig.java` (配置 PgMessageDbClient)

### Step 2: 漸進式實作策略 🎯

#### 選項 A: 先用 InMemory，後加 Outbox（推薦）
```java
// 第一階段：快速啟動
@Configuration
@Profile("inmemory")
public class InMemoryRepositoryConfig {
    @Bean
    public Repository<Product, ProductId> productRepository(MessageBus messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
}
```

#### 選項 B: 直接實作 Outbox（需要完整配置）
如果選擇直接實作 Outbox，必須同時創建所有必要元件。

### Step 3: 必要的配置檔案模板

#### 3.0 application.properties 配置（解決 DataSource 問題）
```properties
# application.properties - 預設配置
spring.profiles.active=inmemory

# application-inmemory.properties - InMemory Profile
# 關鍵：排除 DataSource 和 JPA 自動配置
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration

# application-outbox.properties - Outbox Profile  
# 需要資料庫配置
spring.datasource.url=jdbc:postgresql://localhost:5432/aiscrum
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### Step 3: 必要的 Java 配置模板

#### 3.1 UseCaseInjection.java（簡化版，先支援 InMemory）
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.aiscrum.common.GenericInMemoryRepository;
import tw.teddysoft.aiscrum.product.entity.*;
import tw.teddysoft.aiscrum.product.usecase.*;
import tw.teddysoft.ezddd.cqrs.usecase.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

@Configuration
public class UseCaseInjection {
    
    // 先提供 InMemory 版本，確保應用可以啟動
    @Bean
    @Profile({"default", "inmemory", "test-inmemory"})
    public Repository<Product, ProductId> productRepositoryInMemory(MessageBus messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    // UseCase beans
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> repository) {
        return new CreateProductService(repository);
    }
    
    @Bean
    public GetProductsUseCase getProductsUseCase(
            ProductsProjection projection) {
        return new GetProductsService(projection);
    }
}
```

#### 3.2 完整的 Outbox 配置（第二階段）
```java
@Configuration
@Profile({"outbox", "test-outbox", "prod-outbox"})
public class OutboxRepositoryConfig {
    
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
}
```

### Step 4: 驗證清單 ✅

執行以下命令驗證配置正確性：

```bash
# 1. 編譯測試
mvn clean compile

# 2. 檢查 Spring 容器是否可以啟動
mvn spring-boot:run -Dspring.profiles.active=inmemory

# 3. 檢查 Bean 是否正確註冊
mvn test -Dtest=ApplicationContextTest
```

### Step 5: 錯誤診斷流程

如果遇到 Repository Bean 缺失錯誤：

1. **檢查 Active Profile**
   ```bash
   grep "spring.profiles.active" src/main/resources/application.yml
   ```

2. **檢查 Repository Bean 定義**
   ```bash
   grep -r "@Bean.*Repository" src/
   ```

3. **檢查依賴完整性**
   - 如果是 Outbox：檢查 OrmClient、PgMessageDbClient、OutboxStore 是否都有定義
   - 如果是 InMemory：檢查 MessageBus 是否有定義

## 🚨 關鍵提醒

### DO ✅
1. **永遠從簡單開始**：先用 InMemory Repository 確保應用可以啟動
2. **漸進式增強**：應用啟動後再逐步加入 Outbox 支援
3. **Profile 隔離**：不同 Profile 使用不同的 Repository 實作
4. **完整性檢查**：Outbox 需要完整的配置鏈，缺一不可

### DON'T ❌
1. **不要跳過 InMemory 階段**：除非你確定所有 Outbox 配置都正確
2. **不要混合 Profile**：避免在同一個 Configuration 類別中定義不同 Profile 的 beans
3. **不要忽略錯誤訊息**：Bean not found 通常意味著配置鏈某處斷裂

## 實作順序建議 📋

1. **Phase 1: 基礎設施**
   - 創建 4 個共用類別（DateProvider, GenericInMemoryRepository, MyInMemoryMessageBroker, MyInMemoryMessageProducer）
   - 創建基本的 Spring Boot 主程式

2. **Phase 2: Domain 模型**
   - 創建 Entity 和 Value Objects
   - 實作基本的 domain 邏輯

3. **Phase 3: InMemory 實作**
   - 創建 UseCase Service 類別
   - 配置 InMemory Repository
   - 確保應用可以啟動

4. **Phase 4: Outbox 增強**（可選）
   - 創建 Data、Mapper、OrmClient
   - 配置完整的 Outbox 鏈
   - 切換到 Outbox Profile 測試

## 參考資源
- [完整 Spring Boot 配置指南](.ai/tech-stacks/java-ca-ezddd-spring/COMPLETE-SPRING-BOOT-SETUP-GUIDE.md)
- [Outbox Pattern 實作指南](.ai/prompts/outbox-sub-agent-prompt.md)
- [常見錯誤案例](.ai/COMMON-PITFALLS.md)