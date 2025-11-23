# Base Test Classes Templates

這些是新專案必須產生的測試基礎類別，提供 Profile-based Testing 支援。

## 🚨 BaseSpringBootTest - Spring Boot 測試基礎類別
# ⚠️ 重要：必須放在 src/test/java 目錄
# 完整路徑：src/test/java/[rootPackage]/test/base/BaseSpringBootTest.java

```java
package [rootPackage].test.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for all Spring Boot tests.
 * 
 * IMPORTANT: Do NOT add @ActiveProfiles here!
 * Profile switching is controlled by:
 * 1. Environment variable SPRING_PROFILES_ACTIVE
 * 2. Maven profile settings
 * 3. Test suite static initializers
 * 
 * This design allows tests to run under different profiles without code changes.
 */
@SpringBootTest
public abstract class BaseSpringBootTest {
    
    // Intentionally empty - provides Spring Boot test context
    // All profile-specific configurations are handled by Spring's profile mechanism
    
}
```

## 🚨 BaseUseCaseTest - Use Case 測試基礎類別
# ⚠️ 重要：必須放在 src/test/java 目錄
# 完整路徑：src/test/java/[rootPackage]/test/base/BaseUseCaseTest.java

```java
package [rootPackage].test.base;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import [rootPackage].common.MyInMemoryMessageBroker;
import tw.teddysoft.ezddd.common.Converter;
import tw.teddysoft.ezddd.data.io.ezes.relay.EzesCatchUpRelay;
import tw.teddysoft.ezddd.data.io.ezes.relay.MessageDbToDomainEventDataConverter;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageData;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageDbClient;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.entity.InternalDomainEvent;
import tw.teddysoft.ezddd.message.broker.adapter.InMemoryMessageBroker;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for UseCase tests with Spring Boot context.
 * 
 * Extends BaseSpringBootTest to provide:
 * - Full Spring dependency injection for UseCases
 * - Profile-switchable repository implementations
 * - Domain event capturing for assertions
 * 
 * This replaces the manual TestContext pattern with real Spring DI.
 * 
 * IMPORTANT: Do NOT use @ActiveProfiles on test classes!
 * The profile is determined by:
 * 1. Environment variable SPRING_PROFILES_ACTIVE
 * 2. Maven profile (-Ptest-inmemory or -Ptest-outbox)
 * 3. Test suite static initializer
 * 
 * Usage:
 * ```java
 * @SpringBootTest
 * @EzFeature
 * @EzFeatureReport
 * public class CreateProductUseCaseTest extends BaseUseCaseTest {
 *     @Autowired
 *     private CreateProductUseCase createProductUseCase;
 *     
 *     @EzScenario
 *     public void should_create_product() {
 *         // Test implementation
 *         // This test will work with both test-inmemory and test-outbox profiles
 *     }
 * }
 * ```
 */
public abstract class BaseUseCaseTest extends BaseSpringBootTest {

    private String TEST_RDB_SCRUM_CHECKPOINT_PATH = "./scrum_checkpoint_test.txt";

    @Autowired
    protected MessageBus<DomainEvent> messageBus;

    @Autowired(required = false)
    @Qualifier("pgMessageDbClient")
    protected PgMessageDbClient pgMessageDbClient;

    @Autowired
    protected MessageProducer<DomainEventData> messageProducer;

    @Autowired
    protected MyInMemoryMessageBroker messageBroker;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    protected ExecutorService executor;

    public FakeEventListener fakeEventListener;

    @BeforeEach
    public void setUpEventCapture() {

        System.out.println("activeProfile = " + activeProfile);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        fakeEventListener = new FakeEventListener();

        if (activeProfile.contains("inmemory")) {
            System.out.println("===> Running in InMemory mode - Outbox components not initialized");
            messageBus.register(event -> {
                if (event != null) {
                    System.out.println("Add event to fakeEventListener: " + event);
                    fakeEventListener.capturedEvents.add(event);
                }
            });
            return;
        }

        if (jdbcTemplate != null && activeProfile.contains("outbox")) {
            // Delete checkpoint file before test
            File checkpointFile = new File(TEST_RDB_SCRUM_CHECKPOINT_PATH);
            if (checkpointFile.exists()) {
                if (checkpointFile.delete()) {
                    System.out.println("Checkpoint file deleted: " + TEST_RDB_SCRUM_CHECKPOINT_PATH);
                } else {
                    System.err.println("Failed to delete checkpoint file: " + TEST_RDB_SCRUM_CHECKPOINT_PATH);
                }
            }
        }

        // Clean up messages table for outbox profile tests
        if (jdbcTemplate != null && activeProfile.contains("outbox")) {
            try {
                // Delete all messages from the message_store.messages table
                jdbcTemplate.execute("DELETE FROM message_store.messages");
                System.out.println("✅ Cleaned up message_store.messages table");
                
                // Reset the global_position sequence to initial value
                jdbcTemplate.execute("ALTER SEQUENCE message_store.messages_global_position_seq RESTART WITH 1");
                System.out.println("✅ Reset global_position sequence to 1");
            } catch (Exception e) {
                System.err.println("⚠️ Could not clean messages table or reset sequence: " + e.getMessage());
                // Continue with test even if cleanup fails
            }

            if (messageBroker != null && pgMessageDbClient != null && messageProducer != null) {
                executor.execute(messageBroker);
                executor.execute(createEzesCatchUpEventRelay(
                        pgMessageDbClient,
                        messageProducer,
                        TEST_RDB_SCRUM_CHECKPOINT_PATH));
                messageBroker.register(fakeEventListener);
            }

        }
    }
    
    @AfterEach
    public void tearDown() {
        // Shutdown executor service properly
        if (executor != null) {
            try {
                executor.shutdownNow();
                // Wait again for tasks to respond to being cancelled
                if (!executor.awaitTermination(20, TimeUnit.MILLISECONDS)) {
                    System.err.println("Executor did not terminate");
                }
            } catch (InterruptedException e) {
                // Re-cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    public class FakeEventListener {

        public List<DomainEvent> capturedEvents = new CopyOnWriteArrayList<>();

        @Subscribe
        public void handleDomainEvent(DomainEventData event) {
            System.out.println("FakeEventListener received DomainEventData: " + event);
            this.capturedEvents.add(DomainEventMapper.toDomain(event));
        }
    }

    private EzesCatchUpRelay<DomainEventData> createEzesCatchUpEventRelay(
            MessageDbClient messageDbClient,
            MessageProducer<DomainEventData> producer,
            String checkpointPath) {

        Converter<MessageData, DomainEventData> converter = new MessageDbToDomainEventDataConverter();
        EzesCatchUpRelay.RelayConfiguration<DomainEventData> configuration = EzesCatchUpRelay.RelayConfiguration.of(
                messageDbClient,
                producer,
                Path.of(checkpointPath),
                converter
        );
        return new EzesCatchUpRelay<>(configuration);
    }

    /**
     * Get all domain events that were published during the test.
     * 
     * @return List of captured domain events
     */
    protected List<DomainEvent> getCapturedEvents() {
        return new ArrayList<>(fakeEventListener.capturedEvents);
    }
    
    /**
     * Clear all captured events.
     * Useful when testing multiple scenarios in one test method.
     */
    protected void clearCapturedEvents() {
        fakeEventListener.capturedEvents.clear();
    }
    
    /**
     * Get the last captured event.
     * 
     * @return The last domain event or null if no events were captured
     */
    protected DomainEvent getLastCapturedEvent() {
        if (fakeEventListener.capturedEvents.isEmpty()) {
            return null;
        }
        return fakeEventListener.capturedEvents.get(fakeEventListener.capturedEvents.size() - 1);
    }
    
    /**
     * Get captured events of a specific type.
     * 
     * @param eventClass The class of events to filter by
     * @param <T> The event type
     * @return List of events of the specified type
     */
    @SuppressWarnings("unchecked")
    protected <T extends DomainEvent> List<T> getCapturedEventsOfType(Class<T> eventClass) {
        return fakeEventListener.capturedEvents.stream()
                .filter(eventClass::isInstance)
                .map(event -> (T) event)
                .toList();
    }
}
```

## 使用說明

### 1. 新專案設置時
當 AI 被要求「產生 BaseUseCaseTest」時，應該：
1. 將 `[rootPackage]` 替換為實際的 package 名稱（從 `.dev/project-config.json` 讀取）
2. 建立正確的目錄結構
3. 產生這兩個類別

### 2. Profile 支援
這些基礎類別支援以下 profiles：
- `test-inmemory`: 使用記憶體內的 Repository 實作
- `test-outbox`: 使用 PostgreSQL + Outbox Pattern

### 3. 重要規範
- **絕對不要在這些類別上加 @ActiveProfiles**
- Profile 由外部控制（環境變數、Maven、Test Suite）
- 這樣設計讓測試可以在不同 profile 下執行而不需修改程式碼

### 4. 依賴關係
BaseUseCaseTest 依賴以下共用類別（必須先產生）：
- MyInMemoryMessageBroker（在 local-utils.md 中定義）
- 各種 ezddd 框架類別（透過 Maven 依賴取得）

## 與其他模板的關係
- 必須先產生 `local-utils.md` 中的共用類別
- 測試類別應該參考 `use-case-test-example.md` 的範例寫法
- 遵循 `test-generation-prompt.md` 的規範