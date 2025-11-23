# REST Controller 編碼規範

本文件定義 REST Controller 層的編碼標準，包含 Controller 結構、Request/Response DTO、錯誤處理等規範。

## 🔴 Controller 實作標準流程（2024-08-15 更新）

### 實作 Controller 的正確步驟
1. **生成 Controller 程式碼**
2. **立即檢查並配置所需的 UseCase Bean**
   - 檢查 `UseCaseConfiguration.java` 是否包含必要的 Bean 定義
   - 如果缺少，立即添加 Bean 配置
3. **生成 Controller 測試程式碼**
4. **執行測試並驗證**
   - 執行單一 Controller 測試：`mvn test -Dtest=ControllerNameTest -q`
   - 仔細檢查是否有 `Failed to load ApplicationContext` 錯誤
   - 如果有錯誤，先修正再繼續
5. **執行所有測試確保無 regression**
   - 執行：`mvn test -q`
   - 確認所有現有測試仍然通過
6. **只有在所有測試通過後才宣告完成**

### 常見錯誤與解決方案
| 錯誤訊息 | 原因 | 解決方案 |
|---------|------|----------|
| `Failed to load ApplicationContext` | UseCase Bean 未配置 | 在 `UseCaseConfiguration.java` 添加 Bean |
| `No qualifying bean of type` | 缺少依賴注入配置 | 檢查並添加必要的 @Bean 方法 |
| `UnsatisfiedDependencyException` | 構造函數參數無法注入 | 確認所有依賴都有對應的 Bean |

## 🔴 必須遵守的規則 (MUST FOLLOW)

### 1. REST API 路徑設計原則（2024-08-15 新增）
**用巢狀的建立端點、用扁平的資源位址**

當處理 Aggregate Root 之間的關聯時，必須遵循以下設計原則：

#### 核心規則
1. **建立（Create）**：使用巢狀路徑表達歸屬關係
   - 範例：`POST /v1/api/products/{productId}/pbis`
   - 語意：在特定 Product 的 PBI 集合中新增項目

2. **資源位址（Canonical URL）**：使用扁平路徑尊重獨立性
   - 範例：`GET/PATCH/DELETE /v1/api/pbis/{pbiId}`
   - 語意：PBI 作為 Aggregate Root 有獨立的資源位址

#### 設計理由
- **Aggregate Root 獨立性**：每個 Aggregate Root 有自己的識別與生命週期
- **業務前置條件**：巢狀建立端點自然表達「必須先有父資源」的約束
- **錯誤語意清晰**：父資源不存在時返回 404 很直觀

#### 完整路由範例
```java
// PBI (Product Backlog Item) 路由
POST   /v1/api/products/{productId}/pbis    // 建立 PBI（檢查 Product 存在）
GET    /v1/api/pbis/{pbiId}                 // 查詢單筆 PBI
PATCH  /v1/api/pbis/{pbiId}                 // 更新 PBI
DELETE /v1/api/pbis/{pbiId}                 // 刪除 PBI
GET    /v1/api/products/{productId}/pbis    // 列出某 Product 的所有 PBI

// Task 路由
POST   /v1/api/pbis/{pbiId}/tasks          // 建立 Task（檢查 PBI 存在）
GET    /v1/api/tasks/{taskId}              // 查詢單筆 Task
PATCH  /v1/api/tasks/{taskId}              // 更新 Task
DELETE /v1/api/tasks/{taskId}              // 刪除 Task
```

#### 錯誤處理
- 父資源不存在時必須返回 404
- 範例：`POST /products/{productId}/pbis` 當 productId 不存在時返回 404 PRODUCT_NOT_FOUND

### 2. Spring @RequestMapping 註解使用規則（2024-08-15 新增）
**Controller 必須正確使用 @RequestMapping 註解，避免路徑映射錯誤**

