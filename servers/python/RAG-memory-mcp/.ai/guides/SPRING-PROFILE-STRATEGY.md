# Spring Profile 策略完整指南 🎯

## 🔴 核心原則：InMemory First, Outbox Later

### 為什麼這個策略如此重要？
1. **快速啟動**：InMemory 不需要任何外部依賴
2. **避免配置地獄**：Outbox 需要完整的配置鏈
3. **漸進式開發**：先確保業務邏輯正確，再加入持久化

## 📊 Profile 架構圖

```
┌─────────────────────────────────────────────┐
│           Application Profiles              │
├─────────────────────────────────────────────┤
│                                             │
│  開發階段（Development）                     │
│  ├── default (= inmemory)                  │
│  ├── inmemory ✅ (推薦起始點)               │
│  └── outbox   ⚠️  (需要完整配置)           │
│                                             │
│  測試階段（Testing）                         │
│  ├── test-inmemory ✅ (單元測試)           │
│  └── test-outbox   ⚠️  (整合測試)         │
│                                             │
│  生產階段（Production）                      │
│  └── prod-outbox ⚠️ (需要完整配置)         │
│                                             │
└─────────────────────────────────────────────┘
```

## 🛡️ 兩大錯誤的完整解決方案

### 錯誤 1：Repository Bean Not Found

#### 問題診斷
```bash
# 檢查哪個 Profile 正在使用
echo "Active Profile: $(grep spring.profiles.active application.properties)"

# 檢查對應 Profile 的 Repository 配置
grep -r "@Profile.*inmemory" src/ | grep -i repository
```

#### 解決方案：確保每個 Profile 都有對應的 Repository

```java
@Configuration
public class RepositoryConfig {
    
    // ✅ InMemory Profile - 簡單快速
    @Bean
    @Profile({"default", "inmemory", "test-inmemory"})
    public Repository<Product, ProductId> productRepositoryInMemory(
            MessageBus messageBus) {
        return new GenericInMemoryRepository<>(messageBus);
    }
    
    // ⚠️ Outbox Profile - 需要完整配置鏈
    @Bean
    @Profile({"outbox", "test-outbox", "prod-outbox"})
    public Repository<Product, ProductId> productRepositoryOutbox(
            OutboxStore<ProductData, String> outboxStore) {
        return new OutboxRepository<>(
            new OutboxRepositoryPeerAdapter<>(outboxStore),
            ProductMapper.newMapper()
        );
    }
}
```

### 錯誤 2：DataSource Configuration Failed

#### 問題診斷
```bash
# 檢查是否有排除 DataSource 自動配置
grep "spring.autoconfigure.exclude" application-inmemory.properties
```

#### 解決方案：InMemory Profile 必須排除 DataSource

```properties
# application-inmemory.properties
# 🔴 這是最關鍵的配置！
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
```

## 📋 完整的 Profile 配置檢查清單

### ✅ InMemory Profile 檢查清單
- [ ] `application-inmemory.properties` 存在
- [ ] 包含 `spring.autoconfigure.exclude` 排除 DataSource
- [ ] Java 配置中有 `@Profile("inmemory")` 的 Repository Bean
- [ ] Java 配置中有 `@Profile("inmemory")` 的 MessageBus Bean
- [ ] 不需要任何資料庫配置

### ⚠️ Outbox Profile 檢查清單
- [ ] `application-outbox.properties` 存在
- [ ] 包含完整的 `spring.datasource.*` 配置
- [ ] 包含 `messagestore.postgres.*` 配置
- [ ] Java 配置中有 `@Profile("outbox")` 的 Repository Bean
- [ ] 已創建 ProductData 類別（實作 OutboxData）
- [ ] 已創建 ProductMapper 類別（包含 OutboxMapper 內部類）
- [ ] 已創建 ProductOrmClient 介面
- [ ] 已配置 PgMessageDbClient Bean
- [ ] 已配置 OutboxStore Bean
- [ ] 已配置 EzOutboxClient Bean

