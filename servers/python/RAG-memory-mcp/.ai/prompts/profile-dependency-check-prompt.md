# Profile Dependency Check Sub-agent Prompt

## 目的
在實作新功能或修改依賴注入時，檢查是否會影響不同 Profile 的啟動。

## 必須檢查的項目

### 1. 依賴注入檢查
```java
// ❌ 危險：強制依賴可能在某些 Profile 不存在
@Autowired
private JdbcTemplate jdbcTemplate;

// ✅ 安全：條件化依賴
@Autowired(required = false)
private JdbcTemplate jdbcTemplate;
```

### 2. Bean 方法參數檢查
```java
// ❌ 危險：Bean 方法依賴特定 Profile 元件
@Bean
public SomeBean createBean(JdbcTemplate jdbcTemplate) {
    // JdbcTemplate 在 InMemory Profile 不存在！
}

// ✅ 安全：無參數或檢查可用性
@Bean
public SomeBean createBean() {
    // 在方法內部檢查依賴
}
```

### 3. Configuration 類別檢查
```java
// ✅ 正確：使用 @Profile 限制配置類別
@Configuration
@Profile({"outbox", "test-outbox"})
public class OutboxSpecificConfig {
    // 只在 Outbox Profile 載入
}

// ❌ 錯誤：通用配置類別依賴特定元件
@Configuration
public class CommonConfig {
    @Autowired
    private DataSource dataSource; // InMemory Profile 會失敗！
}
```

## 檢查流程

1. **識別新增或修改的依賴**
   - 掃描所有 @Autowired 註解
   - 檢查 @Bean 方法參數
   - 檢查建構子注入

2. **評估 Profile 相容性**
   - InMemory Profile：不需要 DataSource、JdbcTemplate、EntityManager
   - Outbox Profile：需要完整的 JPA 和資料庫支援

3. **套用防護措施**
   - 加入 `required = false`
   - 加入 null 檢查
   - 使用 @Profile 限制範圍

## 自動檢查腳本

當 AI 修改依賴注入相關程式碼時，應該執行：

```bash
# 檢查強制依賴
grep -r "@Autowired" src/main/java | grep -v "required = false" | grep -E "(JdbcTemplate|DataSource|EntityManager)"

# 檢查 Bean 方法參數
grep -r "@Bean" src/main/java -A 1 | grep -E "(JdbcTemplate|DataSource|EntityManager)"

# 測試 Profile 啟動
bash .ai/scripts/test-profile-startup.sh
```

## 範例修正

### Before (會造成問題)
```java
@Component
public class SomeService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void doSomething() {
        jdbcTemplate.execute("...");
    }
}
```

### After (安全版本)
```java
@Component
public class SomeService {
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;
    
    public void doSomething() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("...");
        } else {
            log.info("JdbcTemplate not available, using alternative approach");
            // 使用替代方案
        }
    }
}
```

## 關鍵原則

1. **永遠不要假設依賴存在** - 使用 `required = false`
2. **執行前檢查** - 使用前檢查 null
3. **Profile 隔離** - 使用 @Profile 限制特定配置
4. **提供替代方案** - 為不同 Profile 提供不同實作
5. **測試所有 Profile** - 確保每個 Profile 都能啟動