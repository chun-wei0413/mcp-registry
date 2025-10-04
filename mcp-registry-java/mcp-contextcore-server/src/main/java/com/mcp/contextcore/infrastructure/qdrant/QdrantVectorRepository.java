package com.mcp.contextcore.infrastructure.qdrant;

import com.mcp.contextcore.domain.repository.VectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qdrant Vector Repository Implementation
 *
 * Provides vector storage and similarity search using Qdrant vector database.
 */
@Slf4j
@Repository
public class QdrantVectorRepository implements VectorRepository {

    private final WebClient webClient;
    private final String collectionName;

    public QdrantVectorRepository(
            @Value("${qdrant.api.url:http://localhost:6333}") String qdrantUrl,
            @Value("${qdrant.collection.name:context_logs}") String collectionName
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(qdrantUrl)
                .build();
        this.collectionName = collectionName;
        log.info("Initialized QdrantVectorRepository with collection: {}", collectionName);
    }

    @Override
    public Mono<Void> storeVector(String logId, List<Float> vector, List<String> tags,
                                   String module, String type) {
        log.debug("Storing vector for logId: {}", logId);

        Map<String, Object> payload = new HashMap<>();
        if (tags != null && !tags.isEmpty()) {
            payload.put("tags", tags);
        }
        if (module != null) {
            payload.put("module", module);
        }
        if (type != null) {
            payload.put("type", type);
        }

        Map<String, Object> point = Map.of(
                "id", logId,
                "vector", vector,
                "payload", payload
        );

        Map<String, Object> request = Map.of("points", List.of(point));

        return webClient.put()
                .uri("/collections/{collection}/points", collectionName)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Successfully stored vector for logId: {}", logId))
                .doOnError(error -> log.error("Failed to store vector for logId {}: {}", logId, error.getMessage()))
                .onErrorResume(error -> Mono.error(
                        new RuntimeException("Failed to store vector: " + error.getMessage(), error)
                ));
    }

    @Override
    public Flux<ScoredLogId> searchSimilar(List<Float> vector, int limit, List<String> tags,
                                            String module, String type) {
        log.debug("Searching similar vectors with limit: {}", limit);

        Map<String, Object> filter = buildFilter(tags, module, type);
        Map<String, Object> searchRequest = new HashMap<>();
        searchRequest.put("vector", vector);
        searchRequest.put("limit", limit);
        searchRequest.put("with_payload", false);

        if (!filter.isEmpty()) {
            searchRequest.put("filter", filter);
        }

        return webClient.post()
                .uri("/collections/{collection}/points/search", collectionName)
                .bodyValue(searchRequest)
                .retrieve()
                .bodyToMono(QdrantSearchResponse.class)
                .flatMapMany(response -> {
                    if (response.result() == null) {
                        return Flux.empty();
                    }
                    log.debug("Found {} similar vectors", response.result().size());
                    return Flux.fromIterable(response.result())
                            .map(result -> new ScoredLogId(
                                    result.id().toString(),
                                    result.score().floatValue()
                            ));
                })
                .doOnError(error -> log.error("Failed to search vectors: {}", error.getMessage()))
                .onErrorResume(error -> Flux.error(
                        new RuntimeException("Failed to search vectors: " + error.getMessage(), error)
                ));
    }

    @Override
    public Mono<Void> deleteVector(String logId) {
        log.debug("Deleting vector for logId: {}", logId);

        Map<String, Object> deleteRequest = Map.of(
                "points", List.of(logId)
        );

        return webClient.post()
                .uri("/collections/{collection}/points/delete", collectionName)
                .bodyValue(deleteRequest)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.debug("Successfully deleted vector for logId: {}", logId))
                .doOnError(error -> log.error("Failed to delete vector for logId {}: {}", logId, error.getMessage()));
    }

    @Override
    public Mono<Long> count() {
        log.debug("Counting vectors in collection: {}", collectionName);

        return webClient.get()
                .uri("/collections/{collection}", collectionName)
                .retrieve()
                .bodyToMono(QdrantCollectionInfo.class)
                .map(info -> {
                    long count = info.result().pointsCount();
                    log.debug("Total vectors in collection: {}", count);
                    return count;
                })
                .doOnError(error -> log.error("Failed to count vectors: {}", error.getMessage()))
                .onErrorReturn(0L);
    }

    /**
     * Builds a Qdrant filter from the given criteria
     */
    private Map<String, Object> buildFilter(List<String> tags, String module, String type) {
        Map<String, Object> filter = new HashMap<>();
        List<Map<String, Object>> must = new java.util.ArrayList<>();

        if (tags != null && !tags.isEmpty()) {
            must.add(Map.of("key", "tags", "match", Map.of("any", tags)));
        }

        if (module != null) {
            must.add(Map.of("key", "module", "match", Map.of("value", module)));
        }

        if (type != null) {
            must.add(Map.of("key", "type", "match", Map.of("value", type)));
        }

        if (!must.isEmpty()) {
            filter.put("must", must);
        }

        return filter;
    }

    /**
     * Response objects for Qdrant API
     */
    private record QdrantSearchResponse(List<SearchResult> result) {}
    private record SearchResult(Object id, Number score) {}
    private record QdrantCollectionInfo(CollectionResult result) {}
    private record CollectionResult(long pointsCount) {}
}
