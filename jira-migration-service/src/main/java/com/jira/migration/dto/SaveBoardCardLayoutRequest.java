package com.jira.migration.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveBoardCardLayoutRequest {
    private UUID projectId;
    private List<CardFieldSelection> fields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardFieldSelection {
        private String fieldKey;
        private Integer displayOrder;
        private String position;
        private Boolean visible;
    }
}
