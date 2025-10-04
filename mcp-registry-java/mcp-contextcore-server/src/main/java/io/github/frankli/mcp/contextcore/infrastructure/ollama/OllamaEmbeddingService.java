package io.github.frankli.mcp.contextcore.infrastructure.ollama;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.frankli.mcp.contextcore.domain.repository.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Ollama implementation of EmbeddingService
 */
@Slf4j
@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private final OkHttpClient client;
    private final Gson gson;
    private final String ollamaUrl;
    private final String modelName;
    private final int dimension;

    public OllamaEmbeddingService(
            @Value("${contextcore.ollama.host:localhost}") String host,
            @Value("${contextcore.ollama.port:11434}") int port,
            @Value("${contextcore.ollama.model:nomic-embed-text}") String modelName,
            @Value("${contextcore.ollama.dimension:768}") int dimension) {

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        this.gson = new Gson();
        this.ollamaUrl = String.format("http://%s:%d", host, port);
        this.modelName = modelName;
        this.dimension = dimension;

        log.info("OllamaEmbeddingService initialized: url={}, model={}, dimension={}",
                ollamaUrl, modelName, dimension);
    }

    @Override
    public Mono<List<Float>> embed(String text) {
        return Mono.fromCallable(() -> {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", modelName);
            requestBody.addProperty("prompt", text);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(ollamaUrl + "/api/embeddings")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Ollama API request failed: " + response.code());
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (!jsonResponse.has("embedding")) {
                    throw new IOException("Ollama response missing 'embedding' field");
                }

                // Convert JsonArray to List<Float>
                List<Float> embedding = gson.fromJson(
                        jsonResponse.getAsJsonArray("embedding"),
                        new com.google.gson.reflect.TypeToken<List<Double>>(){}.getType())
                        .stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList());

                log.debug("Generated embedding for text (length={}): {} dimensions",
                        text.length(), embedding.size());

                return embedding;
            }
        }).subscribeOn(Schedulers.boundedElastic());
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
        return Mono.fromCallable(() -> {
            Request request = new Request.Builder()
                    .url(ollamaUrl + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                boolean healthy = response.isSuccessful();
                log.debug("Ollama health check: {}", healthy ? "OK" : "FAILED");
                return healthy;
            } catch (IOException e) {
                log.warn("Ollama health check failed", e);
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
