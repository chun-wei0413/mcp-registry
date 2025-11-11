package com.mcp.postgresql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.postgresql.service.DatabaseConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * PostgreSQL 連線管理工具 BDD 測試
 *
 * 功能：作為 MCP Server 的管理員，我希望能夠管理 PostgreSQL 資料庫連線
 * 以便我可以建立、測試、列出和移除資料庫連線
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostgreSQL 連線管理工具")
class ConnectionManagementToolTest {

    @Mock
    private DatabaseConnectionService connectionService;

    private ConnectionManagementTool connectionTool;

    @BeforeEach
    void setUp() {
        connectionTool = new ConnectionManagementTool(connectionService);
    }

    @Nested
    @DisplayName("工具基本資訊")
    class ToolBasicInformation {

        @Test
        @DisplayName("場景：查詢工具名稱")
        void shouldReturnCorrectToolName() {
            // Given: PostgreSQL 連線管理工具已初始化
            // When: 請求工具名稱
            String toolName = connectionTool.getToolName();

            // Then: 應該返回正確的工具名稱
            assertEquals("postgresql_connection_management", toolName);
        }

        @Test
        @DisplayName("場景：查詢工具描述")
        void shouldReturnCorrectDescription() {
            // Given: PostgreSQL 連線管理工具已初始化
            // When: 請求工具描述
            String description = connectionTool.getDescription();

            // Then: 描述應該包含 PostgreSQL 和連線管理關鍵字
            assertNotNull(description);
            assertTrue(description.contains("PostgreSQL"));
            assertTrue(description.contains("連線管理"));
        }

        @Test
        @DisplayName("場景：查詢參數結構")
        void shouldReturnValidParameterSchema() {
            // Given: PostgreSQL 連線管理工具已初始化
            // When: 請求參數結構
            Map<String, Object> schema = connectionTool.getParameterSchema();

            // Then: 應該返回有效的 JSON Schema 結構
            assertNotNull(schema);
            assertTrue(schema.containsKey("type"));
            assertTrue(schema.containsKey("properties"));
            assertTrue(schema.containsKey("required"));
        }
    }

    @Nested
    @DisplayName("建立新連線")
    class AddNewConnection {

        @Test
        @DisplayName("場景：使用有效參數建立連線")
        void shouldAddConnectionSuccessfully() {
            // Given: 資料庫連線服務可以成功建立連線
            when(connectionService.addConnection(any())).thenReturn(true);

            // And: 提供有效的連線參數
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "test-connection",
                "host", "localhost",
                "port", 5432,
                "database", "testdb",
                "username", "testuser",
                "password", "testpass"
            );

            // When: 執行建立連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功建立連線
            assertTrue(result.isSuccess());
            assertEquals("PostgreSQL 連線建立成功", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：連線 ID 為空時應該失敗")
        void shouldFailWhenConnectionIdIsEmpty() {
            // Given: 提供空的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", ""
            );

            // When: 嘗試建立連線
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並返回錯誤訊息
            assertFalse(result.isSuccess());
            assertEquals("Connection ID 不能為空", result.getError());
        }
    }

    @Nested
    @DisplayName("測試連線")
    class TestConnection {

        @Test
        @DisplayName("場景：測試有效連線")
        void shouldTestConnectionSuccessfully() {
            // Given: 存在一個有效的連線
            when(connectionService.testConnection(anyString())).thenReturn(true);

            // And: 提供連線 ID 進行測試
            Map<String, Object> arguments = Map.of(
                "action", "test",
                "connectionId", "test-connection"
            );

            // When: 執行連線測試
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功測試連線
            assertTrue(result.isSuccess());
            assertEquals("連線測試完成", result.getContent());
        }
    }

    @Nested
    @DisplayName("移除連線")
    class RemoveConnection {

        @Test
        @DisplayName("場景：移除存在的連線")
        void shouldRemoveConnectionSuccessfully() {
            // Given: 存在一個可以移除的連線
            when(connectionService.removeConnection(anyString())).thenReturn(true);

            // And: 提供要移除的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "remove",
                "connectionId", "test-connection"
            );

            // When: 執行移除連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功移除連線
            assertTrue(result.isSuccess());
            assertEquals("連線移除成功", result.getContent());
        }
    }

    @Nested
    @DisplayName("錯誤處理")
    class ErrorHandling {

        @Test
        @DisplayName("場景：使用不支援的操作")
        void shouldFailWithUnsupportedAction() {
            // Given: 提供不支援的操作類型
            Map<String, Object> arguments = Map.of(
                "action", "invalid_action",
                "connectionId", "test-connection"
            );

            // When: 嘗試執行不支援的操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並返回錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("不支援的操作"));
        }
    }
}