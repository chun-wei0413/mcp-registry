package com.mcp.postgresql.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * PostgreSQL 資料庫配置
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.mcp.postgresql.repository")
@ConfigurationProperties(prefix = "mcp.postgresql")
public class PostgreSqlConfig extends AbstractR2dbcConfiguration {

    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        // 這裡會被 Spring Boot 的自動配置覆蓋
        // 實際的連線工廠由 application.yml 中的 spring.r2dbc 配置決定
        return super.connectionFactory();
    }
}