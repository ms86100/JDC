package com.jira.board.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardConfigResponse {
    private java.util.UUID boardId;
    private List<QuickFilterConfig> quickFilters;
    private SwimlaneConfigResponse swimlane;
    private boolean showWorkVsCapacity;
    private CardColorConfig cardColors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickFilterConfig {
        private String id;
        private String name;
        private String jql;
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SwimlaneConfigResponse {
        private boolean enabled;
        private String field;
        private List<String> collapsedSwimlanes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardColorConfig {
        private boolean enabled;
        private String field;
    }
}