package com.mcp.contextcore.config;

import com.mcp.contextcore.controller.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Tools Configuration
 *
 * Registers ContextCore tools as MCP tool callbacks.
 * Each controller handles a specific MCP tool for better separation of concerns.
 */
@Slf4j
@Configuration
public class McpToolsConfiguration {

    /**
     * Registers all ContextCore MCP tool controllers.
     * Spring AI will automatically expose these as MCP tools.
     */
    @Bean
    public ToolCallbackProvider contextCoreToolCallbacks(
            AddLogController addLogController,
            SearchLogsController searchLogsController,
            GetLogController getLogController,
            ListLogsController listLogsController,
            GetProjectContextController getProjectContextController) {

        log.info("Registering ContextCore MCP tools");

        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        addLogController,
                        searchLogsController,
                        getLogController,
                        listLogsController,
                        getProjectContextController
                )
                .build();
    }
}
