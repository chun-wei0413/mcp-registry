package tw.teddysoft.aiscrum.test.base;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import tw.teddysoft.aiscrum.common.MyInMemoryMessageBroker;
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
 * 
 * @author AI-SCRUM Team
 */
public abstract class BaseUseCaseTest extends BaseSpringBootTest {

    private String TEST_RDB_SCRUM_CHECKPOINT_PATH = "./scrum_checkpoint_test.txt";

    @Autowired
    protected MessageBus<DomainEvent> messageBus;

    @Autowired(required = false)
    @Qualifier("pgMessageDbClientInScrum")
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
                    System.out.println("Add event to  fakeEventListener: " + event);
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
                // Wait for tasks to complete
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

    public class FakeEventListener{

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
//        return new ArrayList<>(capturedEvents);
        return new ArrayList<>(fakeEventListener.capturedEvents);
    }
    
    /**
     * Clear all captured events.
     * Useful when testing multiple scenarios in one test method.
     */
    protected void clearCapturedEvents() {
        //capturedEvents.clear();
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