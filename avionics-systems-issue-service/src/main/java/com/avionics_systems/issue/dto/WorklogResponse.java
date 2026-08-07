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
public class WorklogResponse {
    private UUID id;
    private UUID issueId;
    private UUID authorId;
    private String authorName;
    private Long timeSpentSeconds;
    private String workDescription;
    private LocalDateTime startedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String visibility;
    private UUID visibilityGroupId;

    public String getTimeWorkedFormatted() {
        long s = timeSpentSeconds != null ? timeSpentSeconds : 0;
        long weeks = s / (5 * 8 * 3600);
        s = s % (5 * 8 * 3600);
        long days = s / (8 * 3600);
        s = s % (8 * 3600);
        long hours = s / 3600;
        s = s % 3600;
        long minutes = s / 60;
        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");
        return sb.toString().trim();
    }
}