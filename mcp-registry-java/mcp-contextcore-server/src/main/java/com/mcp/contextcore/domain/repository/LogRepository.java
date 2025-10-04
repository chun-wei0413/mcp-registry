package com.mcp.contextcore.domain.repository;

import com.mcp.contextcore.domain.entity.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Log Repository Interface
 *
 * Defines operations for log persistence (SQLite)
 */
public interface LogRepository {

    /**
     * Saves a log entry
     *
     * @param log the log to save
     * @return the saved log
     */
    Mono<Log> save(Log log);

    /**
     * Finds a log by ID
     *
     * @param id the log ID
     * @return the log if found
     */
    Mono<Log> findById(String id);

    /**
     * Finds logs by IDs
     *
     * @param ids the log IDs
     * @return the found logs
     */
    Flux<Log> findByIds(List<String> ids);

    /**
     * Lists all logs with optional filters
     *
     * @param tags filter by tags (null for no filter)
     * @param module filter by module (null for no filter)
     * @param type filter by type (null for no filter)
     * @param limit maximum number of results
     * @return the logs matching the criteria
     */
    Flux<Log> findAll(List<String> tags, String module, Log.LogType type, Integer limit);

    /**
     * Deletes a log by ID
     *
     * @param id the log ID
     * @return true if deleted, false if not found
     */
    Mono<Boolean> deleteById(String id);

    /**
     * Counts total logs
     *
     * @return total number of logs
     */
    Mono<Long> count();

    /**
     * Finds logs by date range
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return the logs in the date range
     */
    Flux<Log> findByDateRange(Instant from, Instant to);
}
