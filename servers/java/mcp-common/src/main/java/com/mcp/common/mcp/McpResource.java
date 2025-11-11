package com.mcp.common.mcp;

import java.util.Map;

/**
 * MCP Resource base interface
 *
 * Defines the basic structure of all MCP resources
 * Follows Model Context Protocol specifications
 */
public interface McpResource {

    /**
     * Resource URI
     */
    String getResourceUri();

    /**
     * Resource description
     */
    String getDescription();

    /**
     * Resource type
     */
    String getMimeType();

    /**
     * Get resource content
     *
     * @param parameters Query parameters
     * @return Resource content
     */
    McpResourceResult getContent(Map<String, Object> parameters);
}