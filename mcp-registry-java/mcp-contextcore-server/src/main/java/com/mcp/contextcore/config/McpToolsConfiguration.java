package com.mcp.contextcore.config;

import com.mcp.contextcore.mcp.ContextCoreTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Tools Configuration
 *
 * Registers ContextCore tools as MCP tool callbacks.
 */
@Slf4j
@Configuration
public class McpToolsConfiguration {

    /**
     * Registers ContextCore tools as MCP tool callbacks.
     * Spring AI will automatically expose these as MCP tools.
     */
    @Bean
    public ToolCallbackProvider contextCoreToolCallbacks(ContextCoreTools contextCoreTools) {
        log.info("Registering ContextCore MCP tools");

        return MethodToolCallbackProvider.builder()
                .toolObjects(contextCoreTools)
                .build();
    }
}
