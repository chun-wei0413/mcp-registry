# DTO 範本 (Data Transfer Object Templates)

本目錄包含資料傳輸物件 (DTO) 的設計範本，用於在不同層之間傳遞資料。

## 📁 目錄內容

- **PlanDto.java** - 基本 DTO 範本
  - 展示標準的 DTO 結構
  - Fluent setter pattern
  - 基本資料類型處理

- **ProjectDto.java** - 巢狀 DTO 範本
  - 展示 DTO 之間的組合關係
  - 父子關係的處理
  - 集合的初始化

- **TaskDto.java** - 複雜 DTO 範本
  - 包含各種資料類型
  - 集合和列舉的處理
  - Optional 欄位的設計

## 🎯 DTO 設計原則

### 1. 職責單一
DTO 只負責資料傳輸，不包含業務邏輯：
```java
public class PlanDto {
    private String id;
    private String name;
    
    // ✅ 純粹的 getter/setter
    public String getId() { return id; }
    
    // ❌ 不應包含業務邏輯
    public boolean isValid() { 
        return name != null && !name.isEmpty(); 
    }
}
```

### 2. 不依賴領域模型
DTO 不應該直接包含 Entity 或 Value Object：
```java
// ❌ 錯誤：包含領域物件
public class TaskDto {
    private Task task;  // 不應該包含 Entity
    private TaskId taskId;  // 不應該包含 Value Object
}

// ✅ 正確：只包含基本類型
public class TaskDto {
    private String taskId;  // 使用基本類型
    private String name;
}
```

### 3. Fluent Setter Pattern
使用 fluent setter 提升可讀性：
```java
TaskDto task = new TaskDto()
    .setId("task-1")
    .setName("Implement feature")
    .setStatus("PENDING");
```

## 📝 DTO vs Entity vs Value Object

| 特性 | DTO | Entity | Value Object |
|-----|-----|---------|--------------|
| 用途 | 資料傳輸 | 業務實體 | 業務概念 |
| 可變性 | 可變 | 可變 | 不可變 |
| 身份識別 | 無 | 有 (ID) | 無 |
| 業務邏輯 | 無 | 有 | 有 |
| 驗證 | 基本驗證 | 業務規則 | 建構時驗證 |

## 🏗️ DTO 結構模式

### 1. 基本結構
```java
public class BasicDto {
    // 私有欄位
    private String id;
    private String name;
    
    // 預設建構子
    public BasicDto() {
    }
    
    // Getter
    public String getId() {
        return id;
    }
    
    // Fluent Setter
    public BasicDto setId(String id) {
        this.id = id;
        return this;
    }
}
```

### 2. 集合處理
```java
public class CollectionDto {
    private List<ItemDto> items;
    
    // 初始化集合避免 null
    public CollectionDto() {
        this.items = new ArrayList<>();
    }
    
    // 提供便利方法
    public CollectionDto addItem(ItemDto item) {
        this.items.add(item);
        return this;
    }
}
```

### 3. Optional 欄位
```java
public class OptionalFieldDto {
    private String requiredField;
    private String optionalField;  // 可能為 null
    private List<String> tags;     // 永不為 null
    
    public OptionalFieldDto() {
        this.tags = new ArrayList<>();  // 避免 null 集合
    }
}
```

## 🚀 使用建議

### 1. 命名規範
- DTO 類別名稱以 `Dto` 結尾
- 使用業務術語而非技術術語
- 保持與領域模型的對應關係

### 2. 序列化考量
```java
// 如果需要 JSON 序列化
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SerializableDto {
    @JsonProperty("plan_id")
    private String planId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;
}
```

### 3. 驗證策略
```java
// 使用 Bean Validation
public class ValidatedDto {
    @NotNull
    @Size(min = 1, max = 100)
    private String name;
    
    @Email
    private String email;
}
```

## ⚠️ 常見錯誤

### 1. 貧血 DTO
```java
// ❌ 只有 public 欄位，沒有封裝
public class AnemicDto {
    public String id;
    public String name;
}
```

### 2. 過度設計
```java
// ❌ DTO 不需要繼承
public class OverEngineeredDto extends BaseDto 
    implements Serializable, Cloneable {
    // 過度複雜
}
```

### 3. 循環引用
```java
// ❌ 避免循環引用
public class ParentDto {
    private List<ChildDto> children;
}

public class ChildDto {
    private ParentDto parent;  // 循環引用
}
```

## 📊 DTO 使用流程

```
Controller → DTO → UseCase → Entity → Repository
    ↑                                        ↓
    └──────────── DTO ← Mapper ←─────────────┘
```

## 💡 最佳實踐

1. **保持簡單** - DTO 應該是簡單的資料容器
2. **避免繼承** - 使用組合而非繼承
3. **不可變優先** - 考慮使用 record (Java 14+)
4. **明確轉換** - 使用 Mapper 進行轉換
5. **版本相容** - 考慮 API 版本控制

## 📚 相關資源
- [Mapper 範本](../mapper/README.md)
- [UseCase 範本](../usecase/README.md)
- [Controller 範本](../controller/README.md)
- [Martin Fowler - Data Transfer Object](https://martinfowler.com/eaaCatalog/dataTransferObject.html)