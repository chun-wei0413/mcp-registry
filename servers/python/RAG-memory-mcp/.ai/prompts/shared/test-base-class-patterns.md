# Test Base Class Patterns for Sub-agents

## JUnit 5 + Spring Boot Test Lifecycle

### Execution Order
```
1. @BeforeAll (static) - 類別級別初始化
2. @BeforeEach - 每個測試方法前執行（包含父類別的）
3. Test Method
4. @AfterEach - 每個測試方法後執行（包含父類別的）
5. @AfterAll (static) - 類別級別清理
```

## Base Test Class Design Rules

### Rule 1: Never Call Parent Setup Methods Manually
```java
// ❌ WRONG - Causes duplicate initialization
public class MyTest extends BaseUseCaseTest {
    @BeforeEach
    void setUp() {
        super.setUpEventCapture(); // DON'T DO THIS!
    }
}

// ✅ CORRECT - Let JUnit handle parent methods
public class MyTest extends BaseUseCaseTest {
    @BeforeEach
    void setUp() {
        // Only do test-specific setup
        clearCapturedEvents(); // Use utility methods instead
    }
}
```

### Rule 2: Event Capture Pattern
```java
public abstract class BaseUseCaseTest {
    private FakeEventListener eventListener;

    @BeforeEach
    void setUpEventCapture() {
        // Register once per test
        if (eventListener == null) {
            eventListener = new FakeEventListener();
            messageBus.register(eventListener);
        }
        // Clear events for test isolation
        eventListener.clear();
    }

    @AfterEach
    void tearDownEventCapture() {
        // Optional: unregister if needed
        if (eventListener != null) {
            messageBus.unregister(eventListener);
            eventListener = null;
        }
    }
}
```

### Rule 3: Test Isolation Methods
```java
// Provide utility methods for test-specific needs
protected void clearCapturedEvents() {
    if (fakeEventListener != null) {
        fakeEventListener.capturedEvents.clear();
    }
}

protected void clearRepository() {
    if (repository instanceof GenericInMemoryRepository) {
        ((GenericInMemoryRepository) repository).clear();
    }
}
```

## Common Anti-patterns to Avoid

### Anti-pattern 1: Manual Parent Method Calls
```java
// ❌ NEVER DO THIS
@BeforeEach
void setUp() {
    super.setUp();           // Duplicate execution
    super.setUpEventCapture(); // Duplicate registration
}
```

### Anti-pattern 2: Multiple Event Registrations
```java
// ❌ WRONG - Registers multiple times
@BeforeEach
void setUp() {
    messageBus.register(event -> capturedEvents.add(event));
    messageBus.register(fakeEventListener); // Duplicate!
}
```

### Anti-pattern 3: Not Clearing State Between Tests
```java
// ❌ WRONG - Tests affect each other
@Test
void test1() {
    // Creates data but doesn't clean up
    repository.save(entity);
}

@Test
void test2() {
    // Sees data from test1!
    assertEquals(0, repository.count()); // FAILS
}
```

## Correct Test Setup Pattern

```java
@SpringBootTest
public class CreateProductBacklogItemServiceTest extends BaseUseCaseTest {

    @Autowired
    private CreateProductBacklogItemUseCase useCase;

    @Autowired
    private Repository<ProductBacklogItem, PbiId> repository;

    @BeforeEach
    void setUp() {
        // Only test-specific setup, no parent calls

        // 1. Clear repository for isolation
        if (repository instanceof GenericInMemoryRepository) {
            ((GenericInMemoryRepository) repository).clear();
        }

        // 2. Clear captured events (utility method from parent)
        clearCapturedEvents();

        // 3. Test-specific data setup if needed
        // setupTestData();
    }

    @Test
    void should_do_something() {
        // Test implementation
        // Parent's @BeforeEach already ran automatically
    }
}
```

## Integration Checklist for Sub-agents

When generating test classes:

1. **Never generate `super.setUpXXX()` calls in @BeforeEach**
2. **Use utility methods** like `clearCapturedEvents()` instead
3. **Let JUnit handle parent class lifecycle methods**
4. **Clear state** (repository, events) for test isolation
5. **Don't register event listeners** - base class handles it

## Spring Boot Test Context Caching

### Context Cache Key Components
- Test class
- @ActiveProfiles
- @TestPropertySource
- @ContextConfiguration
- @MockBean definitions

### Cache Invalidation
- Different profiles = different context
- Different properties = different context
- @DirtiesContext = forces new context

### Best Practice
```java
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseSpringBootTest {
    // Reuse context within class, new context for each class
}
```