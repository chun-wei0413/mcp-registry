package io.github.frankli.mcp.contextcore.controller.dto;

import io.github.frankli.mcp.contextcore.domain.entity.Log;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for log data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponse {
    private String id;
    private String title;
    private String content;
    private List<String> tags;
    private String module;
    private String type;
    private Instant timestamp;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Converts domain Log to LogResponse
     */
    public static LogResponse from(Log log) {
        return LogResponse.builder()
                .id(log.getId())
                .title(log.getTitle())
                .content(log.getContent())
                .tags(log.getTags())
                .module(log.getModule())
                .type(log.getType() != null ? log.getType().getValue() : null)
                .timestamp(log.getTimestamp())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
