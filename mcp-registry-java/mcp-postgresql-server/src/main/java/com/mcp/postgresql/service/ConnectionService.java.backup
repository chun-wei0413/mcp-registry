package com.mcp.postgresql.service;

import com.mcp.common.exception.ConnectionException;
import com.mcp.common.model.ConnectionInfo;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL 連線管理服務
 */
@Service
@Slf4j
public class ConnectionService {

    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();
    private final Map<String, ConnectionPool> connectionPools = new ConcurrentHashMap<>();

    /**
     * 建立資料庫連線
     */
    public Mono<ConnectionInfo> addConnection(ConnectionInfo connectionInfo) {
        String connectionId = connectionInfo.getConnectionId();

        log.info("Adding PostgreSQL connection: {}", connectionId);

        if (connections.containsKey(connectionId)) {
            return Mono.error(new ConnectionException.ConnectionAlreadyExists(connectionId));
        }

        return createConnectionPool(connectionInfo)
                .doOnSuccess(pool -> {
                    connectionPools.put(connectionId, pool);
                    ConnectionInfo updatedInfo = connectionInfo.toBuilder()
                            .status(ConnectionInfo.ConnectionStatus.CONNECTED)
                            .build();
                    connections.put(connectionId, updatedInfo);
                    log.info("PostgreSQL connection added successfully: {}", connectionId);
                })
                .map(pool -> connectionInfo.toBuilder()
                        .status(ConnectionInfo.ConnectionStatus.CONNECTED)
                        .build())
                .onErrorMap(error -> {
                    log.error("Failed to add PostgreSQL connection: {}", connectionId, error);
                    return new ConnectionException.ConnectionFailure(connectionId, error);
                });
    }

    /**
     * 測試連線
     */
    public Mono<Boolean> testConnection(String connectionId) {
        log.info("Testing PostgreSQL connection: {}", connectionId);

        ConnectionPool pool = connectionPools.get(connectionId);
        if (pool == null) {
            return Mono.error(new ConnectionException.ConnectionNotFound(connectionId));
        }

        return Mono.from(pool.create())
                .flatMap(connection ->
                    Mono.from(connection.createStatement("SELECT 1")
                            .execute())
                            .then(Mono.fromRunnable(connection::close))
                            .thenReturn(true)
                )
                .onErrorReturn(false)
                .doOnSuccess(result -> log.info("Connection test result for {}: {}", connectionId, result));
    }

    /**
     * 列出所有連線
     */
    public Flux<ConnectionInfo> listConnections() {
        log.info("Listing all PostgreSQL connections");
        return Flux.fromIterable(connections.values());
    }

    /**
     * 移除連線
     */
    public Mono<Boolean> removeConnection(String connectionId) {
        log.info("Removing PostgreSQL connection: {}", connectionId);

        ConnectionPool pool = connectionPools.remove(connectionId);
        ConnectionInfo connectionInfo = connections.remove(connectionId);

        if (connectionInfo == null) {
            return Mono.just(false);
        }

        if (pool != null && !pool.isDisposed()) {
            return pool.dispose()
                    .then(Mono.just(true))
                    .doOnSuccess(result -> log.info("PostgreSQL connection removed: {}", connectionId));
        }

        return Mono.just(true);
    }

    /**
     * 取得連線池
     */
    public ConnectionPool getConnectionPool(String connectionId) {
        ConnectionPool pool = connectionPools.get(connectionId);
        if (pool == null) {
            throw new ConnectionException.ConnectionNotFound(connectionId);
        }
        return pool;
    }

    /**
     * 健康檢查
     */
    public Mono<Map<String, Object>> healthCheck() {
        log.info("Performing PostgreSQL MCP Server health check");

        return Flux.fromIterable(connections.keySet())
                .flatMap(this::testConnection)
                .collectList()
                .map(results -> {
                    long healthyConnections = results.stream().mapToLong(result -> result ? 1 : 0).sum();
                    return Map.of(
                        "total_connections", connections.size(),
                        "healthy_connections", healthyConnections,
                        "server_status", "running"
                    );
                });
    }

    /**
     * 建立連線池
     */
    private Mono<ConnectionPool> createConnectionPool(ConnectionInfo connectionInfo) {
        try {
            // 建立 PostgreSQL 連線工廠
            PostgresqlConnectionConfiguration config = PostgresqlConnectionConfiguration.builder()
                    .host(connectionInfo.getHost())
                    .port(connectionInfo.getPort())
                    .database(connectionInfo.getDatabase())
                    .username(connectionInfo.getUsername())
                    .password(connectionInfo.getPassword())
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            ConnectionFactory connectionFactory = new PostgresqlConnectionFactory(config);

            // 建立連線池配置
            ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
                    .maxIdleTime(Duration.ofMinutes(30))
                    .maxLifeTime(Duration.ofHours(2))
                    .maxAcquireTime(Duration.ofSeconds(30))
                    .maxCreateConnectionTime(Duration.ofSeconds(30))
                    .initialSize(1)
                    .maxSize(connectionInfo.getPoolSize())
                    .validationQuery("SELECT 1")
                    .build();

            ConnectionPool pool = new ConnectionPool(poolConfig);

            // 測試連線
            return Mono.from(pool.create())
                    .flatMap(connection ->
                        Mono.from(connection.createStatement("SELECT version()")
                                .execute())
                                .then(Mono.fromRunnable(connection::close))
                    )
                    .thenReturn(pool);

        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}