# AI Prompt for Reactor Use Case Generation

## Context
When generating Reactor use cases in this ai-kanban project, follow the established patterns for event-driven architecture with Clean Architecture and the ezddd framework.

## Important Implementation Notes
Based on actual implementation experience:

1. **Reactor Interface Method**: The Reactor interface from ezddd uses `execute(DomainEvent event)` method (per ADR-018)
2. **Reactor Interface Definition**: Must extend `Reactor<DomainEvent>`, NOT `Reactor<DomainEventData>` or just `Reactor`
3. **MessageBus Type**: Use `MessageBus<DomainEvent>` with generic type parameter
4. **BlockingMessageBus**: Use for synchronous testing - it doesn't have a `subscribe()` method, use `register()` instead
5. **Event Type Checking**: Use Java pattern matching with `instanceof` for type safety
6. **Spring Boot Registration**: See `.dev/specs/pbi/usecase/reactor/register-reactor-for-in-memory-repository-example.java`

## What is a Reactor?
According to UBIQUITOUS-LANGUAGE.md, a Reactor is an event handler in the Use Case Layer with common uses:
- Execute cross-aggregate eventual consistency
- Project Read Models in CQRS architecture (Projector)
- Forward events to frontend via WebSocket
- Transform Internal Domain Events to External Domain Events

## File Structure Pattern

For a Reactor named "[Action][Target]Reactor", generate the following files:

### 1. Reactor Interface
Location: `src/main/java/tw/teddysoft/aiscrum/[feature]/usecase/reactor/[Action][Target]Reactor.java`

```java
package tw.teddysoft.aiscrum.[feature].usecase.reactor;

import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor;

public interface [Action][Target]Reactor extends Reactor<DomainEvent> {
}
```

### 2. Reactor Service Implementation
Location: `src/main/java/tw/teddysoft/aikanban/[feature]/usecase/service/[Action][Target]Service.java`

```java
package [rootPackage].[feature].usecase.service;


// Import any other necessary classes

import static tw.teddysoft.ucontract.Contract.requireNotNull;

public class [Action][Target]Service implements [Action][Target]Reactor {

    // Inject necessary use cases or services
    private final [TargetUseCase] [targetUseCase];

    public [Action][Target]Service([TargetUseCase] [targetUseCase]) {
        requireNotNull("[TargetUseCase]", [targetUseCase]);
        this.[targetUseCase] = [targetUseCase];
    }

    @Override
    public void execute(Object event) {
        requireNotNull("Event", event);
        
        if (event instanceof [Source]Events.[EventType] [eventVar]) {
            // Extract data from event
            [TargetInput] input = new [TargetInput]();
            input.set[Field1]([eventVar].[field1]());
            input.set[Field2]([eventVar].[field2]());
            // Set other required fields
            
            // Execute target use case
            [targetUseCase].execute(input);
        }
        // Can handle multiple event types if needed
    }
}
```

### 3. Reactor Test
Location: `src/test/java/tw/teddysoft/aikanban/[feature]/usecase/[Action][Target]ReactorTest.java`

```java
package tw.teddysoft.aikanban.[feature].usecase;

import tw.teddysoft.aikanban.[feature].entity.*;
import tw.teddysoft.aikanban.[feature].usecase.port.in.reactor.[Action][Target]Reactor;
import tw.teddysoft.aikanban.[feature].usecase.service.[Action][Target]Service;
import [rootPackage].common.entity.DateProvider;
import [rootPackage].common.adapter.out.repository.GenericInMemoryRepository;
import tw.teddysoft.aikanban.[sourcefeature].entity.[Source]Events;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.EzFeatureReport;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;
import tw.teddysoft.ezspec.visitor.PlainTextReport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EzFeature
@EzFeatureReport
public class [Action][Target]ReactorTest {
    
    static String FEATURE_NAME = "[Action] [Target]";
    static Feature feature = Feature.New(FEATURE_NAME);
    
    @BeforeAll
    static void beforeAll() {
        feature.initialize();
    }
    
    @BeforeEach
    void setUp() {
        TestContext.reset();
    }
    
    @EzScenario
    public void [event_type]_triggers_[action]() {
        
        feature.newScenario()
                .Given("necessary entities exist", env -> {
                    // Set up any required entities
                    // Save to repositories
                    // Clear events
                })
                .And("a reactor is registered", env -> {
                    [Action][Target]Reactor reactor = getContext().newReactor();
                    getContext().messageBus().register(reactor);
                    
                    env.put("reactor", reactor);
                })
                .When("the triggering event occurs", env -> {
                    // Create the event that triggers the reactor
                    [Source]Events.[EventType] event = new [Source]Events.[EventType](
                        // Event parameters
                    );
                    
                    // Post event to message bus
                    getContext().messageBus().post(event);
                    
                    env.put("event", event);
                })
                .Then("the expected action should occur", env -> {
                    // Verify the reactor's action was executed
                    // Check state changes
                    // Verify any new events published
                })
                .And("expected events should be published", env -> {
                    List<DomainEvent> events = getContext().getPublishedEvents();
                    
                    // Verify specific events were published as result
                })
                .Execute();
    }
    
    @EzScenario
    public void reactor_ignores_non_relevant_events() {
        
        feature.newScenario()
                .Given("a reactor is registered", env -> {
                    [Action][Target]Reactor reactor = getContext().newReactor();
                    getContext().messageBus().register(reactor);
                    
                    env.put("reactor", reactor);
                })
                .When("a non-relevant event is posted", env -> {
                    // Create a different type of event
                    // Post to message bus
                })
                .Then("no action should occur", env -> {
                    // Verify no state changes
                    // Verify no unexpected events published
                })
                .Execute();
    }
    
    @AfterAll
    static void afterAll() {
        PlainTextReport report = new PlainTextReport();
        feature.accept(report);
        System.out.println(report.toString());
    }
    
    private TestContext getContext() {
        return TestContext.getInstance();
    }
    
    static class TestContext {
        private static TestContext instance;
        // Declare necessary repositories
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
            
            // Create repositories with MessageBus
            // Initialize necessary services
        }
        
        public static TestContext getInstance() {
            if (instance == null) {
                instance = new TestContext();
            }
            return instance;
        }
        
        public static void reset() {
            instance = null;
        }
        
        public [Action][Target]Reactor newReactor() {
            // Create necessary use cases
            // Return new reactor instance
        }
        
        public MessageBus<DomainEvent> messageBus() {
            return messageBus;
        }
        
        public List<DomainEvent> getPublishedEvents() {
            return new ArrayList<>(publishedEvents);
        }
        
        public void clearPublishedEvents() {
            publishedEvents.clear();
        }
    }
}
```

