package com.uymbot.docservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Semantic search query")
public class SearchRequest {

    @NotBlank(message = "Query must not be blank")
    @Schema(description = "Natural language search query", example = "programming language")
    private String query;

    @Min(1) @Max(50)
    @Schema(description = "Maximum number of results to return", defaultValue = "5", minimum = "1", maximum = "50")
    private int nResults = 5;
}
