package com.mcp.postgresql.controller;

import com.mcp.common.model.ConnectionInfo;
import com.mcp.postgresql.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.ai.mcp.server.annotation.ToolFunction;
import org.springframework.ai.mcp.server.annotation.ToolParameter;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 連線管理 MCP 工具控制器
 */
@Controller
@McpTool
@RequiredArgsConstructor
@Slf4j
public class ConnectionController {

    private final ConnectionService connectionService;

    /**
     * 建立資料庫連線
     */
    @ToolFunction(
        name = "add_connection",
        description = "建立 PostgreSQL 資料庫連線"
    )
    public Mono<Map<String, Object>> addConnection(
            @ToolParameter(name = "connection_id", description = "連線唯一識別碼")
            String connectionId,

            @ToolParameter(name = "host", description = "資料庫主機位址")
            String host,

            @ToolParameter(name = "port", description = "資料庫埠號")
            Integer port,

            @ToolParameter(name = "database", description = "資料庫名稱")
            String database,

            @ToolParameter(name = "user", description = "使用者名稱")
            String user,

            @ToolParameter(name = "password", description = "密碼")
            String password,

            @ToolParameter(name = "pool_size", description = "連線池大小", required = false)
            Integer poolSize) {

        log.info("Adding PostgreSQL connection: {}", connectionId);

        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .connectionId(connectionId)
                .host(host)
                .port(port)
                .database(database)
                .username(user)
                .password(password)
                .poolSize(poolSize != null ? poolSize : 10)
                .build();

        return connectionService.addConnection(connectionInfo)
                .map(result -> Map.of(
                    "success", true,
                    "message", "Connection added successfully",
                    "connection_id", connectionId,
                    "status", result.getStatus().toString()
                ))
                .onErrorReturn(Map.of(
                    "success", false,
                    "message", "Failed to add connection",
                    "connection_id", connectionId
                ));
    }

    /**
     * 測試連線狀態
     */
    @ToolFunction(
        name = "test_connection",
        description = "測試 PostgreSQL 資料庫連線狀態"
    )
    public Mono<Map<String, Object>> testConnection(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId) {

        log.info("Testing PostgreSQL connection: {}", connectionId);

        return connectionService.testConnection(connectionId)
                .map(isHealthy -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "healthy", isHealthy,
                    "message", isHealthy ? "Connection is healthy" : "Connection is unhealthy"
                ))
                .onErrorReturn(Map.of(
                    "success", false,
                    "connection_id", connectionId,
                    "healthy", false,
                    "message", "Connection test failed"
                ));
    }

    /**
     * 列出所有連線
     */
    @ToolFunction(
        name = "list_connections",
        description = "列出所有 PostgreSQL 資料庫連線"
    )
    public Mono<Map<String, Object>> listConnections() {
        log.info("Listing all PostgreSQL connections");

        return connectionService.listConnections()
                .collectList()
                .map(connections -> Map.of(
                    "success", true,
                    "connections", connections,
                    "count", connections.size()
                ))
                .onErrorReturn(Map.of(
                    "success", false,
                    "message", "Failed to list connections"
                ));
    }

    /**
     * 移除連線
     */
    @ToolFunction(
        name = "remove_connection",
        description = "移除 PostgreSQL 資料庫連線"
    )
    public Mono<Map<String, Object>> removeConnection(
            @ToolParameter(name = "connection_id", description = "連線識別碼")
            String connectionId) {

        log.info("Removing PostgreSQL connection: {}", connectionId);

        return connectionService.removeConnection(connectionId)
                .map(removed -> Map.of(
                    "success", true,
                    "connection_id", connectionId,
                    "message", removed ? "Connection removed successfully" : "Connection not found"
                ))
                .onErrorReturn(Map.of(
                    "success", false,
                    "connection_id", connectionId,
                    "message", "Failed to remove connection"
                ));
    }

    /**
     * 健康檢查
     */
    @ToolFunction(
        name = "health_check",
        description = "檢查 PostgreSQL MCP Server 健康狀態"
    )
    public Mono<Map<String, Object>> healthCheck(
            @ToolParameter(name = "connection_id", description = "連線識別碼", required = false)
            String connectionId) {

        log.info("Health check for PostgreSQL MCP Server");

        if (connectionId != null) {
            return testConnection(connectionId);
        }

        return connectionService.healthCheck()
                .map(health -> Map.of(
                    "success", true,
                    "status", "healthy",
                    "server", "PostgreSQL MCP Server",
                    "details", health
                ))
                .onErrorReturn(Map.of(
                    "success", false,
                    "status", "unhealthy",
                    "server", "PostgreSQL MCP Server"
                ));
    }
}