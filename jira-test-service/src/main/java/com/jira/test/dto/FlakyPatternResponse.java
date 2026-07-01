package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlakyPatternResponse {

    private UUID id;
    private UUID testId;
    private String patternType;
    private String patternDescription;
    private BigDecimal frequencyScore;
    private List<String> affectedEnvironments;
    private List<String> affectedBuilds;
    private String rootCauseCategory;
    private String suggestedFix;
    private BigDecimal confidenceScore;
    private LocalDateTime createdAt;
}