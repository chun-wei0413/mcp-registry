package com.mcp.postgresql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * PostgreSQL MCP Server 主程式
 */
@SpringBootApplication(scanBasePackages = {
    "com.mcp.common",
    "com.mcp.postgresql"
})
@ConfigurationPropertiesScan
public class PostgreSqlMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostgreSqlMcpServerApplication.class, args);
    }
}