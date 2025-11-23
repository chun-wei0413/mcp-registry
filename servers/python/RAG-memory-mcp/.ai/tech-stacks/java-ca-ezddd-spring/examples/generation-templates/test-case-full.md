# AI Prompt for Test Case Generation with GenericInMemoryRepository and BlockingMessageBus

## Context
When generating test cases for use cases in this ai-kanban project, follow the patterns established in CreateBoardUseCaseTest.java for proper integration with the ezddd framework.

## Test Structure Requirements

### 1. Import Statements
Always include these imports for test cases:
```java
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import [rootPackage].common.adapter.out.repository.GenericInMemoryRepository;
```

### 2. TestContext Pattern
Use a static inner class TestContext with singleton pattern:

```java
static class TestContext {
    private static TestContext instance;
    private GenericInMemoryRepository<[Aggregate], [AggregateId]> [aggregate]Repository;
    private MessageBus<DomainEvent> messageBus;
    private List<DomainEvent> publishedEvents;
    
    private TestContext() {
        publishedEvents = new ArrayList<>();
        
        // Create BlockingMessageBus
        messageBus = new BlockingMessageBus();
        
        // Register a reactor to capture domain events
        messageBus.register(event -> {
            if (event instanceof DomainEvent) {
                publishedEvents.add((DomainEvent) event);
            }
        });
        
        // Create GenericInMemoryRepository with MessageBus
        [aggregate]Repository = new GenericInMemoryRepository<>(
            messageBus,
            [aggregate] -> [AggregateId].valueOf([aggregate].getId())
        );
    }
    
    public static TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
        }
        return instance;
    }
    
    public [CreateUseCaseName]UseCase new[CreateUseCaseName]UseCase() {
        return new [CreateUseCaseName]Service([aggregate]Repository);
    }
    
    public GenericInMemoryRepository<[Aggregate], [AggregateId]> [aggregate]Repository() {
        return [aggregate]Repository;
    }
    
    public List<DomainEvent> getPublishedEvents() {
        return new ArrayList<>(publishedEvents);
    }
    
    public void clearPublishedEvents() {
        publishedEvents.clear();
    }
}
```

### 3. Key Points to Remember

1. **MessageBus Generic Type**: Always use `MessageBus<DomainEvent>` with the generic type parameter, not raw `MessageBus`.

2. **BlockingMessageBus**: Use `tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus` implementation.

3. **Event Capture**: Register a reactor on the messageBus to capture domain events:
   ```java
   messageBus.register(event -> {
       if (event instanceof DomainEvent) {
           publishedEvents.add((DomainEvent) event);
       }
   });
   ```

4. **GenericInMemoryRepository Constructor**: Pass two parameters:
   - `MessageBus` instance
   - Function to extract ID from aggregate (e.g., `board -> BoardId.valueOf(board.getId())`)

5. **Repository Type**: Use `GenericInMemoryRepository<T extends EsAggregateRoot<String, ?>, ID>` which implements `Repository<T, ID>`.

### 4. Test Scenario Pattern

When writing test scenarios:
```java
.And("a [aggregate] created event should be published", env -> {
    List<DomainEvent> events = getContext().getPublishedEvents();
    assertEquals(1, events.size());
    assertTrue(events.get(0) instanceof [Aggregate]Events.[Aggregate]Created);
    [Aggregate]Events.[Aggregate]Created event = ([Aggregate]Events.[Aggregate]Created) events.get(0);
    var input = env.get("input", Create[Aggregate]Input.class);
    assertEquals(input.getId(), event.[aggregate]Id());
    // Assert other event properties
})
```

### 5. Repository Behavior

The GenericInMemoryRepository will:
- Call `messageBus.post()` for each domain event when `save()` or `delete()` is called
- Clear domain events from the aggregate after posting them
- Store aggregates in memory using a HashSet

## Example Usage

When asked to create a test for "CreateWorkflow" use case, apply this pattern:
- Replace `[Aggregate]` with `Workflow`
- Replace `[AggregateId]` with `WorkflowId`
- Replace `[aggregate]` with `workflow`
- Replace `[CreateUseCaseName]` with `CreateWorkflow`

This ensures consistent test structure and proper integration with the ezddd framework's event-driven architecture.