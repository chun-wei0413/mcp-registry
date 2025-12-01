package com.mcpregistry.core.entity;

/**
 * 資料庫連線狀態枚舉
 */
public enum ConnectionStatus {
    CREATED("已創建"),
    CONNECTED("已連接"),
    FAILED("連接失敗"),
    DISCONNECTED("已斷線");

    private final String displayName;

    ConnectionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isHealthy() {
        return this == CONNECTED;
    }

    public boolean isAvailable() {
        return this == CONNECTED || this == CREATED;
    }
}