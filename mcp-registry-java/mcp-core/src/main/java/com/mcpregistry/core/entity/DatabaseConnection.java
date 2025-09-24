package com.mcpregistry.core.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Database connection aggregate root
 * Manages the lifecycle and state of a single database connection
 */
public class DatabaseConnection {

    private final ConnectionId id;
    private final ConnectionInfo connectionInfo;
    private ConnectionStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private String lastError;

    public DatabaseConnection(ConnectionId id, ConnectionInfo connectionInfo) {
        this.id = Objects.requireNonNull(id, "ConnectionId cannot be null");
        this.connectionInfo = Objects.requireNonNull(connectionInfo, "ConnectionInfo cannot be null");
        this.status = ConnectionStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = this.createdAt;
    }

    /**
     * Mark connection as connected state
     */
    public void markConnected() {
        if (this.status == ConnectionStatus.DISCONNECTED) {
            throw new IllegalStateException("Disconnected connection cannot be reconnected, please create a new connection");
        }
        this.status = ConnectionStatus.CONNECTED;
        this.lastAccessedAt = LocalDateTime.now();
        this.lastError = null;
    }

    /**
     * Mark connection as connection failed
     */
    public void markConnectionFailed(String error) {
        this.status = ConnectionStatus.FAILED;
        this.lastError = Objects.requireNonNull(error, "Error message cannot be null");
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Mark connection as disconnected state
     */
    public void markDisconnected() {
        this.status = ConnectionStatus.DISCONNECTED;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Update last accessed time
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Check if connection is healthy
     */
    public boolean isHealthy() {
        return status == ConnectionStatus.CONNECTED;
    }

    /**
     * Check if connection is available
     */
    public boolean isAvailable() {
        return status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CREATED;
    }

    /**
     * Get connection display information (without sensitive data)
     */
    public String getDisplayInfo() {
        return String.format("%s://%s:%d/%s (%s)",
                connectionInfo.getServerType().getDriverName(),
                connectionInfo.getHost(),
                connectionInfo.getPort(),
                connectionInfo.getDatabase(),
                status.getDisplayName());
    }

    // Getters
    public ConnectionId getId() { return id; }
    public ConnectionInfo getConnectionInfo() { return connectionInfo; }
    public ConnectionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public String getLastError() { return lastError; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConnection that = (DatabaseConnection) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DatabaseConnection{" +
                "id=" + id +
                ", serverType=" + connectionInfo.getServerType() +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}