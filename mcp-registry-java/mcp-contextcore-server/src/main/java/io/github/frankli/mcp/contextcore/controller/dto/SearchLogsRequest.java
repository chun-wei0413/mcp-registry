package io.github.frankli.mcp.contextcore.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for searching logs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLogsRequest {
    private String query;
    private Integer limit;
    private List<String> tags;
    private String module;
    private String type;
    private String dateFrom; // ISO 8601 format
    private String dateTo;   // ISO 8601 format
}