## 🚀 漸進式實作步驟

### Step 1: 從 InMemory 開始（Day 1）

```bash
# 1. 創建基本配置
echo "spring.profiles.active=inmemory" > application.properties

# 2. 創建 InMemory profile 配置
cat > application-inmemory.properties << 'EOF'
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
EOF

# 3. 啟動測試
mvn spring-boot:run
```

### Step 2: 確認 InMemory 運作正常（Day 2-3）
- 實作所有 Use Cases
- 完成業務邏輯
- 通過所有單元測試

### Step 3: 準備 Outbox 配置（Day 4-5）
只有在 InMemory 完全正常後才進行：

1. 創建 Data 類別
2. 創建 Mapper 類別
3. 創建 OrmClient 介面
4. 配置 Outbox Beans

### Step 4: 切換到 Outbox（Day 6）
```bash
# 修改 active profile
echo "spring.profiles.active=outbox" > application.properties

# 啟動測試
mvn spring-boot:run
```

## 🔍 Profile 診斷工具

### 診斷腳本：check-profile.sh
```bash
#!/bin/bash

echo "=== Spring Profile 診斷 ==="

# 1. 檢查 Active Profile
ACTIVE_PROFILE=$(grep "spring.profiles.active" application.properties | cut -d'=' -f2)
echo "Active Profile: $ACTIVE_PROFILE"

# 2. 檢查對應的配置檔案
CONFIG_FILE="application-${ACTIVE_PROFILE}.properties"
if [ -f "$CONFIG_FILE" ]; then
    echo "✅ 配置檔案存在: $CONFIG_FILE"
else
    echo "❌ 配置檔案缺失: $CONFIG_FILE"
fi

# 3. 檢查 DataSource 排除（for InMemory）
if [[ "$ACTIVE_PROFILE" == *"inmemory"* ]]; then
    if grep -q "spring.autoconfigure.exclude" "$CONFIG_FILE"; then
        echo "✅ DataSource 已排除"
    else
        echo "❌ DataSource 未排除 - 會導致啟動失敗！"
    fi
fi

# 4. 檢查 Repository Bean 配置
echo "Repository Beans:"
grep -r "@Profile.*$ACTIVE_PROFILE" src/ | grep -i repository | wc -l
```

## 📊 Profile 決策樹

```
開始新專案？
    │
    ├─ 是 → 使用 inmemory profile
    │        │
    │        ├─ 應用啟動成功？
    │        │   ├─ 是 → 繼續開發
    │        │   └─ 否 → 檢查 DataSource 排除配置
    │        │
    │        └─ 功能完成？
    │            ├─ 是 → 考慮加入 Outbox
    │            └─ 否 → 繼續使用 InMemory
    │
    └─ 否（既有專案）
         │
         ├─ 需要持久化？
         │   ├─ 是 → 使用 outbox profile
         │   └─ 否 → 使用 inmemory profile
         │
         └─ 檢查完整配置鏈
```

## ⚠️ 關鍵提醒

### 永遠記住
1. **InMemory First**：除非你 100% 確定 Outbox 配置正確
2. **Profile 不要混用**：一個 Bean 只屬於特定 Profiles
3. **DataSource 必須排除**：InMemory 模式下這是最常見的錯誤
4. **Outbox 需要完整鏈**：缺少任何一環都會失敗

### 絕對不要
1. ❌ 在沒有測試 InMemory 的情況下直接用 Outbox
2. ❌ 忘記排除 DataSource 自動配置
3. ❌ 在同一個 Configuration 類別混合不同 Profile 的 Beans
4. ❌ 假設 Spring Boot 會自動處理一切

## 🎯 最終目標

```
Day 1-2: InMemory 啟動成功 ✅
Day 3-4: 業務功能完成 ✅
Day 5-6: Outbox 配置準備 ⚠️
Day 7:   切換到 Outbox ⚠️
```

記住：**寧可在 InMemory 停留太久，也不要過早切換到 Outbox！**