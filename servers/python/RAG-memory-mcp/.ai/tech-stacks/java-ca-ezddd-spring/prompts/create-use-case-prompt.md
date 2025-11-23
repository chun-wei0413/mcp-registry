# Prompt Template: Create Use Case

## Input Format
```yaml
use_case_name: CreateTag
aggregate: Tag
description: Create a new tag for organizing tasks
input_fields:
  - planId: String (required)
  - name: String (required, max 50 chars)
  - color: String (required, hex color)
business_rules:
  - Tag name must be unique within a plan
  - Color must be valid hex format (#RRGGBB)
  - Plan must exist and not be deleted
```

## Expected Output

Generate the following files:

### 1. Use Case Interface
Path: `src/main/java/tw/teddysoft/example/[aggregate]/usecase/port/in/[UseCase]UseCase.java`

```java
package tw.teddysoft.example.[aggregate].usecase.port.in;

import tw.teddysoft.ezddd.cqrs.usecase.CqrsOutput;
import tw.teddysoft.ezddd.cqrs.usecase.command.Command;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Input;

public interface [UseCase]UseCase extends Command<[UseCase]UseCase.[UseCase]Input, CqrsOutput> {
    
    class [UseCase]Input implements Input {
        // public fields based on input_fields
        
        public static [UseCase]Input create() {
            return new [UseCase]Input();
        }
    }
}
```

### 2. Use Case Service Implementation
Path: `src/main/java/tw/teddysoft/example/[aggregate]/usecase/service/[UseCase]Service.java`

⚠️ **IMPORTANT**: 
- Service implementations MUST be in the `[aggregate].usecase.service` package
- NOT in `[aggregate].usecase.port.in` 
- NOT in `[aggregate].usecase.[usecasename]`
- NOT in any other location!
- NO EXCEPTIONS - All services go in the service package!

```java
package tw.teddysoft.example.[aggregate].usecase.service;

// imports...

public class [UseCase]Service implements [UseCase]UseCase {
    
    private final Repository<[Aggregate], [Aggregate]Id> repository;
    
    public [UseCase]Service(Repository<[Aggregate], [Aggregate]Id> repository) {
        requireNotNull("Repository", repository);
        this.repository = repository;
    }
    
    @Override
    public CqrsOutput execute([UseCase]Input input) {
        // 1. Validate input
        // 2. Load aggregate
        // 3. Execute domain logic
        // 4. Save aggregate
        // 5. Return output
    }
}
```

### 3. Test Case
Path: `src/test/java/tw/teddysoft/aiplan/[aggregate]/usecase/[UseCase]UseCaseTest.java`

```java
@EzFeature
@EzFeatureReport
public class [UseCase]UseCaseTest {
    
    static Feature feature = Feature.New("[UseCase] Use Case");
    
    @EzScenario
    public void [use_case_snake_case]_successfully() {
        // BDD test following pattern
    }
    
    @EzScenario
    public void [use_case_snake_case]_fails_when_[condition]() {
        // Error scenario test
    }
}
```

## Validation Checklist
- [ ] All required fields have validation
- [ ] Business rules are enforced
- [ ] Domain events are published
- [ ] Error cases are handled
- [ ] Tests cover success and failure paths