package com.mcpregistry.core.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 資料庫連線聚合根
 * 管理單一資料庫連線的生命週期和狀態
 */
public class DatabaseConnection {

    private final ConnectionId id;
    private final ConnectionInfo connectionInfo;
    private ConnectionStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private String lastError;

    public DatabaseConnection(ConnectionId id, ConnectionInfo connectionInfo) {
        this.id = Objects.requireNonNull(id, "ConnectionId 不能為空");
        this.connectionInfo = Objects.requireNonNull(connectionInfo, "ConnectionInfo 不能為空");
        this.status = ConnectionStatus.CREATED;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = this.createdAt;
    }

    /**
     * 標記連線為已連接狀態
     */
    public void markConnected() {
        if (this.status == ConnectionStatus.DISCONNECTED) {
            throw new IllegalStateException("已斷線的連線無法重新連接，請創建新連線");
        }
        this.status = ConnectionStatus.CONNECTED;
        this.lastAccessedAt = LocalDateTime.now();
        this.lastError = null;
    }

    /**
     * 標記連線為連接失敗
     */
    public void markConnectionFailed(String error) {
        this.status = ConnectionStatus.FAILED;
        this.lastError = Objects.requireNonNull(error, "錯誤訊息不能為空");
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 標記連線為斷線狀態
     */
    public void markDisconnected() {
        this.status = ConnectionStatus.DISCONNECTED;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 更新最後存取時間
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 檢查連線是否健康
     */
    public boolean isHealthy() {
        return status == ConnectionStatus.CONNECTED;
    }

    /**
     * 檢查連線是否可用
     */
    public boolean isAvailable() {
        return status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CREATED;
    }

    /**
     * 獲取連線的顯示資訊（不包含敏感資料）
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