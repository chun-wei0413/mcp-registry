package {rootPackage}.io.springboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import {rootPackage}.common.MyInMemoryMessageBroker;
import {rootPackage}.io.springboot.config.TestDataInitializer;
import tw.teddysoft.ezddd.common.Converter;
import tw.teddysoft.ezddd.data.io.ezes.relay.EzesCatchUpRelay;
import tw.teddysoft.ezddd.data.io.ezes.relay.MessageDbToDomainEventDataConverter;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageData;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageDbClient;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.message.broker.adapter.InMemoryMessageBroker;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Spring Boot Application Main Class Template
 * 
 * This template provides the basic structure for a Spring Boot application
 * using EZDDD framework with support for both InMemory and Outbox patterns.
 * 
 * Replace {rootPackage} with your actual package name (e.g., tw.teddysoft.aiscrum)
 * 
 * Features:
 * - Automatic profile detection (inmemory/outbox)
 * - Virtual thread executor for async processing
 * - Outbox pattern support with PostgreSQL
 * - Test data initialization (optional)
 * - Graceful shutdown handling
 */
@SpringBootApplication(scanBasePackages = "{rootPackage}")
public class AiScrumApp {

//    private String RDB_SCRUM_CHECKPOINT_PATH = "./scrum_checkpoint.txt";

    private static int appInstanceCount = 0;
    private final int appInstanceId;
    private final ExecutorService executor;
    private final MessageBus<DomainEvent> messageBus;
    // Note: Add your own Reactor dependencies here if needed
    // Example: private final MyEventReactor myEventReactor;
    private final PgMessageDbClient pgMessageDbClient;
    private final MessageProducer<DomainEventData> messageProducer;
    private final MyInMemoryMessageBroker messageBroker;
    private final ApplicationContext applicationContext;
    private final JdbcTemplate jdbcTemplate;
    private final EzesCatchUpRelay<DomainEventData> ezesCatchUpRelay;

    @Autowired
    public AiScrumApp(
            @Autowired(required = false) MessageBus<DomainEvent> messageBus,
            // Add your own Reactor dependencies here if needed
            // @Autowired(required = false) MyEventReactor myEventReactor,
            @Autowired(required = false) PgMessageDbClient pgMessageDbClient,
            @Autowired(required = false) MessageProducer<DomainEventData> messageProducer,
            @Autowired(required = false) MyInMemoryMessageBroker messageBroker,
            @Autowired(required = false) EzesCatchUpRelay<DomainEventData> ezesCatchUpRelay,
            ApplicationContext applicationContext,
            @Autowired(required = false) JdbcTemplate jdbcTemplate) {
        this.appInstanceId = ++appInstanceCount;
        System.out.println("===> Creating AiScrumApp instance #" + appInstanceId);

        // Create executor service for async message processing
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        // Assign injected dependencies
        this.messageBus = messageBus;
        // this.myEventReactor = myEventReactor;
        this.pgMessageDbClient = pgMessageDbClient;
        this.messageProducer = messageProducer;
        this.messageBroker = messageBroker;
        this.ezesCatchUpRelay = ezesCatchUpRelay;
        this.applicationContext = applicationContext;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @PostConstruct
    public void init() {
        // Initialize Bootstrap configuration for domain event type mappers
        {rootPackage}.io.springboot.config.BootstrapConfig.initialize();
        System.out.println("===> BootstrapConfig initialized - DomainEventTypeMappers registered");

        // Only initialize outbox-related components if they are available
        if (messageBroker != null && pgMessageDbClient != null && messageProducer != null) {
            executor.execute(messageBroker);
            executor.execute(ezesCatchUpRelay);
            
            // Register your Reactors with the message broker here
            // if (myEventReactor != null) {
            //     messageBroker.register(myEventReactor);
            // }
            
            System.out.println("===> Running in Outbox mode - Message broker and relay started");
        } else {
            System.out.println("===> Running in InMemory mode - Outbox components not initialized");
        }

        // Register Reactors with MessageBus if needed
        // Reactors are typically configured via Spring beans and automatically registered
        // Example:
        // if (myEventReactor != null && messageBus != null) {
        //     System.out.println("===> Registering MyEventReactor");
        //     messageBus.register(myEventReactor);
        // }
        
        System.out.println("===> AiScrumApp initialized with ExecutorService for async processing");
        System.out.println("===> Reactors will be triggered automatically when domain events are published");
        
        // Initialize test data if available
        initializeTestData();
    }

    private void initializeTestData() {
        System.out.println("===> Starting test data initialization from AiScrumApp...");
        
        try {
            // Check if TestDataInitializer bean exists (it won't exist for test profiles)
            String[] beanNames = applicationContext.getBeanNamesForType(TestDataInitializer.class);
            if (beanNames.length == 0) {
                System.out.println("===> TestDataInitializer not available (likely test profile), skipping test data initialization");
                return;
            }
            
            // Get TestDataInitializer bean from Spring context
            TestDataInitializer testDataInitializer = applicationContext.getBean(TestDataInitializer.class);
            
            if (testDataInitializer != null && jdbcTemplate != null) {
                // Call the initTestData method directly and execute the CommandLineRunner
                testDataInitializer.initTestData(jdbcTemplate).run(new String[]{});
                System.out.println("===> Test data initialization completed successfully");
            } else {
                System.out.println("===> TestDataInitializer or JdbcTemplate not available, skipping test data initialization");
            }
        } catch (Exception e) {
            System.err.println("===> Error initializing test data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("===> Shutting down AiScrumApp...");
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        SpringApplication.run(AiScrumApp.class, args);
    }
}