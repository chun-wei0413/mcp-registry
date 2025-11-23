# Use Case 編碼規範

本文件定義 Use Case 層的編碼標準，包含 Interface、Service、Input/Output 和 Mapper 設計。

## 🔴 必須遵守的規則 (MUST FOLLOW)

### 1. Use Case Interface 結構
**Input 和 Output 必須宣告為 Use Case Interface 的 inner class**

```java
// ✅ 正確：Input/Output 作為 inner class
public interface CreateProductUseCase extends Command<
    CreateProductUseCase.CreateProductInput, 
    ProductDto> {
    
    // Input 必須是 static inner class
    class CreateProductInput implements Input {
        public String productId;
        public String name;
        public String userId;

        public CreateProductInput() {}

        public CreateProductInput(String productId, String name, String userId) {
            this.productId = productId;
            this.name = name;
            this.userId = userId;
        }
    }
    
    // Command 使用 CqrsOutput，Query 可自定義 Output inner class
    // Query Output 範例:
    class GetProductOutput implements Output {
        public ExitCode exitCode;
        public String message;
        public ProductDto product;

        public GetProductOutput() {}

        public GetProductOutput(ExitCode exitCode, String message, ProductDto product) {
            this.exitCode = exitCode;
            this.message = message;
            this.product = product;
        }
    }
}

// ❌ 錯誤：Input/Output 在外部定義
public class CreateProductInput { } // 錯誤！
public class CreateProductOutput { } // 錯誤！
```

### 2. Command vs Query 分離

#### Command Use Case
```java
// ✅ Command：修改狀態，返回 CqrsOutput
public interface CreateProductUseCase extends Command<
    CreateProductUseCase.CreateProductInput, 
    ProductDto> {
}

public class CreateProductService implements CreateProductUseCase {
    private final ProductRepository repository;
    private final MessageBus messageBus;

    public CreateProductService(ProductRepository repository, MessageBus messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }
    
    @Override
    public CqrsOutput<ProductDto> execute(CreateProductInput input) {
        // 1. 創建 Aggregate
        Product product = new Product(
            ProductId.valueOf(input.productId),
            input.name,
            UserId.valueOf(input.userId)
        );
        
        // 2. 保存
        repository.save(product);
        
        // 3. 發布事件
        messageBus.publish(product.getUncommittedEvents());
        
        // 4. 返回結果
        return CqrsOutput.of(ProductMapper.toDto(product));
    }
}
```

#### Query Use Case
```java
// ✅ Query：只讀取，不修改狀態
public interface GetProductUseCase extends Query<
    GetProductUseCase.GetProductInput,
    GetProductUseCase.GetProductOutput> {
    
    class GetProductInput implements Input {
        public String productId;

        public GetProductInput() {}

        public GetProductInput(String productId) {
            this.productId = productId;
        }
    }
    
    class GetProductOutput implements Output {
        public ExitCode exitCode;
        public String message;
        public ProductDto product;

        public GetProductOutput() {}

        public GetProductOutput(ExitCode exitCode, String message, ProductDto product) {
            this.exitCode = exitCode;
            this.message = message;
            this.product = product;
        }
    }
}

public class GetProductService implements GetProductUseCase {
    private final ProductDtoProjection projection;

    public GetProductService(ProductDtoProjection projection) {
        this.projection = projection;
    }
    
    @Override
    public GetProductOutput execute(GetProductInput input) {
        Optional<ProductDto> product = projection.findById(input.productId);
        
        if (product.isPresent()) {
            return GetProductOutput.builder()
                .exitCode(ExitCode.SUCCESS)
                .product(product.get())
                .build();
        } else {
            return GetProductOutput.builder()
                .exitCode(ExitCode.FAILURE)
                .message("Product not found")
                .build();
        }
    }
}
```

## 🔄 Mapper 設計原則