#### 🔴 關鍵規則：直接在方法層級指定完整路徑
當 Controller 只有單一端點時，不要在 class 層級使用 @RequestMapping，而是直接在方法的 @XxxMapping 註解中指定完整路徑。

```java
// ✅ 正確：直接在方法層級指定完整路徑
@RestController
public class ReestimateTaskController {
    
    @PutMapping("/v1/api/pbis/{pbiId}/tasks/{taskId}/reestimate")
    public ResponseEntity<?> reestimateTask(
        @PathVariable String pbiId,
        @PathVariable String taskId,
        @Valid @RequestBody ReestimateTaskRequest request) {
        // Implementation
    }
}

// ✅ 正確：當有多個端點時，可在 class 層級定義基礎路徑
@RestController
@RequestMapping("/v1/api/products")
public class ProductController {
    
    @GetMapping("/{id}")  // 完整路徑：/v1/api/products/{id}
    public ResponseEntity<?> getProduct(@PathVariable String id) { }
    
    @PostMapping  // 完整路徑：/v1/api/products
    public ResponseEntity<?> createProduct(@RequestBody Request request) { }
}

// ❌ 錯誤：單一端點卻使用 class 層級的完整路徑
@RestController
@RequestMapping("/v1/api/pbis/{pbiId}/tasks/{taskId}/reestimate")  // 錯誤！
public class ReestimateTaskController {
    
    @PutMapping  // Spring 無法正確映射這個端點
    public ResponseEntity<?> reestimateTask(/* ... */) { }
}

// ❌ 錯誤：混淆 class 和 method 層級的路徑
@RestController
@RequestMapping("/v1/api")  // 基礎路徑
public class ReestimateTaskController {
    
    @PutMapping("/pbis/{pbiId}/tasks/{taskId}/reestimate")  // 錯誤的組合方式
    public ResponseEntity<?> reestimateTask(/* ... */) { }
}
```

#### 最佳實踐建議
1. **單一端點 Controller**：直接在 @XxxMapping 方法註解中指定完整路徑
2. **多端點 Controller**：在 @RequestMapping 定義共同基礎路徑，方法註解定義相對路徑
3. **路徑參數**：確保路徑參數在正確的層級定義
4. **測試驗證**：Controller 實作後立即執行整合測試，確認端點可正確訪問

#### 常見錯誤症狀
- 所有測試返回 404 Not Found
- Spring Boot 啟動時沒有顯示端點映射
- 無法透過 REST client 訪問端點
- **Integration Test 單獨執行成功但全部執行失敗（2024-08-15）**

### 3. UseCase Output 處理規則（2024-08-15 更新）
**Controller 必須正確處理 UseCase 的執行結果**

```java
// ✅ 正確：必須接收並處理 UseCase 的 output
@PostMapping
public ResponseEntity<?> setProductGoal(@PathVariable String productId, @RequestBody Request request) {
    // 執行 UseCase 並接收 output
    CqrsOutput output = setProductGoalUseCase.execute(input);
    
    // 根據 ExitCode 處理不同情況
    if (output.getExitCode() == ExitCode.FAILURE) {
        String message = output.getMessage();
        
        // 根據錯誤訊息判斷具體錯誤類型
        if (message != null && message.toLowerCase().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("RESOURCE_NOT_FOUND", message, traceId));
        }
        
        return ResponseEntity.badRequest()
            .body(new ApiError("OPERATION_FAILED", message, traceId));
    }
    
    // 成功情況
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(response);
}

// ❌ 錯誤：執行 UseCase 但忽略返回值
@PostMapping
public ResponseEntity<?> setProductGoal(@PathVariable String productId, @RequestBody Request request) {
    // 錯誤！執行但不處理結果
    setProductGoalUseCase.execute(input);
    
    // 錯誤！永遠返回成功
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
}
```

#### 關鍵原則
1. **必須接收 UseCase 的 output**：不能執行後忽略結果
2. **根據 ExitCode 決定 HTTP 狀態碼**：
   - `ExitCode.SUCCESS` → 2xx (200, 201, 202 等)
   - `ExitCode.FAILURE` → 根據錯誤訊息返回適當的 4xx 狀態碼
