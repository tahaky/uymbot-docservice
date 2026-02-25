package com.uymbot.docservice.service;

import com.uymbot.docservice.dto.RagChunkResponse;
import com.uymbot.docservice.dto.RagDocumentMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * HTTP client for the RAG Chunking/Parser service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagClient {

    private final RestTemplate restTemplate;

    @Value("${rag.service.base-url}")
    private String ragServiceBaseUrl;

    public RagDocumentMeta getDocument(String ragDocumentId) {
        String url = ragServiceBaseUrl + "/api/documents/" + ragDocumentId;
        log.debug("Fetching RAG document metadata from {}", url);
        return restTemplate.getForObject(url, RagDocumentMeta.class);
    }

    public List<RagChunkResponse> getChunks(String ragDocumentId) {
        String url = ragServiceBaseUrl + "/api/documents/" + ragDocumentId + "/chunks";
        log.debug("Fetching RAG document chunks from {}", url);
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<RagChunkResponse>>() {}
        ).getBody();
    }
}
