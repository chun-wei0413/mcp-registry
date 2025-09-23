package com.mcpregistry.core.entity;

import java.util.Objects;
import java.util.UUID;

/**
 * 查詢執行識別碼值對象
 */
public class QueryId {
    private final String value;

    private QueryId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("QueryId 不能為空");
        }
        this.value = value.trim();
    }

    public static QueryId of(String value) {
        return new QueryId(value);
    }

    public static QueryId generate() {
        return new QueryId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryId queryId = (QueryId) o;
        return Objects.equals(value, queryId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "QueryId{" + value + "}";
    }
}