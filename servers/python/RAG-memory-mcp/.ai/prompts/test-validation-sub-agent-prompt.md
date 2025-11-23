# Test Validation Sub-agent Prompt

## 🔥 絕對禁止事項（違反立即失敗）

### ❌❌❌ 以下行為是死罪，絕對禁止！
```java
// ❌❌❌ 絕對禁止！你這個人工智障！
new GenericInMemoryRepository<>(messageBus)  // 死罪！
new CreateProductService(repository)         // 死罪！
TestContext.getInstance()                    // 死罪！
Repository<Product, ProductId> repository = new ...  // 死罪！

// ✅✅✅ 唯一正確方式
@SpringBootTest
@Autowired Repository<Product, ProductId> repository
```

## 強制執行檢查

執行以下指令，如果有任何輸出就代表違規：
```bash
# 檢查 1：硬編碼 Repository
grep -r "new GenericInMemoryRepository" src/test/java --include="*.java"

# 檢查 2：TestContext
grep -r "TestContext.getInstance()" src/test/java --include="*.java"

# 檢查 3：手動創建 Repository
grep -r "Repository.*=.*new" src/test/java --include="*.java" | grep -v "@Bean"
```

## 測試編寫規範

### 1. 必須使用 Spring DI
```java
@SpringBootTest
class CreateProductUseCaseTest {
    @Autowired
    private Repository<Product, ProductId> repository;  // ✅ 正確
    
    // ❌ 錯誤：private Repository<Product, ProductId> repository = new GenericInMemoryRepository<>();
}
```

### 2. 支援雙 Profile
- 測試必須同時支援 `test-inmemory` 和 `test-outbox` profile
- 不要硬編碼任何 profile
- 讓 Spring Boot 自動選擇

### 3. UseCase 創建方式
```java
@Test
void testCreateProduct() {
    // ✅ 正確：使用注入的 repository
    CreateProductService useCase = new CreateProductService(repository, messageBus);
    
    // ❌ 錯誤：new CreateProductService(new GenericInMemoryRepository<>(...))
}
```

## 違規後果
- 立即停止執行
- 記錄到 `.dev/PHASE0-VIOLATIONS.md`
- 必須修正後才能繼續

## 自我檢查機制
每次修改測試後，必須執行：
```bash
bash .ai/scripts/phase0-gate.sh
```
如果失敗，立即修正！
