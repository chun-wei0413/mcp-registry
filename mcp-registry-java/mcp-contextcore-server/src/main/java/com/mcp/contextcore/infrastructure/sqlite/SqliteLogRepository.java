package com.mcp.contextcore.infrastructure.sqlite;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mcp.contextcore.domain.entity.Log;
import com.mcp.contextcore.domain.repository.LogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite implementation of LogRepository
 */
@Slf4j
@Repository
public class SqliteLogRepository implements LogRepository {

    private final Connection connection;
    private final Gson gson;

    public SqliteLogRepository(Connection connection) {
        this.connection = connection;
        this.gson = new Gson();
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS logs (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                tags TEXT,
                module TEXT,
                type TEXT,
                timestamp INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """;

        String createIndexSql1 = "CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON logs(timestamp)";
        String createIndexSql2 = "CREATE INDEX IF NOT EXISTS idx_logs_module ON logs(module)";
        String createIndexSql3 = "CREATE INDEX IF NOT EXISTS idx_logs_type ON logs(type)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql);
            stmt.execute(createIndexSql1);
            stmt.execute(createIndexSql2);
            stmt.execute(createIndexSql3);
            log.info("SQLite database initialized successfully");
        } catch (SQLException e) {
            log.error("Failed to initialize SQLite database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    @Override
    public Mono<Log> save(Log log) {
        return Mono.fromCallable(() -> {
            String sql = """
                INSERT OR REPLACE INTO logs (id, title, content, tags, module, type, timestamp, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, log.getId());
                stmt.setString(2, log.getTitle());
                stmt.setString(3, log.getContent());
                stmt.setString(4, log.getTags() != null ? gson.toJson(log.getTags()) : null);
                stmt.setString(5, log.getModule());
                stmt.setString(6, log.getType() != null ? log.getType().getValue() : null);
                stmt.setLong(7, log.getTimestamp().toEpochMilli());
                stmt.setLong(8, log.getCreatedAt().toEpochMilli());
                stmt.setLong(9, log.getUpdatedAt().toEpochMilli());

                stmt.executeUpdate();
                log.info("Log saved: id={}, title={}", log.getId(), log.getTitle());
                return log;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Log> findById(String id) {
        return Mono.fromCallable(() -> {
            String sql = "SELECT * FROM logs WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToLog(rs);
                    }
                    return null;
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Log> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(ids)
                .flatMap(this::findById)
                .filter(log -> log != null);
    }

    @Override
    public Flux<Log> findAll(List<String> tags, String module, Log.LogType type, Integer limit) {
        return Flux.defer(() -> {
            StringBuilder sql = new StringBuilder("SELECT * FROM logs WHERE 1=1");
            List<Object> params = new ArrayList<>();

            if (module != null && !module.isEmpty()) {
                sql.append(" AND module = ?");
                params.add(module);
            }

            if (type != null) {
                sql.append(" AND type = ?");
                params.add(type.getValue());
            }

            // Note: Tags filtering is done in-memory due to JSON format
            sql.append(" ORDER BY timestamp DESC");

            if (limit != null && limit > 0) {
                sql.append(" LIMIT ?");
                params.add(limit);
            }

            return Mono.fromCallable(() -> {
                try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Log> logs = new ArrayList<>();
                        while (rs.next()) {
                            Log log = mapResultSetToLog(rs);
                            // Filter by tags in-memory
                            if (tags == null || tags.isEmpty() || hasAnyTag(log, tags)) {
                                logs.add(log);
                            }
                        }
                        return logs;
                    }
                }
            }).flatMapMany(Flux::fromIterable);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> deleteById(String id) {
        return Mono.fromCallable(() -> {
            String sql = "DELETE FROM logs WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                int rows = stmt.executeUpdate();
                boolean deleted = rows > 0;
                if (deleted) {
                    log.info("Log deleted: id={}", id);
                }
                return deleted;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Long> count() {
        return Mono.fromCallable(() -> {
            String sql = "SELECT COUNT(*) FROM logs";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Log> findByDateRange(Instant from, Instant to) {
        return Mono.fromCallable(() -> {
            String sql = "SELECT * FROM logs WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, from.toEpochMilli());
                stmt.setLong(2, to.toEpochMilli());

                try (ResultSet rs = stmt.executeQuery()) {
                    List<Log> logs = new ArrayList<>();
                    while (rs.next()) {
                        logs.add(mapResultSetToLog(rs));
                    }
                    return logs;
                }
            }
        }).flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Log mapResultSetToLog(ResultSet rs) throws SQLException {
        String tagsJson = rs.getString("tags");
        List<String> tags = tagsJson != null ?
                gson.fromJson(tagsJson, new TypeToken<List<String>>(){}.getType()) : null;

        String typeValue = rs.getString("type");
        Log.LogType logType = typeValue != null ? Log.LogType.fromValue(typeValue) : null;

        return Log.builder()
                .id(rs.getString("id"))
                .title(rs.getString("title"))
                .content(rs.getString("content"))
                .tags(tags)
                .module(rs.getString("module"))
                .type(logType)
                .timestamp(Instant.ofEpochMilli(rs.getLong("timestamp")))
                .createdAt(Instant.ofEpochMilli(rs.getLong("created_at")))
                .updatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")))
                .build();
    }

    private boolean hasAnyTag(Log log, List<String> tags) {
        if (log.getTags() == null) {
            return false;
        }
        for (String tag : tags) {
            if (log.getTags().contains(tag)) {
                return true;
            }
        }
        return false;
    }
}
