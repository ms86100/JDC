package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExportTemplateRequest {

    @NotBlank
    private String name;

    private String description;

    @Builder.Default
    private String templateType = "CSV";

    @Builder.Default
    private String outputFormat = "CSV";

    @NotNull
    private String sourceType;

    private List<Map<String, String>> columns;

    private String groupBy;

    private String sortBy;

    @Builder.Default
    private String sortDirection = "ASC";

    private String headerText;

    private String footerText;

    private String filterJql;
}