### 1. Mapper 位置和結構
```java
// ✅ 獨立的 Mapper 類別
public class ProductMapper {
    
    // Aggregate to DTO
    public static ProductDto toDto(Product product) {
        if (product == null) return null;
        
        return ProductDto.builder()
            .productId(product.getId().value())
            .name(product.getName())
            .state(product.getState().name())
            .createdAt(product.getCreatedAt())
            .build();
    }
    
    // DTO to Response (if needed)
    public static ProductResponse toResponse(ProductDto dto) {
        // ...
    }
    
    // Entity to DTO
    public static TaskDto toDto(Task task) {
        // ...
    }
}
```

### 2. Mapper 使用原則
- **單一職責**：每個 Mapper 負責一個 Aggregate 的轉換
- **靜態方法**：使用 static 方法，無狀態
- **Null 安全**：處理 null 輸入
- **不包含業務邏輯**：只做資料轉換

## 🎯 Service 實作模式

### 1. 依賴注入
```java
public class CreateProductService implements CreateProductUseCase {
    // 使用 final fields + 構造函數注入
    private final ProductRepository repository;
    private final MessageBus messageBus;
    private final ProductDtoProjection projection;

    public CreateProductService(ProductRepository repository,
                                MessageBus messageBus,
                                ProductDtoProjection projection) {
        this.repository = repository;
        this.messageBus = messageBus;
        this.projection = projection;
    }

    // 不要使用 @Autowired field injection
}
```

### 2. 事務管理
```java
public class CreateProductService implements CreateProductUseCase {
    
    @Override
    public CqrsOutput<ProductDto> execute(CreateProductInput input) {
        // 事務內的操作
    }
}
```

### 3. 錯誤處理
```java
@Override
public CqrsOutput<ProductDto> execute(CreateProductInput input) {
    try {
        // 業務邏輯
        Product product = new Product(...);
        repository.save(product);
        return CqrsOutput.of(ProductMapper.toDto(product));
        
    } catch (DuplicateProductException e) {
        // 業務異常：返回失敗結果
        return CqrsOutput.of(ExitCode.FAILURE, e.getMessage());
        
    } catch (Exception e) {
        // 系統異常：記錄並拋出
        log.error("Failed to create product", e);
        throw new SystemException("Failed to create product", e);
    }
}
```

## 🎯 Input/Output 設計準則

### 1. Input 設計
```java
class CreateProductInput implements Input {
    public String productId;      // 使用 String，不用 domain object
    public String name;
    public String userId;

    public CreateProductInput() {}

    public CreateProductInput(String productId, String name, String userId) {
        this.productId = productId;
        this.name = name;
        this.userId = userId;
    }

    // 驗證方法（可選）
    public void validate() {
        requireNotNull("Product ID", productId);
        requireNotNull("Name", name);
    }
}
```

### 2. Output 設計
```java
// Command Output：使用 CqrsOutput
CqrsOutput<ProductDto> output = CqrsOutput.of(dto);

// Query Output：自定義 Output class
class GetProductOutput implements Output {
    public ExitCode exitCode;
    public String message;
    public ProductDto product;

    public GetProductOutput() {}

    public GetProductOutput(ExitCode exitCode, String message, ProductDto product) {
        this.exitCode = exitCode;
        this.message = message;
        this.product = product;
    }

    // 成功結果
    public static GetProductOutput success(ProductDto product) {
        return new GetProductOutput(ExitCode.SUCCESS, null, product);
    }

    // 失敗結果
    public static GetProductOutput failure(String message) {
        return new GetProductOutput(ExitCode.FAILURE, message, null);
    }
}
```

## 🎯 DTO 設計

### 1. DTO 結構
```java
public class ProductDto {
    private String productId;
    private String name;
    private String state;
    private String creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 相關的子物件
    private List<TaskDto> tasks;
    private ProductGoalDto goal;
}
```

### 2. DTO 原則
- **扁平化**：避免深層嵌套
- **完整性**：包含前端需要的所有資料
- **簡單類型**：使用 String、基本型別
- **無業務邏輯**：純資料容器

## 🔍 檢查清單

### Use Case Interface
- [ ] Input/Output 是 inner class
- [ ] Input 實作 Input 介面
- [ ] Output 實作 Output 介面或使用 CqrsOutput
- [ ] 有 create() 靜態方法