3. **解析錯誤訊息以判斷錯誤類型**：
   - 包含 "not found" → 404 Not Found
   - 包含 "already exists" → 409 Conflict
   - 包含 "unauthorized" → 401 Unauthorized
   - 其他業務錯誤 → 400 Bad Request

### 2. 測試案例品質要求（2024-08-15 新增）
**Controller 測試必須避免無意義的檢查，專注於業務價值**

#### 🔴 禁止的無意義測試
```java
// ❌ 錯誤：重複測試 Bean Validation 機制
@Test
void should_return_400_when_name_is_missing() { /* 測試 @NotBlank */ }

@Test  
void should_return_400_when_description_is_missing() { /* 測試 @NotBlank */ }

@Test
void should_return_400_when_state_is_missing() { /* 測試 @NotBlank */ }

// ❌ 錯誤：重複測試 @Size 驗證機制
@Test
void should_return_400_when_name_exceeds_max_length() { /* 測試 @Size */ }

@Test
void should_return_400_when_description_exceeds_max_length() { /* 測試 @Size */ }

// ❌ 錯誤：測試相同功能的不同變化
@Test
void should_work_with_idempotency_key() { /* 有 header */ }

@Test  
void should_work_without_idempotency_key() { /* 沒有 header */ }

// ❌ 錯誤：過度詳細的 JSON 結構檢查
.andExpect(jsonPath("$.definitionOfDone.criteria[0]").value("All tests pass"))
.andExpect(jsonPath("$.definitionOfDone.note").value("Standard definition of done"))
```

#### ✅ 推薦的有意義測試
```java
// ✅ 正確：測試一種驗證機制的代表性案例
@Test
void should_return_400_when_required_fields_are_missing() {
    // 選擇一個代表性欄位測試 @NotBlank 機制
    request.setName(null);
    // 驗證 Bean Validation 有正確運作
}

@Test
void should_return_400_when_field_length_validation_fails() {
    // 選擇一個代表性欄位測試 @Size 機制  
    request.setName("a".repeat(101));
    // 驗證長度限制有正確運作
}

// ✅ 正確：測試業務邏輯和錯誤處理
@Test
void should_return_404_when_product_not_found() {
    // 測試 Controller 如何處理 UseCase 的 FAILURE 結果
}

// ✅ 正確：簡化的 JSON 結構檢查
.andExpect(jsonPath("$.id").value(productId))
.andExpect(jsonPath("$.name").exists())
.andExpect(jsonPath("$.definitionOfDone").exists())
```

#### 測試品質準則
1. **一個機制一個測試**：Bean Validation、長度檢查等框架機制只需代表性測試
2. **聚焦業務邏輯**：重點測試 Controller 如何處理 UseCase 結果和 HTTP 映射
3. **簡化 JSON 檢查**：只驗證關鍵欄位存在，不過度檢查 DTO 內部結構
4. **合併相似測試**：相同功能的不同變化可以合併為一個測試
5. **測試名稱要清楚**：測試名稱應該明確表達測試目的和業務價值

#### 無意義測試的定義
- 測試框架功能而非業務邏輯
- 重複測試相同的驗證機制
- 測試 DTO 序列化/反序列化細節
- 不會因業務需求變更而失敗的測試

### 3. Request/Response DTO 位置
**Request 和 Response DTO 必須宣告為 Controller 的 inner class**

