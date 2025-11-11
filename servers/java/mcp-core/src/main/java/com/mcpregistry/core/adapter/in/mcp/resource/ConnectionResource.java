package com.mcpregistry.core.adapter.in.mcp.resource;

import com.mcpregistry.core.entity.DatabaseConnection;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MCP Resource: 連線資源
 *
 * 提供 MCP 協議的連線資源訪問
 * 遵循 Clean Architecture 的 Input Adapter 模式
 */
public class ConnectionResource {

    private final DatabaseConnectionRepository connectionRepository;

    public ConnectionResource(DatabaseConnectionRepository connectionRepository) {
        this.connectionRepository = Objects.requireNonNull(connectionRepository, "ConnectionRepository 不能為空");
    }

    /**
     * MCP Resource: connections
     * 獲取所有連線清單
     *
     * @return MCP 資源內容
     */
    public McpResourceResult getConnections() {
        try {
            List<DatabaseConnection> connections = connectionRepository.findAll();

            List<Map<String, Object>> connectionData = connections.stream()
                .map(this::convertConnectionToMap)
                .collect(Collectors.toList());

            return McpResourceResult.success("資料庫連線清單", connectionData);

        } catch (Exception e) {
            return McpResourceResult.error("獲取連線清單失敗: " + e.getMessage());
        }
    }

    /**
     * MCP Resource: healthy_connections
     * 獲取健康的連線清單
     */
    public McpResourceResult getHealthyConnections() {
        try {
            List<DatabaseConnection> healthyConnections = connectionRepository.findAllHealthy();

            List<Map<String, Object>> connectionData = healthyConnections.stream()
                .map(this::convertConnectionToMap)
                .collect(Collectors.toList());

            return McpResourceResult.success("健康連線清單", connectionData);

        } catch (Exception e) {
            return McpResourceResult.error("獲取健康連線清單失敗: " + e.getMessage());
        }
    }

    /**
     * MCP Resource: connection_details/{connectionId}
     * 獲取特定連線的詳細資訊
     */
    public McpResourceResult getConnectionDetails(String connectionId) {
        try {
            if (connectionId == null || connectionId.trim().isEmpty()) {
                return McpResourceResult.error("連線 ID 不能為空");
            }

            var connectionOpt = connectionRepository.findById(
                com.mcpregistry.core.entity.ConnectionId.of(connectionId)
            );

            if (connectionOpt.isEmpty()) {
                return McpResourceResult.error("連線不存在: " + connectionId);
            }

            DatabaseConnection connection = connectionOpt.get();
            Map<String, Object> detailedData = convertConnectionToDetailedMap(connection);

            return McpResourceResult.success("連線詳細資訊", detailedData);

        } catch (Exception e) {
            return McpResourceResult.error("獲取連線詳情失敗: " + e.getMessage());
        }
    }

    private Map<String, Object> convertConnectionToMap(DatabaseConnection connection) {
        return Map.of(
            "id", connection.getId().getValue(),
            "host", connection.getConnectionInfo().getHost(),
            "port", connection.getConnectionInfo().getPort(),
            "database", connection.getConnectionInfo().getDatabase(),
            "serverType", connection.getConnectionInfo().getServerType().getDisplayName(),
            "status", connection.getStatus().getDisplayName(),
            "isHealthy", connection.isHealthy(),
            "displayInfo", connection.getDisplayInfo(),
            "createdAt", connection.getCreatedAt().toString(),
            "lastAccessedAt", connection.getLastAccessedAt().toString()
        );
    }

    private Map<String, Object> convertConnectionToDetailedMap(DatabaseConnection connection) {
        var basicInfo = convertConnectionToMap(connection);

        // 添加更多詳細資訊
        return Map.of(
            "basic", basicInfo,
            "connectionInfo", Map.of(
                "username", connection.getConnectionInfo().getUsername(),
                "poolSize", connection.getConnectionInfo().getPoolSize(),
                "r2dbcUrl", connection.getConnectionInfo().getR2dbcUrl()
            ),
            "statusDetails", Map.of(
                "status", connection.getStatus().name(),
                "isAvailable", connection.isAvailable(),
                "lastError", connection.getLastError() != null ? connection.getLastError() : "無錯誤"
            ),
            "timestamps", Map.of(
                "createdAt", connection.getCreatedAt().toString(),
                "lastAccessedAt", connection.getLastAccessedAt().toString()
            )
        );
    }

    /**
     * MCP Resource 執行結果的值對象
     */
    public static class McpResourceResult {
        public final boolean isError;
        public final String content;
        public final Object data;

        private McpResourceResult(boolean isError, String content, Object data) {
            this.isError = isError;
            this.content = content;
            this.data = data;
        }

        public static McpResourceResult success(String description, Object data) {
            return new McpResourceResult(false, description, data);
        }

        public static McpResourceResult error(String message) {
            return new McpResourceResult(true, message, null);
        }
    }
}