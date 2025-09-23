package com.mcp.common.model;

/**
 * 資料庫連線狀態
 */
public enum ConnectionStatus {
    CREATED("已建立"),
    CONNECTING("連線中"),
    CONNECTED("已連線"),
    DISCONNECTED("已斷線"),
    ERROR("錯誤"),
    TIMEOUT("逾時");

    private final String description;

    ConnectionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}