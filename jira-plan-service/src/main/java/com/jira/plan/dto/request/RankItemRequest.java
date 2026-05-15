package com.jira.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankItemRequest {
    private UUID itemId;
    private String beforeRank;  // Rank of item before target position
    private String afterRank;   // Rank of item after target position
    private UUID userId;
}