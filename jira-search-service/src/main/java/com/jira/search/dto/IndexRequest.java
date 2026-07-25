package com.jira.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexRequest {

    @NotBlank(message = "{validation.entity-type.required}")
    private String entityType;

    @NotNull(message = "{validation.entity-id.required}")
    private UUID entityId;

    @NotBlank(message = "{validation.title.required}")
    private String title;

    private String content;
}