package com.mcp.mysql.tool;

import com.mcp.common.mcp.McpToolResult;
import com.mcp.mysql.service.DatabaseConnectionService;
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
 * MySQL 連線管理工具 BDD 測試
 *
 * 功能：作為 MCP Server 的管理員，我希望能夠管理 MySQL 資料庫連線
 * 以便我可以建立、測試、列出和移除 MySQL 資料庫連線，並支援各種 MySQL 特定配置
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MySQL 連線管理工具")
class ConnectionManagementToolBDDTest {

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
        @DisplayName("場景：驗證 MySQL 工具名稱")
        void shouldReturnCorrectToolName() {
            // Given: MySQL 連線管理工具已初始化
            // When: 請求工具名稱
            String toolName = connectionTool.getToolName();

            // Then: 應該返回 MySQL 專用的工具名稱
            assertEquals("mysql_connection_management", toolName);
        }

        @Test
        @DisplayName("場景：驗證 MySQL 工具描述")
        void shouldReturnCorrectDescription() {
            // Given: MySQL 連線管理工具已初始化
            // When: 請求工具描述
            String description = connectionTool.getDescription();

            // Then: 描述應該包含 MySQL 和連線管理關鍵字
            assertNotNull(description);
            assertTrue(description.contains("MySQL"));
            assertTrue(description.contains("連線管理"));
        }
    }

    @Nested
    @DisplayName("建立 MySQL 連線")
    class AddMySQLConnection {

        @Test
        @DisplayName("場景：使用標準參數建立 MySQL 連線")
        void shouldAddStandardMySQLConnection() {
            // Given: MySQL 連線服務可以成功建立連線
            when(connectionService.addConnection(any())).thenReturn(true);

            // And: 提供標準的 MySQL 連線參數
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "mysql-prod",
                "host", "mysql.example.com",
                "port", 3306,
                "database", "production_db",
                "username", "app_user",
                "password", "secure_password"
            );

            // When: 執行建立 MySQL 連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功建立 MySQL 連線
            assertTrue(result.isSuccess());
            assertEquals("MySQL 連線建立成功", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：使用自訂埠號建立 MySQL 連線")
        void shouldAddMySQLConnectionWithCustomPort() {
            // Given: MySQL 服務運行在非標準埠
            when(connectionService.addConnection(any())).thenReturn(true);

            // And: 提供自訂埠號的連線參數
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "mysql-custom",
                "host", "custom-mysql.example.com",
                "port", 3307,  // 非標準埠號
                "database", "custom_db",
                "username", "custom_user",
                "password", "custom_pass",
                "maxPoolSize", 15
            );

            // When: 執行建立自訂埠連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功建立自訂埠的 MySQL 連線
            assertTrue(result.isSuccess());
            assertEquals("MySQL 連線建立成功", result.getContent());
        }

        @Test
        @DisplayName("場景：使用 SSL 配置建立安全 MySQL 連線")
        void shouldAddSecureMySQLConnectionWithSSL() {
            // Given: 需要建立加密的 MySQL 連線
            when(connectionService.addConnection(any())).thenReturn(true);

            // And: 提供 SSL 安全連線參數
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "mysql-secure",
                "host", "secure-mysql.example.com",
                "port", 3306,
                "database", "secure_db",
                "username", "secure_user",
                "password", "secure_password",
                "useSSL", true,
                "verifyServerCertificate", false
            );

            // When: 執行建立 SSL 連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功建立安全的 MySQL 連線
            assertTrue(result.isSuccess());
            assertEquals("MySQL 連線建立成功", result.getContent());
        }

        @Test
        @DisplayName("場景：使用預設埠號建立連線")
        void shouldAddConnectionWithDefaultPort() {
            // Given: 不指定埠號，使用 MySQL 預設埠
            when(connectionService.addConnection(any())).thenReturn(true);

            // And: 提供不包含埠號的連線參數
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "mysql-default",
                "host", "localhost",
                // port 未指定，應該使用預設值 3306
                "database", "test_db",
                "username", "test_user",
                "password", "test_pass"
            );

            // When: 執行建立預設埠連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功使用預設埠建立連線
            assertTrue(result.isSuccess());
            assertEquals("MySQL 連線建立成功", result.getContent());
        }
    }

    @Nested
    @DisplayName("測試 MySQL 連線")
    class TestMySQLConnection {

        @Test
        @DisplayName("場景：測試健康的 MySQL 連線")
        void shouldTestHealthyMySQLConnection() {
            // Given: 存在一個健康的 MySQL 連線
            when(connectionService.testConnection(anyString())).thenReturn(true);

            // And: 提供要測試的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "test",
                "connectionId", "mysql-prod"
            );

            // When: 執行 MySQL 連線測試
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功通過連線測試
            assertTrue(result.isSuccess());
            assertEquals("MySQL 連線測試完成", result.getContent());
        }

        @Test
        @DisplayName("場景：測試不健康的 MySQL 連線")
        void shouldFailTestingUnhealthyMySQLConnection() {
            // Given: MySQL 連線不可用或不健康
            when(connectionService.testConnection(anyString())).thenReturn(false);

            // And: 提供不健康的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "test",
                "connectionId", "mysql-unhealthy"
            );

            // When: 執行連線測試
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該測試失敗
            assertFalse(result.isSuccess());
            assertEquals("MySQL 連線測試失敗", result.getError());
        }
    }

    @Nested
    @DisplayName("移除 MySQL 連線")
    class RemoveMySQLConnection {

        @Test
        @DisplayName("場景：移除存在的 MySQL 連線")
        void shouldRemoveExistingMySQLConnection() {
            // Given: 存在可以移除的 MySQL 連線
            when(connectionService.removeConnection(anyString())).thenReturn(true);

            // And: 提供要移除的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "remove",
                "connectionId", "mysql-old"
            );

            // When: 執行移除 MySQL 連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功移除連線
            assertTrue(result.isSuccess());
            assertEquals("MySQL 連線移除成功", result.getContent());
        }

        @Test
        @DisplayName("場景：嘗試移除不存在的連線")
        void shouldFailRemovingNonExistentConnection() {
            // Given: 嘗試移除的連線不存在
            when(connectionService.removeConnection(anyString())).thenReturn(false);

            // And: 提供不存在的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "remove",
                "connectionId", "non-existent-mysql"
            );

            // When: 執行移除不存在連線的操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該移除失敗
            assertFalse(result.isSuccess());
            assertEquals("連線不存在或移除失敗", result.getError());
        }
    }

    @Nested
    @DisplayName("列出 MySQL 連線")
    class ListMySQLConnections {

        @Test
        @DisplayName("場景：列出所有 MySQL 連線")
        void shouldListAllMySQLConnections() {
            // Given: 系統中存在多個 MySQL 連線
            when(connectionService.listConnections()).thenReturn(Map.of(
                "mysql-prod", Map.of(
                    "host", "mysql-prod.example.com",
                    "database", "production",
                    "status", "active",
                    "engine", "InnoDB"
                ),
                "mysql-dev", Map.of(
                    "host", "mysql-dev.example.com",
                    "database", "development",
                    "status", "active",
                    "engine", "InnoDB"
                ),
                "mysql-test", Map.of(
                    "host", "mysql-test.example.com",
                    "database", "testing",
                    "status", "inactive",
                    "engine", "MyISAM"
                )
            ));

            // And: 請求列出所有連線
            Map<String, Object> arguments = Map.of(
                "action", "list"
            );

            // When: 執行列出連線操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功返回所有 MySQL 連線列表
            assertTrue(result.isSuccess());
            assertEquals("連線列表獲取完成", result.getContent());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("場景：列出空的連線列表")
        void shouldListEmptyConnectionsList() {
            // Given: 系統中沒有任何 MySQL 連線
            when(connectionService.listConnections()).thenReturn(Map.of());

            // And: 請求列出連線
            Map<String, Object> arguments = Map.of(
                "action", "list"
            );

            // When: 執行列出空連線列表操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該成功返回空列表
            assertTrue(result.isSuccess());
            assertEquals("連線列表獲取完成", result.getContent());
        }
    }

    @Nested
    @DisplayName("MySQL 特定錯誤處理")
    class MySQLSpecificErrorHandling {

        @Test
        @DisplayName("場景：連線 ID 為空時應該失敗")
        void shouldFailWhenConnectionIdIsEmpty() {
            // Given: 提供空的連線 ID
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", ""
            );

            // When: 嘗試建立空 ID 連線
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並返回錯誤訊息
            assertFalse(result.isSuccess());
            assertEquals("Connection ID 不能為空", result.getError());
        }

        @Test
        @DisplayName("場景：主機名稱為空時應該失敗")
        void shouldFailWhenHostIsEmpty() {
            // Given: 提供空的主機名稱
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "test-mysql",
                "host", ""
            );

            // When: 嘗試使用空主機名稱建立連線
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並返回主機名稱錯誤
            assertFalse(result.isSuccess());
            assertEquals("主機名稱不能為空", result.getError());
        }

        @Test
        @DisplayName("場景：資料庫名稱為空時應該失敗")
        void shouldFailWhenDatabaseNameIsEmpty() {
            // Given: 提供空的資料庫名稱
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "test-mysql",
                "host", "localhost",
                "port", 3306,
                "database", "",
                "username", "user",
                "password", "pass"
            );

            // When: 嘗試使用空資料庫名稱建立連線
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並返回資料庫名稱錯誤
            assertFalse(result.isSuccess());
            assertEquals("資料庫名稱不能為空", result.getError());
        }

        @Test
        @DisplayName("場景：埠號超出範圍時應該失敗")
        void shouldFailWhenPortOutOfRange() {
            // Given: 提供超出有效範圍的埠號
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "invalid-port",
                "host", "localhost",
                "port", 99999,  // 無效的埠號
                "database", "test_db",
                "username", "user",
                "password", "pass"
            );

            // When: 嘗試使用無效埠號建立連線
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並返回埠號錯誤
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("埠號範圍") || result.getError().contains("port"));
        }

        @Test
        @DisplayName("場景：MySQL 連線超時")
        void shouldHandleMySQLConnectionTimeout() {
            // Given: MySQL 伺服器無回應導致連線超時
            when(connectionService.addConnection(any()))
                .thenThrow(new RuntimeException("Connection timeout"));

            // And: 提供連線到無回應伺服器的參數
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "timeout-mysql",
                "host", "unreachable-mysql.example.com",
                "port", 3306,
                "database", "test_db",
                "username", "user",
                "password", "pass"
            );

            // When: 嘗試連線到無回應的 MySQL 伺服器
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並包含超時錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("Connection timeout"));
        }

        @Test
        @DisplayName("場景：MySQL 認證失敗")
        void shouldHandleMySQLAuthenticationFailure() {
            // Given: MySQL 伺服器拒絕認證
            when(connectionService.addConnection(any()))
                .thenThrow(new RuntimeException("Access denied for user"));

            // And: 提供錯誤的認證資訊
            Map<String, Object> arguments = Map.of(
                "action", "add",
                "connectionId", "auth-fail",
                "host", "mysql.example.com",
                "port", 3306,
                "database", "secure_db",
                "username", "wrong_user",
                "password", "wrong_password"
            );

            // When: 嘗試使用錯誤認證連線 MySQL
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並包含認證錯誤訊息
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("Access denied"));
        }

        @Test
        @DisplayName("場景：不支援的操作類型")
        void shouldFailWithUnsupportedAction() {
            // Given: 提供不支援的操作類型
            Map<String, Object> arguments = Map.of(
                "action", "unsupported_mysql_action",
                "connectionId", "test-mysql"
            );

            // When: 嘗試執行不支援的操作
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並返回不支援操作的錯誤
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("不支援的操作"));
        }

        @Test
        @DisplayName("場景：缺少必要的操作參數")
        void shouldFailWhenActionParameterMissing() {
            // Given: 沒有提供操作類型參數
            Map<String, Object> arguments = Map.of(
                "connectionId", "test-mysql"
                // 缺少 "action" 參數
            );

            // When: 嘗試執行沒有操作類型的命令
            McpToolResult result = connectionTool.execute(arguments);

            // Then: 應該失敗並要求指定操作類型
            assertFalse(result.isSuccess());
            assertEquals("必須指定操作類型 (action)", result.getError());
        }
    }
}