```java
// ✅ 正確：Request/Response 作為 inner class
@RestController
@RequestMapping("/api/products")
public class CreateProductController {
    
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody CreateProductRequest request) {
        // Implementation
    }
    
    // Request DTO as static inner class
    public static class CreateProductRequest {
        @NotBlank(message = "Product name is required")
        @Size(min = 1, max = 100)
        @JsonProperty("name")
        private String name;
        
        @NotBlank(message = "User ID is required")
        @JsonProperty("userId")
        private String userId;
        
        // Default constructor for JSON deserialization
        public CreateProductRequest() {}
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
    
    // Response DTO as static inner class (if needed)
    public static class CreateProductResponse {
        private String productId;
        private String status;
        private String location;
        
        // Constructor, getters, setters
    }
}

// ❌ 錯誤：獨立的 Request/Response 檔案
// CreateProductRequest.java - 錯誤！
public class CreateProductRequest {
    // ...
}
```

#### 理由
- **內聚性**：Request/Response 與 Controller 緊密相關
- **可維護性**：減少檔案數量，相關程式碼集中管理
- **命名空間**：避免與其他模組的 DTO 命名衝突
- **測試便利**：測試時容易找到相關的 DTO 定義

## 🎯 Controller 結構規範

### 1. 基本結構
```java
@RestController
@RequestMapping("/api/v1/products")  // 包含版本號
public class ProductController {

    // 使用 final fields + 構造函數注入
    private final GetProductUseCase getProductUseCase;
    private final CreateProductUseCase createProductUseCase;

    public ProductController(GetProductUseCase getProductUseCase,
                           CreateProductUseCase createProductUseCase) {
        this.getProductUseCase = getProductUseCase;
        this.createProductUseCase = createProductUseCase;
    }
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    
    // 每個 HTTP 方法對應一個端點
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) { }
    
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody CreateProductRequest request) { }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody UpdateProductRequest request) { }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) { }
}
```

### 2. 依賴注入
```java
// ✅ 正確：構造函數注入
@RestController
public class ProductController {
    private final ProductUseCase useCase;
    
    public ProductController(ProductUseCase useCase) {
        // Controller 使用 Objects.requireNonNull，不用 Contract
        this.useCase = Objects.requireNonNull(useCase, "useCase cannot be null");
    }
}

// ❌ 錯誤：Field injection
@RestController
public class ProductController {
    @Autowired
    private ProductUseCase useCase;  // 避免使用！
}
```

## 🎯 HTTP 狀態碼映射

### 成功狀態碼
```java
// GET - 200 OK
return ResponseEntity.ok(productDto);

// POST - 201 Created
URI location = URI.create("/api/products/" + productId);
return ResponseEntity.created(location).body(response);

// PUT - 200 OK
return ResponseEntity.ok(updatedProduct);

// DELETE - 204 No Content
return ResponseEntity.noContent().build();

// Async operation - 202 Accepted
return ResponseEntity.accepted()
    .header("Operation-Id", operationId)
    .body(response);
```

### 錯誤狀態碼
```java
// 400 Bad Request - 驗證錯誤
if (request == null || !isValid(request)) {
    return ResponseEntity.badRequest()
        .body(new ApiError("INVALID_REQUEST", "Request validation failed", traceId));
}

// 404 Not Found - 資源不存在
if (output.getExitCode() == ExitCode.FAILURE && output.getMessage().contains("not found")) {
    return ResponseEntity.notFound().build();
}

// 409 Conflict - 資源衝突
if (output.getMessage().contains("already exists")) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ApiError("DUPLICATE_RESOURCE", message, traceId));
}

// 500 Internal Server Error - 系統錯誤
catch (Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred", traceId));
}
```

## 🎯 錯誤處理

### 1. ApiError 結構
```java
// 統一的錯誤回應格式（作為 Controller 的 inner class 或共用類別）
public static class ApiError {
    private final String code;
    private final String message;
    private final String traceId;
    private final LocalDateTime timestamp;
    
    public ApiError(String code, String message, String traceId) {
        this.code = code;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
}
```

