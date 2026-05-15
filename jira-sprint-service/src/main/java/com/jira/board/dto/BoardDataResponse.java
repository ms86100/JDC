package com.jira.board.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDataResponse {
    private AgileBoardResponse board;
    private List<BoardColumnResponse> columns;
    private List<BoardIssueResponse> issues;
    private SprintInfo activeSprint;
    private VelocityResponse velocity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintInfo {
        private UUID id;
        private String name;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int capacity;
        private int committed;
    }
}