# Spring Boot Application Properties 模板集 🔧

## 🎯 Purpose
提供完整的 application.properties 配置模板，避免 DataSource 和 Repository Bean 配置問題。

## 📋 配置檔案結構

```
src/main/resources/
├── application.properties              # 主配置
├── application-inmemory.properties     # InMemory Profile
├── application-outbox.properties       # Outbox Profile  
├── application-test.properties         # 測試配置
├── application-test-inmemory.properties # InMemory 測試
└── application-test-outbox.properties  # Outbox 測試
```

## 1️⃣ application.properties（主配置）

```properties
# ========================================
# 主配置檔案 - 預設使用 InMemory Profile
# ========================================

# Profile 設定 - 預設使用 inmemory
spring.profiles.active=inmemory

# 應用程式基本設定
spring.application.name=ai-scrum
server.port=8080

# Jackson 設定
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.serialization.indent-output=true

# 日誌設定
logging.level.root=INFO
logging.level.tw.teddysoft.aiscrum=DEBUG
logging.level.org.springframework.web=DEBUG
```

## 2️⃣ application-inmemory.properties（InMemory Profile）

```properties
# ========================================
# InMemory Profile - 不使用資料庫
# ========================================

# 🔴 關鍵配置：排除所有資料庫相關的自動配置
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# 日誌設定
logging.level.tw.teddysoft.aiscrum=DEBUG
logging.level.tw.teddysoft.ezddd=DEBUG

# 停用 JPA 相關功能
spring.jpa.enabled=false
```

## 3️⃣ application-outbox.properties（Outbox Profile）

```properties
# ========================================
# Outbox Profile - 使用 PostgreSQL + Outbox Pattern
# ========================================

# 資料庫連線設定
spring.datasource.url=jdbc:postgresql://localhost:5432/aiscrum?currentSchema=public
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# JPA 設定
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false

# Entity 掃描路徑
spring.jpa.packages-to-scan=\
  tw.teddysoft.aiscrum,\
  tw.teddysoft.ezddd.data.io.ezes.store

# Message Store 設定（Outbox Pattern）
messagestore.postgres.url=${spring.datasource.url}
messagestore.postgres.user=${spring.datasource.username}
messagestore.postgres.password=${spring.datasource.password}

# Outbox 輪詢設定
outbox.polling.interval=5000
outbox.polling.batch-size=100
```

## 4️⃣ application-test.properties（測試主配置）

```properties
# ========================================
# 測試環境主配置
# ========================================

# 預設使用 test-inmemory profile
spring.profiles.active=test-inmemory

# 測試環境設定
spring.main.allow-bean-definition-overriding=true
logging.level.root=WARN
logging.level.tw.teddysoft.aiscrum=DEBUG
```

## 5️⃣ application-test-inmemory.properties（InMemory 測試）

```properties
# ========================================
# InMemory 測試環境 - 不使用資料庫
# ========================================

# 排除資料庫自動配置
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration

# 測試設定
spring.test.mockmvc.print=true
```

## 6️⃣ application-test-outbox.properties（Outbox 測試）

```properties
# ========================================
# Outbox 測試環境 - 使用測試資料庫
# ========================================

# 測試資料庫設定（使用不同 port 避免衝突）
spring.datasource.url=jdbc:postgresql://localhost:5800/testdb?currentSchema=message_store
spring.datasource.username=postgres
spring.datasource.password=root
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA 測試設定
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Message Store 測試設定
messagestore.postgres.url=${spring.datasource.url}
messagestore.postgres.user=${spring.datasource.username}
messagestore.postgres.password=${spring.datasource.password}

# 測試交易設定
spring.test.database.replace=none
```

## 🚨 關鍵配置解釋

### 1. 為什麼需要 spring.autoconfigure.exclude？

InMemory 模式不需要資料庫，但 Spring Boot 看到 classpath 有 JPA 依賴就會自動配置 DataSource。
必須明確排除這些自動配置：

```properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

### 2. Profile 命名規範

- `inmemory` - 開發環境，使用記憶體儲存
- `outbox` - 生產環境，使用 Outbox Pattern
- `test-inmemory` - 測試環境，記憶體模式
- `test-outbox` - 測試環境，Outbox 模式

### 3. 資料庫 Port 分離

- 開發環境：5432（預設 PostgreSQL port）
- 測試環境：5800（避免衝突）

## 🔍 診斷命令

```bash
# 檢查 active profile
mvn spring-boot:run -Dspring-boot.run.arguments=--debug | grep "Active profile"

# 測試 InMemory 模式（應該成功啟動）
mvn spring-boot:run -Dspring.profiles.active=inmemory

# 測試 Outbox 模式（需要資料庫）
mvn spring-boot:run -Dspring.profiles.active=outbox

# 查看自動配置報告
mvn spring-boot:run -Ddebug=true | grep "Exclusions"
```

## ⚠️ 常見錯誤與解決

### 錯誤 1：Failed to configure a DataSource

**原因**：InMemory profile 沒有正確排除 DataSource 自動配置

**解決**：確認 `application-inmemory.properties` 包含：
```properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### 錯誤 2：No qualifying bean of type 'Repository'

**原因**：Profile 對應的 Repository Bean 沒有定義

**解決**：確認 Java 配置中有對應的 `@Profile` 註解：
```java
@Bean
@Profile({"inmemory", "default"})
public Repository<Product, ProductId> productRepository() {
    // InMemory 實作
}
```

### 錯誤 3：Entity 類別找不到

**原因**：Outbox profile 下 JPA 沒有掃描到 Entity 類別

**解決**：在 `application-outbox.properties` 加入：
```properties
spring.jpa.packages-to-scan=tw.teddysoft.aiscrum
```

## 📝 最佳實踐

1. **永遠從 InMemory 開始開發**
2. **確保每個 Profile 都有完整配置**
3. **使用不同的資料庫 Port 區分環境**
4. **測試時明確指定 Profile**
5. **Production 環境使用環境變數覆蓋敏感資訊**

## 參考連結
- [Spring Boot Properties 文檔](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Spring Profiles 指南](https://www.baeldung.com/spring-profiles)