package com.jira.migration.dto.wizard;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WizardUploadResultDto {
    private UUID sessionId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private List<String> detectedHeaders;
    private String detectedEntityType;
    private String attachmentColumn;
    private String parentColumn;
    private String epicColumn;
    private List<String> detectedEntityTypes;
    private Integer totalRows;
    private List<List<String>> previewRows;
    private boolean success;
    private String errorMessage;
    private UUID uploadId;
    private String virusScanStatus;
}
