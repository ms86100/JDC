package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Test trend data point
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestTrendResponse {

    private LocalDateTime date;
    private String status;
    private String testEnv;
    private Long durationMs;
}