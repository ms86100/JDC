package com.avionics_systems.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueLinkResponse {
    private UUID id;
    private UUID sourceIssueId;
    private String sourceIssueKey;
    private UUID targetIssueId;
    private String targetIssueKey;
    private String linkType;
    private String linkTypeLabel;
    private Integer sequence;
    private LocalDateTime createdAt;
}