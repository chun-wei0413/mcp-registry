# AI Coding Guide for AI-Plan System

> 🤖 This guide is specifically designed for AI assistants (LLMs) to effectively work with this codebase.

## Quick Start

### 1. Essential Reading Order
1. **CODING-STANDARDS.md** - 🚨 集中的編碼規範（優先閱讀）
2. **This file** (AI-CODING-GUIDE.md) - Overview and quick reference
3. **TEMPLATE-USAGE-GUIDE.md** - 範本選擇與使用決策指南 🆕
4. **CLAUDE.md** - Project-specific rules and conventions
5. **design.md** - Architecture patterns and templates
6. **Relevant aggregate spec** in `.dev/specs/[aggregate]/`

### 2. Project Overview
- **Purpose**: Todo List application with advanced DDD architecture
- **Tech Stack**: Java 21, Spring Boot, DDD, Event Sourcing, CQRS
- **Code Stats**: 155 Java files, 17,003 lines
- **Test Coverage**: 163 tests, BDD style with ezSpec

### 3. Key Principles
- ✅ **DO**: Follow existing patterns, use EAGER loading, write tests first
- ❌ **DON'T**: Generate ezddd/ezspec classes, use LAZY loading, skip validation

## Architecture Layers

Clean Architecture 分層設計，詳見 [examples/](./examples/INDEX.md)

```
┌─────────────────────────────────────────────────────────┐
│                    IO Layer (Spring Boot)                │
├─────────────────────────────────────────────────────────┤
│                  Adapter Layer (In/Out)                  │
├─────────────────────────────────────────────────────────┤
│                 Use Case Layer (Service)                 │
├─────────────────────────────────────────────────────────┤
│                Entity Layer (Domain Model)               │
└─────────────────────────────────────────────────────────┘
```

### Package Structure Rules
```
src/main/java/tw/teddysoft/aiplan/
└── [aggregate]/                     # Feature/Aggregate name (e.g., plan, tag)
    ├── entity/                      # Domain entities and events
    │   ├── [Aggregate].java        # Aggregate root
    │   ├── [Aggregate]Events.java  # Domain events
    │   └── [ValueObject].java      # Value objects
    ├── usecase/
    │   ├── port/
    │   │   ├── in/                 # Input ports (interfaces)
    │   │   │   └── [UseCase]UseCase.java
    │   │   └── out/                # Output ports
    │   │       ├── [Aggregate]Data.java  # JPA entities
    │   │       └── projection/     # Query projections
    │   └── service/                # ⚠️ ALL Service implementations MUST go here!
    │       ├── [UseCase]Service.java     # Implements use case interface
    │       ├── [Query]Service.java       # Query service implementations
    │       └── [Reactor]Service.java     # Reactor implementations
    └── adapter/
        ├── in/                      # Input adapters (Controllers)
        └── out/                     # Output adapters (Repositories)
```

## Code Generation Workflow

### Step 1: Understand the Task
```bash
# Check for task definition
cat .dev/tasks/task-XXX.json

# Understand the aggregate
cat .dev/specs/[aggregate]/entity/[aggregate]-spec.md
```

### Step 2: Generate Code Following Patterns
1. **For Use Cases**: See `examples/usecase/`
2. **For Tests**: See `examples/test/`
3. **For Entities**: See existing aggregates in `src/main/java/*/entity/`

### Step 3: Validate Your Generation
```bash
# Compile check
mvn compile

# Run specific test
mvn test -Dtest=YourGeneratedTest

# Check all tests still pass
mvn test
```

## Common Patterns Reference

### Use Case Pattern
```java
// Interface
public interface CreateTaskUseCase extends Command<CreateTaskUseCase.CreateTaskInput, CqrsOutput> {
    class CreateTaskInput implements Input {
        public String planId;
        public String projectName;
        public String name;
        
        public static CreateTaskInput create() {
            return new CreateTaskInput();
        }
    }
}

// Implementation (MUST be in [aggregate].usecase.service package)
// 注意：不要加 @Service 或 @Component 註解！(2025-08-17 更新)
public class CreateTaskService implements CreateTaskUseCase {
    private final Repository<Plan, PlanId> repository;
    
    public CreateTaskService(Repository<Plan, PlanId> repository) {
        requireNotNull("Repository", repository);
        this.repository = repository;
    }
    
    @Override
    public CqrsOutput execute(CreateTaskInput input) {
        // Implementation
    }
}

// 在 UseCaseConfiguration 中註冊為 Bean
@Configuration
public class UseCaseConfiguration {
    @Bean
    public CreateTaskUseCase createTaskUseCase(Repository<Plan, PlanId> repository) {
        return new CreateTaskService(repository);
    }
}
```

### Test Pattern (ezSpec)
```java
@EzFeature
public class CreateTaskUseCaseTest {
    static Feature feature = Feature.New("Create Task");
    
    @EzScenario
    public void create_task_successfully() {
        feature.newScenario()
            .Given("a plan exists", env -> {
                // Setup
            })
            .When("user creates a task", env -> {
                // Action
            })
            .Then("task should be created", env -> {
                // Assertion
            })
            .Execute();
    }
}
```

## Important Constraints

### 1. External Dependencies (NEVER Generate)
- `tw.teddysoft.ezddd.*` - Event Sourcing framework
- `tw.teddysoft.ezspec.*` - BDD test framework  
- `tw.teddysoft.ucontract.*` - Contract framework

### 2. JPA Rules
- Always use `fetch = FetchType.EAGER`
- Use `@ElementCollection` for simple collections
- Avoid complex `@ManyToMany` relationships

