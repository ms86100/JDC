package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFolderRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String name;

    private String description;

    private UUID parentId;

    private String folderType;

    private String icon;

    private String color;

    private String filterCriteria;

    private List<String> tags;
}