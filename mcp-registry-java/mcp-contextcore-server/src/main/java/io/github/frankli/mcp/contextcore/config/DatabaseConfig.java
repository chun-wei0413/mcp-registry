package io.github.frankli.mcp.contextcore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Configuration
 *
 * Configures SQLite connection
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${contextcore.sqlite.path:./data/contextcore.db}")
    private String dbPath;

    @Bean
    public Connection sqliteConnection() throws SQLException {
        log.info("Initializing SQLite connection: path={}", dbPath);

        // Ensure parent directory exists
        java.io.File dbFile = new java.io.File(dbPath);
        java.io.File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("Created database directory: {}", parentDir.getAbsolutePath());
            }
        }

        // Create SQLite connection
        String url = "jdbc:sqlite:" + dbPath;
        Connection connection = DriverManager.getConnection(url);

        // Enable foreign keys
        connection.createStatement().execute("PRAGMA foreign_keys = ON");

        log.info("SQLite connection established successfully");
        return connection;
    }
}
