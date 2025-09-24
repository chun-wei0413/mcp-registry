package com.mcp.postgresql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * PostgreSQL MCP Server main application
 */
@SpringBootApplication(scanBasePackages = {
    "com.mcp.common",
    "com.mcp.postgresql",
    "com.mcpregistry.core"
})
@ConfigurationPropertiesScan
public class PostgreSQLMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostgreSQLMcpServerApplication.class, args);
    }
}