package io.github.frankli.mcp.contextcore.domain.repository;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Embedding Service Interface
 *
 * Defines operations for text-to-vector conversion (Ollama)
 */
public interface EmbeddingService {

    /**
     * Generates an embedding vector for the given text
     *
     * @param text the text to embed
     * @return the embedding vector
     */
    Mono<List<Float>> embed(String text);

    /**
     * Gets the dimension of the embedding vectors
     *
     * @return the vector dimension (e.g., 768 for nomic-embed-text)
     */
    int getDimension();

    /**
     * Gets the name of the embedding model
     *
     * @return the model name (e.g., "nomic-embed-text")
     */
    String getModelName();

    /**
     * Checks if the embedding service is healthy
     *
     * @return true if the service is available
     */
    Mono<Boolean> isHealthy();
}
