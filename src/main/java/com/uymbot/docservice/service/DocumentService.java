package com.uymbot.docservice.service;

import com.uymbot.docservice.dto.DocumentRequest;
import com.uymbot.docservice.dto.DocumentResponse;
import com.uymbot.docservice.dto.DocumentUpdateRequest;
import com.uymbot.docservice.dto.RagChunkResponse;
import com.uymbot.docservice.dto.RagDocumentMeta;
import com.uymbot.docservice.dto.RagImportRequest;
import com.uymbot.docservice.exception.DocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String TITLE_KEY = "_title";
    private static final String CHUNK_INDEX_KEY = "chunkIndex";
    private static final String TOTAL_CHUNKS_KEY = "totalChunks";

    private final ChromaDbService chromaDbService;
    private final EmbeddingService embeddingService;
    private final RagClient ragClient;
    private final TextChunkingService textChunkingService;

    // ------------------------------------------------------------------ CREATE
    /**
     * Splits the document content into optimal chunks for OpenAI embeddings,
     * embeds each chunk, and stores them all in ChromaDB.
     *
     * @return one {@link DocumentResponse} per chunk
     */
    public List<DocumentResponse> create(DocumentRequest req) {
        List<String> chunks = textChunkingService.split(req.getContent());
        int totalChunks = chunks.size();
        log.debug("Creating document '{}' as {} chunk(s)", req.getTitle(), totalChunks);

        List<DocumentResponse> responses = new ArrayList<>(totalChunks);
        for (int i = 0; i < totalChunks; i++) {
            String chunkText = chunks.get(i);
            String chunkId = UUID.randomUUID().toString();

            Map<String, Object> meta = buildMeta(req.getTitle(), req.getMetadata());
            meta.put(CHUNK_INDEX_KEY, i);
            meta.put(TOTAL_CHUNKS_KEY, totalChunks);

            float[] embedding = embeddingService.embed(chunkText);
            chromaDbService.add(chunkId, chunkText, meta, embedding);

            Map<String, Object> responseMetadata = new HashMap<>(
                    req.getMetadata() == null ? Map.of() : req.getMetadata());
            responseMetadata.put(CHUNK_INDEX_KEY, i);
            responseMetadata.put(TOTAL_CHUNKS_KEY, totalChunks);

            responses.add(DocumentResponse.builder()
                    .id(chunkId)
                    .title(req.getTitle())
                    .content(chunkText)
                    .metadata(responseMetadata)
                    .build());
        }
        return responses;
    }

    // -------------------------------------------------------------------- READ
    public DocumentResponse getById(String id) {
        return chromaDbService.getById(id)
                .map(result -> toResponse(id, result, 0))
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    public List<DocumentResponse> listAll(int limit, int offset) {
        Map<?, ?> result = chromaDbService.list(limit, offset);
        return toResponseList(result);
    }

    // ------------------------------------------------------------------ UPDATE
    public DocumentResponse update(String id, DocumentUpdateRequest req) {
        DocumentResponse existing = getById(id);   // throws 404 if not found

        String newTitle   = req.getTitle()   != null ? req.getTitle()   : existing.getTitle();
        String newContent = req.getContent() != null ? req.getContent() : existing.getContent();
        Map<String, Object> newMeta = req.getMetadata() != null ? req.getMetadata() : existing.getMetadata();

        float[] embedding = embeddingService.embed(newContent);
        Map<String, Object> meta = buildMeta(newTitle, newMeta);
        chromaDbService.update(id, newContent, meta, embedding);

        return DocumentResponse.builder()
                .id(id)
                .title(newTitle)
                .content(newContent)
                .metadata(newMeta)
                .build();
    }

    // ------------------------------------------------------------------ DELETE
    public void delete(String id) {
        getById(id);  // throws 404 if not found
        chromaDbService.delete(id);
    }

    // ----------------------------------------------------------------- IMPORT FROM RAG
    public List<DocumentResponse> importFromRag(String ragDocumentId, RagImportRequest req) {
        RagDocumentMeta ragDoc = ragClient.getDocument(ragDocumentId);
        List<RagChunkResponse> chunks = ragClient.getChunks(ragDocumentId);

        String separator = req.getJoinSeparator() != null ? req.getJoinSeparator() : "\n\n";
        String content = chunks.stream()
                .map(c -> c.getText() != null ? c.getText() : "")
                .collect(Collectors.joining(separator));

        String title = req.getTitle() != null ? req.getTitle()
                : (ragDoc != null && ragDoc.getFilename() != null ? ragDoc.getFilename() : "RAG Document");

        Map<String, Object> mergedMeta = new HashMap<>();
        if (req.getMetadata() != null) mergedMeta.putAll(req.getMetadata());
        mergedMeta.put("ragDocumentId", ragDocumentId);
        if (ragDoc != null && ragDoc.getFilename() != null) mergedMeta.put("ragFilename", ragDoc.getFilename());
        mergedMeta.put("importedFrom", "rag");

        return create(DocumentRequest.builder()
                .title(title)
                .content(content)
                .metadata(mergedMeta)
                .build());
    }

    // ------------------------------------------------------------------ SEARCH
    public List<DocumentResponse> search(String query, int nResults) {
        int count = chromaDbService.count();
        if (count == 0) return List.of();
        int n = Math.min(nResults, count);
        float[] embedding = embeddingService.embed(query);
        Map<?, ?> result = chromaDbService.query(embedding, n);
        return toQueryResponseList(result);
    }

    // ----------------------------------------------------------------- helpers
    private Map<String, Object> buildMeta(String title, Map<String, Object> extra) {
        Map<String, Object> meta = new HashMap<>();
        if (extra != null) meta.putAll(extra);
        meta.put(TITLE_KEY, title);
        return meta;
    }

    @SuppressWarnings("unchecked")
    private DocumentResponse toResponse(String id, Map<?, ?> result, int index) {
        List<?> ids       = (List<?>) result.get("ids");
        List<?> documents = (List<?>) result.get("documents");
        List<?> metadatas = (List<?>) result.get("metadatas");

        Map<String, Object> meta = new HashMap<>((Map<String, Object>) metadatas.get(index));
        String title = (String) meta.remove(TITLE_KEY);
        return DocumentResponse.builder()
                .id((String) ids.get(index))
                .title(title == null ? "" : title)
                .content((String) documents.get(index))
                .metadata(meta)
                .build();
    }

    private List<DocumentResponse> toResponseList(Map<?, ?> result) {
        List<?> ids = (List<?>) result.get("ids");
        if (ids == null || ids.isEmpty()) return List.of();
        List<DocumentResponse> docs = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            docs.add(toResponse((String) ids.get(i), result, i));
        }
        return docs;
    }

    @SuppressWarnings("unchecked")
    private List<DocumentResponse> toQueryResponseList(Map<?, ?> result) {
        // Query results are nested: ids[0], documents[0], metadatas[0]
        List<List<String>> ids       = (List<List<String>>) result.get("ids");
        List<List<String>> documents = (List<List<String>>) result.get("documents");
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) result.get("metadatas");

        if (ids == null || ids.isEmpty() || ids.get(0).isEmpty()) return List.of();

        List<String> idRow    = ids.get(0);
        List<String> docRow   = documents.get(0);
        List<Map<String, Object>> metaRow = metadatas.get(0);

        List<DocumentResponse> docs = new ArrayList<>();
        for (int i = 0; i < idRow.size(); i++) {
            Map<String, Object> meta = new HashMap<>(metaRow.get(i));
            String title = (String) meta.remove(TITLE_KEY);
            docs.add(DocumentResponse.builder()
                    .id(idRow.get(i))
                    .title(title == null ? "" : title)
                    .content(docRow.get(i))
                    .metadata(meta)
                    .build());
        }
        return docs;
    }
}
