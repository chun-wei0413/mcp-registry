# Outbox Pattern 測試配置指南

## 概述
本文件說明如何配置和撰寫 Outbox Pattern 的整合測試。

## 測試配置

### 1. Profile 配置
所有 Outbox 相關測試使用 `test-outbox` profile，配置檔案位於：
- `src/test/resources/application-test-outbox.yml`

### 2. 資料庫配置
```yaml
# application-test-outbox.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5800/board?currentSchema=message_store
    username: postgres
    password: root
    driver-class-name: org.postgresql.Driver

# messagestore 配置使用變數引用，避免重複
messagestore:
  postgres:
    url: ${spring.datasource.url}      # 引用上面的配置
    user: ${spring.datasource.username} # 引用上面的配置
    password: ${spring.datasource.password}
```

**配置說明**：
- `spring.datasource.*`：Spring Boot 主要資料源，JPA/Hibernate 使用
- `messagestore.postgres.*`：PgMessageDbClient 使用（透過變數引用避免重複）
- 不需要 `spring.datasource.scrum.*`（未被使用）

**注意事項**：
- 使用 PostgreSQL 測試資料庫（port 5800）
- Schema: message_store
- 需要先啟動測試資料庫

### 3. 測試類別配置
```java
@SpringBootTest(classes = AiScrumApp.class)
@Transactional  // 每個測試後自動 rollback
@ActiveProfiles("test-outbox")  // 載入 test-outbox 配置
@EzFeature  // 使用 ezSpec BDD 測試框架
@EzFeatureReport
public class YourOutboxRepositoryTest {
    // 測試程式碼
}
```

## 🔴 必要測試案例

**重要**：每個 Aggregate 的 OutboxRepository 都必須包含以下標準測試案例。完整範例請參考 [ProductOutboxRepositoryTest.java](./ProductOutboxRepositoryTest.java)

### 必須包含的測試案例：

1. **資料持久化測試** (`should_persist_[aggregate]_to_database_with_all_fields`)
   - 驗證所有欄位正確儲存到資料庫
   - 包括複雜物件（如 Goal、DefinitionOfDone）的 JSON 序列化

2. **資料讀取測試** (`should_retrieve_[aggregate]_with_complete_data`)
   - 驗證從資料庫讀取的完整性
   - 確認複雜物件正確反序列化

3. **軟刪除測試** (`should_soft_delete_[aggregate]`)
   - 驗證使用 `save()` 而非 `delete()` 執行軟刪除
   - 確認 `isDeleted` 標記設置正確

4. **版本控制測試** (`should_handle_version_control_for_optimistic_locking`)
   - 驗證樂觀鎖機制
   - 確認版本號正確遞增

## 測試範例

### 軟刪除測試注意事項
```java
// ✅ 正確：軟刪除使用 save 而非 delete
product.markAsDelete("userId");
repository.save(product);  // 不是 delete()

// 驗證 isDeleted 欄位
Query query = entityManager.createNativeQuery(
    "SELECT is_deleted FROM message_store.product WHERE id = ?1"
);
Boolean isDeleted = (Boolean) query.getSingleResult();
assertThat(isDeleted).isTrue();
```

### 版本控制測試
```java
// 版本號從 0 開始是正常的
assertThat(product.getVersion()).isGreaterThanOrEqualTo(0);
```

## 常見問題

### Q1: 為什麼不使用測試基礎類別？
**A**: 保持測試類別的配置明確可見，提高可讀性。每個測試都清楚顯示其配置，不需要查看繼承的類別。

### Q2: 如何共用測試輔助方法？
**A**: 使用組合而非繼承：
```java
@Component
public class OutboxTestHelper {
    public void verifyDataInDatabase(...) { }
}

// 在測試中注入使用
@Autowired
private OutboxTestHelper helper;
```

### Q3: 如何針對特定測試調整配置？
**A**: 使用 `@TestPropertySource` 覆蓋特定屬性：
```java
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true"  // 僅此測試顯示 SQL
})
```

## 檢查清單

測試 Outbox Repository 時，確保：
- [ ] 使用 `@ActiveProfiles("test-outbox")`
- [ ] 加上 `@Transactional` 自動 rollback
- [ ] PostgreSQL 測試資料庫正在運行（port 5800）
- [ ] Mapper 正確處理 `isDeleted` 欄位
- [ ] 軟刪除使用 `save()` 而非 `delete()`
- [ ] 版本號驗證接受 >= 0

## 相關文件
- [ADR-019: Outbox Pattern Implementation](../../.dev/adr/ADR-019-outbox-pattern-implementation.md)
- [Outbox Repository Coding Standards](../../.ai/tech-stacks/java-ca-ezddd-spring/coding-standards/repository-standards.md)