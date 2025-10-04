package io.github.frankli.mcp.contextcore.infrastructure.qdrant;

import io.github.frankli.mcp.contextcore.domain.repository.VectorRepository;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Qdrant implementation of VectorRepository
 */
@Slf4j
@Repository
public class QdrantVectorRepository implements VectorRepository {

    private final QdrantClient client;
    private final String collectionName;
    private final int dimension;

    public QdrantVectorRepository(
            @Value("${contextcore.qdrant.host:localhost}") String host,
            @Value("${contextcore.qdrant.port:6334}") int port,
            @Value("${contextcore.qdrant.collection:contextcore_logs}") String collectionName,
            @Value("${contextcore.ollama.dimension:768}") int dimension) {

        this.client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build());
        this.collectionName = collectionName;
        this.dimension = dimension;

        log.info("QdrantVectorRepository initialized: host={}, port={}, collection={}, dimension={}",
                host, port, collectionName, dimension);
    }

    @PostConstruct
    public void init() {
        try {
            ensureCollectionExists();
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant collection", e);
            throw new RuntimeException("Failed to initialize Qdrant", e);
        }
    }

    private void ensureCollectionExists() throws ExecutionException, InterruptedException {
        // Check if collection exists
        Collections.ListCollectionsResponse response = client.listCollectionsAsync().get();
        boolean exists = response.getCollectionsList().stream()
                .anyMatch(col -> col.getName().equals(collectionName));

        if (!exists) {
            log.info("Creating Qdrant collection: {}", collectionName);

            Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                    .setSize(dimension)
                    .setDistance(Collections.Distance.Cosine)
                    .build();

            client.createCollectionAsync(
                    collectionName,
                    Collections.VectorsConfig.newBuilder()
                            .setParams(vectorParams)
                            .build()
            ).get();

            log.info("Qdrant collection created successfully: {}", collectionName);
        } else {
            log.info("Qdrant collection already exists: {}", collectionName);
        }
    }

    @Override
    public Mono<Void> storeVector(String logId, List<Float> vector, List<String> tags,
                                   String module, String type) {
        return Mono.fromRunnable(() -> {
            try {
                Map<String, Points.Value> payload = new HashMap<>();

                if (tags != null && !tags.isEmpty()) {
                    Points.Value tagsValue = Points.Value.newBuilder()
                            .setListValue(Points.ListValue.newBuilder()
                                    .addAllValues(tags.stream()
                                            .map(tag -> Points.Value.newBuilder()
                                                    .setStringValue(tag)
                                                    .build())
                                            .collect(Collectors.toList()))
                                    .build())
                            .build();
                    payload.put("tags", tagsValue);
                }

                if (module != null) {
                    payload.put("module", Points.Value.newBuilder()
                            .setStringValue(module)
                            .build());
                }

                if (type != null) {
                    payload.put("type", Points.Value.newBuilder()
                            .setStringValue(type)
                            .build());
                }

                payload.put("log_id", Points.Value.newBuilder()
                        .setStringValue(logId)
                        .build());

                Points.PointStruct point = Points.PointStruct.newBuilder()
                        .setId(Points.PointId.newBuilder().setUuid(logId).build())
                        .setVectors(Points.Vectors.newBuilder()
                                .setVector(Points.Vector.newBuilder()
                                        .addAllData(vector)
                                        .build())
                                .build())
                        .putAllPayload(payload)
                        .build();

                client.upsertAsync(collectionName, Collections.singletonList(point)).get();

                log.debug("Vector stored for log: id={}", logId);
            } catch (Exception e) {
                log.error("Failed to store vector for log: id={}", logId, e);
                throw new RuntimeException("Failed to store vector", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Flux<ScoredLogId> searchSimilar(List<Float> vector, int limit, List<String> tags,
                                            String module, String type) {
        return Mono.fromCallable(() -> {
            try {
                Points.SearchPoints.Builder searchBuilder = Points.SearchPoints.newBuilder()
                        .setCollectionName(collectionName)
                        .addAllVector(vector)
                        .setLimit(limit)
                        .setWithPayload(Points.WithPayloadSelector.newBuilder()
                                .setEnable(true)
                                .build());

                // Build filters
                List<Points.Condition> conditions = new ArrayList<>();

                if (tags != null && !tags.isEmpty()) {
                    for (String tag : tags) {
                        conditions.add(Points.Condition.newBuilder()
                                .setField(Points.FieldCondition.newBuilder()
                                        .setKey("tags")
                                        .setMatch(Points.Match.newBuilder()
                                                .setKeyword(tag)
                                                .build())
                                        .build())
                                .build());
                    }
                }

                if (module != null) {
                    conditions.add(Points.Condition.newBuilder()
                            .setField(Points.FieldCondition.newBuilder()
                                    .setKey("module")
                                    .setMatch(Points.Match.newBuilder()
                                            .setKeyword(module)
                                            .build())
                                    .build())
                            .build());
                }

                if (type != null) {
                    conditions.add(Points.Condition.newBuilder()
                            .setField(Points.FieldCondition.newBuilder()
                                    .setKey("type")
                                    .setMatch(Points.Match.newBuilder()
                                            .setKeyword(type)
                                            .build())
                                    .build())
                            .build());
                }

                if (!conditions.isEmpty()) {
                    searchBuilder.setFilter(Points.Filter.newBuilder()
                            .addAllMust(conditions)
                            .build());
                }

                Points.SearchResponse response = client.searchAsync(searchBuilder.build()).get();

                List<ScoredLogId> results = response.getResultList().stream()
                        .map(scoredPoint -> {
                            String logId = scoredPoint.getPayloadMap()
                                    .get("log_id")
                                    .getStringValue();
                            float score = scoredPoint.getScore();
                            return new ScoredLogId(logId, score);
                        })
                        .collect(Collectors.toList());

                log.debug("Vector search completed: found {} results", results.size());
                return results;
            } catch (Exception e) {
                log.error("Vector search failed", e);
                throw new RuntimeException("Vector search failed", e);
            }
        }).flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteVector(String logId) {
        return Mono.fromRunnable(() -> {
            try {
                Points.PointsSelector selector = Points.PointsSelector.newBuilder()
                        .setPoints(Points.PointsIdsList.newBuilder()
                                .addIds(Points.PointId.newBuilder().setUuid(logId).build())
                                .build())
                        .build();

                client.deleteAsync(collectionName, selector).get();

                log.debug("Vector deleted for log: id={}", logId);
            } catch (Exception e) {
                log.error("Failed to delete vector for log: id={}", logId, e);
                throw new RuntimeException("Failed to delete vector", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Long> count() {
        return Mono.fromCallable(() -> {
            try {
                Collections.CollectionInfo info = client.getCollectionInfoAsync(collectionName).get();
                long pointsCount = info.getPointsCount();
                log.debug("Total vectors in collection: {}", pointsCount);
                return pointsCount;
            } catch (Exception e) {
                log.error("Failed to count vectors", e);
                return 0L;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
