package com.mcp.mysql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * MySQL MCP Server main application class
 */
@SpringBootApplication(scanBasePackages = {
    "com.mcp.common",
    "com.mcp.mysql",
    "com.mcpregistry.core"
})
@ConfigurationPropertiesScan
public class MySQLMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MySQLMcpServerApplication.class, args);
    }
}