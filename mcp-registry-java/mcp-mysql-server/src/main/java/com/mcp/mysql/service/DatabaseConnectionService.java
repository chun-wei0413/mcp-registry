package com.mcp.mysql.service;

import com.mcp.common.model.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MySQL 資料庫連線管理服務
 *
 * 負責管理所有 MySQL 連線的生命週期
 * 提供連線池、健康檢查和連線狀態管理
 */
@Service
public class DatabaseConnectionService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionService.class);

    // 連線資訊儲存
    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();

    // 連線池儲存（實際實現中應使用專業的連線池）
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    // 讀寫鎖保護連線操作
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 新增資料庫連線
     */
    public boolean addConnection(ConnectionInfo connectionInfo) {
        lock.writeLock().lock();
        try {
            String connectionId = connectionInfo.getConnectionId();

            if (connections.containsKey(connectionId)) {
                log.warn("連線 {} 已存在", connectionId);
                return false;
            }

            // 驗證連線資訊
            if (!validateConnectionInfo(connectionInfo)) {
                log.error("連線資訊驗證失敗: {}", connectionId);
                return false;
            }

            // 建立連線池（這裡使用模擬實現）
            DataSource dataSource = createDataSource(connectionInfo);

            // 測試連線
            if (!testDataSourceConnection(dataSource)) {
                log.error("連線測試失敗: {}", connectionId);
                return false;
            }

            // 儲存連線資訊
            connections.put(connectionId, connectionInfo);
            dataSources.put(connectionId, dataSource);

            log.info("成功新增 MySQL 連線: {}", connectionId);
            return true;

        } catch (Exception e) {
            log.error("新增連線失敗: " + connectionInfo.getConnectionId(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 測試連線狀態
     */
    public boolean testConnection(String connectionId) {
        lock.readLock().lock();
        try {
            DataSource dataSource = dataSources.get(connectionId);
            if (dataSource == null) {
                log.warn("連線不存在: {}", connectionId);
                return false;
            }

            return testDataSourceConnection(dataSource);

        } catch (Exception e) {
            log.error("測試連線失敗: " + connectionId, e);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 移除連線
     */
    public boolean removeConnection(String connectionId) {
        lock.writeLock().lock();
        try {
            if (!connections.containsKey(connectionId)) {
                log.warn("嘗試移除不存在的連線: {}", connectionId);
                return false;
            }

            // 關閉連線池
            DataSource dataSource = dataSources.remove(connectionId);
            if (dataSource != null) {
                closeDataSource(dataSource);
            }

            // 移除連線資訊
            connections.remove(connectionId);

            log.info("成功移除連線: {}", connectionId);
            return true;

        } catch (Exception e) {
            log.error("移除連線失敗: " + connectionId, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 獲取連線
     */
    public Connection getConnection(String connectionId) throws SQLException {
        lock.readLock().lock();
        try {
            DataSource dataSource = dataSources.get(connectionId);
            if (dataSource == null) {
                throw new SQLException("連線不存在: " + connectionId);
            }

            return dataSource.getConnection();

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取所有連線資訊
     */
    public Map<String, ConnectionInfo> getAllConnections() {
        lock.readLock().lock();
        try {
            return Map.copyOf(connections);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 驗證連線資訊
     */
    private boolean validateConnectionInfo(ConnectionInfo connectionInfo) {
        return connectionInfo.getHost() != null && !connectionInfo.getHost().trim().isEmpty() &&
               connectionInfo.getPort() != null && connectionInfo.getPort() > 0 &&
               connectionInfo.getDatabase() != null && !connectionInfo.getDatabase().trim().isEmpty() &&
               connectionInfo.getUsername() != null && !connectionInfo.getUsername().trim().isEmpty() &&
               connectionInfo.getPassword() != null && !connectionInfo.getPassword().trim().isEmpty();
    }

    /**
     * 建立資料源（簡化實現）
     */
    private DataSource createDataSource(ConnectionInfo connectionInfo) {
        // 這裡應該使用真正的連線池實現，如 HikariCP
        // 目前返回一個模擬的 DataSource
        return new MockDataSource(connectionInfo);
    }

    /**
     * 測試資料源連線
     */
    private boolean testDataSourceConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5秒超時
        } catch (SQLException e) {
            log.error("連線測試失敗", e);
            return false;
        }
    }

    /**
     * 關閉資料源
     */
    private void closeDataSource(DataSource dataSource) {
        // 實際實現中應該關閉連線池
        log.info("關閉資料源: {}", dataSource.getClass().getSimpleName());
    }

    /**
     * 模擬 DataSource 實現（僅用於編譯通過）
     */
    private static class MockDataSource implements DataSource {
        private final ConnectionInfo connectionInfo;

        public MockDataSource(ConnectionInfo connectionInfo) {
            this.connectionInfo = connectionInfo;
        }

        @Override
        public Connection getConnection() throws SQLException {
            // 實際實現中應該返回真正的 MySQL 連線
            throw new SQLException("Mock DataSource - 實際實現待完成");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        // 其他 DataSource 方法的空實現
        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException { return null; }
        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {}
        @Override
        public int getLoginTimeout() throws SQLException { return 0; }
        @Override
        public void setLoginTimeout(int seconds) throws SQLException {}
        @Override
        public java.util.logging.Logger getParentLogger() { return null; }
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
    }
}