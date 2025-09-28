package com.mcp.mysql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.mysql.service.DatabaseConnectionService;
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
 * MySQL 連線管理工具測試
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
        assertEquals("mysql_connection_management", connectionTool.getToolName());
    }

    @Test
    void shouldReturnCorrectDescription() {
        String description = connectionTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("MySQL"));
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
            "connectionId", "test-mysql-connection",
            "host", "localhost",
            "port", 3306,
            "database", "testdb",
            "username", "testuser",
            "password", "testpass"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("MySQL 連線建立成功", result.getContent());
        assertNotNull(result.getData());
    }

    @Test
    void shouldAddConnectionWithCustomPortSuccessfully() {
        // Arrange
        when(connectionService.addConnection(any())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "test-mysql-custom-port",
            "host", "mysql.example.com",
            "port", 3307,
            "database", "production_db",
            "username", "prod_user",
            "password", "prod_pass",
            "maxPoolSize", 10
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("MySQL 連線建立成功", result.getContent());
        assertNotNull(result.getData());
    }

    @Test
    void shouldAddConnectionWithSslSuccessfully() {
        // Arrange
        when(connectionService.addConnection(any())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "test-mysql-ssl",
            "host", "secure-mysql.example.com",
            "port", 3306,
            "database", "secure_db",
            "username", "secure_user",
            "password", "secure_pass",
            "useSSL", true,
            "verifyServerCertificate", false
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("MySQL 連線建立成功", result.getContent());
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
    void shouldFailWhenHostIsEmpty() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "test-conn",
            "host", ""
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("主機名稱不能為空", result.getError());
    }

    @Test
    void shouldFailWhenDatabaseIsEmpty() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "test-conn",
            "host", "localhost",
            "port", 3306,
            "database", "",
            "username", "user",
            "password", "pass"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("資料庫名稱不能為空", result.getError());
    }

    @Test
    void shouldTestConnectionSuccessfully() {
        // Arrange
        when(connectionService.testConnection(anyString())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "test",
            "connectionId", "test-mysql-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("MySQL 連線測試完成", result.getContent());
    }

    @Test
    void shouldFailTestConnectionWhenNotConnected() {
        // Arrange
        when(connectionService.testConnection(anyString())).thenReturn(false);

        Map<String, Object> arguments = Map.of(
            "action", "test",
            "connectionId", "test-mysql-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("MySQL 連線測試失敗", result.getError());
    }

    @Test
    void shouldRemoveConnectionSuccessfully() {
        // Arrange
        when(connectionService.removeConnection(anyString())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "remove",
            "connectionId", "test-mysql-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("MySQL 連線移除成功", result.getContent());
    }

    @Test
    void shouldFailRemoveConnectionWhenNotFound() {
        // Arrange
        when(connectionService.removeConnection(anyString())).thenReturn(false);

        Map<String, Object> arguments = Map.of(
            "action", "remove",
            "connectionId", "non-existent-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("連線不存在或移除失敗", result.getError());
    }

    @Test
    void shouldListConnectionsSuccessfully() {
        // Arrange
        when(connectionService.listConnections()).thenReturn(Map.of(
            "test-conn-1", Map.of("host", "mysql1.example.com", "database", "db1", "status", "active"),
            "test-conn-2", Map.of("host", "mysql2.example.com", "database", "db2", "status", "inactive")
        ));

        Map<String, Object> arguments = Map.of(
            "action", "list"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("連線列表獲取完成", result.getContent());
        assertNotNull(result.getData());
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

    @Test
    void shouldFailWhenActionIsMissing() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "connectionId", "test-connection"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("必須指定操作類型 (action)", result.getError());
    }

    @Test
    void shouldHandleConnectionTimeout() {
        // Arrange
        when(connectionService.addConnection(any()))
            .thenThrow(new RuntimeException("Connection timeout"));

        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "timeout-test",
            "host", "unreachable-host.example.com",
            "port", 3306,
            "database", "testdb",
            "username", "testuser",
            "password", "testpass"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Connection timeout"));
    }

    @Test
    void shouldHandleAuthenticationFailure() {
        // Arrange
        when(connectionService.addConnection(any()))
            .thenThrow(new RuntimeException("Access denied for user"));

        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "auth-fail-test",
            "host", "localhost",
            "port", 3306,
            "database", "testdb",
            "username", "wrong_user",
            "password", "wrong_pass"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Access denied"));
    }

    @Test
    void shouldValidatePortRange() {
        // Arrange
        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "invalid-port-test",
            "host", "localhost",
            "port", 99999, // 無效的埠號
            "database", "testdb",
            "username", "testuser",
            "password", "testpass"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("埠號範圍") || result.getError().contains("port"));
    }

    @Test
    void shouldHandleDefaultPort() {
        // Arrange
        when(connectionService.addConnection(any())).thenReturn(true);

        Map<String, Object> arguments = Map.of(
            "action", "add",
            "connectionId", "default-port-test",
            "host", "localhost",
            // port 未指定，應該使用預設值 3306
            "database", "testdb",
            "username", "testuser",
            "password", "testpass"
        );

        // Act
        McpToolResult result = connectionTool.execute(arguments);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals("MySQL 連線建立成功", result.getContent());
    }
}