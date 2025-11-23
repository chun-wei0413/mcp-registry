package {rootPackage}.io.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import {rootPackage}.common.MyInMemoryMessageBroker;
import {rootPackage}.common.MyInMemoryMessageProducer;
import {rootPackage}.common.adapter.out.repository.GenericInMemoryRepository;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.impl.BlockingMessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

/**
 * Spring Configuration for Use Case Layer Beans
 * 
 * This configuration provides:
 * - MessageBus for domain event handling
 * - MessageBroker for async message processing (if using outbox)
 * - MessageProducer for outbox pattern support
 * - Use case service beans
 * - Projection beans (query side)
 * - Reactor beans (event handling)
 * 
 * Repository beans are provided by profile-specific configurations:
 * - InMemoryRepositoryConfig for 'inmemory' profile
 * - OutboxRepositoryConfig for 'outbox' profile
 * 
 * Replace {rootPackage} with your actual package name (e.g., tw.teddysoft.aiscrum)
 */
@Configuration
public class UseCaseConfiguration {

    /**
     * MessageBus for synchronous domain event handling
     * Used by all profiles for in-process event distribution
     */
    @Bean
    public MessageBus<DomainEvent> messageBus() {
        return new BlockingMessageBus<>();
    }

    /**
     * Custom InMemory Message Broker for async message processing
     * Used primarily in outbox profile for event relay
     */
    @Bean
    public MyInMemoryMessageBroker messageBroker() {
        return new MyInMemoryMessageBroker();
    }

    /**
     * Message Producer for publishing domain events
     * Connects to the message broker for event distribution
     */
    @Bean
    public MessageProducer<DomainEventData> messageProducer(MyInMemoryMessageBroker messageBroker) {
        return new MyInMemoryMessageProducer(messageBroker);
    }

    // ========================================
    // Repository Beans Configuration
    // ========================================
    // Repository beans are provided by profile-specific configurations:
    // - InMemoryRepositoryConfig for 'inmemory' profile
    // - OutboxRepositoryConfig for 'outbox' profile
    //
    // Example for InMemory profile:
    // @Bean
    // @Profile({"inmemory", "test-inmemory"})
    // public Repository<Product, ProductId> productRepository(MessageBus<DomainEvent> messageBus) {
    //     return new GenericInMemoryRepository<>(messageBus);
    // }
    
    // ========================================
    // Use Case Service Beans
    // ========================================
    // Define your use case service beans here
    // Example:
    // @Bean
    // public CreateProductUseCase createProductUseCase(Repository<Product, ProductId> productRepository) {
    //     return new CreateProductService(productRepository);
    // }
    //
    // @Bean
    // public GetProductUseCase getProductUseCase(ProductsProjection productsProjection) {
    //     return new GetProductService(productsProjection);
    // }
    //
    // @Bean
    // public DeleteProductUseCase deleteProductUseCase(Repository<Product, ProductId> productRepository) {
    //     return new DeleteProductService(productRepository);
    // }
    
    // ========================================
    // Projection Beans (for Query Side)
    // ========================================
    // Define your projection beans here
    // Example:
    // @Bean
    // @Profile("!outbox")  // InMemory projections not needed for outbox profile
    // public ProductsProjection productsProjection(Repository<Product, ProductId> productRepository) {
    //     InMemoryProductsProjection projection = new InMemoryProductsProjection();
    //     
    //     // Populate store from repository if needed
    //     if (productRepository instanceof GenericInMemoryRepository<Product, ProductId> inMemoryRepo) {
    //         inMemoryRepo.findAll().forEach(product -> {
    //             ProductData data = ProductMapper.toData(product);
    //             projection.save(data);
    //         });
    //     }
    //     
    //     return projection;
    // }
    
    // ========================================
    // Reactor Beans (for Event Handling)
    // ========================================
    // Define your reactor beans here
    // Reactors should extend Reactor<DomainEventData>
    // Example:
    // @Bean
    // public MyEventReactor myEventReactor(
    //         MyInquiry myInquiry,
    //         Repository<MyAggregate, MyId> repository) {
    //     return new MyEventReactorService(myInquiry, repository);
    // }
    
    // ========================================
    // Inquiry Beans (for Cross-Aggregate Queries)
    // ========================================
    // Define your inquiry beans here
    // Example for InMemory profile:
    // @Bean
    // @Profile("inmemory | test-inmemory")
    // public FindItemsByParentIdInquiry inMemoryFindItemsByParentIdInquiry(
    //         Repository<Item, ItemId> itemRepository) {
    //     return new InMemoryFindItemsByParentIdInquiry(itemRepository);
    // }
    //
    // Note: JPA-based inquiries are automatically registered by Spring Data JPA
    // for profiles: prod, dev, outbox, test-outbox, default
    
    // ========================================
    // Archive Beans (for Query Model CRUD)
    // ========================================
    // Define your archive beans here (for cross-BC reference data)
    // Example:
    // @Bean
    // @Profile("!outbox")  // For non-outbox profiles
    // public UserArchive userArchive(UserOrmClient userOrmClient) {
    //     return new JpaUserArchive(userOrmClient);
    // }
}