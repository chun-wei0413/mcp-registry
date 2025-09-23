package com.mcpregistry.core.entity;

import java.util.Objects;

/**
 * MCP Server 識別碼值對象
 * 不可變且具有值語義
 */
public class ServerId {
    private final String value;

    private ServerId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ServerId 不能為空");
        }
        this.value = value.trim();
    }

    public static ServerId of(String value) {
        return new ServerId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerId serverId = (ServerId) o;
        return Objects.equals(value, serverId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ServerId{" + value + "}";
    }
}