### 2. 錯誤處理模式
```java
@PostMapping
public ResponseEntity<?> createProduct(@RequestBody CreateProductRequest request) {
    String traceId = UUID.randomUUID().toString();
    
    try {
        // 輸入驗證
        if (request == null || request.getName() == null) {
            return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_INPUT", "Product name is required", traceId));
        }
        
        // 執行 Use Case
        CreateProductInput input = mapToInput(request);
        CqrsOutput<ProductDto> output = createProductUseCase.execute(input);
        
        // 處理結果
        if (output.getExitCode() == ExitCode.SUCCESS) {
            URI location = URI.create("/api/products/" + output.getId());
            return ResponseEntity.created(location).body(output.getData());
        } else {
            return handleFailure(output, traceId);
        }
        
    } catch (IllegalArgumentException e) {
        // 業務異常
        return ResponseEntity.badRequest()
            .body(new ApiError("INVALID_ARGUMENT", e.getMessage(), traceId));
            
    } catch (Exception e) {
        // 系統異常
        log.error("Unexpected error, traceId: {}", traceId, e);
        return ResponseEntity.status(500)
            .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred", traceId));
    }
}
```

## 🎯 請求驗證

### 1. 使用 Bean Validation
```java
public static class CreateProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 100, message = "Product name must be between 1 and 100 characters")
    private String name;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    private String currency;
}
```

### 2. 自定義驗證
```java
@GetMapping("/{id}")
public ResponseEntity<?> getProduct(@PathVariable String id) {
    // 自定義驗證
    if (id == null || id.trim().isEmpty() || "null".equalsIgnoreCase(id)) {
        return ResponseEntity.badRequest()
            .body(new ApiError("INVALID_ID", "Product ID cannot be null or empty", traceId));
    }
    
    // 繼續處理...
}
```

## 🎯 RESTful 設計原則

### 1. URL 設計
```java
// ✅ 正確：使用複數名詞
@RequestMapping("/api/products")
@RequestMapping("/api/users")

// ❌ 錯誤：使用動詞或單數
@RequestMapping("/api/getProduct")  // 錯誤！
@RequestMapping("/api/product")     // 錯誤！
```

### 2. HTTP 方法使用
```java
@GetMapping("/{id}")        // 獲取單一資源
@GetMapping                 // 獲取資源列表
@PostMapping               // 創建新資源
@PutMapping("/{id}")       // 完整更新資源
@PatchMapping("/{id}")     // 部分更新資源
@DeleteMapping("/{id}")    // 刪除資源
```

### 3. 查詢參數
```java
@GetMapping
public ResponseEntity<?> listProducts(
    @RequestParam(required = false) String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "name") String sortBy) {
    // 實作分頁和過濾
}
```

## 🎯 回應 Header 處理

```java
return ResponseEntity.ok()
    .header("X-Total-Count", String.valueOf(totalCount))
    .header("X-Page-Number", String.valueOf(pageNumber))
    .header("Cache-Control", "max-age=3600")
    .header("ETag", generateETag(resource))
    .body(response);
```

## 🔴 Integration Test 隔離問題（2024-08-15 新增）

### 問題描述
當 Controller 在 class 層級使用 `@RequestMapping` 並包含路徑參數時，Spring Boot Integration Test 可能會發生測試隔離問題：
- 單獨執行測試時成功
- 執行全部測試時失敗（返回 404）
- 多個 Integration Test 之間互相干擾

### 根本原因
Spring Boot 在處理多個 Integration Test 時，如果 Controller 使用 class 層級的 `@RequestMapping` 且包含路徑參數（如 `{pbiId}`、`{taskId}`），可能會因為 Spring Context 重用而導致路徑映射混亂。

### 解決方案

#### 方案 1：修改 Controller 結構（推薦）
```java
// ✅ 推薦：直接在方法層級定義完整路徑
@RestController
public class ReestimateTaskController {
    @PutMapping("/v1/api/pbis/{pbiId}/tasks/{taskId}/reestimate")
    public ResponseEntity<?> reestimateTask(...) { }
}
```

