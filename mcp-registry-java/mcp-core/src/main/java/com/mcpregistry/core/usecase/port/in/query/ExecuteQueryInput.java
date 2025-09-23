package com.mcpregistry.core.usecase.port.in.query;

import java.util.List;

/**
 * 執行查詢的輸入 DTO
 */
public class ExecuteQueryInput {
    public String connectionId;
    public String query;
    public List<Object> parameters;
    public Integer fetchSize; // 可選，用於分頁查詢

    // 預設建構子
    public ExecuteQueryInput() {}

    public ExecuteQueryInput(String connectionId, String query, List<Object> parameters) {
        this.connectionId = connectionId;
        this.query = query;
        this.parameters = parameters;
    }

    public ExecuteQueryInput(String connectionId, String query, List<Object> parameters, Integer fetchSize) {
        this.connectionId = connectionId;
        this.query = query;
        this.parameters = parameters;
        this.fetchSize = fetchSize;
    }
}