### 3. Event Sourcing Rules
- All state changes emit Domain Events
- Events must be registered in `BootstrapConfig`
- Use Repository pattern with OutboxRepository

## Debugging Tips

### Common Errors and Solutions

1. **"Unsupported event for getting mapping"**
   - Solution: Register event in `BootstrapConfig.java`

2. **"LazyInitializationException"**
   - Solution: Add `fetch = FetchType.EAGER` to JPA annotation

3. **"null value in column 'stream_name'"**
   - Solution: Set stream name in mapper: `data.setStreamName(entity.getStreamName())`
   - See [Mapper Examples](./examples/mapper/)

## Learning Path

- **Basic patterns**: See `examples/usecase/` for CRUD operations
- **DDD patterns**: See `examples/aggregate/` for domain models
- **Advanced patterns**: See `examples/projection/` for CQRS queries
- **Event Sourcing**: Study actual implementation in `src/main/java/*/entity/`

## Task Execution Guide

When given a task file (`.dev/tasks/task-XXX.json`):

1. **Read** the task requirements carefully
2. **Check** existing similar implementations
3. **Generate** code following the patterns
4. **Test** your implementation
5. **Update** the task file with results

```json
{
  "results": [{
    "timestamp": "2025-07-31T10:00:00+08:00",
    "status": "done",
    "summary": "Successfully implemented CreateTag use case",
    "outputFiles": [
      "CreateTagUseCase.java",
      "CreateTagService.java",
      "CreateTagUseCaseTest.java"
    ]
  }]
}
```

## 框架參考 (Framework Reference)

### ezddd Framework
- **Package**: `tw.teddysoft.ezddd`
- **用途**: 簡化 Event Sourcing 和 DDD 實作
- **核心類別**:
  - `EsAggregateRoot<ID, E>` - Event Sourcing Aggregate 基礎類別
  - `Entity<ID>` - Entity 泛型介面（Aggregate 內部實體必須實作）
  - `ValueObject` - Value Object 標記介面（值物件必須實作）
  - `DomainEvent` - 領域事件基礎類別
  - `Repository<T, ID>` - Repository 泛型介面
  - `MessageBus` - 事件發布介面
  - `Command<I, O>` - Command Use Case 介面
  - `Query<I, O>` - Query Use Case 介面
  - `CqrsOutput` - Command 標準輸出
  - `ExitCode` - 執行結果狀態碼

### ezSpec Framework
- **Package**: `tw.teddysoft.ezspec`
- **用途**: BDD (行為驅動開發) 測試框架
- **主要註解**:
  - `@EzFeature` - 標記測試類別為功能測試
  - `@EzScenario` - 標記測試方法為場景測試
- **測試結構**: Given-When-Then 格式
- **使用範例**: 參見 [測試範例](./examples/test-example.md)

### ucontract Framework
- **Package**: `tw.teddysoft.ucontract`
- **用途**: Design by Contract 實作
- **主要類別**: `Contract`
- **常用方法**:
  - `Contract.requireNotNull(param, "message")` - 非空檢查
  - `Contract.require(condition, "message")` - 前置條件
  - `Contract.ensure(condition, "message")` - 後置條件
  - `Contract.invariant(condition, "message")` - 不變條件
- **使用範例**: 主要在 Aggregate 建構子和業務方法中使用

### Spring Boot Dependencies
- **版本**: 3.x
- **核心依賴**:
  - Spring Web
  - Spring Data JPA
  - Spring Validation
  - Spring Test
- **配置位置**: `io.springboot.config`

## 外部依賴處理原則

**重要**: 以下套件來自外部函式庫，絕對不要自行創建這些類別：
1. `tw.teddysoft.ezddd.*` - ezddd 框架
2. `tw.teddysoft.ezspec.*` - ezSpec 測試框架
3. `tw.teddysoft.ucontract.*` - ucontract 契約框架

如果遇到找不到類別的編譯錯誤：
1. 檢查 Maven 依賴配置
2. 確認私有 Repository 認證設定
3. 執行 `mvn clean install`

## Code Generation Checklist

選擇正確的範本前，請確認：
1. **任務類型**: Command (修改) vs Query (查詢)
2. **涉及層級**: Domain → UseCase → Adapter → Interface
3. **範本選擇**: 參考 [TEMPLATE-USAGE-GUIDE.md](./TEMPLATE-USAGE-GUIDE.md)
4. **依賴關係**: DTO ↔ Domain ↔ Persistence

常見範本組合：
- **創建功能**: CreateXxxUseCase + Service + DTO + Controller
- **查詢功能**: GetXxxUseCase + Projection + DTO
- **複雜查詢**: FindXxxByYyyInquiry（用於 Reactor 中的跨聚合查詢）
- **軟刪除**: XxxArchive + ArchivedXxx DTO（用於審計和歷史記錄）
- **持久化**: XxxData (Entity) + XxxMapper + Repository

## Need Help?

1. **Pattern not clear?** Check `examples/` 目錄
2. **Which template to use?** Check [TEMPLATE-USAGE-GUIDE.md](./TEMPLATE-USAGE-GUIDE.md)
3. **Framework usage?** Check Framework Reference section above
4. **Examples needed?** Check [TEMPLATE-INDEX.md](./examples/TEMPLATE-INDEX.md)
5. **Architecture question?** Read [Examples Index](./examples/INDEX.md)
6. **Still stuck?** Check [FAQ](./FAQ.md)

Remember: When in doubt, follow existing patterns in the codebase!