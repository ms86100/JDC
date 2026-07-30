package com.avionics_systems.sprint.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDataResponse {
    private AgileBoardResponse board;
    @Builder.Default
    private List<BoardColumnResponse> columns = new ArrayList<>();
    @Builder.Default
    private List<BoardSprintResponse> sprints = new ArrayList<>();
}