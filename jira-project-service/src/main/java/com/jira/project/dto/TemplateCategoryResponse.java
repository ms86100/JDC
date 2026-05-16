package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for listing templates by category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCategoryResponse {
    private String categoryName;
    private String categoryIcon;
    private List<ProjectTemplateResponse> templates;
}