package com.mcpregistry.core.entity;

import java.util.Objects;
import java.util.UUID;

/**
 * 資料庫連線識別碼值對象
 */
public class ConnectionId {
    private final String value;

    private ConnectionId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ConnectionId 不能為空");
        }
        this.value = value.trim();
    }

    public static ConnectionId of(String value) {
        return new ConnectionId(value);
    }

    public static ConnectionId generate() {
        return new ConnectionId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionId that = (ConnectionId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ConnectionId{" + value + "}";
    }
}