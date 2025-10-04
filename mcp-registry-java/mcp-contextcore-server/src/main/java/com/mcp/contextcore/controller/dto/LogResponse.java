package com.mcp.contextcore.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcp.contextcore.domain.entity.Log;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Log Response DTO
 *
 * Response object for log entry information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponse {

    /**
     * Unique identifier for the log entry
     */
    @JsonProperty("id")
    private String id;

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
     * Type of the log entry
     */
    @JsonProperty("type")
    private String type;

    /**
     * Timestamp when the log was created
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Timestamp when the log was created
     */
    @JsonProperty("created_at")
    private Instant createdAt;

    /**
     * Timestamp when the log was last updated
     */
    @JsonProperty("updated_at")
    private Instant updatedAt;

    /**
     * Creates a LogResponse from a Log entity
     */
    public static LogResponse from(Log log) {
        return LogResponse.builder()
                .id(log.getId())
                .title(log.getTitle())
                .content(log.getContent())
                .tags(log.getTags())
                .module(log.getModule())
                .type(log.getType().getValue())
                .timestamp(log.getTimestamp())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