### Service 實作
- [ ] 使用構造函數注入
- [ ] 有適當的事務管理
- [ ] 正確處理異常
- [ ] 發布 Domain Events（Command）
- [ ] 不修改狀態（Query）

### Mapper
- [ ] 獨立的 Mapper 類別
- [ ] 使用靜態方法
- [ ] 處理 null 值
- [ ] 沒有業務邏輯

### DTO
- [ ] 使用簡單的 POJO 或 record
- [ ] 包含所有必要欄位
- [ ] 使用簡單類型
- [ ] 無業務邏輯

## 📋 快速複製模板

### Command Use Case 完整模板

#### Interface
```java
package [package].[aggregate].usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface [Operation][Aggregate]UseCase extends Command<
    [Operation][Aggregate]UseCase.[Operation][Aggregate]Input, 
    CqrsOutput> {
    
    class [Operation][Aggregate]Input implements Input {
        public String [aggregate]Id;
        public String parameter1;
        public String userId;
        public String requestId;  // 用於冪等性

        public [Operation][Aggregate]Input() {}

        public [Operation][Aggregate]Input(String [aggregate]Id, String parameter1, String userId, String requestId) {
            this.[aggregate]Id = [aggregate]Id;
            this.parameter1 = parameter1;
            this.userId = userId;
            this.requestId = requestId;
        }

        public void validate() {
            requireNotNull("[Aggregate] ID", [aggregate]Id);
            requireNotNull("Parameter", parameter1);
        }
    }
}
```

#### Service Implementation
```java
package [package].[aggregate].usecase.service;

import static tw.teddysoft.ucontract.Contract.*;

public class [Operation][Aggregate]Service implements [Operation][Aggregate]UseCase {

    private final Repository<[Aggregate], [Aggregate]Id> repository;
    private final MessageBus<DomainEvent> messageBus;

    public [Operation][Aggregate]Service(Repository<[Aggregate], [Aggregate]Id> repository,
                                         MessageBus<DomainEvent> messageBus) {
        this.repository = repository;
        this.messageBus = messageBus;
    }
    
    @Override
    public CqrsOutput execute([Operation][Aggregate]Input input) {
        try {
            // 驗證輸入
            input.validate();
            
            // 載入 Aggregate
            [Aggregate] [aggregate] = repository.findById([Aggregate]Id.valueOf(input.[aggregate]Id))
                .orElse(null);
            
            if ([aggregate] == null) {
                return CqrsOutput.create()
                    .setId(input.[aggregate]Id)
                    .setExitCode(ExitCode.FAILURE)
                    .setMessage("[Aggregate] not found");
            }
            
            // 執行業務邏輯
            [aggregate].doOperation(input.parameter1);
            
            // 修改事件 metadata（冪等性）
            List<DomainEvent> events = [aggregate].getUncommittedEvents();
            for (DomainEvent event : events) {
                if (event instanceof [Aggregate]Events e) {
                    Map<String, String> metadata = e.metadata();
                    metadata.put("requestId", input.requestId != null ? input.requestId : UUID.randomUUID().toString());
                    metadata.put("userId", input.userId != null ? input.userId : "system");
                    metadata.put("source", "api");
                }
            }
            
            // 儲存並發布事件
            repository.save([aggregate]);
            messageBus.publish(events);
            
            // 返回結果
            return CqrsOutput.create()
                .setId([aggregate].getId().value())
                .setExitCode(ExitCode.SUCCESS);
                
        } catch (BusinessException e) {
            return CqrsOutput.create()
                .setExitCode(ExitCode.FAILURE)
                .setMessage(e.getMessage());
                
        } catch (Exception e) {
            log.error("Failed to execute [Operation][Aggregate]", e);
            throw new UseCaseFailureException(e);
        }
    }
}
```

### Query Use Case 完整模板

