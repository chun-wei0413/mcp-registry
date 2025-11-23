package {rootPackage}.io.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import {rootPackage}.common.adapter.out.repository.GenericInMemoryRepository;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;

/**
 * Configuration for InMemory Repository Implementations
 * 
 * This configuration is activated when:
 * - Profile is 'inmemory' or 'test-inmemory'
 * - Property 'repository.type' is set to 'inmemory'
 * 
 * Replace {rootPackage} with your actual package name (e.g., tw.teddysoft.aiscrum)
 * Replace example entities (Product, ProductId) with your domain entities
 */
@Configuration
@Profile({"inmemory", "test-inmemory"})
@ConditionalOnProperty(name = "repository.type", havingValue = "inmemory", matchIfMissing = true)
public class InMemoryRepositoryConfig {

    // ========================================
    // Repository Bean Definitions
    // ========================================
    // Define repository beans for your domain entities here
    // Each aggregate root should have its own repository
    
    // Example: Product Repository
    // @Bean
    // public Repository<Product, ProductId> productRepository(MessageBus<DomainEvent> messageBus) {
    //     return new GenericInMemoryRepository<>(messageBus);
    // }
    
    // Example: Sprint Repository
    // @Bean
    // public Repository<Sprint, SprintId> sprintRepository(MessageBus<DomainEvent> messageBus) {
    //     return new GenericInMemoryRepository<>(messageBus);
    // }
    
    // Example: ProductBacklogItem Repository
    // @Bean
    // public Repository<ProductBacklogItem, PbiId> productBacklogItemRepository(MessageBus<DomainEvent> messageBus) {
    //     return new GenericInMemoryRepository<>(messageBus);
    // }
    
    // Example: ScrumTeam Repository
    // @Bean
    // public Repository<ScrumTeam, ScrumTeamId> scrumTeamRepository(MessageBus<DomainEvent> messageBus) {
    //     return new GenericInMemoryRepository<>(messageBus);
    // }
    
    // ========================================
    // InMemory Projection Implementations
    // ========================================
    // InMemory projections should be defined in UseCaseConfiguration
    // They are typically shared across profiles
    
    // ========================================
    // InMemory Inquiry Implementations
    // ========================================
    // For cross-aggregate queries in InMemory mode
    // Example:
    // @Bean
    // public FindItemsByParentIdInquiry findItemsByParentIdInquiry(
    //         Repository<Item, ItemId> itemRepository) {
    //     return new InMemoryFindItemsByParentIdInquiry(itemRepository);
    // }
    
    // ========================================
    // Notes:
    // ========================================
    // 1. GenericInMemoryRepository automatically publishes domain events to MessageBus
    // 2. Data is stored in a ConcurrentHashMap (thread-safe)
    // 3. Supports soft delete with isDeleted flag
    // 4. No persistence - data is lost when application stops
    // 5. Perfect for unit testing and development
    // 6. Repository interface has only 3 methods: findById(), save(), delete()
    // 7. For more complex queries, use Projection or Inquiry patterns
}