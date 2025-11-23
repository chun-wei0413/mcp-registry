# Profile Configuration Sub-agent 使用範例

## 📋 使用場景

### 場景 1：新專案初始化
當你從零開始建立新專案，需要配置 Spring Profiles。

### 場景 2：解決 Bean Not Found 錯誤
當你遇到 `Repository bean not found` 錯誤時。

### 場景 3：解決 DataSource 配置錯誤
當你遇到 `Failed to configure a DataSource` 錯誤時。

## 🔧 使用方式

### 基本用法
```
請使用 profile-config-sub-agent workflow 配置 Spring Profiles
```

### 指定特定需求
```
請使用 profile-config-sub-agent workflow 為 Product 和 Sprint aggregates 配置 InMemory 和 Outbox profiles
```

### 修復錯誤
```
我遇到 Repository bean not found 錯誤，請使用 profile-config-sub-agent workflow 修復
```

## 📝 實際範例

### 輸入
```
請使用 profile-config-sub-agent workflow 配置 Spring Profiles，專案包含 Product aggregate
```

### 預期輸出

#### 1. Properties 檔案

**application.properties**
```properties
spring.profiles.active=inmemory
spring.application.name=ai-scrum
```

**application-inmemory.properties**
```properties
# 排除 DataSource 和 JPA 自動配置
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

spring.jpa.enabled=false
```

**application-outbox.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/aiscrum
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

messagestore.postgres.url=${spring.datasource.url}
messagestore.postgres.user=${spring.datasource.username}
messagestore.postgres.password=${spring.datasource.password}
```

#### 2. Configuration 類別

**CommonConfiguration.java**
```java
package tw.teddysoft.aiscrum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tw.teddysoft.aiscrum.product.entity.*;
import tw.teddysoft.aiscrum.product.usecase.*;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import java.util.Objects;

@Configuration
public class CommonConfiguration {
    
    @Bean
    public CreateProductUseCase createProductUseCase(
            Repository<Product, ProductId> repository) {
        return new CreateProductService(Objects.requireNonNull(repository));
    }
    
    @Bean
    public GetProductsUseCase getProductsUseCase(
            ProductsProjection projection) {
        return new GetProductsService(Objects.requireNonNull(projection));
    }
}
```

**InMemoryConfiguration.java**
```java
package tw.teddysoft.aiscrum.config.inmemory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tw.teddysoft.aiscrum.common.GenericInMemoryRepository;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageBroker;
import tw.teddysoft.aiscrum.product.entity.*;
import tw.teddysoft.ezddd.cqrs.usecase.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import javax.sql.DataSource;

@Configuration
@Profile({"default", "inmemory", "test-inmemory"})
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryConfiguration {
    
    @Bean
    public MessageBus messageBus() {
        MyInMemoryMessageBroker broker = new MyInMemoryMessageBroker();
        Thread brokerThread = new Thread(broker);
        brokerThread.setDaemon(true);
        brokerThread.start();
        return broker;
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
```

#### 3. 驗證腳本

**validate-profile.sh**
```bash
#!/bin/bash

echo "=== Profile Configuration Validation ==="

# Check InMemory profile
echo "Testing InMemory profile..."
mvn spring-boot:run -Dspring.profiles.active=inmemory &
PID=$!
sleep 10

if ps -p $PID > /dev/null; then
    echo "✅ InMemory profile started successfully"
    kill $PID
else
    echo "❌ InMemory profile failed to start"
fi

# Check for DataSource exclusion
if grep -q "spring.autoconfigure.exclude" application-inmemory.properties; then
    echo "✅ DataSource autoconfiguration is excluded"
else
    echo "❌ DataSource autoconfiguration is NOT excluded"
fi

echo "=== Validation Complete ==="
```

## 🎯 成功標準

使用 profile-config-sub-agent 後，應該達到：

1. ✅ **InMemory Profile 可以啟動**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=inmemory
   # 應該成功啟動，無錯誤
   ```

2. ✅ **無 Bean Not Found 錯誤**
   - 所有 UseCase 都能找到對應的 Repository

3. ✅ **無 DataSource 錯誤**
   - InMemory profile 不會嘗試連接資料庫

4. ✅ **Profile 完全隔離**
   - InMemory 和 Outbox 配置不會互相干擾

## ⚠️ 常見問題

### Q1: Sub-agent 沒有正確排除 DataSource？
**A**: 確保 sub-agent 讀取了必要的參考文檔，特別是 `PREVENT-REPOSITORY-BEAN-MISSING.md`

### Q2: 產生的配置不完整？
**A**: 明確告訴 sub-agent 你的專案包含哪些 Aggregates

### Q3: Outbox 配置太複雜？
**A**: 可以先只要求 InMemory 配置：
```
請使用 profile-config-sub-agent workflow 只配置 InMemory profile
```

## 🔗 相關資源

- [Profile Configuration Sub-agent Prompt](.ai/prompts/profile-config-sub-agent-prompt.md)
- [防止 Spring Boot 啟動失敗指南](.ai/guides/PREVENT-REPOSITORY-BEAN-MISSING.md)
- [Profile 配置複雜性解決方案](.ai/guides/PROFILE-CONFIGURATION-COMPLEXITY-SOLUTION.md)
- [Application Properties 模板](.ai/tech-stacks/java-ca-ezddd-spring/templates/application-properties-templates.md)