package com.jira.board.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwimlaneDataResponse {
    private List<Swimlane> swimlanes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Swimlane {
        private String key;
        private String label;
        private List<BoardIssueResponse> issues;
    }
}