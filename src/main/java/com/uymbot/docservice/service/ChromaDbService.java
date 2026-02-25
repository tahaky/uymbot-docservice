package com.uymbot.docservice.service;

import com.uymbot.docservice.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Thin wrapper around the ChromaDB HTTP API (v1).
 * Handles collection initialisation and all CRUD / query operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChromaDbService {

    private final RestTemplate restTemplate;
    private final EmbeddingService embeddingService;

    @Qualifier("chromaHost")
    @org.springframework.beans.factory.annotation.Autowired
    private String chromaHost;

    @Value("${chromadb.collection-name}")
    private String collectionName;

    private volatile String collectionId;

    // ------------------------------------------------------------------ init
    private String getCollectionId() {
        if (collectionId == null) {
            synchronized (this) {
                if (collectionId == null) {
                    collectionId = initCollection();
                }
            }
        }
        return collectionId;
    }

    private String initCollection() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", collectionName);
        body.put("get_or_create", true);
        body.put("metadata", Map.of("hnsw:space", "cosine"));

        Map<?, ?> response = restTemplate.postForObject(
                chromaHost + "/api/v1/collections", body, Map.class);
        String id = (String) Objects.requireNonNull(response).get("id");
        log.info("ChromaDB collection '{}' ready, id={}", collectionName, id);
        return id;
    }

    // ------------------------------------------------------------------ add
    public void add(String id, String content, Map<String, Object> metadata, float[] embedding) {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", List.of(id));
        body.put("documents", List.of(content));
        body.put("metadatas", List.of(metadata));
        body.put("embeddings", List.of(toList(embedding)));

        restTemplate.postForObject(
                chromaHost + "/api/v1/collections/" + getCollectionId() + "/add",
                body, Void.class);
    }

    // ------------------------------------------------------------------ get by id
    public Optional<Map<?, ?>> getById(String id) {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", List.of(id));
        body.put("include", List.of("documents", "metadatas"));

        Map<?, ?> result = restTemplate.postForObject(
                chromaHost + "/api/v1/collections/" + getCollectionId() + "/get",
                body, Map.class);

        List<?> ids = (List<?>) Objects.requireNonNull(result).get("ids");
        if (ids == null || ids.isEmpty()) return Optional.empty();
        return Optional.of(result);
    }

    // ------------------------------------------------------------------ list
    public Map<?, ?> list(int limit, int offset) {
        Map<String, Object> body = new HashMap<>();
        body.put("include", List.of("documents", "metadatas"));
        body.put("limit", limit);
        body.put("offset", offset);

        return restTemplate.postForObject(
                chromaHost + "/api/v1/collections/" + getCollectionId() + "/get",
                body, Map.class);
    }

    // ------------------------------------------------------------------ update
    public void update(String id, String content, Map<String, Object> metadata, float[] embedding) {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", List.of(id));
        body.put("documents", List.of(content));
        body.put("metadatas", List.of(metadata));
        body.put("embeddings", List.of(toList(embedding)));

        restTemplate.postForObject(
                chromaHost + "/api/v1/collections/" + getCollectionId() + "/update",
                body, Void.class);
    }

    // ------------------------------------------------------------------ delete
    public void delete(String id) {
        Map<String, Object> body = Map.of("ids", List.of(id));
        restTemplate.postForObject(
                chromaHost + "/api/v1/collections/" + getCollectionId() + "/delete",
                body, Void.class);
    }

    // ------------------------------------------------------------------ count
    public int count() {
        Integer c = restTemplate.getForObject(
                chromaHost + "/api/v1/collections/" + getCollectionId() + "/count",
                Integer.class);
        return c == null ? 0 : c;
    }

    // ------------------------------------------------------------------ query
    public Map<?, ?> query(float[] queryEmbedding, int nResults) {
        Map<String, Object> body = new HashMap<>();
        body.put("query_embeddings", List.of(toList(queryEmbedding)));
        body.put("n_results", nResults);
        body.put("include", List.of("documents", "metadatas"));

        return restTemplate.postForObject(
                chromaHost + "/api/v1/collections/" + getCollectionId() + "/query",
                body, Map.class);
    }

    // ------------------------------------------------------------------ helper
    private static List<Float> toList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}
