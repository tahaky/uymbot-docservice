package com.uymbot.docservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating a new document")
public class DocumentRequest {

    @NotBlank(message = "Title must not be blank")
    @Schema(description = "Title of the document", example = "Python Guide")
    private String title;

    @NotBlank(message = "Content must not be blank")
    @Schema(description = "Text content of the document", example = "Python is a high-level programming language.")
    private String content;

    @Schema(description = "Optional key-value metadata", example = "{\"source\": \"web\"}")
    private Map<String, Object> metadata;
}
