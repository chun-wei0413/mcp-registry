package io.github.frankli.mcp.contextcore.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for adding a log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddLogRequest {
    private String title;
    private String content;
    private List<String> tags;
    private String module;
    private String type; // "feature" | "bug" | "decision" | "note"
}
