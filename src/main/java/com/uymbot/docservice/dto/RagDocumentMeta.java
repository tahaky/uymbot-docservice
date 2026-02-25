package com.uymbot.docservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagDocumentMeta {

    private String id;
    private String filename;
    private String format;
    private String status;
    private Integer chunkCount;
    private String createdAt;
    private String updatedAt;
}
