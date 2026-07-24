package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTemplateResponse {

    private UUID id;
    private String name;
    private String description;
    private String templateType;
    private String outputFormat;
    private String sourceType;
    private List<Map<String, String>> columns;
    private String groupBy;
    private String sortBy;
    private String sortDirection;
    private String headerText;
    private String footerText;
    private String filterJql;
    private Boolean isSystem;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
