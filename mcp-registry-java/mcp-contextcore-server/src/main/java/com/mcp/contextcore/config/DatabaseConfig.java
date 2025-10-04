package com.mcp.contextcore.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * Database Configuration
 *
 * Configures R2DBC connection and database initialization for SQLite.
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.r2dbc.url}")
    private String databaseUrl;

    /**
     * Initializes the database schema on startup
     */
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        log.info("Initializing database: {}", databaseUrl);

        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();

        // Add schema initialization script if exists
        try {
            ClassPathResource schemaResource = new ClassPathResource("schema.sql");
            if (schemaResource.exists()) {
                log.info("Found schema.sql, executing database initialization");
                populator.addPopulators(new ResourceDatabasePopulator(schemaResource));
            } else {
                log.info("No schema.sql found, skipping database initialization");
            }
        } catch (Exception e) {
            log.warn("Error checking for schema.sql: {}", e.getMessage());
        }

        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
