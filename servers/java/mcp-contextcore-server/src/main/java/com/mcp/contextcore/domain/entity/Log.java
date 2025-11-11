package com.mcp.contextcore.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Log Domain Entity
 *
 * Represents a development log entry in the ContextCore system.
 * This is a pure domain object with business logic and validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log {

    /**
     * Unique identifier for the log entry
     */
    private String id;

    /**
     * Title of the log entry
     */
    private String title;

    /**
     * Content of the log entry
     */
    private String content;

    /**
     * Tags associated with this log (e.g., ["auth", "backend"])
     */
    private List<String> tags;

    /**
     * Module this log belongs to (e.g., "authentication")
     */
    private String module;

    /**
     * Type of the log entry: feature | bug | decision | note
     */
    private LogType type;

    /**
     * Timestamp when the log was created
     */
    private Instant timestamp;

    /**
     * Timestamp when the log was created (same as timestamp for new logs)
     */
    private Instant createdAt;

    /**
     * Timestamp when the log was last updated
     */
    private Instant updatedAt;

    /**
     * Enum for log types
     */
    public enum LogType {
        FEATURE("feature"),
        BUG("bug"),
        DECISION("decision"),
        NOTE("note");

        private final String value;

        LogType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static LogType fromValue(String value) {
            for (LogType type : LogType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return NOTE; // Default to NOTE if unknown
        }
    }

    /**
     * Factory method to create a new log entry
     */
    public static Log create(String title, String content, List<String> tags,
                            String module, LogType type) {
        Instant now = Instant.now();
        return Log.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .content(content)
                .tags(tags)
                .module(module)
                .type(type != null ? type : LogType.NOTE)
                .timestamp(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Validates the log entry
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Log title cannot be empty");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Log content cannot be empty");
        }

        if (title.length() > 500) {
            throw new IllegalArgumentException("Log title cannot exceed 500 characters");
        }
    }

    /**
     * Gets the full text for embedding (title + content)
     */
    public String getFullText() {
        return title + "\n\n" + content;
    }

    /**
     * Updates the timestamp
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
