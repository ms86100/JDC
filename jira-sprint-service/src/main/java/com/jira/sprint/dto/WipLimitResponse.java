package com.jira.sprint.dto;

import lombok.*;
import java.util.UUID;

/**
 * DTO for WIP (Work In Progress) limit configuration and status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WipLimitResponse {

    private UUID boardId;
    private UUID columnId;
    private String columnName;
    private Integer wipLimit;
    private Integer currentCount;
    private Integer remainingCapacity;
    private Boolean isLimitExceeded;
    private Boolean isLimitEnabled;

    /**
     * Calculate remaining capacity (how many more issues can be added).
     */
    public Integer getRemainingCapacity() {
        if (wipLimit == null || wipLimit <= 0) {
            return Integer.MAX_VALUE; // No limit
        }
        return Math.max(0, wipLimit - currentCount);
    }

    /**
     * Check if the WIP limit is exceeded.
     */
    public Boolean getIsLimitExceeded() {
        if (wipLimit == null || wipLimit <= 0) {
            return false; // No limit, so not exceeded
        }
        return currentCount > wipLimit;
    }
}