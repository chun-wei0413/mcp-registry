package com.mcp.contextcore.infrastructure.ollama;

import com.mcp.contextcore.domain.repository.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Ollama Embedding Service Implementation
 *
 * Provides text-to-vector conversion using Ollama's embedding API.
 * Uses the nomic-embed-text model for generating 768-dimensional embeddings.
 */
@Slf4j
@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private final WebClient webClient;
    private final String modelName;
    private final int dimension;

    public OllamaEmbeddingService(
            @Value("${ollama.api.url:http://localhost:11434}") String ollamaUrl,
            @Value("${ollama.embedding.model:nomic-embed-text}") String modelName,
            @Value("${ollama.embedding.dimension:768}") int dimension
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(ollamaUrl)
                .build();
        this.modelName = modelName;
        this.dimension = dimension;
        log.info("Initialized OllamaEmbeddingService with model: {}, dimension: {}", modelName, dimension);
    }

    @Override
    public Mono<List<Float>> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Text cannot be empty"));
        }

        log.debug("Generating embedding for text (length: {})", text.length());

        return webClient.post()
                .uri("/api/embeddings")
                .bodyValue(Map.of(
                        "model", modelName,
                        "prompt", text
                ))
                .retrieve()
                .bodyToMono(OllamaEmbeddingResponse.class)
                .map(response -> {
                    if (response.embedding() == null || response.embedding().isEmpty()) {
                        throw new RuntimeException("Ollama returned empty embedding");
                    }
                    log.debug("Successfully generated embedding with {} dimensions", response.embedding().size());
                    return response.embedding();
                })
                .doOnError(error -> log.error("Failed to generate embedding: {}", error.getMessage()))
                .onErrorResume(error -> Mono.error(
                        new RuntimeException("Failed to generate embedding: " + error.getMessage(), error)
                ));
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public Mono<Boolean> isHealthy() {
        log.debug("Checking Ollama service health");

        return webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.debug("Ollama service is healthy");
                    return true;
                })
                .onErrorResume(error -> {
                    log.error("Ollama service health check failed: {}", error.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Response object for Ollama embedding API
     */
    private record OllamaEmbeddingResponse(List<Float> embedding) {}
}
