package com.mcp.contextcore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Configuration
 *
 * Configures JDBC connection for SQLite database.
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${contextcore.sqlite.path:./data/contextcore.db}")
    private String databasePath;

    /**
     * Creates SQLite JDBC connection
     */
    @Bean
    public Connection sqliteConnection() throws SQLException {
        log.info("Initializing SQLite database: {}", databasePath);

        // Ensure data directory exists
        File dbFile = new File(databasePath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            log.info("Created data directory: {} (success={})", parentDir.getAbsolutePath(), created);
        }

        // Create SQLite connection
        String jdbcUrl = "jdbc:sqlite:" + databasePath;
        Connection connection = DriverManager.getConnection(jdbcUrl);

        log.info("SQLite connection established: {}", jdbcUrl);
        return connection;
    }
}
