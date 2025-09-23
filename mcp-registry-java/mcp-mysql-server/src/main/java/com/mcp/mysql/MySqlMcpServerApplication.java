package com.mcp.mysql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * MySQL MCP Server 主程式
 */
@SpringBootApplication(scanBasePackages = {
    "com.mcp.common",
    "com.mcp.mysql"
})
@ConfigurationPropertiesScan
public class MySqlMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MySqlMcpServerApplication.class, args);
    }
}