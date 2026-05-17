package com.jira.project.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLevelResponse {

    private UUID id;
    private UUID schemeId;
    private String name;
    private String description;
    private String levelType;
    private Integer sequence;
    private LocalDateTime createdAt;
}