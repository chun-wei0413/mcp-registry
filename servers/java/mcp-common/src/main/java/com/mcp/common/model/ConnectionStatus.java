package com.mcp.common.model;

/**
 * Database connection status
 */
public enum ConnectionStatus {
    CREATED("Created"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected"),
    ERROR("Error"),
    TIMEOUT("Timeout");

    private final String description;

    ConnectionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}