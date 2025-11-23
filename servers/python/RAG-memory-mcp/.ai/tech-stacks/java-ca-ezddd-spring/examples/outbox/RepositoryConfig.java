package tw.teddysoft.example.io.springboot.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import tw.teddysoft.example.io.springboot.config.orm.PlanOrmClient;
import tw.teddysoft.example.io.springboot.config.orm.TagOrmClient;
import tw.teddysoft.example.plan.entity.Plan;
import tw.teddysoft.example.plan.entity.PlanId;
import tw.teddysoft.example.plan.usecase.port.out.PlanData;
import tw.teddysoft.example.tag.entity.Tag;
import tw.teddysoft.example.tag.entity.TagId;
import tw.teddysoft.ezddd.data.adapter.repository.es.EsRepositoryPeerAdapter;
import tw.teddysoft.ezddd.data.adapter.repository.es.EventStore;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxRepositoryPeerAdapter;
import tw.teddysoft.ezddd.data.adapter.repository.outbox.OutboxStore;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxClient;
import tw.teddysoft.ezddd.data.io.ezoutbox.EzOutboxStoreAdapter;
import tw.teddysoft.ezddd.entity.DomainEvent;
import tw.teddysoft.ezddd.message.broker.adapter.out.producer.InMemoryMessageProducer;
import tw.teddysoft.ezddd.message.broker.adapter.out.producer.KafkaMessageProducer;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventData;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageBus;
import tw.teddysoft.ezddd.usecase.port.inout.messaging.MessageProducer;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.outbox.OutboxRepository;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.RepositoryPeer;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.es.EsRepository;
import tw.teddysoft.ezddd.usecase.port.out.repository.impl.es.EventStoreData;
import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
import tw.teddysoft.ezddd.data.io.esdb.store.*;
import tw.teddysoft.example.plan.usecase.port.PlanMapper;
import tw.teddysoft.example.tag.usecase.port.TagMapper;
import tw.teddysoft.example.tag.usecase.port.out.TagData;

import javax.validation.BootstrapConfiguration;
import java.util.List;
import java.util.Properties;

@Configuration("PlanRepositoryInjection")
@EnableConfigurationProperties(value = ConfigProperty.class)
@AutoConfigureAfter({DataSourceConfig.class, BootstrapConfiguration.class})
public class RepositoryConfig {

    @Value("$plan_internal_queue.kafka.topic}")
    private String BOARD_BC_TOPIC;

    private final DomainEventTypeMapper domainEventTypeMapper;

    private ConfigProperty configProperty;
    private PgMessageDbClient pgMessageDbClient;
    private EsdbClientPool esdbClientPool;
    private PlanOrmClient planOrmStoreClient;
    private TagOrmClient tagOrmStoreClient;

    @Autowired
    public RepositoryConfig(
            ConfigProperty configProperty,
            @Qualifier("domainEventTypeMapperInPlan") DomainEventTypeMapper domainEventTypeMapper,
            PlanOrmClient planOrmStoreClient,
            TagOrmClient tagOrmStoreClient,
            @Qualifier("pgMessageDbClientInPlan") PgMessageDbClient pgMessageDbClient,
            EsdbClientPool esdbClientPool) {

        this.configProperty = configProperty;
        this.domainEventTypeMapper = domainEventTypeMapper;
        this.pgMessageDbClient = pgMessageDbClient;
        this.planOrmStoreClient = planOrmStoreClient;
        this.tagOrmStoreClient = tagOrmStoreClient;
        this.esdbClientPool = esdbClientPool;
    }

    @Bean
    public Repository<Plan, PlanId> planRepository() {
          return new OutboxRepository<>(new OutboxRepositoryPeerAdapter<>(planOutboxStore()), PlanMapper.newMapper());

//        return  switch (configProperty.getDataSource().toUpperCase()){
//            case "RDB" ->
//                new OutboxRepository<>(new OutboxRepositoryPeerAdapter<>(planOutboxStore()), PlanMapper.newMapper());
//            case "ESDB" ->
//                new EsRepository<>(esAggregateStoreAdapter(), Plan.class, Plan.CATEGORY);
//            default -> throw new RuntimeException("Unknown datasource: " + configProperty.getDataSource());
//        };
    }

    @Bean
    public Repository<Tag, TagId> tagRepository() {
        return new OutboxRepository<>(new OutboxRepositoryPeerAdapter<>(tagOutboxStore()), TagMapper.newMapper());
    }


    @Bean
    public OutboxStore<PlanData, String> planOutboxStore() {
        return EzOutboxStoreAdapter.createOutboxStore(planOutboxClient());
    }

    @Bean
    public EzOutboxClient<PlanData, String> planOutboxClient() {
        return new EzOutboxClient<>(planOrmStoreClient, pgMessageDbClient);
    }

    @Bean
    public OutboxStore<TagData, String> tagOutboxStore() {
        return EzOutboxStoreAdapter.createOutboxStore(tagOutboxClient());
    }

    @Bean
    public EzOutboxClient<TagData, String> tagOutboxClient() {
        return new EzOutboxClient<>(tagOrmStoreClient, pgMessageDbClient);
    }

    @Bean
    public RepositoryPeer<EventStoreData, String> esAggregateStoreAdapter() {
        return new EsRepositoryPeerAdapter(eventStore());
    }

    @Bean
    public EventStore eventStore() {

        return EzOutboxStoreAdapter.createUnmodifiableEventStore(unmodifiableOutboxClient());
//        return  switch (configProperty.getDataSource().toUpperCase()){
//            case "RDB" ->
//                    EzOutboxStoreAdapter.createUnmodifiableEventStore(unmodifiableOutboxClient());
//            case "ESDB" ->
//                    new EsdbStoreAdapter(esdbClientPool);
//            default -> throw new RuntimeException("Unknown event store: " + configProperty.getDataSource());
//        };
    }

    @Bean
    public EzOutboxClient unmodifiableOutboxClient() {
        return new EzOutboxClient<>(null, pgMessageDbClient);
    }


}
