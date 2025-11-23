package tw.teddysoft.example.io.springboot.config;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import tw.teddysoft.example.plan.entity.PlanEvents;
import tw.teddysoft.example.tag.entity.TagEvents;
import tw.teddysoft.ezddd.data.io.esdb.store.EsdbClientPool;
import tw.teddysoft.ezddd.data.io.esdb.store.EsdbSingleClientPool;
import tw.teddysoft.ezddd.data.io.ezes.store.MessageDataMapper;
import tw.teddysoft.ezddd.data.io.ezes.store.PgMessageDbClient;
import tw.teddysoft.ezddd.entity.DomainEventTypeMapper;
import tw.teddysoft.ezddd.usecase.port.inout.domainevent.DomainEventMapper;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

@Configuration("PlanBootstrapConfiguration")
@AutoConfigureAfter({DataSourceConfig.class})
public class BootstrapConfig {

    private ConfigProperty configProperty;
    private final String ESDB_URL;
    private final int CLIENT_POOL_SIZE;

    @Autowired
    public BootstrapConfig(
            ConfigProperty configProperty,
            @Value("${esdb.url}") String url,
            @Value("${esdb.client.pool.size}") String clientPoolSize){

        this.configProperty = configProperty;
        this.ESDB_URL = url;
        this.CLIENT_POOL_SIZE = Integer.parseInt(clientPoolSize);
    }

    @Bean(name = "esdbClientPoolInPlan", destroyMethod = "dummyDestroy")
    public EsdbClientPool esdbClientPool() {
        if (configProperty.getDataSource().equalsIgnoreCase("ESDB")) {
            return new EsdbSingleClientPool(ESDB_URL);
        }
        else return new EsdbClientPool.NullEsdbMultiClientsPool();
    }


    @Bean(name="domainEventTypeMapperInPlan")
    public DomainEventTypeMapper domainEventTypeMapper() {

        DomainEventTypeMapper domainEventTypeMapper = DomainEventTypeMapper.create();
        PlanEvents.mapper().getMap().forEach((key, value) -> {
            domainEventTypeMapper.put(key, value);
        });
        TagEvents.mapper().getMap().forEach((key, value) -> {
            domainEventTypeMapper.put(key, value);
        });
        MessageDataMapper.setMapper(domainEventTypeMapper);
        DomainEventMapper.setMapper(domainEventTypeMapper);

        return domainEventTypeMapper;
    }

    @Bean(name = "entityManagerInPlan")
    public EntityManager getEntityManager(final @Qualifier("planEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        return entityManagerFactoryBean.getObject().createEntityManager();
    }

    @Bean(name = "pgMessageDbClientInPlan")
    public PgMessageDbClient pgMessageDbClient(final @Qualifier("entityManagerInPlan") EntityManager entityManager) {
        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(PgMessageDbClient.class);
    }
}
