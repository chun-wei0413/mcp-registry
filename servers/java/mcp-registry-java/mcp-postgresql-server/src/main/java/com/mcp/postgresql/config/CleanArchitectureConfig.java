package com.mcp.postgresql.config;

import com.mcpregistry.core.adapter.out.query.MockDatabaseQueryExecutor;
import com.mcpregistry.core.adapter.out.repository.InMemoryDatabaseConnectionRepository;
import com.mcpregistry.core.adapter.out.repository.InMemoryQueryExecutionRepository;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;
import com.mcpregistry.core.usecase.port.out.DatabaseQueryExecutor;
import com.mcpregistry.core.usecase.port.out.QueryExecutionRepository;
import com.mcpregistry.core.usecase.service.AddConnectionService;
import com.mcpregistry.core.usecase.service.ExecuteQueryService;
import com.mcpregistry.core.usecase.port.in.connection.AddConnectionUseCase;
import com.mcpregistry.core.usecase.port.in.query.ExecuteQueryUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clean Architecture dependency injection configuration
 *
 * Configures dependency relationships between layers, following dependency inversion principle
 * Outer layers depend on inner layers, communicating through interfaces (Ports)
 */
@Configuration
public class CleanArchitectureConfig {

    // ================== Output Adapters (Infrastructure Layer) ==================

    @Bean
    public DatabaseConnectionRepository databaseConnectionRepository() {
        return new InMemoryDatabaseConnectionRepository();
    }

    @Bean
    public QueryExecutionRepository queryExecutionRepository() {
        return new InMemoryQueryExecutionRepository();
    }

    @Bean
    public DatabaseQueryExecutor databaseQueryExecutor() {
        return new MockDatabaseQueryExecutor();
    }

    // ================== Use Cases (Application Layer) ==================

    @Bean
    public AddConnectionUseCase addConnectionUseCase(
            DatabaseConnectionRepository connectionRepository,
            DatabaseQueryExecutor queryExecutor) {
        return new AddConnectionService(connectionRepository, queryExecutor);
    }

    @Bean
    public ExecuteQueryUseCase executeQueryUseCase(
            DatabaseConnectionRepository connectionRepository,
            DatabaseQueryExecutor queryExecutor,
            QueryExecutionRepository queryExecutionRepository) {
        return new ExecuteQueryService(connectionRepository, queryExecutionRepository, queryExecutor);
    }

    // ================== Input Adapters (Interface Layer) ==================
    // MCP Tools and Resources will automatically inject the above Use Cases
}