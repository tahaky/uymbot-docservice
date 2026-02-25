package com.uymbot.docservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagChunkResponse {

    private String chunkId;
    private String documentId;
    private String stableId;
    private String text;
    private String chunkType;
    private String hash;
    private Map<String, Object> metadata;
    private Integer wordCount;
    private Integer charCount;
    private String createdAt;
    private String updatedAt;
}
