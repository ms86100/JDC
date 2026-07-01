package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlakyTrendResponse {

    private String date;
    private Integer flakyTestCount;
    private Integer stableTestCount;
    private BigDecimal averagePassRate;
    private BigDecimal averageFlakyScore;
}