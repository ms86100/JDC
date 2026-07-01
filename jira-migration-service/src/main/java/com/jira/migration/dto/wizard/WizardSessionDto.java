package com.jira.migration.dto.wizard;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WizardSessionDto {
    private UUID sessionId;
    private String step;
    private String importType;
    private String status;
    private UUID targetProjectId;
    private UUID migrationJobId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String fileName;
    private Long fileSize;
    private List<String> detectedHeaders;
    private String detectedEntityType;
    private String attachmentColumn;
    private String parentColumn;
    private String epicColumn;
    private Integer totalRows;
    private Map<String, Object> validationResult;
    private List<Map<String, Object>> fieldMappings;
    private List<Map<String, Object>> userMappings;
    private List<Map<String, Object>> optionMappings;
    private Map<String, Object> workflowStatusMappings;
    private Map<String, Object> fieldDefaults;
    private Map<String, Object> importOptions;
    private Map<String, Object> sessionData;
    private List<List<String>> previewRows;
}
