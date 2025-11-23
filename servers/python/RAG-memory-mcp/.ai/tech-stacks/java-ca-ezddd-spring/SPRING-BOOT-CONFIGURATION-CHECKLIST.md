# Spring Boot 配置檢查清單 🔥

## ⚠️ 必須避免的常見錯誤

這份清單記錄了在實驗 V14 中發生的所有 Spring Boot 配置錯誤，確保下次不再重複。

## 1. 資料庫連線配置

### ❌ 錯誤：使用錯誤的 port
```yaml
# 錯誤
url: jdbc:postgresql://localhost:5500/board  # 5500 不存在

# 正確
url: jdbc:postgresql://localhost:5432/board  # PostgreSQL 預設 port
url: jdbc:postgresql://localhost:6600/board  # 或 Docker mapped port
```

### ❌ 錯誤：schema 配置方式錯誤
```yaml
# 錯誤 - 單獨的 schema 欄位
message:
  store:
    schema: message_store

# 正確 - 在 URL 中指定
url: jdbc:postgresql://localhost:5432/board?currentSchema=message_store
```

## 2. Maven 依賴配置

### ❌ 錯誤：ByteBuddy 只在 test scope
```xml
<!-- 錯誤 -->
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy</artifactId>
    <version>${byte-buddy.version}</version>
    <scope>test</scope>  <!-- Hibernate 運行時需要！ -->
</dependency>

<!-- 正確 -->
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy</artifactId>
    <version>${byte-buddy.version}</version>
    <!-- 不要加 scope，讓它在 runtime 可用 -->
</dependency>
```

### ❌ 錯誤：缺少 Jakarta Persistence API
```xml
<!-- 必須明確加入（Spring Boot 3.x）-->
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
</dependency>
```

## 3. JPA Entity 配置

### ❌ 錯誤：String 欄位使用 @Enumerated
```java
// 錯誤
@Column(name = "state")
@Enumerated(EnumType.STRING)  // String 不能用 @Enumerated！
private String state;

// 正確
@Column(name = "state")
private String state;
```

### ❌ 錯誤：忘記掃描框架的 Entity
```java
// 錯誤 - 只掃描自己的套件
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum"
})

// 正確 - 包含 ezddd 框架的 entity
@EntityScan(basePackages = {
    "tw.teddysoft.aiscrum",
    "tw.teddysoft.ezddd.data.io.ezes.store"  // MessageData entity
})
```

## 4. Spring Bean 配置

### ❌ 錯誤：Repository 只在 test profile
```java
// 錯誤 - 忘記 prod-outbox
@Bean
@Profile("test-outbox")
public Repository<Product, ProductId> productRepository(...) {
    // ...
}

// 正確 - 包含所有需要的 profiles
@Bean
@Profile({"test-outbox", "prod-outbox"})
public Repository<Product, ProductId> productRepository(...) {
    // ...
}
```

### ❌ 錯誤：Bean 重複定義
```java
// 錯誤 - 手動定義已經被 @EnableJpaRepositories 掃描的 Bean
@Bean
public ProductOrmClient productOrmClient(...) {
    // Spring Data JPA 已經自動創建了！
}

// 正確 - 讓 Spring Data JPA 自動處理
@Repository  // 只要加註解
public interface ProductOrmClient extends SpringJpaClient<ProductData, String> {
}
```

## 5. Application Properties 配置

### ❌ 錯誤：Server port 衝突
```yaml
# 檢查 Docker 或其他服務是否已使用該 port
server:
  port: 6600  # 確認 port 未被佔用

# 使用前先檢查
# lsof -i :6600
```

## 6. Profile 配置策略

### ✅ 正確的 Profile 架構
```yaml
# application.yml - 預設使用生產 profile
spring:
  profiles:
    active: prod-outbox

# application-prod-outbox.yml - 生產環境
server:
  port: 8080  # 或其他未被佔用的 port

# application-test-inmemory.yml - 測試環境（記憶體）
test:
  repository:
    type: inmemory

# application-test-outbox.yml - 測試環境（資料庫）  
test:
  repository:
    type: outbox
```

## 7. 必要的配置檢查腳本

創建一個驗證腳本 `.ai/scripts/check-spring-config.sh`：

```bash
#!/bin/bash

echo "🔍 檢查 Spring Boot 配置..."

# 檢查 ByteBuddy scope
if grep -q "<scope>test</scope>" pom.xml | grep -A2 -B2 "byte-buddy"; then
    echo "❌ ByteBuddy 不應該只在 test scope"
fi

# 檢查 @Enumerated on String
if grep -q "@Enumerated.*String" src/main/java/**/*.java; then
    echo "❌ String 欄位不能使用 @Enumerated"
fi

# 檢查 EntityScan
if ! grep -q "tw.teddysoft.ezddd" src/main/java/**/JpaConfiguration.java; then
    echo "⚠️ 可能缺少 ezddd entity 掃描"
fi

# 檢查 Profile 配置
if ! grep -q "prod-outbox" src/main/java/**/UseCaseConfiguration.java; then
    echo "⚠️ 可能缺少 prod-outbox profile 支援"
fi
```

## 8. 初始化專案時的必做事項

1. **先執行 port 檢查**
   ```bash
   lsof -i :6600
   lsof -i :8080
   lsof -i :5432
   ```

2. **確認 PostgreSQL 連線**
   ```bash
   psql -h localhost -p 5432 -U postgres -d board -c "SELECT 1"
   ```

3. **驗證 Maven 依賴**
   ```bash
   mvn dependency:tree | grep byte-buddy
   mvn dependency:tree | grep jakarta
   ```

## 9. 除錯指令

當啟動失敗時，按順序執行：

```bash
# 1. 清理並重新編譯
mvn clean compile

# 2. 檢查編譯錯誤
mvn compile -X 2>&1 | grep ERROR

# 3. 執行單元測試（不啟動伺服器）
mvn test -Dtest=SimpleCreateProductUseCaseTest

# 4. 使用 test profile 啟動（較簡單）
mvn spring-boot:run -Dspring.profiles.active=test-inmemory

# 5. 檢查 Bean 創建問題
mvn spring-boot:run -Dlogging.level.org.springframework.beans=DEBUG
```

## 10. 永遠記住的原則

1. **Profile 一致性**：所有相關的 Bean 必須支援相同的 profiles
2. **依賴完整性**：Runtime 需要的依賴不能只放在 test scope
3. **Entity 掃描**：包含所有需要的套件，包括框架的
4. **Port 管理**：使用前先檢查，避免衝突
5. **Schema 配置**：優先在 URL 中指定，不用單獨欄位

---

**重要**：這份清單應該在每次創建新專案時先閱讀一遍，避免重複錯誤！