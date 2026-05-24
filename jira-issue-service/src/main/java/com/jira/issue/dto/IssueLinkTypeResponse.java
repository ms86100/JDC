package com.jira.issue.dto;

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
public class IssueLinkTypeResponse {
    private UUID id;
    private String name;
    private String inward;
    private String outward;
    private Boolean isActive;
    private LocalDateTime createdAt;
}