package com.uymbot.docservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Optional overrides for importing a document from the RAG service")
public class RagImportRequest {

    @Schema(description = "Override title; defaults to RAG document filename", example = "My Document")
    private String title;

    @Schema(description = "Extra metadata merged into the stored document metadata", example = "{\"source\": \"rag\"}")
    private Map<String, Object> metadata;

    @Schema(description = "Separator used when joining chunks; defaults to \"\\n\\n\"", example = "\\n\\n")
    private String joinSeparator;
}
