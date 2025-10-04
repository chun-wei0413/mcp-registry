package io.github.frankli.mcp.contextcore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * ContextCore MCP Application
 *
 * Main entry point for the ContextCore MCP Server
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "io.github.frankli.mcp.contextcore")
public class ContextCoreMCPApplication {

    public static void main(String[] args) {
        log.info("Starting ContextCore MCP Server...");
        SpringApplication.run(ContextCoreMCPApplication.class, args);
        log.info("ContextCore MCP Server started successfully");
    }
}
