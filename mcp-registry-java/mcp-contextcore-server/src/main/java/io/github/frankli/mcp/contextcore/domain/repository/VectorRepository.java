package io.github.frankli.mcp.contextcore.domain.repository;

import io.github.frankli.mcp.contextcore.domain.entity.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Vector Repository Interface
 *
 * Defines operations for vector storage and search (Qdrant)
 */
public interface VectorRepository {

    /**
     * Stores a log's vector embedding
     *
     * @param logId the log ID
     * @param vector the embedding vector
     * @param tags the log's tags
     * @param module the log's module
     * @param type the log's type
     * @return completion signal
     */
    Mono<Void> storeVector(String logId, List<Float> vector, List<String> tags,
                           String module, String type);

    /**
     * Searches for similar logs by vector
     *
     * @param vector the query vector
     * @param limit maximum number of results
     * @param tags filter by tags (null for no filter)
     * @param module filter by module (null for no filter)
     * @param type filter by type (null for no filter)
     * @return list of log IDs with similarity scores
     */
    Flux<ScoredLogId> searchSimilar(List<Float> vector, int limit, List<String> tags,
                                     String module, String type);

    /**
     * Deletes a vector by log ID
     *
     * @param logId the log ID
     * @return completion signal
     */
    Mono<Void> deleteVector(String logId);

    /**
     * Counts total vectors
     *
     * @return total number of vectors
     */
    Mono<Long> count();

    /**
     * Result object containing log ID and similarity score
     */
    record ScoredLogId(String logId, float score) {}
}
