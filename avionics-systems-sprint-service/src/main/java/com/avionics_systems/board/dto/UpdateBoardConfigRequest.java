package com.avionics_systems.board.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoardConfigRequest {
    private List<BoardConfigResponse.QuickFilterConfig> quickFilters;
    private BoardConfigResponse.SwimlaneConfigResponse swimlane;
    private Boolean showWorkVsCapacity;
    private BoardConfigResponse.CardColorConfig cardColors;
}