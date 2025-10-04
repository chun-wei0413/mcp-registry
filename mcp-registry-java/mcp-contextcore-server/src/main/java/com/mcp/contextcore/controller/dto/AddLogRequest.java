package com.mcp.contextcore.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Add Log Request DTO
 *
 * Request object for adding a new log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddLogRequest {

    /**
     * Title of the log entry
     */
    @JsonProperty("title")
    private String title;

    /**
     * Content of the log entry
     */
    @JsonProperty("content")
    private String content;

    /**
     * Tags associated with this log
     */
    @JsonProperty("tags")
    private List<String> tags;

    /**
     * Module this log belongs to
     */
    @JsonProperty("module")
    private String module;

    /**
     * Type of the log entry: feature | bug | decision | note
     */
    @JsonProperty("type")
    private String type;
}
