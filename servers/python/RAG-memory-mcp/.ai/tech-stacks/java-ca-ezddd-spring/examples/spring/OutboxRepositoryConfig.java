package {rootPackage}.io.springboot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import {rootPackage}.common.MyInMemoryMessageBroker;
import {rootPackage}.common.MyInMemoryMessageProducer;
// Import your ORM clients here
// import {rootPackage}.io.springboot.config.orm.ProductOrmClient;
// Import your entity and data classes here
// import {rootPackage}.product.entity.Product;
// import {rootPackage}.product.entity.ProductId;
// import {rootPackage}.product.usecase.port.ProductMapper;
// import {rootPackage}.product.usecase.port.out.ProductData;

import tw.teddysoft.ezddd.common.Converter;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeerAdapter;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore;
import tw.teddysoft.ezddd.data.io.ezes.relay.EzesCatchUpRelay;
import tw.teddysoft.ezddd.data.io.ezes.relay.MessageDbToDomainEventDataConverter;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageData;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageDbClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxStoreAdapter;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;

import java.nio.file.Path;

/**
 * Configuration for Outbox Repository Implementations
 * 
 * This configuration is activated when:
 * - Profile is 'outbox' or 'test-outbox'
 * 
 * Replace {rootPackage} with your actual package name (e.g., tw.teddysoft.aiscrum)
 * Replace example entities (Product, ProductId) with your domain entities
 * 
 * IMPORTANT: For each aggregate root, you need:
 * 1. An OrmClient class in io.springboot.config.orm package
 * 2. A Mapper class with inner OutboxMapper implementation
 * 3. A repository bean definition in this configuration
 */
@Configuration
@Profile({"outbox", "test-outbox"})
public class OutboxRepositoryConfig {

    @Autowired
    @Qualifier("pgMessageDbClient")
    private PgMessageDbClient pgMessageDbClient;

    @Autowired
    private MessageProducer<DomainEventData> messageProducer;

    // ========================================
    // Outbox Store Configuration
    // ========================================
    
    @Bean
    public OutboxStore outboxStore() {
        EzOutboxClient<DomainEventData> ezOutboxClient = new EzOutboxClient<>(pgMessageDbClient);
        return new EzOutboxStoreAdapter(ezOutboxClient);
    }

    // ========================================
    // Repository Bean Definitions
    // ========================================
    // Define repository beans for your domain entities here
    // Each aggregate root should have its own repository with Outbox support
    
    // Example: Product Repository with Outbox
    // @Bean
    // @Primary
    // public Repository<Product, ProductId> productRepository(ProductOrmClient productOrmClient) {
    //     OutboxRepositoryPeerAdapter<Product, ProductId, ProductData> peer = 
    //         new OutboxRepositoryPeerAdapter<>(
    //             productOrmClient,
    //             new ProductMapper.Mapper(),
    //             outboxStore()
    //         );
    //     return new OutboxRepository<>(peer);
    // }
    
    // Example: Sprint Repository with Outbox
    // @Bean
    // @Primary
    // public Repository<Sprint, SprintId> sprintRepository(SprintOrmClient sprintOrmClient) {
    //     OutboxRepositoryPeerAdapter<Sprint, SprintId, SprintData> peer = 
    //         new OutboxRepositoryPeerAdapter<>(
    //             sprintOrmClient,
    //             new SprintMapper.Mapper(),
    //             outboxStore()
    //         );
    //     return new OutboxRepository<>(peer);
    // }
    
    // Example: ProductBacklogItem Repository with Outbox
    // @Bean
    // @Primary
    // public Repository<ProductBacklogItem, PbiId> productBacklogItemRepository(
    //         ProductBacklogItemOrmClient productBacklogItemOrmClient) {
    //     OutboxRepositoryPeerAdapter<ProductBacklogItem, PbiId, ProductBacklogItemData> peer = 
    //         new OutboxRepositoryPeerAdapter<>(
    //             productBacklogItemOrmClient,
    //             new ProductBacklogItemMapper.Mapper(),
    //             outboxStore()
    //         );
    //     return new OutboxRepository<>(peer);
    // }

    // ========================================
    // IMPLEMENTATION CHECKLIST
    // ========================================
    // For each aggregate root in your domain:
    // 
    // 1. Create OrmClient class:
    //    - Location: {rootPackage}.io.springboot.config.orm.{Entity}OrmClient
    //    - Extends: SpringJpaClient<{Entity}Data, {EntityId}>
    //    - Annotate with: @Component
    //
    // 2. Create Mapper class with inner OutboxMapper:
    //    - Location: {rootPackage}.{aggregate}.usecase.port.{Entity}Mapper
    //    - Contains: static class Mapper implements OutboxMapper<{Entity}, {Entity}Data>
    //    - Must be inner class (ADR-019)
    //
    // 3. Create Data class:
    //    - Location: {rootPackage}.{aggregate}.usecase.port.out.{Entity}Data
    //    - Annotate with: @Entity, @Table
    //    - Mark transient fields with @Transient: domainEventDatas, streamName
    //
    // 4. Add repository bean in this configuration:
    //    - Use OutboxRepositoryPeerAdapter
    //    - Wrap with OutboxRepository
    //    - Annotate with @Bean and @Primary
    //
    // 5. Test your repository:
    //    - Create test class extending ezSpec
    //    - Test save, findById, delete operations
    //    - Verify domain events are persisted
    // ========================================
}