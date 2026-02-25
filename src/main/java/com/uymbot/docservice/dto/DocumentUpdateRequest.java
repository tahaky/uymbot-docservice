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
@Schema(description = "Request body for updating an existing document (all fields optional)")
public class DocumentUpdateRequest {

    @Schema(description = "Updated title", example = "Updated Python Guide")
    private String title;

    @Schema(description = "Updated text content", example = "Python 3 is even better.")
    private String content;

    @Schema(description = "Updated metadata")
    private Map<String, Object> metadata;
}