#### Interface
```java
package [package].[aggregate].usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.query.Query;
import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import java.util.List;

public interface Get[Aggregate]sUseCase extends Query<
    Get[Aggregate]sUseCase.Get[Aggregate]sInput, 
    Get[Aggregate]sUseCase.Get[Aggregate]sOutput> {
    
    class Get[Aggregate]sInput implements Input {
        public String userId;
        public String filter;
        public Integer page;
        public Integer size;

        public Get[Aggregate]sInput() {}

        public Get[Aggregate]sInput(String userId, String filter, Integer page, Integer size) {
            this.userId = userId;
            this.filter = filter;
            this.page = page != null ? page : 0;
            this.size = size != null ? size : 20;
        }
    }
    
    class Get[Aggregate]sOutput implements Output {
        public ExitCode exitCode;
        public String message;
        public List<[Aggregate]Dto> [aggregate]s;
        public Integer totalElements;
        public Integer totalPages;

        public Get[Aggregate]sOutput() {}

        public Get[Aggregate]sOutput(ExitCode exitCode, String message, List<[Aggregate]Dto> [aggregate]s,
                                     Integer totalElements, Integer totalPages) {
            this.exitCode = exitCode;
            this.message = message;
            this.[aggregate]s = [aggregate]s;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public static Get[Aggregate]sOutput success(List<[Aggregate]Dto> [aggregate]s) {
            return new Get[Aggregate]sOutput(ExitCode.SUCCESS, null, [aggregate]s,
                                             [aggregate]s.size(), null);
        }

        public static Get[Aggregate]sOutput failure(String message) {
            return new Get[Aggregate]sOutput(ExitCode.FAILURE, message,
                                             Collections.emptyList(), 0, 0);
        }
    }
}
```

#### Service Implementation
```java
package [package].[aggregate].usecase.service;

public class Get[Aggregate]sService implements Get[Aggregate]sUseCase {

    private final [Aggregate]DtosProjection projection;

    public Get[Aggregate]sService([Aggregate]DtosProjection projection) {
        this.projection = projection;
    }
    
    @Override
    public Get[Aggregate]sOutput execute(Get[Aggregate]sInput input) {
        try {
            // 建立 Projection 輸入
            var projectionInput = [Aggregate]DtosProjection.[Aggregate]DtosProjectionInput.builder()
                .userId(input.userId)
                .filter(input.filter)
                .page(input.page)
                .size(input.size)
                .build();
            
            // 查詢資料
            List<[Aggregate]Dto> [aggregate]s = projection.query(projectionInput);
            
            // 返回成功結果
            return Get[Aggregate]sOutput.success([aggregate]s);
            
        } catch (Exception e) {
            log.error("Failed to get [aggregate]s", e);
            return Get[Aggregate]sOutput.failure("Failed to retrieve [aggregate]s: " + e.getMessage());
        }
    }
}
```

### Mapper 模板
```java
package [package].[aggregate].usecase.port;

public class [Aggregate]Mapper {
    
    // Aggregate to DTO
    public static [Aggregate]Dto toDto([Aggregate] [aggregate]) {
        if ([aggregate] == null) return null;
        
        return [Aggregate]Dto.builder()
            .[aggregate]Id([aggregate].getId().value())
            .name([aggregate].getName())
            .state([aggregate].getState().name())
            .createdAt([aggregate].getCreatedAt())
            .updatedAt([aggregate].getUpdatedAt())
            // 映射子物件
            .tasks([aggregate].getTasks().stream()
                .map(TaskMapper::toDto)
                .collect(Collectors.toList()))
            .build();
    }
    
    // Entity to DTO
    public static TaskDto toDto(Task task) {
        if (task == null) return null;
        
        return TaskDto.builder()
            .taskId(task.getId().value())
            .name(task.getName())
            .state(task.getState().name())
            .build();
    }
    
    // List mapping
    public static List<[Aggregate]Dto> toDtos(List<[Aggregate]> [aggregate]s) {
        if ([aggregate]s == null) return Collections.emptyList();
        
        return [aggregate]s.stream()
            .map([Aggregate]Mapper::toDto)
            .collect(Collectors.toList());
    }
}
```

## 相關文件
- [包結構規範](../coding-standards.md#-包結構規範)
- [錯誤處理原則](../coding-standards.md#-錯誤處理原則)
- [Use Case 範例](../examples/usecase/README.md)