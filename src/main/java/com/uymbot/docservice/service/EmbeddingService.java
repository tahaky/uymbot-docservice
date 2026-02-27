package com.uymbot.docservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-backed text embedding service.
 * Calls the OpenAI /v1/embeddings endpoint to produce semantic float vectors.
 */
@Slf4j
@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.embedding-model}")
    private String model;

    @Value("${openai.embedding-url}")
    private String embeddingUrl;

    public EmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to embed must not be null or blank");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of("input", text, "model", model);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<?, ?> response;
        try {
            response = restTemplate.postForObject(embeddingUrl, entity, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI embedding API call failed: " + e.getMessage(), e);
        }

        if (response == null) {
            throw new IllegalStateException("OpenAI embedding API returned null response");
        }
        List<?> data = (List<?>) response.get("data");
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("OpenAI embedding API returned no data");
        }
        List<Double> embeddingValues = (List<Double>) ((Map<?, ?>) data.get(0)).get("embedding");
        if (embeddingValues == null || embeddingValues.isEmpty()) {
            throw new IllegalStateException("OpenAI embedding API returned empty embedding vector");
        }

        float[] result = new float[embeddingValues.size()];
        for (int i = 0; i < embeddingValues.size(); i++) {
            result[i] = embeddingValues.get(i).floatValue();
        }
        log.debug("Embedded text with model={}, dim={}", model, result.length);
        return result;
    }
}
