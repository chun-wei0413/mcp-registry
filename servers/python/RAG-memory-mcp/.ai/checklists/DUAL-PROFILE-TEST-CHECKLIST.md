# Dual-Profile Test Checklist

## 🎯 Purpose
確保新增的 Use Case 能同時支援 InMemory 和 Outbox 兩種 profile，避免測試失敗。

## ✅ 新增 Use Case 時必須檢查

### 1. Repository Configuration
- [ ] **InMemoryRepositoryConfig** 有對應的 repository bean
  ```java
  @Bean("productInMemoryRepository")
  public Repository<Product, ProductId> productInMemoryRepository(MessageBus<DomainEvent> messageBus) {
      return new GenericInMemoryRepository<>(messageBus);
  }
  ```
  
- [ ] **OutboxRepositoryConfig** 有對應的 repository bean
  ```java
  @Bean
  @Primary
  public Repository<Product, ProductId> productRepository() {
      return new OutboxRepository<>(
          new OutboxRepositoryPeerAdapter<>(productOutboxStore()), 
          ProductMapper.newMapper()
      );
  }
  ```

- [ ] **UseCaseConfiguration** 不使用 @Qualifier
  ```java
  // ✅ CORRECT - Let Spring choose based on profile
  @Bean
  public CreateProductUseCase createProductUseCase(
      Repository<Product, ProductId> repository) {
      return new CreateProductService(repository);
  }
  
  // ❌ WRONG - Hardcoded qualifier breaks dual-profile
  @Bean
  public CreateProductUseCase createProductUseCase(
      @Qualifier("productInMemoryRepository") Repository<Product, ProductId> repository) {
      return new CreateProductService(repository);
  }
  ```

### 2. Outbox Infrastructure (for outbox profile)
- [ ] 建立 **OrmClient interface**
  ```java
  package tw.teddysoft.aiscrum.io.springboot.config.orm;
  
  public interface ProductOrmClient extends SpringJpaClient<ProductData, String> {
  }
  ```

- [ ] 建立 **Data 類別** (implements OutboxData)
  - [ ] 所有欄位都有 @Column annotation
  - [ ] **NO @Enumerated on String fields** ⚠️
  - [ ] Transient fields marked with @Transient
  ```java
  @Entity
  @Table(name = "products")
  public class ProductData implements OutboxData<String> {
      @Column(name = "state", nullable = false)
      private String state;  // ✅ String, not enum
      
      @Transient
      private List<DomainEventData> domainEventDatas;
  }
  ```

- [ ] 建立 **Mapper 類別** with inner OutboxMapper
  ```java
  public class ProductMapper {
      static class Mapper implements OutboxMapper<Product, ProductData> {
          // Implementation
      }
  }
  ```

- [ ] **JpaConfiguration** includes OrmClient package
  ```java
  @EnableJpaRepositories(basePackages = {
      "tw.teddysoft.aiscrum.io.springboot.config.orm"
  })
  ```

### 3. Test Suite Structure
- [ ] **InMemoryTestSuite** 正確設定
  ```java
  @Suite
  @SelectClasses({
      InMemoryTestSuite.ProfileSetter.class,  // MUST be first!
      CreateProductUseCaseTest.class
  })
  public class InMemoryTestSuite {
      public static class ProfileSetter {
          static {
              System.setProperty("spring.profiles.active", "test-inmemory");
          }
          @Test void setProfile() { }  // Required
      }
  }
  ```

- [ ] **OutboxTestSuite** 正確設定
  ```java
  @Suite
  @SelectClasses({
      OutboxTestSuite.ProfileSetter.class,  // MUST be first!
      CreateProductUseCaseTest.class
  })
  public class OutboxTestSuite {
      public static class ProfileSetter {
          static {
              System.setProperty("spring.profiles.active", "test-outbox");
          }
          @Test void setProfile() { }  // Required
      }
  }
  ```

- [ ] **ProfileSetter 規則**
  - [ ] 不能有 @SpringBootTest
  - [ ] 不能有 @TestInstance
  - [ ] 必須在 static block 設定 profile
  - [ ] 必須有至少一個 @Test method
  - [ ] 必須是 @SelectClasses 的第一個

### 4. Test Implementation
- [ ] 測試類別繼承 **BaseUseCaseTest**
- [ ] 使用 @Value 取得 active profile
- [ ] Profile-aware event assertions
  ```java
  @Value("${spring.profiles.active:test-inmemory}")
  private String activeProfile;
  
  .And("events should be handled according to profile", env -> {
      if (activeProfile.contains("inmemory")) {
          // Verify events are published immediately
          await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
              // Assert events
          });
      } else if (activeProfile.contains("outbox")) {
          // Events stored in DB, not published immediately
          // Just verify aggregate was saved
      }
  })
  ```

### 5. Common Issues to Check
- [ ] ❌ 避免在 BaseUseCaseTest 使用 @ActiveProfiles
- [ ] ❌ 避免在測試類別硬編碼 profile
- [ ] ❌ 避免使用 TestContext 而非 Spring DI
- [ ] ❌ 避免在 Suite class 的 static block（不會執行）
- [ ] ❌ 避免忘記 @Primary on repository beans

## 🔧 驗證腳本

執行以下腳本驗證配置：
```bash
# 檢查 Data 類別註解
.ai/scripts/check-data-class-annotations.sh

# 驗證雙 profile 配置
.ai/scripts/validate-dual-profile-config.sh

# 執行雙 profile 測試
mvn test -Dtest=InMemory*TestSuite
mvn test -Dtest=Outbox*TestSuite
```

## 📋 Quick Fix Guide

### 問題：NoSuchBeanDefinitionException
**原因**：Repository bean 在該 profile 不存在
**解決**：
1. 檢查 InMemoryRepositoryConfig 和 OutboxRepositoryConfig
2. 確保兩個都有對應的 repository bean
3. 確保 @Profile annotation 正確

### 問題：@Enumerated on String field error
**原因**：Data 類別錯誤使用 @Enumerated
**解決**：
1. 移除 @Enumerated annotation
2. 保留 @Column annotation
3. 確保 enum 轉換為 String

### 問題：ApplicationContext threshold exceeded
**原因**：ProfileSetter 有 @SpringBootTest
**解決**：
1. 移除 @SpringBootTest
2. 移除 @TestInstance
3. 只保留 static block 和空的 @Test method

## 📚 Reference Documents
- `.ai/guides/DUAL-PROFILE-CONFIGURATION-GUIDE.md`
- `.ai/guides/DATA-CLASS-STANDARDS.md`
- `.ai/guides/DUAL-PROFILE-TESTING-GUIDE.md`
- `.dev/adr/ADR-021-profile-based-testing-architecture.md`