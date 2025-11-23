# Controller 範例與模式

本目錄包含 Spring Boot REST Controller 的設計模式說明與實作範例。

## 📋 概述

Controller 是 Clean Architecture 中的 Adapter 層，負責處理 HTTP 請求並調用對應的 Use Case。Controller 應該保持輕量級，只處理請求轉換和回應格式化。

## 🎯 核心概念

### Controller 的職責
- **請求處理**：接收並驗證 HTTP 請求
- **資料轉換**：將請求轉換為 Use Case 輸入
- **調用 Use Case**：執行業務邏輯
- **回應格式化**：將結果轉換為 HTTP 回應

### 分層架構
```
HTTP Request → Controller → UseCase → Domain
      ↓            ↓           ↓         ↓
   Request DTO   Input DTO   Service  Aggregate
```

## 📁 檔案結構

```
controller/
├── README.md                     # 本文件
├── CreateTaskController.java     # 創建任務的 Controller
├── GetTasksController.java       # 查詢任務的 Controller  
└── GlobalExceptionHandler.java   # 全域異常處理
```

## 🔧 實作要點

### 1. 基本 Controller 結構

```java
@RestController
@RequestMapping("/api/[resources]")
public class [Resource]Controller {
    
    private final [Operation]UseCase useCase;
    
    // 建構子注入
    public [Resource]Controller([Operation]UseCase useCase) {
        this.useCase = useCase;
    }
    
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateRequest request) {
        // 1. 轉換請求為 Use Case 輸入
        var input = toInput(request);
        
        // 2. 執行 Use Case
        var output = useCase.execute(input);
        
        // 3. 根據結果返回適當的 HTTP 回應
        if (output.getExitCode() == ExitCode.SUCCESS) {
            return ResponseEntity.ok(toResponse(output));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", output.getMessage()));
        }
    }
}
```

### 2. 請求和回應 DTO

```java
// 請求 DTO
public class CreateTaskRequest {
    @NotBlank(message = "Plan ID is required")
    private String planId;
    
    @NotBlank(message = "Project name is required")
    private String projectName;
    
    @NotBlank(message = "Task name is required")
    private String taskName;
    
    // Getters and Setters
}

// 回應 DTO
public class CreateTaskResponse {
    private String taskId;
    private String message;
    
    public static CreateTaskResponse success(String taskId) {
        var response = new CreateTaskResponse();
        response.taskId = taskId;
        response.message = "Task created successfully";
        return response;
    }
}
```

### 3. 完整的 Controller 範例

```java
@RestController
@RequestMapping("/api/plans/{planId}/tasks")
public class CreateTaskController {
    
    private final CreateTaskUseCase createTaskUseCase;
    
    public CreateTaskController(CreateTaskUseCase createTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
    }
    
    @PostMapping
    public ResponseEntity<?> createTask(
            @PathVariable String planId,
            @RequestBody @Valid CreateTaskRequest request) {
        
        try {
            // 準備 Use Case 輸入
            CreateTaskInput input = CreateTaskInput.create();
            input.planId = PlanId.valueOf(planId);
            input.projectName = ProjectName.valueOf(request.getProjectName());
            input.taskName = request.getTaskName();
            
            // 執行 Use Case
            CqrsOutput output = createTaskUseCase.execute(input);
            
            // 處理結果
            if (output.getExitCode() == ExitCode.SUCCESS) {
                return ResponseEntity.ok(Map.of(
                    "taskId", output.getId(),
                    "message", "Task created successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", output.getMessage()
                ));
            }
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
```

### 4. 查詢 Controller

```java
@RestController
@RequestMapping("/api/tasks")
public class GetTasksController {
    
    private final GetTasksByDateUseCase getTasksByDateUseCase;
    
    public GetTasksController(GetTasksByDateUseCase getTasksByDateUseCase) {
        this.getTasksByDateUseCase = getTasksByDateUseCase;
    }
    
    @GetMapping
    public ResponseEntity<List<TaskDto>> getTasksByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        GetTasksByDateInput input = GetTasksByDateInput.create();
        input.date = date;
        
        List<TaskDto> tasks = getTasksByDateUseCase.execute(input);
        
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/today")
    public ResponseEntity<List<TaskDto>> getTodayTasks() {
        GetTasksByDateInput input = GetTasksByDateInput.create();
        input.date = LocalDate.now();
        
        List<TaskDto> tasks = getTasksByDateUseCase.execute(input);
        
        return ResponseEntity.ok(tasks);
    }
}
```

### 5. 全域異常處理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("INVALID_ARGUMENT", e.getMessage())
        );
    }
    
    @ExceptionHandler(UseCaseFailureException.class)
    public ResponseEntity<ErrorResponse> handleUseCaseFailure(UseCaseFailureException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.of("USE_CASE_FAILURE", e.getMessage())
        );
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("VALIDATION_FAILED", "Validation failed", errors)
        );
    }
    
    public static class ErrorResponse {
        private String code;
        private String message;
        private Map<String, String> details;

        public ErrorResponse() {}

        public ErrorResponse(String code, String message, Map<String, String> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public static ErrorResponse of(String code, String message, Map<String, String> details) {
            return new ErrorResponse(code, message, details);
        }

        public static ErrorResponse of(String code, String message) {
            return new ErrorResponse(code, message, null);
        }

        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, String> getDetails() { return details; }
        public void setDetails(Map<String, String> details) { this.details = details; }
    }
}
```

## 💡 設計原則

### 1. 單一職責
- Controller 只負責 HTTP 層面的處理
- 業務邏輯交給 Use Case
- 資料驗證使用 Bean Validation

### 2. 依賴倒置
- Controller 依賴 Use Case 介面
- 不直接依賴 Domain 或 Infrastructure

### 3. 錯誤處理
- 使用統一的錯誤回應格式
- 適當的 HTTP 狀態碼
- 詳細的錯誤訊息

### 4. RESTful 設計
- 遵循 REST 原則
- 適當的 HTTP 動詞
- 清晰的資源路徑

## 📝 使用範例

### CORS 配置

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
```

### 分頁查詢

```java
@GetMapping
public ResponseEntity<Page<PlanDto>> getPlans(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
    Page<PlanDto> plans = getPlanUseCase.execute(pageable);
    
    return ResponseEntity.ok(plans);
}
```

## ⚠️ 注意事項

1. **避免業務邏輯**
   - 不在 Controller 中進行複雜計算
   - 不直接操作 Domain 物件

2. **輸入驗證**
   - 使用 @Valid 和 Bean Validation
   - 提供清晰的錯誤訊息

3. **安全考量**
   - 適當的認證和授權
   - 輸入消毒防止注入攻擊

4. **API 版本控制**
   - 考慮使用版本前綴（如 /api/v1/）
   - 向後相容的設計

## 🔗 相關資源

- [Use Case 範例](../usecase/)
- [Spring Boot 文檔](https://spring.io/projects/spring-boot)
- [REST API 設計指南](https://restfulapi.net/)
- [測試範例](../test/)