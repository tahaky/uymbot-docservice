package com.uymbot.docservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uymbot.docservice.dto.DocumentRequest;
import com.uymbot.docservice.dto.DocumentResponse;
import com.uymbot.docservice.dto.DocumentUpdateRequest;
import com.uymbot.docservice.dto.RagImportRequest;
import com.uymbot.docservice.dto.SearchRequest;
import com.uymbot.docservice.exception.DocumentNotFoundException;
import com.uymbot.docservice.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
class DocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DocumentService documentService;

    private static final String ID = UUID.randomUUID().toString();

    private DocumentResponse sampleResponse() {
        return DocumentResponse.builder()
                .id(ID)
                .title("Test Title")
                .content("Test content")
                .metadata(Map.of("source", "unit-test"))
                .build();
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    @Test
    void createDocument_returns201() throws Exception {
        DocumentRequest req = new DocumentRequest("Test Title", "Test content", Map.of("source", "unit-test"));
        given(documentService.create(any())).willReturn(List.of(sampleResponse()));

        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(ID))
                .andExpect(jsonPath("$[0].title").value("Test Title"));
    }

    @Test
    void createDocument_missingTitle_returns400() throws Exception {
        DocumentRequest req = new DocumentRequest("", "content", null);

        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── LIST ────────────────────────────────────────────────────────────────

    @Test
    void listDocuments_returns200() throws Exception {
        given(documentService.listAll(anyInt(), anyInt())).willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID));
    }

    // ─── GET BY ID ───────────────────────────────────────────────────────────

    @Test
    void getDocument_found_returns200() throws Exception {
        given(documentService.getById(ID)).willReturn(sampleResponse());

        mockMvc.perform(get("/documents/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test content"));
    }

    @Test
    void getDocument_notFound_returns404() throws Exception {
        given(documentService.getById(anyString())).willThrow(new DocumentNotFoundException("bad-id"));

        mockMvc.perform(get("/documents/{id}", "bad-id"))
                .andExpect(status().isNotFound());
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    @Test
    void updateDocument_returns200() throws Exception {
        DocumentUpdateRequest req = new DocumentUpdateRequest("New Title", null, null);
        DocumentResponse updated = DocumentResponse.builder()
                .id(ID).title("New Title").content("Test content").metadata(Map.of()).build();
        given(documentService.update(eq(ID), any())).willReturn(updated);

        mockMvc.perform(put("/documents/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void updateDocument_notFound_returns404() throws Exception {
        given(documentService.update(anyString(), any())).willThrow(new DocumentNotFoundException("bad-id"));

        mockMvc.perform(put("/documents/{id}", "bad-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @Test
    void deleteDocument_returns204() throws Exception {
        doNothing().when(documentService).delete(ID);

        mockMvc.perform(delete("/documents/{id}", ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDocument_notFound_returns404() throws Exception {
        doThrow(new DocumentNotFoundException("bad-id")).when(documentService).delete(anyString());

        mockMvc.perform(delete("/documents/{id}", "bad-id"))
                .andExpect(status().isNotFound());
    }

    // ─── SEARCH ──────────────────────────────────────────────────────────────

    @Test
    void searchDocuments_returns200() throws Exception {
        given(documentService.search(anyString(), anyInt())).willReturn(List.of(sampleResponse()));

        SearchRequest req = new SearchRequest("test query", 3);
        mockMvc.perform(post("/documents/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID));
    }

    // ─── HEALTH ──────────────────────────────────────────────────────────────

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/documents/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ─── IMPORT FROM RAG ─────────────────────────────────────────────────────

    @Test
    void importFromRag_withBody_returns201() throws Exception {
        String ragId = UUID.randomUUID().toString();
        RagImportRequest req = RagImportRequest.builder()
                .title("Imported Title")
                .metadata(Map.of("source", "rag"))
                .joinSeparator("\n")
                .build();
        DocumentResponse imported = DocumentResponse.builder()
                .id(ID).title("Imported Title").content("chunk1\nchunk2")
                .metadata(Map.of("ragDocumentId", ragId, "importedFrom", "rag"))
                .build();
        given(documentService.importFromRag(eq(ragId), any())).willReturn(List.of(imported));

        mockMvc.perform(post("/documents/import/rag/{ragDocumentId}", ragId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(ID))
                .andExpect(jsonPath("$[0].title").value("Imported Title"));
    }

    @Test
    void importFromRag_noBody_returns201() throws Exception {
        String ragId = UUID.randomUUID().toString();
        DocumentResponse imported = DocumentResponse.builder()
                .id(ID).title("file.pdf").content("some content")
                .metadata(Map.of("ragDocumentId", ragId, "importedFrom", "rag"))
                .build();
        given(documentService.importFromRag(eq(ragId), any())).willReturn(List.of(imported));

        mockMvc.perform(post("/documents/import/rag/{ragDocumentId}", ragId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(ID));
    }

    @Test
    void importFromRag_ragServiceError_returns500() throws Exception {
        String ragId = UUID.randomUUID().toString();
        given(documentService.importFromRag(eq(ragId), any()))
                .willThrow(new RuntimeException("RAG service unavailable"));

        mockMvc.perform(post("/documents/import/rag/{ragDocumentId}", ragId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void importFromRag_invalidUuid_returns400() throws Exception {
        mockMvc.perform(post("/documents/import/rag/{ragDocumentId}", "not-a-valid-uuid"))
                .andExpect(status().isBadRequest());
    }
}
