package com.mcpregistry.core.entity;

/**
 * 查詢執行狀態枚舉
 */
public enum QueryStatus {
    PENDING("待執行"),
    EXECUTING("執行中"),
    COMPLETED("已完成"),
    FAILED("執行失敗"),
    CANCELLED("已取消");

    private final String displayName;

    QueryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRunning() {
        return this == EXECUTING;
    }

    public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}