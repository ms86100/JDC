package com.avionics_systems.migration.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCardLayoutResponse {
    private UUID boardId;
    private UUID projectId;
    private List<EligibleFieldDto> eligibleFields;
    private List<BoardCardFieldDto> selectedFields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibleFieldDto {
        private String fieldKey;
        private String displayName;
        private String fieldType;
        private boolean custom;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoardCardFieldDto {
        private String fieldKey;
        private String displayName;
        private int displayOrder;
        private String position;
        private boolean visible;
    }
}
