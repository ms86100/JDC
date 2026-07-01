package com.jira.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private String entityType;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}