package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaselineSummaryResponse {

    private UUID fixVersionId;
    private UUID projectId;
    private int totalVvos;
    private int releasedCount;
    private int verifiedCount;
    private int cancelledCount;
    private int supersededCount;
}
