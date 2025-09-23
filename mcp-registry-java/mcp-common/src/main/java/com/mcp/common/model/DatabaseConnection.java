package com.mcp.common.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 資料庫連線模型
 *
 * 表示一個資料庫連線的完整資訊
 */
public class DatabaseConnection {

    private final String connectionId;
    private final ConnectionInfo connectionInfo;
    private final ConnectionStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private String lastError;

    public DatabaseConnection(String connectionId, ConnectionInfo connectionInfo) {
        this.connectionId = Objects.requireNonNull(connectionId, "Connection ID cannot be null");
        this.connectionInfo = Objects.requireNonNull(connectionInfo, "Connection info cannot be null");
        this.status = ConnectionStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
    }

    public DatabaseConnection(String connectionId, ConnectionInfo connectionInfo, ConnectionStatus status,
                            LocalDateTime createdAt, LocalDateTime lastUsedAt, String lastError) {
        this.connectionId = connectionId;
        this.connectionInfo = connectionInfo;
        this.status = status;
        this.createdAt = createdAt;
        this.lastUsedAt = lastUsedAt;
        this.lastError = lastError;
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void setLastError(String error) {
        this.lastError = error;
    }

    public boolean isHealthy() {
        return status == ConnectionStatus.CONNECTED && lastError == null;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "connectionId", connectionId,
            "host", connectionInfo.getHost(),
            "port", connectionInfo.getPort(),
            "database", connectionInfo.getDatabase(),
            "status", status.name(),
            "createdAt", createdAt.toString(),
            "lastUsedAt", lastUsedAt.toString(),
            "isHealthy", isHealthy(),
            "lastError", lastError != null ? lastError : ""
        );
    }

    // Getters
    public String getConnectionId() {
        return connectionId;
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public String getLastError() {
        return lastError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConnection that = (DatabaseConnection) o;
        return Objects.equals(connectionId, that.connectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId);
    }

    @Override
    public String toString() {
        return "DatabaseConnection{" +
                "connectionId='" + connectionId + '\'' +
                ", host='" + connectionInfo.getHost() + '\'' +
                ", database='" + connectionInfo.getDatabase() + '\'' +
                ", status=" + status +
                ", isHealthy=" + isHealthy() +
                '}';
    }
}