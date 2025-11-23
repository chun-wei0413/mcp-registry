# JPA Configuration Guide - JPA 配置完整指南

## 🎯 核心原則

所有使用 Spring Data JPA 的介面和類別都必須正確配置，確保 Spring 能夠找到並建立對應的 bean。

## 📦 套件結構規範

每個 Bounded Context 應該遵循以下套件結構：

```
tw.teddysoft.aiscrum.[bounded-context].adapter.out/
├── repository/                           # Outbox Repository 實作
│   └── Jpa[Aggregate]OutboxRepository   # 繼承 JpaRepository
├── database.springboot.projection/       # Projection 實作
│   └── Jpa[Aggregates]Projection        # 繼承 JpaRepository
├── persistence.inquiry/                  # Inquiry 實作
│   └── Jpa[Query]Inquiry                # 繼承 CrudRepository
└── persistence.archive/                  # Archive 實作
    └── Jpa[Aggregate]Archive             # 繼承 JpaRepository 或使用 @Repository
```

## 🔧 @EnableJpaRepositories 配置規則

### 必須加入的套件類型

| 套件類型 | 路徑模式 | 必須加入？ | 原因 |
|---------|---------|-----------|------|
| **repository** | `.adapter.out.repository` | ✅ 必須 | Outbox Repository |
| **projection** | `.adapter.out.database.springboot.projection` | ✅ 必須 | JPA Projection |
| **inquiry** | `.adapter.out.persistence.inquiry` | ✅ 必須 | 查詢介面 |
| **archive** | `.adapter.out.persistence.archive` | ⚠️ 看情況 | 如果繼承 JpaRepository |

### 配置範例

```java
@Configuration
@Profile("!inmemory")
@EnableJpaRepositories(basePackages = {
    // [Bounded Context Name] Bounded Context
    "tw.teddysoft.aiscrum.[context].adapter.out.repository",
    "tw.teddysoft.aiscrum.[context].adapter.out.projection",  // 舊版相容
    "tw.teddysoft.aiscrum.[context].adapter.out.database.springboot.projection",
    "tw.teddysoft.aiscrum.[context].adapter.out.persistence.inquiry",
    "tw.teddysoft.aiscrum.[context].adapter.out.persistence.archive",
})
public class JpaConfiguration {
    // 配置說明
}
```

## 📝 實作類別文件規範

### 1. JPA Projection

```java
/**
 * JPA implementation of [Aggregate]sProjection using Spring Data JPA.
 * 
 * Configuration requirement:
 * - Package MUST be included in @EnableJpaRepositories
 * - Located at: tw.teddysoft.aiscrum.[context].adapter.out.database.springboot.projection
 * 
 * Note: No @Repository annotation needed - Spring Data JPA automatically creates the bean
 * when the package is included in @EnableJpaRepositories.
 */
public interface Jpa[Aggregates]Projection extends [Aggregates]Projection, JpaRepository<[Aggregate]Data, String> {
    // 實作內容
}
```

### 2. Outbox Repository

```java
/**
 * JPA implementation of [Aggregate] Outbox Repository.
 * 
 * Configuration requirement:
 * - Package MUST be included in @EnableJpaRepositories
 * - Located at: tw.teddysoft.aiscrum.[context].adapter.out.repository
 * 
 * Note: Extends JpaRepository - requires @EnableJpaRepositories configuration
 */
public interface Jpa[Aggregate]OutboxRepository extends JpaRepository<[Aggregate]OutboxData, Long> {
    // 實作內容
}
```

### 3. Inquiry

```java
/**
 * JPA implementation of [Query]Inquiry using Spring Data JPA.
 * 
 * Configuration requirement:
 * - Package MUST be included in @EnableJpaRepositories
 * - Located at: tw.teddysoft.aiscrum.[context].adapter.out.persistence.inquiry
 * 
 * Note: Interface extending CrudRepository - requires @EnableJpaRepositories
 */
public interface Jpa[Query]Inquiry extends [Query]Inquiry, CrudRepository<[Entity]Data, String> {
    // 實作內容
}
```

### 4. Archive

```java
/**
 * JPA implementation of [Aggregate]Archive.
 * 
 * Configuration requirement:
 * Option A: If extending JpaRepository
 * - Package MUST be included in @EnableJpaRepositories
 * 
 * Option B: If using @Repository annotation
 * - Will be found by component scan, no need for @EnableJpaRepositories
 * 
 * Located at: tw.teddysoft.aiscrum.[context].adapter.out.persistence.archive
 */
// Option A
public interface Jpa[Aggregate]Archive extends [Aggregate]Archive, JpaRepository<[Aggregate]ArchiveData, String> {
    // 實作內容
}

// Option B
@Repository
public class Jpa[Aggregate]Archive implements [Aggregate]Archive {
    @PersistenceContext
    private EntityManager entityManager;
    // 手動實作
}
```

## ⚠️ 常見問題與解決方案

### 問題 1: Bean not found

**錯誤訊息**:
```
Field xxx required a bean of type 'JpaXxxProjection' that could not be found
```

**解決方案**:
1. 檢查套件是否在 `@EnableJpaRepositories` 的 `basePackages` 中
2. 確認介面繼承了正確的 Spring Data 介面（JpaRepository、CrudRepository 等）

### 問題 2: 重複註冊

**症狀**: Bean 衝突或重複建立

**解決方案**:
- JPA 介面不要加 `@Repository` 註解
- 選擇一種配置方式（@EnableJpaRepositories 或 @Repository），不要混用

### 問題 3: 套件路徑不一致

**解決方案**:
- 嚴格遵循套件命名規範
- 使用自動檢查腳本驗證

## 🔍 自動檢查機制

### 執行檢查腳本

```bash
# 檢查所有 JPA 配置
.ai/scripts/check-jpa-projection-config.sh

# 檢查新增的實作（開發時使用）
.ai/hooks/pre-commit-jpa-check.sh
```

### 檢查項目

1. ✅ 所有 JPA 介面的套件都在 @EnableJpaRepositories 中
2. ✅ JPA 介面沒有不必要的 @Repository 註解
3. ✅ 套件結構符合規範
4. ✅ 文件註解包含配置需求說明

## 📋 新增 JPA 實作檢查清單

當新增任何 JPA 相關實作時，請遵循以下步驟：

- [ ] 1. 確認套件路徑符合規範
- [ ] 2. 在 `JpaConfiguration.java` 的 `@EnableJpaRepositories` 加入套件路徑
- [ ] 3. 加入適當的註解文件說明配置需求
- [ ] 4. 不要在 JPA 介面上加 `@Repository` 註解
- [ ] 5. 執行 `check-jpa-projection-config.sh` 驗證配置
- [ ] 6. 在 commit 前確認所有測試通過

## 🚀 最佳實踐

1. **統一管理**: 所有 JPA 套件都在 JpaConfiguration 中集中管理
2. **明確命名**: 使用清晰一致的套件和類別命名
3. **定期檢查**: 每次新增實作後立即執行檢查腳本
4. **文件記錄**: 在每個實作類別上明確註解配置需求
5. **預留空間**: 為未來的 inquiry/archive 預先配置套件路徑

## 📚 相關文件

- [Projection Standards](./tech-stacks/java-ca-ezddd-spring/coding-standards/projection-standards.md)
- [Archive Standards](./tech-stacks/java-ca-ezddd-spring/coding-standards/archive-standards.md)
- [Outbox Pattern](./ADR-019-outbox-pattern.md)
- [Check Script Documentation](./scripts/README.md#jpa-configuration-checker)