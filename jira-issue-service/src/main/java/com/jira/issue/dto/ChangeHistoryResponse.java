package com.jira.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeHistoryResponse {
    private UUID id;
    private UUID issueId;
    private UUID authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private List<ChangeItemResponse> changes;
}