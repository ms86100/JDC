package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFolderRequest {

    private String name;

    private String description;

    private Integer sortOrder;

    private String icon;

    private String color;

    private List<String> tags;
}