#### 方案 2：使用 @DirtiesContext（暫時解決）
```java
// ⚠️ 暫時方案：強制刷新 Spring Context
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReestimateTaskControllerIntegrationTest {
    // 測試內容
}
```

## 🔴 RestAssured Integration Test 修正（2024-08-15 新增）

### 問題描述
使用 RestAssured 進行 Integration Test 時，可能出現測試隔離問題：
- RestAssured 全域配置在不同測試類之間污染
- Port 配置不正確導致連接失敗
- 多個測試類執行時互相干擾

### ✅ 必須的修正（僅適用於 RestAssured）

#### 1. @BeforeEach setUp() 方法
**🔴 重要：只有使用 RestAssured 的 Integration Test 需要這個修正**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SomeControllerIntegrationTest extends BaseIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.reset();      // 先清乾淨，避免前一類留下的全域設定
        RestAssured.port = port;  // 再設定本測試要用的 port
        RestAssured.basePath = ""; // 明確歸零 basePath
        Mockito.reset(someUseCase); // 重置對應的 UseCase Mock
    }
}
```

#### 2. 每個 given() 明確指定 port
```java
@Test
void should_return_success() {
    // ✅ 正確：每個 given() 都明確指定 port
    given()
        .port(port)  // 🔴 關鍵：明確指定 port
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .body(request)
    .when()
        .post("/v1/api/endpoint")
    .then()
        .statusCode(200);
}
```

#### 錯誤範例
```java
// ❌ 錯誤：缺少 @BeforeEach setUp()
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SomeControllerIntegrationTest extends BaseIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    // 缺少 setUp() 方法！
    
    @Test
    void should_return_success() {
        // ❌ 錯誤：沒有明確指定 port
        given()
            .accept(ContentType.JSON)  // 缺少 .port(port)
            .body(request)
        .when()
            .post("/v1/api/endpoint")
        .then()
            .statusCode(200);
    }
}
```

### 適用範圍
**🔴 重要說明**：
- 這兩個修正**僅適用於使用 RestAssured 的 Integration Test**
- 使用 MockMvc 的測試（如 `@WebMvcTest`）**不需要**這些修正
- Unit Test **不需要**這些修正

### 診斷步驟
1. **單獨執行測試**：`mvn test -Dtest=SpecificControllerIntegrationTest`
2. **執行所有測試**：`mvn test`
3. **如果單獨成功但全部失敗**：檢查 Controller 的 @RequestMapping 結構
4. **如果使用 RestAssured**：確認已加入上述兩個修正
5. **查看 Spring Boot 啟動日誌**：確認端點是否正確註冊

### 最佳實踐
1. **避免在 class 層級使用包含路徑參數的完整路徑**
2. **RestAssured Integration Test 必須使用上述兩個修正**
3. **Integration Test 應該使用 @DirtiesContext 確保測試隔離**
4. **定期執行完整測試套件，不只依賴單一測試執行**

## 🔍 檢查清單

### Controller 結構
- [ ] Request/Response DTO 是 inner class
- [ ] 使用構造函數注入
- [ ] 有 @RestController 註解
- [ ] 有 @RequestMapping 定義基礎路徑
- [ ] 路徑包含版本號（如 /api/v1）

### HTTP 規範
- [ ] 使用正確的 HTTP 方法
- [ ] 返回適當的狀態碼
- [ ] RESTful URL 設計
- [ ] 使用複數資源名稱

### 錯誤處理
- [ ] 統一的錯誤回應格式
- [ ] 包含 traceId 用於追蹤
- [ ] 適當的錯誤訊息
- [ ] 不洩露敏感資訊

### 驗證
- [ ] 使用 Bean Validation 註解
- [ ] 驗證路徑參數
- [ ] 處理 null 和空值
- [ ] 有意義的錯誤訊息

## 相關文件
- [Spring 配置規範](../coding-standards.md#-spring-配置規範)
- [錯誤處理原則](../coding-standards.md#-錯誤處理原則)
- [Controller 範例](../examples/controller/README.md)