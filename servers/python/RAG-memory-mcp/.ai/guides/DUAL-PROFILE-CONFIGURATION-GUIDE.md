# Dual-Profile Configuration Guide

## 概述

本指南說明如何正確設定 Spring Boot 應用程式的雙 Profile 架構，支援 InMemory 和 Outbox 兩種模式。

## 問題背景

### 常見錯誤
❌ **單一 application.properties 包含所有配置**
- 導致 Profile 切換時配置衝突
- DataSource 在 InMemory 模式下仍嘗試連接資料庫
- 測試無法正確切換 Profile

❌ **Sub-agent 誤解 "inmemory" 含義**
- 產生 H2 相關程式碼
- 應該使用 GenericInMemoryRepository，不是任何資料庫

## 正確的配置檔結構

### 1. application.properties（共用設定）
```properties
# Common configuration for all profiles
spring.profiles.active=${SPRING_PROFILES_ACTIVE:outbox}

# Server Configuration
server.port=9090

# Application Configuration  
spring.application.name=AI-SCRUM

# Logging Configuration
logging.level.root=INFO
logging.level.tw.teddysoft.aiscrum=INFO

# Jackson Configuration
spring.jackson.serialization.indent-output=true

# Error handling
server.error.include-message=always
server.error.include-binding-errors=always
```

### 2. application-inmemory.properties
```properties
# InMemory Profile 專用設定
# 重要：不需要任何資料庫配置！

# 禁用 JPA 和 DataSource 自動配置
spring.jpa.enabled=false
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
```

### 3. application-outbox.properties
```properties
# Outbox Profile 專用設定
spring.datasource.url=jdbc:postgresql://localhost:5500/board?currentSchema=message_store
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.event.merge.entity_copy_observer=allow
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

### 4. application-test-inmemory.properties
```properties
# Test InMemory Profile
spring.jpa.enabled=false
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration

logging.level.root=WARN
logging.level.tw.teddysoft.aiscrum=INFO
```

### 5. application-test-outbox.properties
```properties
# Test Outbox Profile - 使用測試資料庫
spring.datasource.url=jdbc:postgresql://localhost:5800/board_test?currentSchema=message_store
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Test Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

logging.level.root=WARN
```

## Profile 對應的 Spring Configuration

### InMemory Profile Configuration
```java
@Configuration
@Profile({"inmemory", "test-inmemory"})
public class InMemoryRepositoryConfig {
    
    @Bean("productRepository")
    public Repository<Product, ProductId> productRepository(
        MessageBus<DomainEvent> messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
}
```

### Outbox Profile Configuration
```java
@Configuration
@Profile({"outbox", "test-outbox"})
@EnableJpaRepositories(basePackages = {
    "tw.teddysoft.aiscrum.io.springboot.config.orm",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum.product.usecase.port.out",
    "tw.teddysoft.ezddd.data.io.ezes.store"
})
public class JpaConfiguration {
    // JPA and OutboxRepository configurations
}
```

## 測試配置

### BaseUseCaseTest
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseUseCaseTest {
    // 不要使用 @ActiveProfiles - 讓 Profile 動態決定
}
```

### application-test.yml
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:test-inmemory}
```

## 驗證步驟

### 1. 驗證 InMemory Profile
```bash
SPRING_PROFILES_ACTIVE=inmemory mvn spring-boot:run
# 應該：
# - 成功啟動在 port 9090
# - 不嘗試連接任何資料庫
# - 日誌中沒有 DataSource 相關訊息
```

### 2. 驗證 Outbox Profile
```bash  
SPRING_PROFILES_ACTIVE=outbox mvn spring-boot:run
# 應該：
# - 成功啟動在 port 9090
# - 連接 PostgreSQL localhost:5500
# - 日誌中顯示 HikariCP 連接池資訊
```

### 3. 驗證測試執行
```bash
# InMemory 測試
SPRING_PROFILES_ACTIVE=test-inmemory mvn test -Dtest=CreateProductServiceTest

# Outbox 測試  
SPRING_PROFILES_ACTIVE=test-outbox mvn test -Dtest=CreateProductServiceTest
```

## 常見問題與解決方案

### Q1: 測試仍嘗試連接資料庫
**原因**: `spring.autoconfigure.exclude` 設定不完整
**解決**: 確保排除所有資料庫相關的自動配置類別

### Q2: Sub-agent 產生 H2 程式碼
**原因**: Sub-agent 誤解 "inmemory" 含義
**解決**: 手動檢查並移除所有 H2 相關程式碼，強調使用 GenericInMemoryRepository

### Q3: Profile 切換不生效
**原因**: application.properties 中有硬編碼的 profile 設定
**解決**: 使用環境變數語法 `${SPRING_PROFILES_ACTIVE:default}`

### Q4: Bean 找不到或重複定義
**原因**: 兩個 Profile 的 Bean 名稱衝突
**解決**: 使用不同的 Bean 名稱或 @Primary 註解

## Sub-agent 使用注意事項

當使用 command-sub-agent 或其他 sub-agent 時：

1. **明確指示 Profile 含義**：
   - inmemory = GenericInMemoryRepository（純 Java 記憶體）
   - outbox = PostgreSQL + OutboxRepository

2. **檢查產生的程式碼**：
   - 不應有 H2 相關 dependency 或配置
   - Repository 配置應該按 Profile 分離

3. **測試配置檢查**：
   - 確保 BaseUseCaseTest 不含 @ActiveProfiles
   - 測試應支援兩種 Profile 切換

這個配置架構確保了專案可以在不同環境下靈活切換，同時避免了配置衝突的問題。