package com.uymbot.docservice.controller;

import com.uymbot.docservice.dto.DocumentRequest;
import com.uymbot.docservice.dto.DocumentResponse;
import com.uymbot.docservice.dto.DocumentUpdateRequest;
import com.uymbot.docservice.dto.RagImportRequest;
import com.uymbot.docservice.dto.SearchRequest;
import com.uymbot.docservice.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Validated
@Tag(name = "Documents", description = "CRUD operations and semantic search for vector DB documents")
public class DocumentController {

    private final DocumentService documentService;

    // ------------------------------------------------------------------ CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new document",
               description = "Embeds the content and stores the document in ChromaDB.")
    @ApiResponse(responseCode = "201", description = "Document created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public DocumentResponse create(@Valid @RequestBody DocumentRequest req) {
        return documentService.create(req);
    }

    // -------------------------------------------------------------------- LIST
    @GetMapping
    @Operation(summary = "List all documents")
    @ApiResponse(responseCode = "200", description = "List of documents")
    public List<DocumentResponse> list(
            @Parameter(description = "Max results") @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int limit,
            @Parameter(description = "Skip offset") @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return documentService.listAll(limit, offset);
    }

    // -------------------------------------------------------------------- GET
    @GetMapping("/{id}")
    @Operation(summary = "Get a document by ID")
    @ApiResponse(responseCode = "200", description = "Document found")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public DocumentResponse getById(@PathVariable String id) {
        return documentService.getById(id);
    }

    // ------------------------------------------------------------------ UPDATE
    @PutMapping("/{id}")
    @Operation(summary = "Update a document (partial update supported)")
    @ApiResponse(responseCode = "200", description = "Document updated")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public DocumentResponse update(@PathVariable String id,
                                   @RequestBody DocumentUpdateRequest req) {
        return documentService.update(id, req);
    }

    // ------------------------------------------------------------------ DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document")
    @ApiResponse(responseCode = "204", description = "Document deleted")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public void delete(@PathVariable String id) {
        documentService.delete(id);
    }

    // ------------------------------------------------------------------ SEARCH
    @PostMapping("/search")
    @Operation(summary = "Semantic similarity search",
               description = "Embeds the query text and returns the most similar documents.")
    @ApiResponse(responseCode = "200", description = "Search results")
    public List<DocumentResponse> search(@Valid @RequestBody SearchRequest req) {
        return documentService.search(req.getQuery(), req.getNResults());
    }

    // ------------------------------------------------------------ IMPORT FROM RAG
    @PostMapping("/import/rag/{ragDocumentId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import a document from the RAG Chunking/Parser service",
               description = "Fetches document metadata and chunks from the RAG service, "
                       + "joins them into a single document, and stores it in ChromaDB.")
    @ApiResponse(responseCode = "201", description = "Document imported and stored")
    @ApiResponse(responseCode = "502", description = "RAG service unreachable or returned an error")
    public DocumentResponse importFromRag(
            @Parameter(description = "UUID of the document in the RAG service")
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "ragDocumentId must be a valid UUID") String ragDocumentId,
            @RequestBody(required = false) RagImportRequest req) {
        return documentService.importFromRag(ragDocumentId, req != null ? req : new RagImportRequest());
    }

    // ------------------------------------------------------------------ HEALTH
    @GetMapping("/health")
    @Operation(summary = "Health check")
    @ApiResponse(responseCode = "200", description = "Service is up")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
