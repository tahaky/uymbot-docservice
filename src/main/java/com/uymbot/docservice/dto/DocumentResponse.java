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
@Schema(description = "Document stored in the vector database")
public class DocumentResponse {

    @Schema(description = "Unique document ID (UUID)")
    private String id;

    @Schema(description = "Title of the document")
    private String title;

    @Schema(description = "Text content of the document")
    private String content;

    @Schema(description = "Key-value metadata")
    private Map<String, Object> metadata;
}
