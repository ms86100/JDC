package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderTemplateRequest {

    private String name;

    private String description;

    private String folderType;

    private String icon;

    private String color;

    private List<String> tags;

    private List<SubFolderTemplate> subFolders;

    private List<String> defaultTestFields;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SubFolderTemplate {
    private String name;
    private String description;
    private String icon;
    private List<SubFolderTemplate> children;
}