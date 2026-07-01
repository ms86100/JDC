package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCatalogResponse {

    private List<TemplateCategoryCatalogDto> categories;
    private List<ProjectTemplateResponse> recommended;
    private List<ProjectTemplateResponse> recentlyUsed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateCategoryCatalogDto {
        private String categoryKey;
        private String name;
        private String description;
        private String icon;
        private String iconEmoji;
        private Integer sortOrder;
        private List<ProjectTemplateResponse> templates;
    }
}
