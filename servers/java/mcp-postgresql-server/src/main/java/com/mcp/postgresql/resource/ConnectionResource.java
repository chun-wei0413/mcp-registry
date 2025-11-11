package com.mcp.postgresql.resource;

import com.mcp.common.mcp.McpResource;
import com.mcp.common.mcp.McpResourceResult;
import com.mcp.common.model.ConnectionInfo;
import com.mcp.postgresql.service.DatabaseConnectionService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * PostgreSQL 連線資源
 *
 * 提供 MCP 連線資源:
 * - connections: 所有連線列表
 * - healthy_connections: 健康連線列表
 * - connection_details/{id}: 特定連線詳情
 */
@Component
public class ConnectionResource implements McpResource {

    private final DatabaseConnectionService connectionService;

    public ConnectionResource(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public String getResourceUri() {
        return "postgresql://connections";
    }

    @Override
    public String getDescription() {
        return "PostgreSQL 連線資源，提供連線狀態和詳細資訊";
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public McpResourceResult getContent(Map<String, Object> parameters) {
        try {
            String resourceType = (String) parameters.get("type");
            String connectionId = (String) parameters.get("connectionId");

            return switch (resourceType != null ? resourceType : "connections") {
                case "connections" -> getAllConnections();
                case "healthy_connections" -> getHealthyConnections();
                case "connection_details" -> getConnectionDetails(connectionId);
                default -> McpResourceResult.error("不支援的資源類型: " + resourceType);
            };

        } catch (Exception e) {
            return McpResourceResult.error("獲取連線資源失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取所有連線列表
     */
    private McpResourceResult getAllConnections() {
        var connections = connectionService.getAllConnections();

        var connectionList = connections.values().stream()
            .map(this::connectionToMap)
            .collect(Collectors.toList());

        return McpResourceResult.success(
            "PostgreSQL 連線列表",
            Map.of(
                "connections", connectionList,
                "totalCount", connectionList.size(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ),
            getMimeType()
        );
    }

    /**
     * 獲取健康連線列表
     */
    private McpResourceResult getHealthyConnections() {
        var connections = connectionService.getAllConnections();

        var healthyConnections = connections.values().stream()
            .filter(connection -> {
                try {
                    return connectionService.testConnection(connection.getConnectionId());
                } catch (Exception e) {
                    return false;
                }
            })
            .map(this::connectionToMap)
            .collect(Collectors.toList());

        return McpResourceResult.success(
            "PostgreSQL 健康連線列表",
            Map.of(
                "healthyConnections", healthyConnections,
                "healthyCount", healthyConnections.size(),
                "totalCount", connections.size(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ),
            getMimeType()
        );
    }

    /**
     * 獲取特定連線詳情
     */
    private McpResourceResult getConnectionDetails(String connectionId) {
        if (connectionId == null || connectionId.trim().isEmpty()) {
            return McpResourceResult.error("Connection ID 不能為空");
        }

        var connections = connectionService.getAllConnections();
        ConnectionInfo connection = connections.get(connectionId);

        if (connection == null) {
            return McpResourceResult.error("連線不存在: " + connectionId);
        }

        // 測試連線健康狀態
        boolean isHealthy = false;
        String healthStatus = "unknown";
        try {
            isHealthy = connectionService.testConnection(connectionId);
            healthStatus = isHealthy ? "healthy" : "unhealthy";
        } catch (Exception e) {
            healthStatus = "error: " + e.getMessage();
        }

        Map<String, Object> details = connectionToMap(connection);
        details.put("isHealthy", isHealthy);
        details.put("healthStatus", healthStatus);
        details.put("detailsRetrievedAt", java.time.LocalDateTime.now().toString());

        return McpResourceResult.success(
            "PostgreSQL 連線詳情",
            details,
            getMimeType()
        );
    }

    /**
     * 將 ConnectionInfo 轉換為 Map
     */
    private Map<String, Object> connectionToMap(ConnectionInfo connection) {
        return Map.of(
            "connectionId", connection.getConnectionId(),
            "host", connection.getHost(),
            "port", connection.getPort(),
            "database", connection.getDatabase(),
            "username", connection.getUsername(),
            "poolSize", connection.getPoolSize(),
            "readOnly", connection.getReadOnly(),
            "status", connection.getStatus().name(),
            "createdAt", connection.getCreatedAt().toString()
        );
    }
}