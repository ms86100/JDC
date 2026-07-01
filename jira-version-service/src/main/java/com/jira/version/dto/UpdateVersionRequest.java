package com.jira.version.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVersionRequest {
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime releaseDate;
    private String semanticVersion;
    private String buildNumber;
    private String branchName;
    private String releaseTrain;
    private String color;
    private Integer sequence;
}