package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardConfigResponse {
    private UUID id;
    private UUID planId;
    private String name;
    private String boardType;
    private String columnConfigMode;
    private String constraintSource;
    private Boolean isEnabled;
    private String cardLayoutMode;
    private String defaultSwimlane;
    private List<BoardColumnResponse> columns;
    private List<BoardQuickFilterResponse> quickFilters;
    private List<BoardSwimlaneResponse> swimlanes;
    private List<BoardCardColorResponse> cardColors;
    private List<BoardDetailFieldResponse> detailFields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}