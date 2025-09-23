package com.mcpregistry.core.usecase.port.out;

import com.mcpregistry.core.entity.ConnectionId;
import com.mcpregistry.core.entity.DatabaseConnection;

import java.util.List;
import java.util.Optional;

/**
 * 資料庫連線 Repository 輸出端口
 *
 * 定義資料存取的抽象接口，遵循依賴反轉原則
 * 實際實現由 Adapter 層提供
 */
public interface DatabaseConnectionRepository {

    /**
     * 保存或更新資料庫連線
     */
    void save(DatabaseConnection connection);

    /**
     * 根據 ID 查找連線
     */
    Optional<DatabaseConnection> findById(ConnectionId connectionId);

    /**
     * 查找所有連線
     */
    List<DatabaseConnection> findAll();

    /**
     * 查找所有健康的連線
     */
    List<DatabaseConnection> findAllHealthy();

    /**
     * 刪除連線
     */
    void delete(ConnectionId connectionId);

    /**
     * 檢查連線是否存在
     */
    boolean existsById(ConnectionId connectionId);

    /**
     * 根據主機和資料庫查找連線
     */
    Optional<DatabaseConnection> findByHostAndDatabase(String host, String database);
}