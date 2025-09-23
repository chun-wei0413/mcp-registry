package com.mcpregistry.core.adapter.out.repository;

import com.mcpregistry.core.entity.ConnectionId;
import com.mcpregistry.core.entity.DatabaseConnection;
import com.mcpregistry.core.usecase.port.out.DatabaseConnectionRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 記憶體內資料庫連線儲存庫實現
 *
 * Output Adapter 實現，提供記憶體內的連線管理
 * 適用於開發和測試環境
 */
public class InMemoryDatabaseConnectionRepository implements DatabaseConnectionRepository {

    private final Map<String, DatabaseConnection> connections = new ConcurrentHashMap<>();

    @Override
    public void save(DatabaseConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("DatabaseConnection 不能為空");
        }
        connections.put(connection.getId().getValue(), connection);
    }

    @Override
    public Optional<DatabaseConnection> findById(ConnectionId connectionId) {
        if (connectionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(connections.get(connectionId.getValue()));
    }

    @Override
    public List<DatabaseConnection> findAll() {
        return List.copyOf(connections.values());
    }

    @Override
    public List<DatabaseConnection> findAllHealthy() {
        return connections.values().stream()
                .filter(DatabaseConnection::isHealthy)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ConnectionId connectionId) {
        if (connectionId != null) {
            connections.remove(connectionId.getValue());
        }
    }

    @Override
    public boolean existsById(ConnectionId connectionId) {
        if (connectionId == null) {
            return false;
        }
        return connections.containsKey(connectionId.getValue());
    }

    @Override
    public Optional<DatabaseConnection> findByHostAndDatabase(String host, String database) {
        if (host == null || database == null) {
            return Optional.empty();
        }

        return connections.values().stream()
                .filter(conn -> host.equals(conn.getConnectionInfo().getHost()) &&
                              database.equals(conn.getConnectionInfo().getDatabase()))
                .findFirst();
    }

    /**
     * 清空所有連線（測試用）
     */
    public void clear() {
        connections.clear();
    }

    /**
     * 獲取連線數量
     */
    public int size() {
        return connections.size();
    }
}