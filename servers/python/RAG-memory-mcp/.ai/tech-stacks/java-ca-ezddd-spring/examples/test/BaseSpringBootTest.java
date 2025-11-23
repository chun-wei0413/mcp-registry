package tw.teddysoft.aiscrum.test.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.context.annotation.Import;
import tw.teddysoft.aiscrum.test.TestInMemoryRepositoryConfiguration;
import tw.teddysoft.aiscrum.test.TestOutboxRepositoryConfiguration;
import tw.teddysoft.aiscrum.testconfig.TestcontainersConfiguration;

/**
 * Base class for all Spring Boot tests that can switch between InMemory and Outbox modes.
 * 
 * This is the foundation for profile-switchable tests that allows:
 * - UseCase tests to use real Spring dependency injection
 * - Controller tests to use real UseCase implementations
 * - Integration tests to test end-to-end flows
 * 
 * Profile configuration:
 * - Default: test-inmemory (set via application-test.yml)
 * - Suite override: Test suites set System.setProperty("spring.profiles.active", profile)
 * - Runtime override: Can be overridden via -Dspring.profiles.active=test-outbox
 * 
 * IMPORTANT: 
 * - NO @ActiveProfiles annotation to allow dynamic profile switching
 * - Profile is determined by application-test.yml or system properties
 * - DO NOT add @Tag annotations - tests should be switchable via Spring profiles only
 * 
 * @author AI-SCRUM Team
 */
@SpringBootTest(
    classes = tw.teddysoft.aiscrum.io.springboot.AiScrumApp.class,
    properties = {
        "aiscrum.test-data.enabled=false",  // Disable test data initialization
        "spring.jpa.show-sql=false",        // Reduce log noise
        "logging.level.org.springframework.web=WARN",
        "logging.level.org.hibernate=WARN"
    }
)
@Import({TestInMemoryRepositoryConfiguration.class, TestOutboxRepositoryConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseSpringBootTest {
    
    /**
     * This base class provides:
     * 1. Full Spring Boot context initialization
     * 2. Profile-based repository switching (InMemory vs Outbox)
     * 3. Real dependency injection (no mocks unless explicitly added)
     * 4. Transaction management support
     * 
     * Tests extending this class can:
     * - Test real UseCase implementations
     * - Test real Controller to UseCase flows
     * - Test complete end-to-end scenarios
     * 
     * The actual repository implementation is determined by the active profile:
     * - test-inmemory: Uses GenericInMemoryRepository (fast, no DB)
     * - test-outbox: Uses OutboxRepository with PostgreSQL
     */
    
    // Subclasses can @Autowired any beans they need
    // No default mocks - tests get real implementations
}