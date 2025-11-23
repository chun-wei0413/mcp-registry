package tw.teddysoft.example.io.springboot.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration("DataSourceConfiguration")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"tw.teddysoft.example", "tw.teddysoft.ezddd.data"},
        entityManagerFactoryRef = "planEntityManagerFactory",
        transactionManagerRef = "planTransactionManager"
)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceConfig {

    public static final String[] ENTITY_PACKAGES = {
            "tw.teddysoft.example.plan.usecase",
            "tw.teddysoft.example.tag.usecase",
            "tw.teddysoft.ezddd.data",
            "tw.teddysoft.ezddd.data.io.ezes.store"};

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.plan")
    public DataSourceProperties planDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.plan.configuration")
    public DataSource planDataSource() {
        return planDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();

    }

    @Bean(name = "planEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean planEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(planDataSource())
                .packages(ENTITY_PACKAGES)
                .build();
    }

    @Bean
    public PlatformTransactionManager planTransactionManager(
            final @Qualifier("planEntityManagerFactory") LocalContainerEntityManagerFactoryBean planEntityManagerFactory) {
        return new JpaTransactionManager(planEntityManagerFactory.getObject());
    }
}
