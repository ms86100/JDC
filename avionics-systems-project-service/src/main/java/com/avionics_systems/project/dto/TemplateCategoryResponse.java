package com.avionics_systems.project.dto;

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
    private String categoryKey;
    private String categoryName;
    private String categoryDescription;
    private String categoryIcon;
    private String categoryIconEmoji;
    private Integer sortOrder;
    private List<ProjectTemplateResponse> templates;
}