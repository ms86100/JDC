package com.jira.project.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoleExportDto {

    private String name;
    private String description;
    private List<String> permissions;
}