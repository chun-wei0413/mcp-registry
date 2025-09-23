package com.mcpregistry.core.entity;

/**
 * 查詢類型枚舉
 */
public enum QueryType {
    SELECT("查詢", true),
    INSERT("插入", false),
    UPDATE("更新", false),
    DELETE("刪除", false),
    DDL("資料定義", false),
    OTHER("其他", false);

    private final String displayName;
    private final boolean isReadOnly;

    QueryType(String displayName, boolean isReadOnly) {
        this.displayName = displayName;
        this.isReadOnly = isReadOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isModifying() {
        return !isReadOnly;
    }
}