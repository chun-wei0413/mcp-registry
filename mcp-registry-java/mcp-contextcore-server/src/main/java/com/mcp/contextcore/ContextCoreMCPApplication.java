package com.mcp.contextcore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ContextCore MCP Application
 *
 * Main entry point for the ContextCore MCP Server
 */
@Slf4j
@SpringBootApplication
public class ContextCoreMCPApplication {

    public static void main(String[] args) {
        log.info("Starting ContextCore MCP Server...");
        SpringApplication.run(ContextCoreMCPApplication.class, args);
        log.info("ContextCore MCP Server started successfully");
    }
}