## Key Implementation Points

### 1. Interface Design
- Reactor interface extends `tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor`
- Place in `port.in.reactor` package to distinguish from commands and queries
- The interface itself is empty - just extends Reactor marker interface

### 2. Service Implementation
- Implements the `execute(Object event)` method from Reactor interface
- Use pattern matching (`instanceof`) to handle specific event types
- Inject necessary use cases or services via constructor
- Apply Contract Programming with `requireNotNull()`

### 3. Event Handling Pattern
```java
@Override
public void execute(Object event) {
    requireNotNull("Event", event);
    
    if (event instanceof SpecificEventType specificEvent) {
        // Handle the specific event
        // Extract data from event
        // Call appropriate use cases or services
    }
    // Can handle multiple event types if needed
}
```

### 4. Testing Strategy
- Use BlockingMessageBus for synchronous testing
- Register reactor with MessageBus
- Post events and verify expected actions
- Test both positive cases (handling relevant events) and negative cases (ignoring irrelevant events)

### 5. Common Reactor Patterns

#### Cross-Aggregate Consistency
```java
// Listen to EventA from AggregateA
// Update AggregateB accordingly
if (event instanceof AggregateAEvents.EventA eventA) {
    UpdateAggregateBInput input = new UpdateAggregateBInput();
    input.setId(eventA.relatedBId());
    input.setSomeField(eventA.someData());
    updateAggregateBUseCase.execute(input);
}
```

**Real Example - NotifyBoardToCommitWorkflow:**
```java
if (event instanceof WorkflowEvents.WorkflowCreated workflowCreated) {
    CommitWorkflowInput input = new CommitWorkflowInput();
    input.setBoardId(workflowCreated.boardId());
    input.setWorkflowId(workflowCreated.workflowId());
    input.setOrder(0); // Default order for newly created workflow
    
    commitWorkflowUseCase.execute(input);
}
```

#### Read Model Projection
```java
// Listen to domain events
// Update read model/view
if (event instanceof EntityEvents.Created created) {
    ReadModel readModel = new ReadModel();
    readModel.setId(created.id());
    readModel.setName(created.name());
    readModelRepository.save(readModel);
}
```

#### Event Transformation
```java
// Listen to internal event
// Publish external event
if (event instanceof InternalEvent internal) {
    ExternalEvent external = new ExternalEvent(
        internal.id(),
        internal.getData()
    );
    externalEventPublisher.publish(external);
}
```

## Naming Conventions

1. **Interface**: `[Action][Target]Reactor`
   - Examples: `NotifyBoardToCommitWorkflowReactor`, `UpdateReadModelReactor`

2. **Implementation**: `[Action][Target]Service`
   - Examples: `NotifyBoardToCommitWorkflowService`, `UpdateReadModelService`

3. **Test**: `[Action][Target]ReactorTest`
   - Examples: `NotifyBoardToCommitWorkflowReactorTest`, `UpdateReadModelReactorTest`

## When to Use Reactors

Use Reactors when:
1. Need to maintain consistency across aggregates
2. Building read models in CQRS
3. Integrating with external systems via events
4. Implementing side effects that shouldn't be in domain logic

Don't use Reactors when:
1. Logic belongs in the aggregate itself
2. Synchronous response is required
3. Simple CRUD operations without event handling

## Integration with MessageBus

In production:
```java
// Register reactor with MessageBus
messageBus.register(reactor);

// MessageBus will automatically call reactor.execute() 
// when matching events are posted
```

In tests:
```java
// Use BlockingMessageBus for synchronous testing
MessageBus<DomainEvent> messageBus = new BlockingMessageBus();
messageBus.register(reactor);
messageBus.post(event); // Reactor executes immediately
```

## Common Pitfalls to Avoid

1. **Wrong Method Name**: Use `execute(Object event)`, not `handle()` or other names
2. **Missing Type Check**: Always check event type with `instanceof` before casting
3. **Wrong MessageBus Type**: Use `MessageBus<DomainEvent>` with generic parameter
4. **Incorrect Registration**: Use `messageBus.register()`, not `subscribe()`
5. **Missing Null Checks**: Always use `requireNotNull()` for parameters

## Verification Checklist

- [ ] Reactor interface extends `tw.teddysoft.ezddd.usecase.port.in.interactor.Reactor`
- [ ] Service implements `execute(Object event)` method
- [ ] Uses pattern matching: `if (event instanceof EventType eventVar)`
- [ ] Injects required use cases via constructor
- [ ] Uses Contract Programming (`requireNotNull`)
- [ ] Test uses `BlockingMessageBus` and `GenericInMemoryRepository`
- [ ] Test verifies both positive case (handles event) and negative case (ignores other events)
- [ ] Events are properly captured and verified in tests