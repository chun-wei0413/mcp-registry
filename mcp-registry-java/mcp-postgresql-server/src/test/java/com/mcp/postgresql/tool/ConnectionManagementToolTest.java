package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.postgresql.service.DatabaseConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * PostgreSQL 連線管理工具測試
 */
@ExtendWith(MockitoExtension.class)
class ConnectionManagementToolTest {

    @Mock
    private DatabaseConnectionService connectionService;

    private ConnectionManagementTool connectionTool;

    @BeforeEach
    void setUp() {
        connectionTool = new ConnectionManagementTool(connectionService);
    }

    @Test
    void shouldReturnCorrectToolName() {
        assertEquals("postgresql_connection_management", connectionTool.getToolName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = connectionTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("PostgreSQL"));
        assertTrue(description.contains("連線管理"));
    }

    @Test
    void shouldReturnValidParameterSchema() {
        Map<String, Object> schema = connectionTool.getParameterSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("type"));
        assertTrue(schema.containsKey("properties"));
        assertTrue(schema.containsKey("required"));
    }

    @Test
    void shouldAddConnectionSuccessfully() {
        // Arrange
        when(connectionService.addConnection(any())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "test-connection",
            "host", "localhost",
            "port", 5432,
            "database", "testdb",
            "username", "testuser",
            "password", "testpass"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("PostgreSQL 連線建立成功", result.getContent());
        assertNotNull(result.getData());
    }

    @Test
    void shouldFailWhenConnectionIdIsEmpty() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", ""
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("Connection ID 不能為空", result.getError());
    }

    @Test
    void shouldTestConnectionSuccessfully() {
        // Arrange
        when(connectionService.testConnection(anyString())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "test",
            "connectionId", "test-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("連線測試完成", result.getContent());
    }

    @Test
    void shouldRemoveConnectionSuccessfully() {
        // Arrange
        when(connectionService.removeConnection(anyString())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "remove",
            "connectionId", "test-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("連線移除成功", result.getContent());
    }

    @Test
    void shouldFailWithUnsupportedAction() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "invalid_action",
            "connectionId", "test-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("不支援的操作"));
    }
}